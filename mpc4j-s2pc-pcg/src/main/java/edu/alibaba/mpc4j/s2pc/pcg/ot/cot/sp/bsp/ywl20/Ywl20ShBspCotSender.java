package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

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
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20.Ywl20ShBspCotPtoDesc.PtoStep;

/**
 * semi-honest YWL20-BSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotSender extends AbstractBspCotSender {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * BP-RDPPRF
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Ywl20ShBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        bpRdpprfSender = BpRdpprfFactory.createSender(senderRpc, receiverParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfSender);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        bpRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
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
        BpRdpprfSenderOutput bpRdpprfSenderOutput = bpRdpprfSender.puncture(batchNum, eachNum, cotSenderOutput);
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
                byte[][] vs = bpRdpprfSenderOutput.get(batchIndex).getV0Array();
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < eachNum; i++) {
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
        BspCotSenderOutput senderOutput = new BspCotSenderOutput(senderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
