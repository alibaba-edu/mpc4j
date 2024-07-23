package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
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
    private byte[][] as;
    /**
     * c used for challenge
     */
    private byte[][] cs;
    /**
     * t0
     */
    private byte[][][] t0;
    /**
     * tc used for c
     */
    private byte[][][] tcs;

    public Wykw21Gf2kCoreVoleSender(Rpc senderRpc, Party receiverParty, Wykw21Gf2kCoreVoleConfig config) {
        super(Wykw21Gf2kCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(fieldL);
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
        tcs = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        stopWatch.start();
        byte[][] chis = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] seed = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + randomOracleKey.length)
                    .putLong(extraInfo).putInt(index).put(randomOracleKey).array();
                return field.createRandom(seed);
            })
            .toArray(byte[][]::new);
        List<byte[]> responsePayload = new LinkedList<>();
        // S computes x = Σ_{i = 0}^{n - 1} (χ_i · u_i) + a, z = Σ_{i = 0}^{n - 1} (χ_i · w_i) + c
        byte[] x = field.createZero();
        for (int i = 0; i < num; i++) {
            field.addi(x, field.mixMul(senderOutput.getX(i), chis[i]));
        }
        byte[] a = field.composite(as);
        field.addi(x, a);
        responsePayload.add(x);
        byte[] z = field.createZero();
        for (int i = 0; i < num; i++) {
            field.addi(z, field.mul(chis[i], senderOutput.getT(i)));
        }
        byte[] c = field.innerProduct(cs);
        field.addi(z, c);
        responsePayload.add(z);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RESPONSE_CHI.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        as = null;
        cs = null;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // S samples a_h ← F_p for h ∈ [0, r)
        as = IntStream.range(0, r)
            .mapToObj(h -> subfield.createRandom(secureRandom))
            .toArray(byte[][]::new);
        // creates t0 and t1 array, each row in t0/t1 corresponds to an X.
        t0 = new byte[num][fieldL][];
        // create tc
        tcs = new byte[r][fieldL][];
        IntStream payLoadStream = IntStream.range(0, (num + r) * fieldL);
        payLoadStream = parallel ? payLoadStream.parallel() : payLoadStream;
        return payLoadStream
            .mapToObj(index -> {
                // current position in t0 and t1
                int rowIndex = index / fieldL;
                int columnIndex = index % fieldL;
                // Let k0 and k1 be the j-th key pair in bast OT, compute t0[i][j] = PRF(k0，i), t1[i][j] = PRF(k1, i)
                byte[] t0Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                byte[] t1Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                byte[] t1 = subfield.createRandom(t1Seed);
                if (index < num * fieldL) {
                    // regular extension
                    t0[rowIndex][columnIndex] = subfield.createRandom(t0Seed);
                    // Compute u = t0[i,j] - t1[i,j] - x[i]
                    return subfield.sub(subfield.sub(t0[rowIndex][columnIndex], t1), xs[rowIndex]);
                } else {
                    // verification extension
                    assert rowIndex >= num && rowIndex < num + r;
                    int h = rowIndex - num;
                    tcs[h][columnIndex] = subfield.createRandom(t0Seed);
                    // Compute uc = tc[j] - tc[j] - a
                    return subfield.sub(subfield.sub(tcs[h][columnIndex], t1), as[h]);
                }
            })
            .collect(Collectors.toList());
    }

    private Gf2kVoleSenderOutput generateSenderOutput() {
        // compute ts
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] ts = outputStream
            .mapToObj(index -> field.mixInnerProduct(t0[index]))
            .toArray(byte[][]::new);
        // compute c
        cs = IntStream.range(0, r)
            .mapToObj(h -> field.mixInnerProduct(tcs[h]))
            .toArray(byte[][]::new);

        return Gf2kVoleSenderOutput.create(field, xs, ts);
    }
}
