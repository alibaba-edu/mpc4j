package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20.Ywl20ShSspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * semi-honest YWL20-SSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Ywl20ShSspCotReceiver extends AbstractSspCotReceiver {
    /**
     * SP-DPPRF config
     */
    private final SpDpprfConfig spDpprfConfig;
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * SP-DPPRF
     */
    private final SpDpprfReceiver spDpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * SP-DPPRF receiver output
     */
    private SpDpprfReceiverOutput spDpprfReceiverOutput;

    public Ywl20ShSspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20ShSspCotConfig config) {
        super(Ywl20ShSspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        spDpprfConfig = config.getSpDpprfConfig();
        spDpprfReceiver = SpDpprfFactory.createReceiver(receiverRpc, senderParty, spDpprfConfig);
        addSubPto(spDpprfReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxCotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, maxNum);
        coreCotReceiver.init(maxCotNum);
        spDpprfReceiver.init(maxNum);
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
        int cotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, num);
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
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        stopWatch.start();
        spDpprfReceiverOutput = spDpprfReceiver.puncture(alpha, num, cotReceiverOutput);
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
        spDpprfReceiverOutput = null;
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, correlateTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private SspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == 1);
        byte[] correlateByteArray = correlatePayload.get(0);
        byte[][] rbArray = spDpprfReceiverOutput.getPprfKeys();
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
