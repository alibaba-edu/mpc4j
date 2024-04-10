package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.AbstractGf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.Wykw21ShGf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleReceiverOutput;

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
    private final BpDpprfSender bpDpprfSender;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Wykw21ShGf2kBspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21ShGf2kBspVoleConfig config) {
        super(Wykw21ShGf2kBspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        bpDpprfSender = BpDpprfFactory.createSender(receiverRpc, senderParty, config.getBpDpprfConfig());
        addSubPto(bpDpprfSender);
    }

    @Override
    public void init(byte[] delta, int maxBatchNum, int maxEachNum) throws MpcAbortException {
        setInitInput(delta, maxBatchNum, maxEachNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPreVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, maxBatchNum, maxEachNum);
        gf2kCoreVoleReceiver.init(delta, maxPreVoleNum);
        bpDpprfSender.init(maxBatchNum, maxEachNum);
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
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum);
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, voleTime);

        stopWatch.start();
        // R computes γ = b - Δ · a'. Here we reuse γ = b
        byte[][] gammaArray = gf2kVoleReceiverOutput.getQ();
        gf2kVoleReceiverOutput = null;
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, aPrimeTime);

        stopWatch.start();
        // R runs GGM to obtain ({v_j}_{j ∈ [0, n}), {(K_0^i, K_1^i)}_{i ∈ [h]}), and sets v[j] = v_j for j ∈ [0, n}.
        BpDpprfSenderOutput bpDpprfSenderOutput = bpDpprfSender.puncture(batchNum, eachNum);
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
                byte[] d = gf2k.createZero();
                byte[] gamma = gammaArray[batchIndex];
                byte[][] vs = bpDpprfSenderOutput.getSpDpprfSenderOutput(batchIndex).getPrfKeys();
                for (int i = 0; i < eachNum; i++) {
                    gf2k.addi(d, vs[i]);
                }
                gf2k.negi(d);
                gf2k.addi(d, gamma);
                gf2kSspVoleReceiverOutputs[batchIndex] = Gf2kSspVoleReceiverOutput.create(delta, vs);
                return d;
            })
            .collect(Collectors.toList());
        DataPacketHeader dsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dsHeader, dsPayload));
        Gf2kBspVoleReceiverOutput receiverOutput = Gf2kBspVoleReceiverOutput.create(gf2kSspVoleReceiverOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
