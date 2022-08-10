package edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotFactory.NotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 2^l选1-HOT协议转换为n选1-OT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/27
 */
public class Lh2nNotReceiver extends AbstractSecureTwoPartyPto implements NotReceiver {
    /**
     * 配置项
     */
    private final Lh2nNotConfig config;
    /**
     * 2^l选1-OT协议
     */
    private final LhotReceiver lhotReceiver;
    /**
     * 最大选择值
     */
    private int n;
    /**
     * 输入比特长度
     */
    private int inputBitLength;
    /**
     * 输入字节长度
     */
    private int inputByteLength;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 选择值输入数组
     */
    private byte[][] choiceInputs;

    public Lh2nNotReceiver(Rpc receiverRpc, Party senderParty, Lh2nNotConfig config) {
        super(Lh2nNotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lhotReceiver = LhotFactory.createReceiver(receiverRpc, senderParty, config.getLhotConfig());
        lhotReceiver.addLogLevel();
        this.config = config;
    }

    @Override
    public NotType getPtoType() {
        return config.getPtoType();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        lhotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        lhotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        lhotReceiver.addLogLevel();
    }

    @Override
    public void init(int n, int maxNum) throws MpcAbortException {
        setInitInput(n, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        lhotReceiver.init(inputBitLength, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private void setInitInput(int n, int maxNum) {
        assert n > 1 : "n must be greater than 1: " + n;
        this.n = n;
        inputBitLength = LongUtils.ceilLog2(n);
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        assert maxNum > 0 : "MaxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    @Override
    public NotReceiverOutput receive(int[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        LotReceiverOutput lotReceiverOutput = lhotReceiver.receive(choiceInputs);
        NotReceiverOutput receiverOutput = NotReceiverOutput.create(
            n, lotReceiverOutput.getOutputBitLength(), choices, lotReceiverOutput.getRbArray()
        );

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private void setPtoInput(int[] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert choices.length > 0 && choices.length <= maxNum : "# of choices must be in range (0, " + maxNum + "]";
        choiceInputs = Arrays.stream(choices).peek(choice -> {
                assert choice >= 0 && choice < n : "choice must be in range [0, " + n + "): " + choice;
            })
            .mapToObj(choice -> IntUtils.nonNegIntToFixedByteArray(choice, inputByteLength))
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
