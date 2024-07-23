package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.AbstractGf2kSspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.aprr24.Aprr24Gf2kSspVodePtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * APRR24 GF2K-SSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kSspVodeReceiver extends AbstractGf2kSspVodeReceiver {
    /**
     * core GF2K-VODE receiver
     */
    private final Gf2kCoreVodeReceiver gf2kCoreVodeReceiver;
    /**
     * SP-DPPRF sender
     */
    private final SpRdpprfSender spRdpprfSender;
    /**
     * GF2K-VODE receiver output
     */
    private Gf2kVodeReceiverOutput gf2kVodeReceiverOutput;

    public Aprr24Gf2kSspVodeReceiver(Rpc receiverRpc, Party senderParty, Aprr24Gf2kSspVodeConfig config) {
        super(Aprr24Gf2kSspVodePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVodeReceiver = Gf2kCoreVodeFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVodeConfig());
        addSubPto(gf2kCoreVodeReceiver);
        spRdpprfSender = SpRdpprfFactory.createSender(receiverRpc, senderParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfSender);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVodeReceiver.init(subfieldL, delta);
        spRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVodeReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        return receive();
    }

    @Override
    public Gf2kSspVodeReceiverOutput receive(int num, Gf2kVodeReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(num, preReceiverOutput);
        gf2kVodeReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kSspVodeReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, 1) to F_VODE, which returns b ∈ {0,1}^κ to R
        int preVodeNum = Gf2kSspVodeFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVodeNum == 1;
        if (gf2kVodeReceiverOutput == null) {
            gf2kVodeReceiverOutput = gf2kCoreVodeReceiver.receive(preVodeNum);
        } else {
            gf2kVodeReceiverOutput.reduce(preVodeNum);
        }
        stopWatch.stop();
        long vodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, vodeTime);

        List<byte[]> aPrimePayload = receiveOtherPartyPayload(PtoStep.SENDER_SENDS_A_PRIME.ordinal());

        stopWatch.start();
        // R send (extend, 1) to F_VODE, which returns b ∈ {0,1}^κ to R
        // R computes γ = b - Δ · a'. Here we cannot reuse γ = b since x can be zero.
        MpcAbortPreconditions.checkArgument(aPrimePayload.size() == 1);
        byte[] aPrime = aPrimePayload.get(0);
        byte[] gamma = gf2kVodeReceiverOutput.getQ(0);
        field.subi(gamma, field.mixMul(aPrime, delta));
        gf2kVodeReceiverOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // R runs GGM to obtain ({v_j}_{j ∈ [0, n}), {(K_0^i, K_1^i)}_{i ∈ [h]}), and sets v[j] = v_j for j ∈ [0, n}.
        SpRdpprfSenderOutput spRdpprfSenderOutput = spRdpprfSender.puncture(num);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, dpprfTime);

        stopWatch.start();
        // R sends d = γ - Σ_{i ∈ [0, n)} v[i] to S
        byte[] d = field.createZero();
        byte[][] vs = spRdpprfSenderOutput.getV0Array();
        for (int i = 0; i < num; i++) {
            field.addi(d, vs[i]);
        }
        field.negi(d);
        field.addi(d, gamma);
        List<byte[]> dPayload = Collections.singletonList(d);
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal(), dPayload);
        Gf2kSspVodeReceiverOutput receiverOutput = Gf2kSspVodeReceiverOutput.create(field, delta, vs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
