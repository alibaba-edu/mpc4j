package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.AbstractGf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVolePtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * KOS16-GF2K-core VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
public class Kos16Gf2kCoreVoleReceiver extends AbstractGf2kCoreVoleReceiver {
    /**
     * base OT receiver
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * GF2K gadget
     */
    private Gf2kGadget gf2kGadget;
    /**
     * base OT receiver output
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * Δ in binary format
     */
    private boolean[] deltaBinary;

    public Kos16Gf2kCoreVoleReceiver(Rpc receiverRpc, Party senderParty, Kos16Gf2kCoreVoleConfig config) {
        super(Kos16Gf2kCoreVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        addSubPto(baseOtReceiver);
    }

    @Override
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kGadget = new Gf2kGadget(envType);
        baseOtReceiver.init();
        deltaBinary = gf2kGadget.decomposition(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

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
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;

    }

    private Gf2kVoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == num * l);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // create matrix Q
        byte[][][] qMatrix = new byte[num][l][];
        IntStream matrixStream = IntStream.range(0, num * l);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // current position for matrix Q
            int rowIndex = index / l;
            int columnIndex = index % l;
            // read u from the payload
            byte[] u = matrixPayloadArray[index];
            // compute t_b = PRF(kb, i), q(i,j) = u + Δ_j * t_b,
            byte[] tbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(extraInfo).putInt(rowIndex).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            byte[] tb = gf2k.createRandom(tbSeed);
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? gf2k.add(u, tb) : tb;
        });
        // composite each row in q using gadget
        Stream<byte[][]> qMatrixStream = Arrays.stream(qMatrix);
        qMatrixStream = parallel ? qMatrixStream.parallel() : qMatrixStream;
        byte[][] q = qMatrixStream
            .map(row -> gf2kGadget.innerProduct(row))
            .toArray(byte[][]::new);
        return Gf2kVoleReceiverOutput.create(delta, q);
    }
}
