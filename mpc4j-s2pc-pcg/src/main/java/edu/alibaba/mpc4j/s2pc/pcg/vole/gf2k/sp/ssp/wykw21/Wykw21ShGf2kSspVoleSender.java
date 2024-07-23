package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.AbstractGf2kSspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21.Wykw21ShGf2kSspVolePtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * semi-honest WYKW21-SSP-GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2022/3/16
 */
public class Wykw21ShGf2kSspVoleSender extends AbstractGf2kSspVoleSender {
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender gf2kCoreVoleSender;
    /**
     * SP-DPPRF receiver
     */
    private final SpRdpprfReceiver spRdpprfReceiver;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21ShGf2kSspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21ShGf2kSspVoleConfig config) {
        super(Wykw21ShGf2kSspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        spRdpprfReceiver = SpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfReceiver);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVoleSender.init(subfieldL);
        spRdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVoleSenderOutput send(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return send();
    }

    @Override
    public Gf2kSspVoleSenderOutput send(int alpha, int num, Gf2kVoleSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preSenderOutput);
        gf2kVoleSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kSspVoleSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVoleNum == 1;
        if (gf2kVoleSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVoleNum)
                .mapToObj(index -> subfield.createNonZeroRandom(secureRandom))
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
        // In the Extend phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S.
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R.
        // Here we cannot reuse β = a, δ = c, x = β, because x can be 0.
        byte[] a = gf2kVoleSenderOutput.getX(0);
        byte[] littleDelta = gf2kVoleSenderOutput.getT(0);
        byte[] beta = subfield.createNonZeroRandom(secureRandom);
        assert subfield.validateNonZeroElement(beta);
        byte[] aPrime = subfield.sub(beta, a);
        List<byte[]> aPrimePayload = Collections.singletonList(aPrime);
        DataPacketHeader aPrimeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_A_PRIME.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(aPrimeHeader, aPrimePayload));
        gf2kVoleSenderOutput = null;
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

        DataPacketHeader dHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_D.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dPayload = rpc.receive(dHeader).getPayload();

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
        Gf2kSspVoleSenderOutput senderOutput = Gf2kSspVoleSenderOutput.create(field, alpha, beta, ws);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
