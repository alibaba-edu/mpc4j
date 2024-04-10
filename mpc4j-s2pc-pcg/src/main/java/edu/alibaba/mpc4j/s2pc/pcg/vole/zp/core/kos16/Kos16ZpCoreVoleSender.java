package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpGadget;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.AbstractZpCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KOS16-ZP-core VOLE sender.
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/06/09
 */
public class Kos16ZpCoreVoleSender extends AbstractZpCoreVoleSender {
    /**
     * base OT sender
     */
    private final BaseOtSender baseOtSender;
    /**
     * Zp gadget
     */
    private ZpGadget zpGadget;
    /**
     * base OT sender output
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t0
     */
    private BigInteger[][] t0;

    public Kos16ZpCoreVoleSender(Rpc senderRpc, Party receiverParty, Kos16ZpCoreVoleConfig config) {
        super(Kos16ZpCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPto(baseOtSender);
    }

    @Override
    public void init(Zp zp, int maxNum) throws MpcAbortException {
        setInitInput(zp, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zpGadget = new ZpGadget(zp);
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZpVoleSenderOutput send(BigInteger[] x) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> matrixPayLoad = generateMatrixPayLoad();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Kos16ZpCoreVolePtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayLoad));
        ZpVoleSenderOutput senderOutput = generateSenderOutput();
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
        t0 = new BigInteger[num][l];
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
                t0[rowIndex][columnIndex] = zp.createRandom(t0Seed);
                BigInteger t1 = zp.createRandom(t1Seed);
                // 计算u = t0[i,j] - t1[i,j] - x[i] mod p
                BigInteger u = zp.sub(zp.sub(t0[rowIndex][columnIndex], t1), x[rowIndex]);
                return BigIntegerUtils.nonNegBigIntegerToByteArray(u, primeByteLength);
            })
            .collect(Collectors.toList());
    }

    private ZpVoleSenderOutput generateSenderOutput() {
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        BigInteger[] t = outputStream
            .mapToObj(index -> zpGadget.innerProduct(t0[index]))
            .toArray(BigInteger[]::new);
        return ZpVoleSenderOutput.create(zp, x, t);
    }

}
