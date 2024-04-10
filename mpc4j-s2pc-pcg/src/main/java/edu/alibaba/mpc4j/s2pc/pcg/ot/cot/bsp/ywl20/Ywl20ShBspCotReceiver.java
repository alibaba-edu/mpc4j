package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20ShBspCotPtoDesc.PtoStep;

/**
 * semi-honest YWL20-BSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotReceiver extends AbstractBspCotReceiver {
    /**
     * BP-DPPRF config
     */
    private final BpDpprfConfig bpDpprfConfig;
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * BP-DPPRF
     */
    private final BpDpprfReceiver bpDpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * BP-DPPRF receiver output
     */
    private BpDpprfReceiverOutput bpDpprfReceiverOutput;

    public Ywl20ShBspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        bpDpprfConfig = config.getBpDpprfConfig();
        bpDpprfReceiver = BpDpprfFactory.createReceiver(receiverRpc, senderParty, bpDpprfConfig);
        addSubPto(bpDpprfReceiver);
    }

    @Override
    public void init(int maxBatchNum, int maxEachNum) throws MpcAbortException {
        setInitInput(maxBatchNum, maxEachNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxCotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, maxBatchNum, maxEachNum);
        coreCotReceiver.init(maxCotNum);
        bpDpprfReceiver.init(maxBatchNum, maxEachNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return receive();
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private BspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int cotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, batchNum, eachNum);
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
        bpDpprfReceiverOutput = bpDpprfReceiver.puncture(alphaArray, eachNum, cotReceiverOutput);
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
        BspCotReceiverOutput receiverOutput = generateReceiverOutput(correlatePayload);
        bpDpprfReceiverOutput = null;
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, correlateTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private BspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == batchNum);
        byte[][] correlateByteArrays = correlatePayload.toArray(new byte[0][]);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SspCotReceiverOutput[] sspCotReceiverOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[][] rbArray = bpDpprfReceiverOutput.getSpDpprfReceiverOutput(batchIndex).getPprfKeys();
                // computes w[α]
                for (int i = 0; i < eachNum; i++) {
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
