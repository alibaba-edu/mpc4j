package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.AbstractGf2kBspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.aprr24.Aprr24Gf2kBspVodePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp.Gf2kSspVodeReceiverOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * APRR24 GF2K-BSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public class Aprr24Gf2kBspVodeReceiver extends AbstractGf2kBspVodeReceiver {
    /**
     * core GF2K-VODE receiver
     */
    private final Gf2kCoreVodeReceiver gf2kCoreVodeReceiver;
    /**
     * BP-DPPRF sender
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * GF2K-VODE receiver output
     */
    private Gf2kVodeReceiverOutput gf2kVodeReceiverOutput;

    public Aprr24Gf2kBspVodeReceiver(Rpc receiverRpc, Party senderParty, Aprr24Gf2kBspVodeConfig config) {
        super(Aprr24Gf2kBspVodePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVodeReceiver = Gf2kCoreVodeFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVodeConfig());
        addSubPto(gf2kCoreVodeReceiver);
        bpRdpprfSender = BpRdpprfFactory.createSender(receiverRpc, senderParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfSender);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVodeReceiver.init(subfieldL, delta);
        bpRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVodeReceiverOutput receive(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return receive();
    }

    @Override
    public Gf2kBspVodeReceiverOutput receive(int batchNum, int eachNum, Gf2kVodeReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preReceiverOutput);
        gf2kVodeReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kBspVodeReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        int preVodeNum = Gf2kBspVodeFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        if (gf2kVodeReceiverOutput == null) {
            gf2kVodeReceiverOutput = gf2kCoreVodeReceiver.receive(preVodeNum);
        } else {
            gf2kVodeReceiverOutput.reduce(preVodeNum);
        }
        stopWatch.stop();
        long vodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, vodeTime);

        List<byte[]> aPrimePayload = receiveOtherPartyPayload(PtoStep.SENDER_SENDS_A_PRIME_ARRAY.ordinal());

        stopWatch.start();
        // R send (extend, 1) to F_VODE, which returns b ∈ {0,1}^κ to R
        // R computes γ = b - Δ · a'. Here we cannot reuse γ = b since x can be zero.
        MpcAbortPreconditions.checkArgument(aPrimePayload.size() == batchNum);
        byte[][] aPrimeArray = aPrimePayload.toArray(new byte[0][]);
        byte[][] gammaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[] gamma = gf2kVodeReceiverOutput.getQ(batchIndex);
                field.subi(gamma, field.mixMul(aPrimeArray[batchIndex], delta));
                return gamma;
            })
            .toArray(byte[][]::new);
        gf2kVodeReceiverOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // R runs GGM to obtain ({v_j}_{j ∈ [0, n}), {(K_0^i, K_1^i)}_{i ∈ [h]}), and sets v[j] = v_j for j ∈ [0, n}.
        BpRdpprfSenderOutput bpRdpprfSenderOutput = bpRdpprfSender.puncture(batchNum, eachNum);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, dpprfTime);

        stopWatch.start();
        // R sends d = γ - Σ_{i ∈ [0, n)} v[i] to S
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        Gf2kSspVodeReceiverOutput[] gf2kSspVodeReceiverOutputs = new Gf2kSspVodeReceiverOutput[batchNum];
        List<byte[]> dsPayload = batchIntStream
            .mapToObj(batchIndex -> {
                byte[] d = field.createZero();
                byte[] gamma = gammaArray[batchIndex];
                byte[][] vs = bpRdpprfSenderOutput.get(batchIndex).getV0Array();
                for (int i = 0; i < eachNum; i++) {
                    field.addi(d, vs[i]);
                }
                field.negi(d);
                field.addi(d, gamma);
                gf2kSspVodeReceiverOutputs[batchIndex] = Gf2kSspVodeReceiverOutput.create(field, delta, vs);
                return d;
            })
            .collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_DS.ordinal(), dsPayload);
        Gf2kBspVodeReceiverOutput receiverOutput = new Gf2kBspVodeReceiverOutput(gf2kSspVodeReceiverOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
