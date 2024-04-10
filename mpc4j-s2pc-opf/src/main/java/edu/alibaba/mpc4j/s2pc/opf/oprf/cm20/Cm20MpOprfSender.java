package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CM20-MP-OPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfSender extends AbstractMpOprfSender {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * n = max(2, batchSize)
     */
    private int n;
    /**
     * n in byte
     */
    private int nByteLength;
    /**
     * PRF output bit length (w)
     */
    private int w;
    /**
     * choices bits
     */
    private boolean[] s;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * matrix C, organized by columns
     */
    private byte[][] matrixC;

    public Cm20MpOprfSender(Rpc senderRpc, Party receiverParty, Cm20MpOprfConfig config) {
        super(Cm20MpOprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxW = Cm20MpOprfPtoDesc.getW(Math.max(maxBatchSize, maxPrfNum));
        coreCotReceiver.init(maxW);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException {
        setPtoInput(batchSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        nByteLength = CommonUtils.getByteLength(n);
        w = Cm20MpOprfPtoDesc.getW(n);
        s = new boolean[w];
        IntStream.range(0, w).forEach(index -> s[index] = secureRandom.nextBoolean());
        cotReceiverOutput = coreCotReceiver.receive(s);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime, "COT");

        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        byte[] prfKey = prfKeyPayload.get(0);
        stopWatch.stop();
        long prfKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, prfKeyTime, "Sender receives PRF Key");

        DataPacketHeader deltaHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DELTA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> deltaPayload = rpc.receive(deltaHeader).getPayload();

        stopWatch.start();
        handleDeltaPayload(deltaPayload);
        Cm20MpOprfSenderOutput senderOutput = new Cm20MpOprfSenderOutput(envType, batchSize, w, prfKey, matrixC);
        matrixC = null;
        stopWatch.stop();
        long deltaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, deltaTime, "Sender generates OPRF");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    @Override
    protected void setPtoInput(int batchSize) {
        super.setPtoInput(batchSize);
        // n = max(2, batchSize)
        n = batchSize == 1 ? 2 : batchSize;
    }

    private void handleDeltaPayload(List<byte[]> deltaPayload) {
        byte[][] deltaArray = deltaPayload.toArray(new byte[0][]);
        Prg prg = PrgFactory.createInstance(envType, nByteLength);
        IntStream wIntStream = IntStream.range(0, w);
        wIntStream = parallel ? wIntStream.parallel() : wIntStream;
        matrixC = wIntStream.mapToObj(index -> {
            // We do not need to use CRHF since we need to call PRG.
            byte[] cColumn = prg.extendToBytes(cotReceiverOutput.getRb(index));
            BytesUtils.reduceByteArray(cColumn, n);
            if (s[index]) {
                BytesUtils.xori(cColumn, deltaArray[index]);
            }
            return cColumn;
        }).toArray(byte[][]::new);
        cotReceiverOutput = null;
    }
}
