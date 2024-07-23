package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.AbstractGf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21.Wykw21ShGf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * semi-honest WYKW21-BSP-GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21ShGf2kBspVoleReceiver extends AbstractGf2kBspVoleReceiver {
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver gf2kCoreVoleReceiver;
    /**
     * BP-DPPRF sender
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Wykw21ShGf2kBspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21ShGf2kBspVoleConfig config) {
        super(Wykw21ShGf2kBspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        bpRdpprfSender = BpRdpprfFactory.createSender(receiverRpc, senderParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfSender);
    }

    @Override
    public void init(int subfieldL, byte[] delta) throws MpcAbortException {
        setInitInput(subfieldL, delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVoleReceiver.init(subfieldL, delta);
        bpRdpprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return receive();
    }

    @Override
    public Gf2kBspVoleReceiverOutput receive(int batchNum, int eachNum, Gf2kVoleReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kBspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_A_PRIME_ARRAY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> aPrimePayload = rpc.receive(aPrimeHeader).getPayload();

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        // R computes γ = b - Δ · a'. Here we cannot reuse γ = b since x can be zero.
        MpcAbortPreconditions.checkArgument(aPrimePayload.size() == batchNum);
        byte[][] aPrimeArray = aPrimePayload.toArray(new byte[0][]);
        byte[][] gammaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[] gamma = gf2kVoleReceiverOutput.getQ(batchIndex);
                field.subi(gamma, field.mixMul(aPrimeArray[batchIndex], delta));
                return gamma;
            })
            .toArray(byte[][]::new);
        gf2kVoleReceiverOutput = null;
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
        Gf2kSspVoleReceiverOutput[] gf2kSspVoleReceiverOutputs = new Gf2kSspVoleReceiverOutput[batchNum];
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
                gf2kSspVoleReceiverOutputs[batchIndex] = Gf2kSspVoleReceiverOutput.create(field, delta, vs);
                return d;
            })
            .collect(Collectors.toList());
        DataPacketHeader dsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dsHeader, dsPayload));
        Gf2kBspVoleReceiverOutput receiverOutput = new Gf2kBspVoleReceiverOutput(gf2kSspVoleReceiverOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
