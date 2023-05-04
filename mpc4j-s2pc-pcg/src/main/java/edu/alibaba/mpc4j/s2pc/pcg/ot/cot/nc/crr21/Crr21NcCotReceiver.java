package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21;

import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCoder;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreator;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorFactory;
import edu.alibaba.mpc4j.common.tool.lpn.ldpc.LdpcCreatorUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
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
     * MSP-COT配置项
     */
    private final MspCotConfig mspCotConfig;
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
     * MSP-COT协议所需的预计算COT数量
     */
    private int preCotSize;
    /**
     * MSP-COT协议所需的COT协议接收方输出
     */
    private CotReceiverOutput rCotReceiverOutput;
    /**
     * LDPC编码类型
     */
    private final LdpcCreatorUtils.CodeType codeType;
    /**
     * LDPC编码器
     */
    private LdpcCoder ldpcCoder;

    public Crr21NcCotReceiver(Rpc receiverRpc, Party senderParty, Crr21NcCotConfig config) {
        super(Crr21NcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        mspCotConfig = config.getMspCotConfig();
        codeType = config.getCodeType();
        mspCotReceiver = MspCotFactory.createReceiver(receiverRpc, senderParty, config.getMspCotConfig());
        addSubPtos(mspCotReceiver);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        logPhaseInfo(PtoState.INIT_BEGIN);
        // 重新初始化时需要清空之前存留的输出
        rCotReceiverOutput = null;

        // 初始化CRR21的编码器。
        stopWatch.start();
        LdpcCreator ldpcCreator = LdpcCreatorFactory
            .createLdpcCreator(codeType, LongUtils.ceilLog2(num, Crr21NcCotPtoDesc.MIN_LOG_N));
        LpnParams lpnParams = ldpcCreator.getLpnParams();
        ldpcCoder = ldpcCreator.createLdpcCoder();
        ldpcCoder.setParallel(parallel);
        stopWatch.stop();
        long encoderInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, encoderInitTime);

        // 初始化MSP-COT协议
        stopWatch.start();
        iterationN = lpnParams.getN();
        iterationT = lpnParams.getT();
        mspCotReceiver.init(iterationT, iterationN);
        preCotSize = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
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
        MspCotReceiverOutput rMspCotReceiverOutput = rCotReceiverOutput == null
            ? mspCotReceiver.receive(iterationT, iterationN)
            : mspCotReceiver.receive(iterationT, iterationN, rCotReceiverOutput);
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
        boolean[] extendB = ldpcCoder.transEncode(initB);
        // z = z * G^T。
        byte[][] initZ = rMspCotReceiverOutput.getRbArray();
        byte[][] extendZ = ldpcCoder.transEncode(initZ);
        // 更新输出。
        CotReceiverOutput receiverOutput = CotReceiverOutput.create(extendB, extendZ);
        rCotReceiverOutput = receiverOutput.split(preCotSize);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
