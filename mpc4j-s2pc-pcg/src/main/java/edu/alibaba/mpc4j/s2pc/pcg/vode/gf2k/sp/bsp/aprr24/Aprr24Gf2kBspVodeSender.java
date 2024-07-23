package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.AbstractGf2kBspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * APRR24 BSP-GF2K-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kBspVodeSender extends AbstractGf2kBspVodeSender {
    /**
     * core GF2K-VODE sender
     */
    private final Gf2kCoreVodeSender gf2kCoreVodeSender;
    /**
     * BP-DPPRF receiver
     */
    private final BpRdpprfReceiver bpRdpprfReceiver;
    /**
     * GF2K-VODE sender output
     */
    private Gf2kVodeSenderOutput gf2kVodeSenderOutput;

    public Aprr24Gf2kBspVodeSender(Rpc senderRpc, Party receiverParty, Aprr24Gf2kBspVodeConfig config) {
        super(Aprr24Gf2kBspVodePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVodeSender = Gf2kCoreVodeFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVodeConfig());
        addSubPto(gf2kCoreVodeSender);
        bpRdpprfReceiver = BpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfReceiver);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVodeSender.init(subfieldL);
        bpRdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVodeSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return send();
    }

    @Override
    public Gf2kBspVodeSenderOutput send(int[] alphaArray, int eachNum, Gf2kVodeSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preSenderOutput);
        gf2kVodeSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kBspVodeSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, 1) to F_VODE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        int preVodeNum = Gf2kBspVodeFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        if (gf2kVodeSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVodeNum)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            gf2kVodeSenderOutput = gf2kCoreVodeSender.send(xs);
        } else {
            gf2kVodeSenderOutput.reduce(preVodeNum);
        }
        stopWatch.stop();
        long vodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, vodeTime);

        stopWatch.start();
        // In the Extend phase, S send (extend, 1) to F_VODE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S.
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R.
        // Here we cannot reuse β = a, δ = c, x = β, because x can be 0.
        byte[][] aArray = gf2kVodeSenderOutput.getX();
        byte[][] littleDeltaArray = gf2kVodeSenderOutput.getT();
        byte[][] betaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[] beta = subfield.createNonZeroRandom(secureRandom);
                assert subfield.validateNonZeroElement(beta);
                return beta;
            })
            .toArray(byte[][]::new);
        byte[][] aPrimeArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> subfield.sub(betaArray[batchIndex], aArray[batchIndex]))
            .toArray(byte[][]::new);
        List<byte[]> aPrimesPayload = Arrays.stream(aPrimeArray).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SENDER_SENDS_A_PRIME_ARRAY.ordinal(), aPrimesPayload);
        gf2kVodeSenderOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        BpRdpprfReceiverOutput bpRdpprfReceiverOutput = bpRdpprfReceiver.puncture(alphaArray, eachNum);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, dpprfTime);

        List<byte[]> dsPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_DS.ordinal());

        stopWatch.start();
        // S defines w[i] = v_i for i ≠ α, and w[α] = δ - (d + Σ_{i ∈ [i ≠ α)} w[i])
        MpcAbortPreconditions.checkArgument(dsPayload.size() == batchNum);
        byte[][] ds = dsPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        Gf2kSspVodeSenderOutput[] gf2kSspVodeSenderOutputs = batchIntStream
            .mapToObj(batchIndex -> {
                int alpha = alphaArray[batchIndex];
                byte[] d = ds[batchIndex];
                byte[] beta = betaArray[batchIndex];
                byte[] littleDelta = littleDeltaArray[batchIndex];
                byte[][] ws = bpRdpprfReceiverOutput.get(batchIndex).getV1Array();
                ws[alpha] = d;
                for (int i = 0; i < eachNum; i++) {
                    if (i != alpha) {
                        field.addi(ws[alpha], ws[i]);
                    }
                }
                field.negi(ws[alpha]);
                field.addi(ws[alpha], littleDelta);
                return Gf2kSspVodeSenderOutput.create(field, alpha, beta, ws);
            })
            .toArray(Gf2kSspVodeSenderOutput[]::new);
        Gf2kBspVodeSenderOutput senderOutput = new Gf2kBspVodeSenderOutput(gf2kSspVodeSenderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
