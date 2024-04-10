package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Gadget;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.AbstractZp64CoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KOS16-Zp64-核VOLE协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/06/09
 */
public class Kos16Zp64CoreVoleSender extends AbstractZp64CoreVoleSender {
    /**
     * base OT sender
     */
    private final BaseOtSender baseOtSender;
    /**
     * Zp64 gadget
     */
    private Zp64Gadget zp64Gadget;
    /**
     * base OT sender output
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t0
     */
    private long[][] t0;

    public Kos16Zp64CoreVoleSender(Rpc senderRpc, Party receiverParty, Kos16Zp64CoreVoleConfig config) {
        super(Kos16Zp64CoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(Zp64 zp64, int maxNum) throws MpcAbortException {
        setInitInput(zp64, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zp64Gadget = new Zp64Gadget(zp64);
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Zp64VoleSenderOutput send(long[] x) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayLoad = generateMatrixPayLoad();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kos16Zp64CoreVolePtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayLoad));
        Zp64VoleSenderOutput senderOutput = generateSenderOutput();
        t0 = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, matrixTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // 创建t0和t1数组, t0和t1的每行对应对应一个X值。
        t0 = new long[num][l];
        IntStream payLoadStream = IntStream.range(0, num * l);
        payLoadStream = parallel ? payLoadStream.parallel() : payLoadStream;
        return payLoadStream
            .mapToObj(index -> {
                // 计算当前处理的t0和t1数组的位置
                int rowIndex = index / l;
                int columnIndex = index % l;
                // 令k0和k1分别是baseOT的第j对密钥，计算 t0[i][j] = PRF(k0，i), t1[i][j] = PRF(k1, i)
                byte[] t0Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                byte[] t1Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                t0[rowIndex][columnIndex] = zp64.createRandom(t0Seed);
                long t1 = zp64.createRandom(t1Seed);
                // 计算u = t0[i,j] - t1[i,j] - x[i] mod p
                long u = zp64.sub(zp64.sub(t0[rowIndex][columnIndex], t1), x[rowIndex]);
                return LongUtils.longToByteArray(u);
            })
            .collect(Collectors.toList());
    }

    private Zp64VoleSenderOutput generateSenderOutput() {
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        long[] t = outputStream
            .mapToLong(index -> zp64Gadget.innerProduct(t0[index]))
            .toArray();
        return Zp64VoleSenderOutput.create(zp64, x, t);
    }

}
