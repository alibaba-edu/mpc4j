package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.AbstractGf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVolePtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * WYKW21-GF2K-core VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public class Wykw21Gf2kCoreVoleReceiver extends AbstractGf2kCoreVoleReceiver {
    /**
     * base OT receiver
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * base OT receiver output
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * Δ in binary format
     */
    private boolean[] deltaBinary;
    /**
     * the random oracle key
     */
    private byte[] randomOracleKey;
    /**
     * b used for challenge
     */
    private byte[][] bs;

    public Wykw21Gf2kCoreVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21Gf2kCoreVoleConfig config) {
        super(Wykw21Gf2kCoreVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtReceiver.init();
        deltaBinary = BinaryUtils.byteArrayToBinary(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        List<byte[]> randomOracleKeyPayload = new LinkedList<>();
        randomOracleKey = BlockUtils.randomBlock(secureRandom);
        randomOracleKeyPayload.add(randomOracleKey);
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomOracleKeyHeader, randomOracleKeyPayload));
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, randomOracleTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        Gf2kVoleReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        stopWatch.start();
        // R samples χ_0, ..., χ_{n-1} ← {0,1}^κ, and sends them to S, use random oracle to sample without communication
        byte[][] chis = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] seed = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + randomOracleKey.length)
                    .putLong(extraInfo).putInt(index).put(randomOracleKey).array();
                return field.createRandom(seed);
            })
            .toArray(byte[][]::new);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RESPONSE_CHI.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 2);
        // get x and z
        byte[] x = responsePayload.remove(0);
        byte[] z = responsePayload.remove(0);
        // y = Σ_{i = 0}^{n - 1} (χ_i · v_i) + b
        byte[] y = field.createZero();
        for (int i = 0; i < num; i++) {
            field.addi(y, field.mul(chis[i], receiverOutput.getQ(i)));
        }
        byte[] b = field.innerProduct(bs);
        field.addi(y, b);
        // y + Δ · x
        field.addi(y, field.mul(delta, x));
        MpcAbortPreconditions.checkArgument(Arrays.equals(y, z));
        bs = null;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private Gf2kVoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == (num + r) * fieldL);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // create matrix Q, add one more Q for generating b
        byte[][][] qMatrix = new byte[num + r][fieldL][];
        IntStream matrixStream = IntStream.range(0, (num + r) * fieldL);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // current position for matrix Q
            int rowIndex = index / fieldL;
            int columnIndex = index % fieldL;
            // read u from the payload
            byte[] u = matrixPayloadArray[index];
            // compute t_b = PRF(kb, i), q(i,j) = u + Δ_j * t_b,
            byte[] tbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(extraInfo).putInt(rowIndex).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            byte[] tb = subfield.createRandom(tbSeed);
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? subfield.add(u, tb) : tb;
        });
        // composite each row in q using gadget
        IntStream qMatrixIndexStream = IntStream.range(0, num);
        qMatrixIndexStream = parallel ? qMatrixIndexStream.parallel() : qMatrixIndexStream;
        byte[][] q = qMatrixIndexStream
            .mapToObj(rowIndex -> field.mixInnerProduct(qMatrix[rowIndex]))
            .toArray(byte[][]::new);
        // composite the last row in q using gadget
        bs = IntStream.range(0, r)
            .mapToObj(h -> field.mixInnerProduct(qMatrix[num + h]))
            .toArray(byte[][]::new);
        return Gf2kVoleReceiverOutput.create(field, delta, q);
    }
}
