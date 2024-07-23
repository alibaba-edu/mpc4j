package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.AbstractGf2kSspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodePtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * APRR24 GF2K-SSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kSspVodeSender extends AbstractGf2kSspVodeSender {
    /**
     * core GF2K-VODE sender
     */
    private final Gf2kCoreVodeSender gf2kCoreVodeSender;
    /**
     * SP-DPPRF receiver
     */
    private final SpRdpprfReceiver spRdpprfReceiver;
    /**
     * GF2K-VODE sender output
     */
    private Gf2kVodeSenderOutput gf2kVodeSenderOutput;

    public Aprr24Gf2kSspVodeSender(Rpc senderRpc, Party receiverParty, Aprr24Gf2kSspVodeConfig config) {
        super(Aprr24Gf2kSspVodePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVodeSender = Gf2kCoreVodeFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVodeConfig());
        addSubPto(gf2kCoreVodeSender);
        spRdpprfReceiver = SpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfReceiver);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVodeSender.init(subfieldL);
        spRdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVodeSenderOutput send(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return send();
    }

    @Override
    public Gf2kSspVodeSenderOutput send(int alpha, int num, Gf2kVodeSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preSenderOutput);
        gf2kVodeSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kSspVodeSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, 1) to F_VODE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        int preVodeNum = Gf2kSspVodeFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVodeNum == 1;
        if (gf2kVodeSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVodeNum)
                .mapToObj(index -> subfield.createNonZeroRandom(secureRandom))
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
        byte[] a = gf2kVodeSenderOutput.getX(0);
        byte[] littleDelta = gf2kVodeSenderOutput.getT(0);
        byte[] beta = subfield.createNonZeroRandom(secureRandom);
        assert subfield.validateNonZeroElement(beta);
        byte[] aPrime = subfield.sub(beta, a);
        List<byte[]> aPrimePayload = Collections.singletonList(aPrime);
        sendOtherPartyPayload(PtoStep.SENDER_SENDS_A_PRIME.ordinal(), aPrimePayload);
        gf2kVodeSenderOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        SpRdpprfReceiverOutput spRdpprfReceiverOutput = spRdpprfReceiver.puncture(alpha, num);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, dpprfTime);

        List<byte[]> dPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal());

        stopWatch.start();
        // S defines w[i] = v_i for i ≠ α, and w[α] = δ - (d + Σ_{i ∈ [i ≠ α)} w[i])
        MpcAbortPreconditions.checkArgument(dPayload.size() == 1);
        byte[] d = dPayload.get(0);
        byte[][] ws = spRdpprfReceiverOutput.getV1Array();
        ws[alpha] = d;
        for (int i = 0; i < num; i++) {
            if (i != alpha) {
                field.addi(ws[alpha], ws[i]);
            }
        }
        field.negi(ws[alpha]);
        field.addi(ws[alpha], littleDelta);
        Gf2kSspVodeSenderOutput senderOutput = Gf2kSspVodeSenderOutput.create(field, alpha, beta, ws);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
