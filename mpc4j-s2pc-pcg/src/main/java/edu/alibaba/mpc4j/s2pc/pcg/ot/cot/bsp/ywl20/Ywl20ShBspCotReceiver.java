package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotPtoDesc.PtoStep;

/**
 * YWL20-BSP-COT半诚实安全协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotReceiver extends AbstractBspCotReceiver {
    /**
     * DPPRF协议配置项
     */
    private final DpprfConfig dpprfConfig;
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * DPPRF协议接收方
     */
    private final DpprfReceiver dpprfReceiver;
    /**
     * COT协议接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * DPPRF接收方输出
     */
    private DpprfReceiverOutput dpprfReceiverOutput;

    public Ywl20ShBspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        dpprfConfig = config.getDpprfConfig();
        dpprfReceiver = DpprfFactory.createReceiver(receiverRpc, senderParty, dpprfConfig);
        dpprfReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和DPPRF协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        dpprfReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
        dpprfReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
        dpprfReceiver.addLogLevel();
    }

    @Override
    public void init(int maxBatchNum, int maxNum) throws MpcAbortException {
        setInitInput(maxBatchNum, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, maxBatchNum, maxNum);
        coreCotReceiver.init(maxCotNum);
        dpprfReceiver.init(maxBatchNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int num) throws MpcAbortException {
        setPtoInput(alphaArray, num);
        return receive();
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private BspCotReceiverOutput receive() throws MpcAbortException {
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int cotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, batchNum, num);
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        if (cotReceiverOutput == null) {
            boolean[] rs = new boolean[cotNum];
            IntStream.range(0, cotNum).forEach(index -> rs[index] = secureRandom.nextBoolean());
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        dpprfReceiverOutput = dpprfReceiver.puncture(alphaArray, num, cotReceiverOutput);
        cotReceiverOutput = null;
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), dpprfTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        BspCotReceiverOutput receiverOutput = generateReceiverOutput(correlatePayload);
        dpprfReceiverOutput = null;
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private BspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == batchNum);
        byte[][] correlateByteArrays = correlatePayload.toArray(new byte[0][]);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SspCotReceiverOutput[] sspCotReceiverOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[][] rbArray = dpprfReceiverOutput.getPprfOutputArray(batchIndex);
                // computes w[α]
                for (int i = 0; i < num; i++) {
                    if (i != alphaArray[batchIndex]) {
                        BytesUtils.xori(correlateByteArrays[batchIndex], rbArray[i]);
                    }
                }
                rbArray[alphaArray[batchIndex]] = correlateByteArrays[batchIndex];
                return SspCotReceiverOutput.create(alphaArray[batchIndex], rbArray);
            })
            .toArray(SspCotReceiverOutput[]::new);
        return BspCotReceiverOutput.create(sspCotReceiverOutputs);
    }
}
