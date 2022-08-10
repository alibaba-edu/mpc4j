package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpGadget;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.AbstractZpCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.ZpVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ShZpCoreVolePtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * KOS16-Zp-核VOLE协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/06/09
 */
public class Kos16ShZpCoreVoleReceiver extends AbstractZpCoreVoleReceiver {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * Zp的Gadget工具
     */
    private ZpGadget zpGadget;
    /**
     * 有限域比特长度
     */
    private int k;
    /**
     * 基础OT协议接收方输出
     */
    private BaseOtReceiverOutput baseOtReceiverOutput;
    /**
     * delta的二进制表示
     */
    boolean[] deltaBinary;

    public Kos16ShZpCoreVoleReceiver(Rpc senderRpc, Party receiverParty, Kos16ShZpCoreVoleConfig config) {
        super(Kos16ShZpCoreVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(senderRpc, receiverParty, config.getBaseOtConfig());
        baseOtReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        baseOtReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        baseOtReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        baseOtReceiver.addLogLevel();
    }

    @Override
    public void init(BigInteger prime, BigInteger delta, int maxNum) throws MpcAbortException {
        setInitInput(prime, delta, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        zpGadget = ZpGadget.createFromPrime(envType, prime);
        k = zpGadget.getK();
        baseOtReceiver.init();
        deltaBinary = zpGadget.decomposition(delta);
        baseOtReceiverOutput = baseOtReceiver.receive(deltaBinary);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public ZpVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        ZpVoleReceiverOutput receiverOutput = handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), matrixTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;

    }

    private ZpVoleReceiverOutput handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == num * k);
        byte[][] matrixPayloadArray = matrixPayload.toArray(new byte[0][]);
        // 创建q矩阵
        BigInteger[][] qMatrix = new BigInteger[num][k];
        IntStream matrixStream = IntStream.range(0, num * k);
        matrixStream = parallel ? matrixStream.parallel() : matrixStream;
        matrixStream.forEach(index -> {
            // 计算当前处理q矩阵的位置(i,j)
            int rowIndex = index / k;
            int columnIndex = index % k;
            // 从payload中读取Zp元素u
            BigInteger u = BigIntegerUtils.byteArrayToNonNegBigInteger(matrixPayloadArray[index]);
            // 计算tb = PRF(kb, i), q(i,j) = u + delta_j * tb,
            byte[] tbSeed = ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putLong(extraInfo).putInt(rowIndex).put(baseOtReceiverOutput.getRb(columnIndex))
                .array();
            BigInteger tb = zpGadget.randomElement(tbSeed);
            qMatrix[rowIndex][columnIndex] = deltaBinary[columnIndex] ? u.add(tb).mod(prime) : tb;
        });
        // 将矩阵q的每一行按照gadget组合为一个Zp元素，得到Zp数组q。
        Stream<BigInteger[]> qMatrixStream = Arrays.stream(qMatrix);
        qMatrixStream = parallel ? qMatrixStream.parallel() : qMatrixStream;
        BigInteger[] q = qMatrixStream
            .map(row -> zpGadget.composition(row))
            .toArray(BigInteger[]::new);
        return ZpVoleReceiverOutput.create(prime, delta, q);
    }
}
