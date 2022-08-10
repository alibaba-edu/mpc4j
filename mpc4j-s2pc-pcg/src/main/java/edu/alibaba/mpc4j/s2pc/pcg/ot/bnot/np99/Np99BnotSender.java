package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotSenderOutput;

import java.util.concurrent.TimeUnit;

/**
 * NP99-基础n选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
public class Np99BnotSender extends AbstractBnotSender {
    /**
     * BaseOT协议发送方
     */
    private final BaseOtSender baseOtSender;
    /**
     * 最大选择数的比特长度
     */
    private int nBitLength;

    public Np99BnotSender(Rpc senderRpc, Party receiverParty, Np99BnotConfig config) {
        super(Np99BnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtSender = BaseOtFactory.createSender(senderRpc, receiverParty, config.getBaseOtConfig());
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
    public void init(int n) throws MpcAbortException {
        setInitInput(n);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        nBitLength = LongUtils.ceilLog2(n);
        baseOtSender.init();
        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BnotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BaseOtSenderOutput baseOtSenderOutput = baseOtSender.send(num * nBitLength);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return new Np99BnotSenderOutput(envType, n, num, baseOtSenderOutput);
    }

}
