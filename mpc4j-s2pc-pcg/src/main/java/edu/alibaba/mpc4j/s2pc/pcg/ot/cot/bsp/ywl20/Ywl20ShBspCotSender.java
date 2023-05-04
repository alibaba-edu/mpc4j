package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSender;
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
    private final BpDpprfConfig bpDpprfConfig;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * DPPRF协议发送方
     */
    private final BpDpprfSender bpDpprfSender;
    /**
     * COT协议发送方输出
     */
    private CotSenderOutput cotSenderOutput;

    public Ywl20ShBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        bpDpprfConfig = config.getBpDpprfConfig();
        bpDpprfSender = BpDpprfFactory.createSender(senderRpc, receiverParty, bpDpprfConfig);
        addSubPtos(bpDpprfSender);
    }

    @Override
    public void init(byte[] delta, int maxBatchNum, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatchNum, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxCotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, maxBatchNum, maxNum);
        coreCotSender.init(delta, maxCotNum);
        bpDpprfSender.init(maxBatchNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
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
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int cotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, batchNum, num);
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
        } else {
            cotSenderOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        stopWatch.start();
        BpDpprfSenderOutput bpDpprfSenderOutput = bpDpprfSender.puncture(batchNum, num, cotSenderOutput);
        cotSenderOutput = null;
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, dpprfTime);

        stopWatch.start();
        byte[][] correlateByteArrays = new byte[batchNum][];
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                correlateByteArrays[batchIndex] = BytesUtils.clone(delta);
                // S sets v = (s_0^h,...,s_{n - 1}^h)
                byte[][] vs = bpDpprfSenderOutput.getSpDpprfSenderOutput(batchIndex).getPrfKeys();
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < num; i++) {
                    BytesUtils.xori(correlateByteArrays[batchIndex], vs[i]);
                }
                return SspCotSenderOutput.create(delta, vs);
            })
            .toArray(SspCotSenderOutput[]::new);
        List<byte[]> correlatePayload = Arrays.stream(correlateByteArrays).collect(Collectors.toList());
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        BspCotSenderOutput senderOutput = BspCotSenderOutput.create(senderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
