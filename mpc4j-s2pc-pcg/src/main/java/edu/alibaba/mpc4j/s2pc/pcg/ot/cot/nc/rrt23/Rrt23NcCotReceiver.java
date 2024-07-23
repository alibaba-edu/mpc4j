package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.rrt23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoder;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory.ExCoderType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotReceiverOutput;

import java.util.concurrent.TimeUnit;

/**
 * RRT23-NC-COT receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class Rrt23NcCotReceiver extends AbstractNcCotReceiver {
    /**
     * MSP-COT
     */
    private final MspCotReceiver mspCotReceiver;
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

    public Rrt23NcCotReceiver(Rpc receiverRpc, Party senderParty, Rrt23NcCotConfig config) {
        super(Rrt23NcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mspCotReceiver = MspCotFactory.createReceiver(receiverRpc, senderParty, config.getMspCotConfig());
        addSubPto(mspCotReceiver);
        exCoderType = config.getCodeType();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
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
        mspCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 执行MSP-COT
        MspCotReceiverOutput rMspCotReceiverOutput = mspCotReceiver.receive(t, n);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, rTime);

        stopWatch.start();
        boolean[] initB = new boolean[n];
        for (int eIndex : rMspCotReceiverOutput.getAlphaArray()) {
            initB[eIndex] = !initB[eIndex];
        }
        boolean[] extendB = exCoder.dualEncode(initB);
        byte[][] initZ = rMspCotReceiverOutput.getRbArray();
        byte[][] extendZ = exCoder.dualEncode(initZ);
        CotReceiverOutput receiverOutput = CotReceiverOutput.create(extendB, extendZ);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
