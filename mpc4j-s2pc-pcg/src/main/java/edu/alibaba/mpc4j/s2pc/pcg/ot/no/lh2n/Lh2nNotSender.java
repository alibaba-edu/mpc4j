package edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotFactory.NotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotSenderOutput;

import java.util.concurrent.TimeUnit;

/**
 * 2^l选1-HOT协议转换为n选1-OT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/27
 */
public class Lh2nNotSender extends AbstractSecureTwoPartyPto implements NotSender {
    /**
     * 配置项
     */
    private final Lh2nNotConfig config;
    /**
     * 2^l选1-OT协议
     */
    private final LhotSender lhotSender;
    /**
     * 最大选择值
     */
    private int n;
    /**
     * 输入比特长度
     */
    private int inputBitLength;
    /**
     * 最大数量
     */
    private int maxNum;

    public Lh2nNotSender(Rpc senderRpc, Party receiverParty, Lh2nNotConfig config) {
        super(Lh2nNotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lhotSender = LhotFactory.createSender(senderRpc, receiverParty, config.getLhotConfig());
        lhotSender.addLogLevel();
        this.config = config;
    }

    @Override
    public NotType getPtoType() {
        return config.getPtoType();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        lhotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        lhotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        lhotSender.addLogLevel();
    }

    @Override
    public void init(int n, int maxNum) throws MpcAbortException {
        setInitInput(n, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        lhotSender.init(inputBitLength, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    private void setInitInput(int n, int maxNum) {
        assert n > 1 : "n must be greater than 1: " + n;
        this.n = n;
        inputBitLength = LongUtils.ceilLog2(n);
        assert maxNum > 0 : "MaxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    @Override
    public NotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        LhotSenderOutput lhotSenderOutput = lhotSender.send(num);
        NotSenderOutput senderOutput = Lh2nNotSenderOutput.create(
            n, lhotSenderOutput.getDelta(), lhotSenderOutput.getQsArray()
        );

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + num + "]";
        extraInfo++;
    }
}
