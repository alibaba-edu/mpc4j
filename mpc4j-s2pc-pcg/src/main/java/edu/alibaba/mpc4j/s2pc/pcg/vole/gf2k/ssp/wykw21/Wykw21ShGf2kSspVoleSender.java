package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.AbstractGf2kSspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21.Wykw21ShGf2kSspVolePtoDesc.PtoStep;

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
    private final SpDpprfReceiver spDpprfReceiver;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21ShGf2kSspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21ShGf2kSspVoleConfig config) {
        super(Wykw21ShGf2kSspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        spDpprfReceiver = SpDpprfFactory.createReceiver(senderRpc, receiverParty, config.getSpDpprfConfig());
        addSubPto(spDpprfReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPreVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, maxNum);
        gf2kCoreVoleSender.init(maxPreVoleNum);
        spDpprfReceiver.init(maxNum);
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
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, num);
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
        byte[] beta = gf2kVoleSenderOutput.getX(0);
        assert !gf2k.isZero(beta);
        byte[] littleDelta = gf2kVoleSenderOutput.getT(0);
        gf2kVoleSenderOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        SpDpprfReceiverOutput spDpprfReceiverOutput = spDpprfReceiver.puncture(alpha, num);
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
        byte[][] ws = spDpprfReceiverOutput.getPprfKeys();
        ws[alpha] = d;
        for (int i = 0; i < num; i++) {
            if (i != alpha) {
                gf2k.addi(ws[alpha], ws[i]);
            }
        }
        gf2k.negi(ws[alpha]);
        gf2k.addi(ws[alpha], littleDelta);
        Gf2kSspVoleSenderOutput senderOutput = Gf2kSspVoleSenderOutput.create(alpha, beta, ws);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
