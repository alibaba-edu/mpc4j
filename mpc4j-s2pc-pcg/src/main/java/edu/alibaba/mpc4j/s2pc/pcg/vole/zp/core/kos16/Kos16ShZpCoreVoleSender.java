package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
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
 * KOS16-ZP-核VOLE协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/06/09
 */
public class Kos16ShZpCoreVoleSender extends AbstractZpCoreVoleSender {
    /**
     * 基础OT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * Zp的Gadget工具
     */
    private ZpGadget zpGadget;
    /**
     * 有限域比特长度
     */
    private int k;
    /**
     * 基础OT协议发送方输出
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * t0
     */
    private BigInteger[][] t0;

    public Kos16ShZpCoreVoleSender(Rpc receiverRpc, Party senderParty, Kos16ShZpCoreVoleConfig config) {
        super(Kos16ShZpCoreVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtSender = BaseOtFactory.createSender(receiverRpc, senderParty, config.getBaseOtConfig());
        baseOtSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        baseOtSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        baseOtSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        baseOtSender.addLogLevel();
    }

    @Override
    public void init(BigInteger prime, int maxNum) throws MpcAbortException {
        setInitInput(prime, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        zpGadget = ZpGadget.createFromPrime(envType, prime);
        k = zpGadget.getK();
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(k);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ZpVoleSenderOutput send(BigInteger[] x) throws MpcAbortException {
        setPtoInput(x);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> matrixPayLoad = generateMatrixPayLoad();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kos16ShZpCoreVolePtoDesc.PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayLoad));
        ZpVoleSenderOutput senderOutput = generateSenderOutput();
        t0 = null;
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), matrixTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> generateMatrixPayLoad() {
        // 创建t0和t1数组, t0和t1的每行对应对应一个X值。
        t0 = new BigInteger[num][k];
        IntStream payLoadStream = IntStream.range(0, num * k);
        payLoadStream = parallel ? payLoadStream.parallel() : payLoadStream;
        return payLoadStream
            .mapToObj(index -> {
                // 计算当前处理的t0和t1数组的位置
                int rowIndex = index / k;
                int columnIndex = index % k;
                // 令k0和k1分别是baseOT的第j对密钥，计算 t0[i][j] = PRF(k0，i), t1[i][j] = PRF(k1, i)
                byte[] t0Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                byte[] t1Seed = ByteBuffer
                    .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).putInt(rowIndex).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                t0[rowIndex][columnIndex] = zpGadget.randomElement(t0Seed);
                BigInteger t1 = zpGadget.randomElement(t1Seed);
                // 计算u = t0[i,j] - t1[i,j] + x[i] mod p
                BigInteger u = t0[rowIndex][columnIndex].subtract(t1).add(x[rowIndex]).mod(prime);
                return BigIntegerUtils.nonNegBigIntegerToByteArray(u, zpGadget.getByteK());
            })
            .collect(Collectors.toList());
    }

    private ZpVoleSenderOutput generateSenderOutput() {
        IntStream outputStream = IntStream.range(0, num);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        BigInteger[] t = outputStream
            .mapToObj(index -> zpGadget.composition(t0[index]).negate().mod(prime))
            .toArray(BigInteger[]::new);
        return ZpVoleSenderOutput.create(prime, x, t);
    }

}
