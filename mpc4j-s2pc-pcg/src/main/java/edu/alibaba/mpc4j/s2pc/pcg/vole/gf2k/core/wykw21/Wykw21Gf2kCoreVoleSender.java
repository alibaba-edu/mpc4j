package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.AbstractGf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21.Wykw21Gf2kCoreVolePtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * WYKW21-GF2K-core VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21Gf2kCoreVoleSender extends AbstractGf2kCoreVoleSender {
    /**
     * base OT sender
     */
    private final BaseOtSender baseOtSender;
    /**
     * GF2K gadget
     */
    private Gf2kGadget gf2kGadget;
    /**
     * base OT sender output
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * the random oracle key
     */
    private byte[] randomOracleKey;
    /**
     * a used for challenge
     */
    private byte[] a;
    /**
     * c used for challenge
     */
    private byte[] c;
    /**
     * t0
     */
    private byte[][][] t0;
    /**
     * tc used for c
     */
    private byte[][] tc;

    public Wykw21Gf2kCoreVoleSender(Rpc senderRpc, Party receiverParty, Wykw21Gf2kCoreVoleConfig config) {
        super(Wykw21Gf2kCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kGadget = new Gf2kGadget(envType);
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        randomOracleKey = randomOracleKeyPayload.remove(0);
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, randomOracleTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVoleSenderOutput send(byte[][] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayLoad = generateMatrixPayLoad();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayLoad));
        Gf2kVoleSenderOutput senderOutput = generateSenderOutput();
        t0 = null;
        tc = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        stopWatch.start();
        byte[][] chis = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] seed = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + randomOracleKey.length)
                    .putLong(extraInfo).putInt(index).put(randomOracleKey).array();
                return gf2k.createRandom(seed);
            })
            .toArray(byte[][]::new);
        List<byte[]> responsePayload = new LinkedList<>();
        // S computes x = Σ_{i = 0}^{n - 1} (χ_i · u_i) + a, z = Σ_{i = 0}^{n - 1} (χ_i · w_i) + c
        byte[] x = gf2k.createZero();
        for (int i = 0; i < num; i++) {
            gf2k.addi(x, gf2k.mul(chis[i], senderOutput.getX(i)));
        }
        gf2k.addi(x, a);
        responsePayload.add(x);
        byte[] z = gf2k.createZero();
        for (int i = 0; i < num; i++) {
            gf2k.addi(z, gf2k.mul(chis[i], senderOutput.getT(i)));
        }
        gf2k.addi(z, c);
        responsePayload.add(z);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RESPONSE_CHI.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        a = null;
        c = null;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // S samples a ← {0,1}^κ
        a = gf2k.createRandom(secureRandom);
        // creates t0 and t1 array, each row in t0/t1 corresponds to an X.
        t0 = new byte[num][l][];
        // create tc
        tc = new byte[l][];
        IntStream payLoadStream = IntStream.range(0, (num + 1) * l);
        payLoadStream = parallel ? payLoadStream.parallel() : payLoadStream;
        return payLoadStream
            .mapToObj(index -> {
                // current position in t0 and t1
                int rowIndex = index / l;
                int columnIndex = index % l;
                // Let k0 and k1 be the j-th key pair in bast OT, compute t0[i][j] = PRF(k0，i), t1[i][j] = PRF(k1, i)
                byte[] t0Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                byte[] t1Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                byte[] t1 = gf2k.createRandom(t1Seed);
                if (index < num * l) {
                    // regular extension
                    t0[rowIndex][columnIndex] = gf2k.createRandom(t0Seed);
                    // Compute u = t0[i,j] - t1[i,j] - x[i]
                    return gf2k.sub(gf2k.sub(t0[rowIndex][columnIndex], t1), xs[rowIndex]);
                } else {
                    // verification extension
                    assert rowIndex == num;
                    tc[columnIndex] = gf2k.createRandom(t0Seed);
                    // Compute uc = tc[j] - tc[j] - a
                    return gf2k.sub(gf2k.sub(tc[columnIndex], t1), a);
                }
            })
            .collect(Collectors.toList());
    }

    private Gf2kVoleSenderOutput generateSenderOutput() {
        // compute ts
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] ts = outputStream
            .mapToObj(index -> gf2kGadget.innerProduct(t0[index]))
            .toArray(byte[][]::new);
        // compute c
        c = gf2kGadget.innerProduct(tc);
        return Gf2kVoleSenderOutput.create(xs, ts);
    }
}
