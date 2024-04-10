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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * CRR21-NC-COT协议接收方。
 *
 * @author Hanwen Feng
 * @date 2022/02/21
 */

public class Crr21NcCotReceiver extends AbstractNcCotReceiver {
    /**
     * MSP-COT协议接收方
     */
    private final MspCotReceiver mspCotReceiver;
    /**
     * LPN参数n
     */
    private int iterationN;
    /**
     * LPN参数t
     */
    private int iterationT;
    /**
     * LDPC编码类型
     */
    private final SilverCodeType silverCodeType;
    /**
     * LDPC编码器
     */
    private SilverCoder silverCoder;

    public Crr21NcCotReceiver(Rpc receiverRpc, Party senderParty, Crr21NcCotConfig config) {
        super(Crr21NcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        silverCodeType = config.getCodeType();
        mspCotReceiver = MspCotFactory.createReceiver(receiverRpc, senderParty, config.getMspCotConfig());
        addSubPto(mspCotReceiver);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        SilverCodeCreator silverCodeCreator = SilverCodeCreatorFactory
            .createInstance(silverCodeType, LongUtils.ceilLog2(num, Crr21NcCotPtoDesc.MIN_LOG_N));
        LpnParams lpnParams = silverCodeCreator.getLpnParams();
        silverCoder = silverCodeCreator.createCoder();
        silverCoder.setParallel(parallel);
        stopWatch.stop();
        long encoderInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, encoderInitTime);

        // 初始化MSP-COT协议
        stopWatch.start();
        iterationN = lpnParams.getN();
        iterationT = lpnParams.getT();
        mspCotReceiver.init(iterationT, iterationN);
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
        MspCotReceiverOutput rMspCotReceiverOutput = mspCotReceiver.receive(iterationT, iterationN);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, rTime);

        stopWatch.start();
        // b = b * G^T。
        boolean[] initB = new boolean[iterationN];
        for (int eIndex : rMspCotReceiverOutput.getAlphaArray()) {
            initB[eIndex] = !initB[eIndex];
        }
        boolean[] extendB = silverCoder.dualEncode(initB);
        // z = z * G^T。
        byte[][] initZ = rMspCotReceiverOutput.getRbArray();
        byte[][] extendZ = silverCoder.dualEncode(initZ);
        // 更新输出。
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
