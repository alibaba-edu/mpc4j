package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.lpn.llc.LocalLinearCoder;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * YWL20-NC-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/02
 */
public class Ywl20NcCotReceiver extends AbstractNcCotReceiver {
    /**
     * 任务ID的PRF
     */
    private final Prf taskIdPrf;
    /**
     * MSP-COT配置项
     */
    private final MspCotConfig mspCotConfig;
    /**
     * MSP-COT协议接收方
     */
    private final MspCotReceiver mspCotReceiver;
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * 迭代LPN参数k
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
     * 迭代过程所需的k个COT协议接收方输出
     */
    private CotReceiverOutput wCotReceiverOutput;
    /**
     * MSP-COT协议所需的预计算COT数量
     */
    private int preCotSize;
    /**
     * MSP-COT协议所需的COT协议接收方输出
     */
    private CotReceiverOutput rCotReceiverOutput;

    public Ywl20NcCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20NcCotConfig config) {
        super(Ywl20NcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        mspCotConfig = config.getMspCotConfig();
        mspCotReceiver = MspCotFactory.createReceiver(receiverRpc, senderParty, config.getMspCotConfig());
        mspCotReceiver.addLogLevel();
        taskIdPrf = PrfFactory.createInstance(getEnvType(), Long.BYTES);
        taskIdPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和MSPCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        mspCotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
        mspCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
        mspCotReceiver.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

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
        coreCotReceiver.init(initK);
        mspCotReceiver.init(iterationT, iterationN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        // 得到初始化阶段的k个COT
        boolean[] choices = new boolean[initK];
        IntStream.range(0, initK).forEach(index -> choices[index] = secureRandom.nextBoolean());
        CotReceiverOutput wInitCotReceiverOutput = coreCotReceiver.receive(choices);
        stopWatch.stop();
        long kInitCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), kInitCotTime);

        stopWatch.start();
        // 初始化矩阵A的种子
        byte[] matrixInitKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(matrixInitKey);
        List<byte[]> matrixInitKeyPayload = new LinkedList<>();
        matrixInitKeyPayload.add(matrixInitKey);
        DataPacketHeader matrixInitKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SETUP_KEY.ordinal(),
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixInitKeyHeader, matrixInitKeyPayload));
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, matrixInitKey, parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyInitTime);

        stopWatch.start();
        // 执行MSP-COT
        MspCotReceiverOutput rInitMspCotReceiverOutput = mspCotReceiver.receive(initT, initN);
        stopWatch.stop();
        long rInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rInitTime);

        stopWatch.start();
        // x = u * A + e
        boolean[] initX = matrixInitA.binaryEncode(wInitCotReceiverOutput.getChoices());
        for (int eIndex : rInitMspCotReceiverOutput.getAlphaArray()) {
            initX[eIndex] = !initX[eIndex];
        }
        // z = w * A + r
        byte[][] initZ = matrixInitA.gf2eEncode(wInitCotReceiverOutput.getRbArray());
        IntStream.range(0, initN).forEach(index ->
            BytesUtils.xori(initZ[index], rInitMspCotReceiverOutput.getRb(index))
        );
        rCotReceiverOutput = CotReceiverOutput.create(initX, initZ);
        wCotReceiverOutput = rCotReceiverOutput.split(iterationK);
        preCotSize = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
        rCotReceiverOutput.reduce(preCotSize);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), extendInitTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 生成迭代矩阵A的种子
        byte[] matrixKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(matrixKey);
        List<byte[]> matrixKeyPayload = new LinkedList<>();
        matrixKeyPayload.add(matrixKey);
        DataPacketHeader matrixKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_ITERATION_LEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixKeyHeader, matrixKeyPayload));
        LocalLinearCoder matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKey, parallel);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Iter. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        stopWatch.start();
        // 执行MSP-COT
        MspCotReceiverOutput rMspCotReceiverOutput = mspCotReceiver.receive(iterationT, iterationN, rCotReceiverOutput);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Iter. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rTime);

        stopWatch.start();
        // x = u * A + e
        boolean[] x = matrixA.binaryEncode(wCotReceiverOutput.getChoices());
        for (int eIndex : rMspCotReceiverOutput.getAlphaArray()) {
            x[eIndex] = !x[eIndex];
        }
        // z = w * A + r
        byte[][] z = matrixA.gf2eEncode(wCotReceiverOutput.getRbArray());
        IntStream.range(0, iterationN).forEach(index ->
            BytesUtils.xori(z[index], rMspCotReceiverOutput.getRb(index))
        );
        // 更新输出
        CotReceiverOutput receiverOutput = CotReceiverOutput.create(x, z);
        wCotReceiverOutput = receiverOutput.split(iterationK);
        rCotReceiverOutput = receiverOutput.split(preCotSize);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Iter. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), extendTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
