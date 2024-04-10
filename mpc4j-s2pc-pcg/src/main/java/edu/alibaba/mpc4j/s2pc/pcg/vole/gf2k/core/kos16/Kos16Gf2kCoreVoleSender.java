package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;
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
 * KOS16-GF2K-core VOLE sender.
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
     * GF2K gadget
     */
    private Gf2kGadget gf2kGadget;
    /**
     * base OT sender output
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t0
     */
    private byte[][][] t0;

    public Kos16Gf2kCoreVoleSender(Rpc senderRpc, Party receiverParty, Kos16Gf2kCoreVoleConfig config) {
        super(Kos16Gf2kCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        Gf2kVoleSenderOutput senderOutput = generateSenderOutput();
        t0 = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // creates t0 and t1 array, each row in t0/t1 corresponds to an X.
        t0 = new byte[num][l][];
        IntStream payLoadStream = IntStream.range(0, num * l);
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
                t0[rowIndex][columnIndex] = gf2k.createRandom(t0Seed);
                byte[] t1 = gf2k.createRandom(t1Seed);
                // Compute u = t0[i,j] - t1[i,j] - x[i]
                return gf2k.sub(gf2k.sub(t0[rowIndex][columnIndex], t1), xs[rowIndex]);
            })
            .collect(Collectors.toList());
    }

    private Gf2kVoleSenderOutput generateSenderOutput() {
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] ts = outputStream
            .mapToObj(index -> gf2kGadget.innerProduct(t0[index]))
            .toArray(byte[][]::new);
        return Gf2kVoleSenderOutput.create(xs, ts);
    }

}
