package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.AbstractSspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20.Ywl20ShSspCotPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * semi-honest YWL20-SSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public class Ywl20ShSspCotSender extends AbstractSspCotSender {
    /**
     * SP-DPPRF config
     */
    private final SpDpprfConfig spDpprfConfig;
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * SP-DPPRF
     */
    private final SpDpprfSender spDpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Ywl20ShSspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShSspCotConfig config) {
        super(Ywl20ShSspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        spDpprfConfig = config.getSpDpprfConfig();
        spDpprfSender = SpDpprfFactory.createSender(senderRpc, receiverParty, spDpprfConfig);
        addSubPto(spDpprfSender);
    }

    @Override
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxCotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, maxNum);
        coreCotSender.init(delta, maxCotNum);
        spDpprfSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SspCotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        return send();
    }

    @Override
    public SspCotSenderOutput send(int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private SspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int cotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, num);
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
        SpDpprfSenderOutput spDpprfSenderOutput = spDpprfSender.puncture(num, cotSenderOutput);
        cotSenderOutput = null;
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, dpprfTime);

        stopWatch.start();
        byte[] correlateByteArray = BytesUtils.clone(delta);
        // S sets v = (s_0^h,...,s_{n - 1}^h)
        byte[][] vs = spDpprfSenderOutput.getPrfKeys();
        // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
        for (int i = 0; i < num; i++) {
            BytesUtils.xori(correlateByteArray, vs[i]);
        }
        SspCotSenderOutput senderOutput = SspCotSenderOutput.create(delta, vs);
        List<byte[]> correlatePayload = Collections.singletonList(correlateByteArray);
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
