package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotReceiverOutput;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * NP99-基础n选1-OT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/07/20
 */
public class Np99BnotReceiver extends AbstractBnotReceiver {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * 最大选择数的比特长度
     */
    private int nBitLength;
    /**
     * KDF
     */
    private final Kdf kdf;

    public Np99BnotReceiver(Rpc receiverRpc, Party senderParty, Np99BnotConfig config) {
        super(Np99BnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(receiverRpc, senderParty, config.getBaseOtConfig());
        baseOtReceiver.addLogLevel();
        kdf = KdfFactory.createInstance(config.getEnvType());
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
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        nBitLength = LongUtils.ceilLog2(n);
        baseOtReceiver.init();
        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BaseOtReceiverOutput baseOtReceiverOutput = baseOtReceiver.receive(generateBinaryChoices(choices));
        BnotReceiverOutput receiverOutput = generateReceiverOutput(baseOtReceiverOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        return receiverOutput;
    }

    private boolean[] generateBinaryChoices(int[] choices) {
        boolean[] binaryChoices = new boolean[nBitLength * choices.length];
        IntStream stream = IntStream.range(0, choices.length);
        stream = parallel ? stream.parallel() : stream;
        stream.forEach(index -> {
            boolean[] binaryChoice = BinaryUtils.byteArrayToBinary(IntUtils.intToByteArray(choices[index]), nBitLength);
            System.arraycopy(binaryChoice, 0, binaryChoices, index * nBitLength, nBitLength);
            }
        );
        return binaryChoices;
    }

    private BnotReceiverOutput generateReceiverOutput(BaseOtReceiverOutput baseOtReceiverOutput) {
        IntStream outputStream = IntStream.range(0, choices.length);
        outputStream = parallel ? outputStream.parallel() : outputStream;
        byte[][] rbArray = new byte[choices.length][];
        outputStream.forEach(index -> {
                    int startIndex = index * nBitLength;
                    ByteBuffer buffer = ByteBuffer.allocate(nBitLength * CommonConstants.BLOCK_BYTE_LENGTH);
                    for (int i = 0; i < nBitLength; i++) {
                        buffer.put(baseOtReceiverOutput.getRb(startIndex + i));
                    }
                    rbArray[index] = kdf.deriveKey(buffer.array());
                }
        );
        return new BnotReceiverOutput(n, choices, rbArray);
    }

}
