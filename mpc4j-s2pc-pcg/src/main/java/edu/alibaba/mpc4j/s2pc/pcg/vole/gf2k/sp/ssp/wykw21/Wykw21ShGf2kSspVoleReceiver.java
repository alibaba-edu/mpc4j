package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.AbstractGf2kSspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21.Wykw21ShGf2kSspVolePtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * semi-honest WYKW21-SSP-GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21ShGf2kSspVoleReceiver extends AbstractGf2kSspVoleReceiver {
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver gf2kCoreVoleReceiver;
    /**
     * SP-DPPRF sender
     */
    private final SpRdpprfSender spRdpprfSender;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Wykw21ShGf2kSspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21ShGf2kSspVoleConfig config) {
        super(Wykw21ShGf2kSspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        spRdpprfSender = SpRdpprfFactory.createSender(receiverRpc, senderParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfSender);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVoleReceiver.init(subfieldL, delta);
        spRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        return receive();
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num, Gf2kVoleReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(num, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kSspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVoleNum == 1;
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, voleTime);

        DataPacketHeader aPrimeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_A_PRIME.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> aPrimePayload = rpc.receive(aPrimeHeader).getPayload();

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        // R computes γ = b - Δ · a'. Here we cannot reuse γ = b since x can be zero.
        MpcAbortPreconditions.checkArgument(aPrimePayload.size() == 1);
        byte[] aPrime = aPrimePayload.get(0);
        byte[] gamma = gf2kVoleReceiverOutput.getQ(0);
        field.subi(gamma, field.mixMul(aPrime, delta));
        gf2kVoleReceiverOutput = null;
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
        DataPacketHeader dHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_D.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dHeader, dPayload));
        Gf2kSspVoleReceiverOutput receiverOutput = Gf2kSspVoleReceiverOutput.create(field, delta, vs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
