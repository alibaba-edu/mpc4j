package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.AbstractGf2kBspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21.Wykw21MaGf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleReceiverOutput;
import org.bouncycastle.crypto.Commitment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * malicious WYKW21-BSP-GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21MaGf2kBspVoleReceiver extends AbstractGf2kBspVoleReceiver {
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver gf2kCoreVoleReceiver;
    /**
     * BP-DPPRF sender
     */
    private final BpRdpprfSender bpRdpprfSender;
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Wykw21MaGf2kBspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21MaGf2kBspVoleConfig config) {
        super(Wykw21MaGf2kBspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        bpRdpprfSender = BpRdpprfFactory.createSender(receiverRpc, senderParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfSender);
        commit = CommitFactory.createInstance(envType, secureRandom);
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
        // We need to invoke F_VOLE two times, one for the Extend phase, one for the Consistency check phase
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        assert preVoleNum == batchNum + r;
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, voleTime);

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
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // R runs GGM to obtain ({v_j}_{j ∈ [0, n}), {(K_0^i, K_1^i)}_{i ∈ [h]}), and sets v[j] = v_j for j ∈ [0, n}.
        BpRdpprfSenderOutput bpRdpprfSenderOutput = bpRdpprfSender.puncture(batchNum, eachNum);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, dpprfTime);

        stopWatch.start();
        // R sends d = γ - Σ_{i ∈ [0, n)} v[i] to S
        Gf2kSspVoleReceiverOutput[] gf2kSspVoleReceiverOutputs = new Gf2kSspVoleReceiverOutput[batchNum];
        IntStream batchIndexStream = IntStream.range(0, batchNum);
        batchIndexStream = parallel ? batchIndexStream.parallel() : batchIndexStream;
        List<byte[]> dsPayload = batchIndexStream
            .mapToObj(batchIndex -> {
                byte[] gamma = gammaArray[batchIndex];
                byte[] d = field.createZero();
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
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        stopWatch.start();
        DataPacketHeader seedHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> seedPayload = rpc.receive(seedHeader).getPayload();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        // R generates χ_i for i ∈ [0, n)
        byte[] seed = seedPayload.get(0);
        batchIndexStream = IntStream.range(0, batchNum);
        batchIndexStream = parallel ? batchIndexStream.parallel() : batchIndexStream;
        byte[][][] chiArrays = batchIndexStream
            .mapToObj(j -> IntStream.range(0, eachNum)
                .mapToObj(i -> {
                    byte[] indexSeed = ByteBuffer
                        .allocate(CommonConstants.BLOCK_BYTE_LENGTH + Long.BYTES + Integer.BYTES + Integer.BYTES)
                        .put(seed)
                        .putLong(extraInfo).putInt(j).putInt(i)
                        .array();
                    return field.createRandom(indexSeed);
                })
                .toArray(byte[][]::new)
            )
            .toArray(byte[][][]::new);
        // R computes y = y^* - Δ · x^*
        DataPacketHeader xStarsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_X_STARS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> xStarsPayload = rpc.receive(xStarsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(xStarsPayload.size() == r);
        byte[][] xStars = xStarsPayload.toArray(new byte[0][]);
        byte[][] ys = IntStream.range(0, r)
            .mapToObj(h -> {
                byte[] yStar = gf2kVoleReceiverOutput.getQ(batchNum + h);
                field.subi(yStar, field.mixMul(xStars[h], delta));
                return yStar;
            })
            .toArray(byte[][]::new);
        gf2kVoleReceiverOutput = null;
        // R computes VB = Σ_{i ∈ [0, n)}(Σ_{j ∈ [0, t)}{χ_{i, j} · v_j[i]}) - Y.
        IntStream indexIntStream = IntStream.range(0, eachNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] batchChiVs = indexIntStream
            .mapToObj(i -> {
                byte[] chiVs = field.createZero();
                for (int j = 0; j < batchNum; j++) {
                    field.addi(chiVs, field.mul(chiArrays[j][i], gf2kSspVoleReceiverOutputs[j].getQ(i)));
                }
                return chiVs;
            })
            .toArray(byte[][]::new);
        byte[] vb = field.createZero();
        for (int i = 0; i < eachNum; i++) {
            field.addi(vb, batchChiVs[i]);
        }
        byte[] y = field.innerProduct(ys);
        field.subi(vb, y);
        stopWatch.stop();
        long vbTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, vbTime, "S computes VB");

        stopWatch.start();
        // R commits VB
        Commitment vbCommitment = commit.commit(vb);
        List<byte[]> vbCommitmentBytesPayload = Collections.singletonList(vbCommitment.getCommitment());
        DataPacketHeader vbCommitmentBytesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_COMMIT_VB.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vbCommitmentBytesHeader, vbCommitmentBytesPayload));
        // R receives VA from S
        DataPacketHeader vaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_VA.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vaPayload = rpc.receive(vaHeader).getPayload();
        MpcAbortPreconditions.checkArgument(vaPayload.size() == 1);
        // R checks VA == VB
        MpcAbortPreconditions.checkArgument(BytesUtils.equals(vaPayload.get(0), vb));
        // R opens VB
        List<byte[]> vbCommitmentSecretPayload = Collections.singletonList(vbCommitment.getSecret());
        DataPacketHeader vbCommitmentSecretHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_OPEN_VB.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vbCommitmentSecretHeader, vbCommitmentSecretPayload));
        stopWatch.stop();
        long checkEqualTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, checkEqualTime, "R checks VA == VB");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
