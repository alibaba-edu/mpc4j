package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBaseNotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotReceiverOutput;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NP99-基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/20
 */
public class Np99BaseNotReceiver extends AbstractBaseNotReceiver {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * 最大选择数的比特长度
     */
    private int maxChoiceBitLength;

    public Np99BaseNotReceiver(Rpc receiverRpc, Party senderParty, Np99BaseNotConfig config) {
        super(Np99BaseNotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
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
    public void init(int maxChoice) throws MpcAbortException {
        setInitInput(maxChoice);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        maxChoiceBitLength = LongUtils.ceilLog2(maxChoice);
        baseOtReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BaseNotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BaseOtReceiverOutput baseOtReceiverOutput = baseOtReceiver.receive(generateBinaryChoices(choices));
        BaseNotReceiverOutput receiverOutput = generateReceiverOutput(baseOtReceiverOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        return receiverOutput;
    }

    private boolean[] generateBinaryChoices(int[] choices) {
        boolean[] binaryChoices = new boolean[maxChoiceBitLength * choices.length];
        IntStream stream = IntStream.range(0, choices.length);
        stream = parallel ? stream.parallel() : stream;
        stream.forEach(index -> {
            boolean[] binaryChoice = BinaryUtils.byteArrayToBinary(IntUtils.intToByteArray(choices[index]), maxChoiceBitLength);
            System.arraycopy(binaryChoice, 0, binaryChoices, index * maxChoiceBitLength, maxChoiceBitLength);
            }
        );
        return binaryChoices;
    }

    private BaseNotReceiverOutput generateReceiverOutput(BaseOtReceiverOutput baseOtReceiverOutput) {
        IntStream outputStream = IntStream.range(0, choices.length);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] rbArray = new byte[choices.length][];
        outputStream.forEach(index -> {
                    int startIndex = index * maxChoiceBitLength;
                    ByteBuffer buffer = ByteBuffer.allocate(maxChoiceBitLength * CommonConstants.BLOCK_BYTE_LENGTH);
                    for (int i = 0; i < maxChoiceBitLength; i++) {
                        buffer.put(baseOtReceiverOutput.getRb(startIndex + i));
                    }
                    rbArray[index] = kdf.deriveKey(buffer.array());
                }
        );
        return new BaseNotReceiverOutput(maxChoice, choices, rbArray);
    }

}
