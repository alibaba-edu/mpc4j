package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotPtoDesc.PtoStep;

/**
 * YWL20-BSP-COT半诚实安全协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotSender extends AbstractBspCotSender {
    /**
     * DPPRF协议配置项
     */
    private final DpprfConfig dpprfConfig;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * DPPRF协议发送方
     */
    private final DpprfSender dpprfSender;
    /**
     * COT协议发送方输出
     */
    private CotSenderOutput cotSenderOutput;

    public Ywl20ShBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        dpprfConfig = config.getDpprfConfig();
        dpprfSender = DpprfFactory.createSender(senderRpc, receiverParty, dpprfConfig);
        dpprfSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和DPPRF协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        dpprfSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotSender.setParallel(parallel);
        dpprfSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotSender.addLogLevel();
        dpprfSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxBatchNum, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatchNum, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, maxBatchNum, maxNum);
        coreCotSender.init(delta, maxCotNum);
        dpprfSender.init(maxBatchNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int num) throws MpcAbortException {
        setPtoInput(batchNum, num);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int cotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, batchNum, num);
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
        } else {
            cotSenderOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        DpprfSenderOutput dpprfSenderOutput = dpprfSender.puncture(batchNum, num, cotSenderOutput);
        cotSenderOutput = null;
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), dpprfTime);

        stopWatch.start();
        byte[][] correlateByteArrays = new byte[batchNum][];
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                correlateByteArrays[batchIndex] = BytesUtils.clone(delta);
                // S sets v = (s_0^h,...,s_{n - 1}^h)
                byte[][] vs = dpprfSenderOutput.getPrfOutputArray(batchIndex);
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < num; i++) {
                    BytesUtils.xori(correlateByteArrays[batchIndex], vs[i]);
                }
                return SspCotSenderOutput.create(delta, vs);
            })
            .toArray(SspCotSenderOutput[]::new);
        List<byte[]> correlatePayload = Arrays.stream(correlateByteArrays).collect(Collectors.toList());
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        BspCotSenderOutput senderOutput = BspCotSenderOutput.create(senderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
