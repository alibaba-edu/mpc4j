package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.Ywl20ShSspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * semi-honest YWL20-SSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Ywl20ShSspCotReceiver extends AbstractSspCotReceiver {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * SP-DPPRF
     */
    private final SpRdpprfReceiver spRdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * SP-DPPRF receiver output
     */
    private SpRdpprfReceiverOutput spRdpprfReceiverOutput;

    public Ywl20ShSspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20ShSspCotConfig config) {
        super(Ywl20ShSspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        spRdpprfReceiver = SpRdpprfFactory.createReceiver(receiverRpc, senderParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        spRdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return receive();
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private SspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        if (cotReceiverOutput == null) {
            boolean[] rs = BinaryUtils.randomBinary(cotNum, secureRandom);
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        stopWatch.start();
        spRdpprfReceiverOutput = spRdpprfReceiver.puncture(alpha, num, cotReceiverOutput);
        cotReceiverOutput = null;
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, dpprfTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        SspCotReceiverOutput receiverOutput = generateReceiverOutput(correlatePayload);
        spRdpprfReceiverOutput = null;
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, correlateTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private SspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == 1);
        byte[] correlateByteArray = correlatePayload.get(0);
        byte[][] rbArray = spRdpprfReceiverOutput.getV1Array();
        // computes w[α]
        for (int i = 0; i < num; i++) {
            if (i != alpha) {
                BytesUtils.xori(correlateByteArray, rbArray[i]);
            }
        }
        rbArray[alpha] = correlateByteArray;
        return SspCotReceiverOutput.create(alpha, rbArray);
    }
}
