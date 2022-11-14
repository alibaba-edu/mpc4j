package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
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
        mspCotReceiver.addLogLevel();
        taskIdPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和MSPCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        mspCotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        mspCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        mspCotReceiver.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
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
        info("{}{} Recv. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encoderInitTime);

        // 初始化MSP-COT协议
        stopWatch.start();
        iterationN = lpnParams.getN();
        iterationT = lpnParams.getT();
        mspCotReceiver.init(iterationT, iterationN);
        preCotSize = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 执行MSP-COT
        MspCotReceiverOutput rMspCotReceiverOutput = rCotReceiverOutput == null
            ? mspCotReceiver.receive(iterationT, iterationN)
            : mspCotReceiver.receive(iterationT, iterationN, rCotReceiverOutput);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Iter. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rTime);

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
        info("{}{} Recv. Iter. Step 2.{}/2.{} ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), extendTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
