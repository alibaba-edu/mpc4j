package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.AbstractGf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16.Kos16Gf2kCoreVolePtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KOS16 GF2K-core-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Kos16Gf2kCoreVoleSender extends AbstractGf2kCoreVoleSender {
    /**
     * base OT sender
     */
    private final BaseOtSender baseOtSender;
    /**
     * base OT sender output
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t0
     */
    private byte[][][] w0;

    public Kos16Gf2kCoreVoleSender(Rpc senderRpc, Party receiverParty, Kos16Gf2kCoreVoleConfig config) {
        super(Kos16Gf2kCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

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
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        stopWatch.start();
        Gf2kVoleSenderOutput senderOutput = generateSenderOutput();
        w0 = null;
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // creates w0 and w1 array, each row in w0 / w1 corresponds to an X.
        w0 = new byte[num][fieldL][];
        IntStream payLoadStream = IntStream.range(0, num * fieldL);
        payLoadStream = parallel ? payLoadStream.parallel() : payLoadStream;
        return payLoadStream
            .mapToObj(index -> {
                // current position in w^i_0 and w^i_1
                int rowIndex = index / fieldL;
                int columnIndex = index % fieldL;
                // PA sets w^i_0 := PRF(K^i_0, j) and w^i_1 := PRF(K^i_1, j) with w^i_0, w^i_1 ∈ F_p
                byte[] w0Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(index).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                byte[] w1Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(index).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                w0[rowIndex][columnIndex] = subfield.createRandom(w0Seed);
                byte[] w1 = subfield.createRandom(w1Seed);
                // PA sends τ^i := w^i_0 − w^i_1 − u ∈ F_p to PB.
                return subfield.sub(subfield.sub(w0[rowIndex][columnIndex], w1), xs[rowIndex]);
            })
            .collect(Collectors.toList());
    }

    private Gf2kVoleSenderOutput generateSenderOutput() {
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] ts = outputStream
            // PA outputs w = <g,w> ∈ F_pr
            .mapToObj(index -> field.mixInnerProduct(w0[index]))
            .toArray(byte[][]::new);
        return Gf2kVoleSenderOutput.create(field, xs, ts);
    }
}
