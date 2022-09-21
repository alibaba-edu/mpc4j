package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.AbstractNcLotSender;

import java.util.concurrent.TimeUnit;

/**
 * 直接NC-2^l选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotSender extends AbstractNcLotSender {
    /**
     * 核2^l选1-OT协议发送方
     */
    private final CoreLotSender coreLotSender;

    public DirectNcLotSender(Rpc senderRpc, Party receiverParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreLotSender = CoreLotFactory.createSender(senderRpc, receiverParty, config.getCoreLotConfig());
        coreLotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreLotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreLotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreLotSender.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreLotSender.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public LotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        LotSenderOutput senderOutput = coreLotSender.send(num);
        stopWatch.stop();
        long coreLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        senderOutput.reduce(num);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), coreLotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
