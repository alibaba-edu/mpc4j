package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.lpn.llc.LocalLinearCoder;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;

/**
 * YWL20-NC-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
public class Ywl20NcCotSender extends AbstractNcCotSender {
    /**
     * MSP-COT配置项
     */
    private final MspCotConfig mspCotConfig;
    /**
     * MSP-COT协议发送方
     */
    private final MspCotSender mspCotSender;
    /**
     * COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * 得带LPN参数k
     */
    private int iterationK;
    /**
     * 迭代LPN参数n
     */
    private int iterationN;
    /**
     * 迭代LPN参数t
     */
    private int iterationT;
    /**
     * 迭代过程所需的k个COT协议发送方输出
     */
    private CotSenderOutput vCotSenderOutput;
    /**
     * MSP-COT协议所需的预计算COT数量
     */
    private int sCotPreSize;
    /**
     * MSP-COT协议所需的COT协议发送方输出
     */
    private CotSenderOutput sCotSenderOutput;

    public Ywl20NcCotSender(Rpc senderRpc, Party receiverParty, Ywl20NcCotConfig config) {
        super(Ywl20NcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        mspCotConfig = config.getMspCotConfig();
        mspCotSender = MspCotFactory.createSender(senderRpc, receiverParty, config.getMspCotConfig());
        mspCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和MSPCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        mspCotSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotSender.setParallel(parallel);
        mspCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotSender.addLogLevel();
        mspCotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        LpnParams setupLpnParams = Ywl20NcCotPtoDesc.getSetupLpnParams(mspCotConfig, num);
        int initK = setupLpnParams.getK();
        int initN = setupLpnParams.getN();
        int initT = setupLpnParams.getT();
        LpnParams iterationLpnParams = Ywl20NcCotPtoDesc.getIterationLpnParams(mspCotConfig, num);
        iterationK = iterationLpnParams.getK();
        iterationN = iterationLpnParams.getN();
        iterationT = iterationLpnParams.getT();
        // 初始化COT协议和MSPCOT协议
        coreCotSender.init(delta, initK);
        mspCotSender.init(delta, iterationT, iterationN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        // 得到初始化阶段的k个COT
        CotSenderOutput vInitCotSenderOutput = coreCotSender.send(initK);
        stopWatch.stop();
        long kInitCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kInitCotTime);

        stopWatch.start();
        // 得到初始化矩阵A的种子
        DataPacketHeader matrixInitKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SETUP_KEY.ordinal(),
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixInitKeyPayload = rpc.receive(matrixInitKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(matrixInitKeyPayload.size() == 1);
        byte[] initKey = matrixInitKeyPayload.remove(0);
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, initKey, parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyInitTime);

        stopWatch.start();
        // 执行MSPCOT
        MspCotSenderOutput sInitMspCotSenderOutput = mspCotSender.send(initT, initN);
        stopWatch.stop();
        long sInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sInitTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] initY = matrixInitA.gf2eEncode(vInitCotSenderOutput.getR0Array());
        IntStream.range(0, initN).forEach(index ->
            BytesUtils.xori(initY[index], sInitMspCotSenderOutput.getR0(index))
        );
        sCotSenderOutput = CotSenderOutput.create(delta, initY);
        vCotSenderOutput = sCotSenderOutput.split(iterationK);
        sCotPreSize = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
        sCotSenderOutput.reduce(sCotPreSize);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), extendInitTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 得到迭代矩阵A的种子
        DataPacketHeader matrixKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_ITERATION_LEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixKeyPayload = rpc.receive(matrixKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(matrixKeyPayload.size() == 1);
        byte[] matrixKey = matrixKeyPayload.remove(0);
        LocalLinearCoder matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKey, parallel);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Iter. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        stopWatch.start();
        // 执行MSPCOT
        MspCotSenderOutput sMspCotSenderOutput = mspCotSender.send(iterationT, iterationN, sCotSenderOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Iter. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), sTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] y = matrixA.gf2eEncode(vCotSenderOutput.getR0Array());
        IntStream.range(0, iterationN).forEach(index ->
            BytesUtils.xori(y[index], sMspCotSenderOutput.getR0(index))
        );
        // 更新输出
        CotSenderOutput senderOutput = CotSenderOutput.create(delta, y);
        vCotSenderOutput = senderOutput.split(iterationK);
        sCotSenderOutput = senderOutput.split(sCotPreSize);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Iter. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(),extendTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
