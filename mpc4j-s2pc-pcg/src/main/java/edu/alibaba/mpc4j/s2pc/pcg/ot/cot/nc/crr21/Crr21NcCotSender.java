package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21;

import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCoder;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreator;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;

/**
 * CRR21-NC-COT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/02/21
 */

public class Crr21NcCotSender extends AbstractNcCotSender {
    /**
     * MSP-COT协议发送方
     */
    private final MspCotSender mspCotSender;
    /**
     * LPN参数n
     */
    private int iterationN;
    /**
     * LPN参数t
     */
    private int iterationT;

    /**
     * Silver编码类型
     */
    private final SilverCodeType silverCodeType;
    /**
     * CRR21的编码器
     */
    private SilverCoder silverCoder;

    public Crr21NcCotSender(Rpc senderRpc, Party receiverParty, Crr21NcCotConfig config) {
        super(Crr21NcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        silverCodeType = config.getCodeType();
        mspCotSender = MspCotFactory.createSender(senderRpc, receiverParty, config.getMspCotConfig());
        addSubPto(mspCotSender);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化CRR21编码器
        SilverCodeCreator silverCodeCreator = SilverCodeCreatorFactory
            .createInstance(silverCodeType, LongUtils.ceilLog2(num, Crr21NcCotPtoDesc.MIN_LOG_N));
        LpnParams lpnParams = silverCodeCreator.getLpnParams();
        silverCoder = silverCodeCreator.createCoder();
        silverCoder.setParallel(parallel);
        stopWatch.stop();
        long encoderInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, encoderInitTime);
        // 初始化MSPCOT协议
        stopWatch.start();
        iterationN = lpnParams.getN();
        iterationT = lpnParams.getT();
        mspCotSender.init(delta, iterationT, iterationN);
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
        // 执行MSPCOT
        MspCotSenderOutput sMspCotSenderOutput = mspCotSender.send(iterationT, iterationN);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        // y = v * G^T。
        byte[][] y = silverCoder.dualEncode(sMspCotSenderOutput.getR0Array());
        // 更新输出。
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
