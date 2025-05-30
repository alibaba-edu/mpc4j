package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.rrt23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoder;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory.ExCoderType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotSenderOutput;

import java.util.concurrent.TimeUnit;

/**
 * RRT23-NC-COT sender.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class Rrt23NcCotSender extends AbstractNcCotSender {
    /**
     * MSP-COT sender
     */
    private final MspCotSender mspCotSender;
    /**
     * coder type
     */
    private final ExCoderType exCoderType;
    /**
     * coder
     */
    private ExCoder exCoder;
    /**
     * n
     */
    private int n;
    /**
     * t
     */
    private int t;

    public Rrt23NcCotSender(Rpc senderRpc, Party receiverParty, Rrt23NcCotConfig config) {
        super(Rrt23NcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        mspCotSender = MspCotFactory.createSender(senderRpc, receiverParty, config.getMspCotConfig());
        addSubPto(mspCotSender);
        exCoderType = config.getCodeType();
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int actualNum = Math.max(num, 1 << Rrt23NcCotPtoDesc.MIN_LOG_N);
        exCoder = ExCoderFactory.createExCoder(exCoderType, actualNum);
        exCoder.setParallel(parallel);
        n = actualNum * ExCoderFactory.getScalar(exCoderType);
        t = ExCoderFactory.getRegularNoiseWeight(exCoderType, n);
        stopWatch.stop();
        long encoderInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, encoderInitTime);

        stopWatch.start();
        mspCotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        MspCotSenderOutput sMspCotSenderOutput = mspCotSender.send(t, n);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        byte[][] y = exCoder.dualEncode(sMspCotSenderOutput.getR0Array());
        CotSenderOutput senderOutput = CotSenderOutput.create(delta, y);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
