package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.iknp03.Iknp03CoreCotPtoDesc.PtoStep;

/**
 * IKNP03-核COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2020/05/27
 */
public class Iknp03CoreCotReceiver extends AbstractCoreCotReceiver {
    /**
     * 基础OT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * 密钥派生函数
     */
    private final Kdf kdf;
    /**
     * 基础OT协议输出
     */
    private BaseOtSenderOutput baseOtSenderOutput;
    /**
     * 布尔矩阵
     */
    private TransBitMatrix tMatrix;

    public Iknp03CoreCotReceiver(Rpc receiverRpc, Party senderParty, Iknp03CoreCotConfig config) {
        super(Iknp03CoreCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtSender = BaseOtFactory.createSender(receiverRpc, senderParty, config.getBaseOtConfig());
        baseOtSender.addLogLevel();
        kdf = KdfFactory.createInstance(envType);
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
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        baseOtSender.init();
        baseOtSenderOutput = baseOtSender.send(CommonConstants.BLOCK_BIT_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> matrixPayload = generateMatrixPayload();
        DataPacketHeader matrixHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixHeader, matrixPayload));
        CotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateMatrixPayload() {
        // 将选择比特组合成byte[]，方便在矩阵中执行xor运算
        byte[] choiceBytes = BinaryUtils.binaryToRoundByteArray(choices);
        // 初始化伪随机数生成器
        Prg prg = PrgFactory.createInstance(envType, choiceBytes.length);
        // 构建矩阵tMatrix
        tMatrix = TransBitMatrixFactory.createInstance(envType, num, CommonConstants.BLOCK_BIT_LENGTH, parallel);
        // 矩阵列加密流
        IntStream columnIndexIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
        return columnIndexIntStream
            .mapToObj(columnIndex -> {
                // Receiver forms m \times k matrices T_0, T_1 such that t_{j, 0} \oplus t_{j, 1} = (r_j || \cdots || r_j)
                byte[] columnSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(columnSeed);
                byte[] column0Bytes = prg.extendToBytes(columnSeed);
                BytesUtils.reduceByteArray(column0Bytes, num);
                tMatrix.setColumn(columnIndex, column0Bytes);
                byte[] column1Bytes = BytesUtils.xor(column0Bytes, choiceBytes);
                // Sender and receiver interact with OT^k_m: the receiver acts as OT sender with input t_0, t_1
                byte[] r0Seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).put(baseOtSenderOutput.getR0(columnIndex))
                    .array();
                r0Seed = kdf.deriveKey(r0Seed);
                byte[] message0 = prg.extendToBytes(r0Seed);
                BytesUtils.reduceByteArray(message0, num);
                BytesUtils.xori(message0, column0Bytes);
                byte[] r1Seed = ByteBuffer.allocate(Long.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                    .putLong(extraInfo).put(baseOtSenderOutput.getR1(columnIndex))
                    .array();
                r1Seed = kdf.deriveKey(r1Seed);
                byte[] message1 = prg.extendToBytes(r1Seed);
                BytesUtils.reduceByteArray(message1, num);
                BytesUtils.xori(message1, column1Bytes);

                return new byte[][]{message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    private CotReceiverOutput generateReceiverOutput() {
        // 生成密钥数组，将矩阵T转置，按行获取
        TransBitMatrix tMatrixTranspose = tMatrix.transpose();
        tMatrix = null;
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(tMatrixTranspose::getColumn)
            .toArray(byte[][]::new);

        return CotReceiverOutput.create(choices, rbArray);
    }
}
