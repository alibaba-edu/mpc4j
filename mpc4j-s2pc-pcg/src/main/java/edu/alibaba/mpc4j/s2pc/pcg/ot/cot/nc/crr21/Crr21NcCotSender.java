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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
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
     * MSP-COT配置项
     */
    private final MspCotConfig mspCotConfig;
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
     * MSP-COT协议所需的预计算COT数量
     */
    private int sCotPreSize;
    /**
     * MSP-COT协议所需的COT协议发送方输出
     */
    private CotSenderOutput sCotSenderOutput;

    /**
     * Silver编码类型
     */
    private final LdpcCreatorUtils.CodeType codeType;
    /**
     * CRR21的编码器
     */
    private LdpcCoder ldpcCoder;

    public Crr21NcCotSender(Rpc senderRpc, Party receiverParty, Crr21NcCotConfig config) {
        super(Crr21NcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        mspCotConfig = config.getMspCotConfig();
        codeType = config.getCodeType();
        mspCotSender = MspCotFactory.createSender(senderRpc, receiverParty, config.getMspCotConfig());
        addSubPtos(mspCotSender);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);
        // 重新初始化时需要清空之前存留的输出
        sCotSenderOutput = null;

        stopWatch.start();
        // 初始化CRR21编码器
        LdpcCreator ldpcCreator = LdpcCreatorFactory
            .createLdpcCreator(codeType, LongUtils.ceilLog2(num, Crr21NcCotPtoDesc.MIN_LOG_N));
        LpnParams lpnParams = ldpcCreator.getLpnParams();
        ldpcCoder = ldpcCreator.createLdpcCoder();
        ldpcCoder.setParallel(parallel);
        stopWatch.stop();
        long encoderInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, encoderInitTime);
        // 初始化MSPCOT协议
        stopWatch.start();
        iterationN = lpnParams.getN();
        iterationT = lpnParams.getT();
        mspCotSender.init(delta, iterationT, iterationN);
        sCotPreSize = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
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
        MspCotSenderOutput sMspCotSenderOutput = (sCotSenderOutput == null)
            ? mspCotSender.send(iterationT, iterationN)
            : mspCotSender.send(iterationT, iterationN, sCotSenderOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        // y = v * G^T。
        byte[][] y = ldpcCoder.transEncode(sMspCotSenderOutput.getR0Array());
        // 更新输出。
        CotSenderOutput senderOutput = CotSenderOutput.create(delta, y);
        sCotSenderOutput = senderOutput.split(sCotPreSize);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

}
