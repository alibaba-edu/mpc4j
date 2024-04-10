package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.AbstractGf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.Wykw21ShGf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * semi-honest WYKW21-BSP-GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21ShGf2kBspVoleSender extends AbstractGf2kBspVoleSender {
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender gf2kCoreVoleSender;
    /**
     * BP-DPPRF receiver
     */
    private final BpDpprfReceiver bpDpprfReceiver;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21ShGf2kBspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21ShGf2kBspVoleConfig config) {
        super(Wykw21ShGf2kBspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        bpDpprfReceiver = BpDpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpDpprfConfig());
        addSubPto(bpDpprfReceiver);
    }

    @Override
    public void init(int maxBatchNum, int maxEachNum) throws MpcAbortException {
        setInitInput(maxBatchNum, maxEachNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPreVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, maxBatchNum, maxEachNum);
        gf2kCoreVoleSender.init(maxPreVoleNum);
        bpDpprfReceiver.init(maxBatchNum, maxEachNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return send();
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum, Gf2kVoleSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preSenderOutput);
        gf2kVoleSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kBspVoleSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum);
        if (gf2kVoleSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVoleNum)
                .mapToObj(index -> gf2k.createRandom(secureRandom))
                .toArray(byte[][]::new);
            gf2kVoleSenderOutput = gf2kCoreVoleSender.send(xs);
        } else {
            gf2kVoleSenderOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, voleTime);

        stopWatch.start();
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R. Here we reuse β = a, δ = c.
        byte[][] betaArray = gf2kVoleSenderOutput.getX();
        Arrays.stream(betaArray).forEach(beta -> {
            assert !gf2k.isZero(beta);
        });
        byte[][] littleDeltaArray = gf2kVoleSenderOutput.getT();
        gf2kVoleSenderOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        BpDpprfReceiverOutput bpDpprfReceiverOutput = bpDpprfReceiver.puncture(alphaArray, eachNum);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, dpprfTime);

        DataPacketHeader dsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dsPayload = rpc.receive(dsHeader).getPayload();

        stopWatch.start();
        // S defines w[i] = v_i for i ≠ α, and w[α] = δ - (d + Σ_{i ∈ [i ≠ α)} w[i])
        MpcAbortPreconditions.checkArgument(dsPayload.size() == batchNum);
        byte[][] ds = dsPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        Gf2kSspVoleSenderOutput[] gf2kSspVoleSenderOutputs = batchIntStream
            .mapToObj(batchIndex -> {
                int alpha = alphaArray[batchIndex];
                byte[] d = ds[batchIndex];
                byte[] beta = betaArray[batchIndex];
                byte[] littleDelta = littleDeltaArray[batchIndex];
                byte[][] ws = bpDpprfReceiverOutput.getSpDpprfReceiverOutput(batchIndex).getPprfKeys();
                ws[alpha] = d;
                for (int i = 0; i < eachNum; i++) {
                    if (i != alpha) {
                        gf2k.addi(ws[alpha], ws[i]);
                    }
                }
                gf2k.negi(ws[alpha]);
                gf2k.addi(ws[alpha], littleDelta);
                return Gf2kSspVoleSenderOutput.create(alpha, beta, ws);
            })
            .toArray(Gf2kSspVoleSenderOutput[]::new);
        Gf2kBspVoleSenderOutput senderOutput = Gf2kBspVoleSenderOutput.create(gf2kSspVoleSenderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
