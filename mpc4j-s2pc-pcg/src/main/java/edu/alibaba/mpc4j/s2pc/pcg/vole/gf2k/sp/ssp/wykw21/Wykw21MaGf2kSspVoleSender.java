package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.AbstractGf2kSspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.wykw21.Wykw21MaGf2kSspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp.Gf2kSspVoleSenderOutput;
import org.bouncycastle.crypto.Commitment;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * malicious WYKW21-SSP-GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Wykw21MaGf2kSspVoleSender extends AbstractGf2kSspVoleSender {
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender gf2kCoreVoleSender;
    /**
     * SP-DPPRF receiver
     */
    private final SpRdpprfReceiver spRdpprfReceiver;
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21MaGf2kSspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21MaGf2kSspVoleConfig config) {
        super(Wykw21MaGf2kSspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        spRdpprfReceiver = SpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getSpDpprfConfig());
        addSubPto(spRdpprfReceiver);
        commit = CommitFactory.createInstance(envType, secureRandom);
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
        // We need to invoke F_VOLE two times, one for the Extend phase, one for the Consistency check phase
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, subfieldL, num);
        assert preVoleNum == 1 + r;
        if (gf2kVoleSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVoleNum)
                .mapToObj(i -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            gf2kVoleSenderOutput = gf2kCoreVoleSender.send(xs);
        } else {
            gf2kVoleSenderOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, voleTime);

        stopWatch.start();
        // In the Extend phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S.
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R.
        // Here we cannot reuse β = a, δ = c since a can be zero.
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
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        SpRdpprfReceiverOutput spRdpprfReceiverOutput = spRdpprfReceiver.puncture(alpha, num);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, dpprfTime);

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
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        stopWatch.start();
        // S samples χ_i for i ∈ [0, n), and extracts χ_α
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        DataPacketHeader seedHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedHeader, seedPayload));
        // In the Consistency check phase, S send (extend, r) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        byte[][] xs = IntStream.range(0, r)
            .mapToObj(h -> gf2kVoleSenderOutput.getX(1 + h))
            .toArray(byte[][]::new);
        byte[][] zs = IntStream.range(0, r)
            .mapToObj(h -> gf2kVoleSenderOutput.getT(1 + h))
            .toArray(byte[][]::new);
        gf2kVoleSenderOutput = null;
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] chiArray = indexIntStream
            .mapToObj(i -> {
                byte[] indexSeed = ByteBuffer
                    .allocate(CommonConstants.BLOCK_BYTE_LENGTH + Long.BYTES + Integer.BYTES)
                    .put(seed)
                    .putLong(extraInfo).putInt(i)
                    .array();
                return field.createRandom(indexSeed);
            })
            .toArray(byte[][]::new);
        byte[][] chiAlpha = field.decomposite(chiArray[alpha]);
        // S then computes x^* = β · χ_α - x
        byte[][] xStars = IntStream.range(0, r)
            .mapToObj(h -> {
                byte[] xStar = subfield.mul(beta, chiAlpha[h]);
                subfield.subi(xStar, xs[h]);
                return xStar;
            })
            .toArray(byte[][]::new);
        // S sends ({χ_i}_{i ∈ [0, n)}, x^*) to R
        List<byte[]> xStarsPayload = Arrays.stream(xStars).collect(Collectors.toList());
        DataPacketHeader xStarsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_X_STARS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xStarsHeader, xStarsPayload));
        // S computes V_A = Σ_{i = 0}^{n - 1} {χ_i · w[i]} - z.
        indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] chiWs = indexIntStream
            .mapToObj(i -> field.mul(chiArray[i], ws[i]))
            .toArray(byte[][]::new);
        byte[] va = field.createZero();
        for (int i = 0; i < num; i++) {
            field.addi(va, chiWs[i]);
        }
        byte[] z = field.innerProduct(zs);
        field.subi(va, z);
        stopWatch.stop();
        long vaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, vaTime, "S computes VA");

        stopWatch.start();
        // S receives commitment VB
        DataPacketHeader vbCommitmentBytesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_COMMIT_VB.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vbCommitmentBytesPayload = rpc.receive(vbCommitmentBytesHeader).getPayload();
        MpcAbortPreconditions.checkArgument(vbCommitmentBytesPayload.size() == 1);
        byte[] commitmentBytes = vbCommitmentBytesPayload.get(0);
        // S sends VA to R
        List<byte[]> vaPayload = Collections.singletonList(va);
        DataPacketHeader vaHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_VA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vaHeader, vaPayload));
        // S receives open VB
        DataPacketHeader vbCommitmentSecretHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_OPEN_VB.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vbCommitmentSecretPayload = rpc.receive(vbCommitmentSecretHeader).getPayload();
        MpcAbortPreconditions.checkArgument(vbCommitmentSecretPayload.size() == 1);
        byte[] commitmentSecret = vbCommitmentSecretPayload.get(0);
        Commitment vbCommitment = new Commitment(commitmentSecret, commitmentBytes);
        // S verifies V_A == V_B
        MpcAbortPreconditions.checkArgument(commit.isRevealed(va, vbCommitment));
        stopWatch.stop();
        long checkEqualTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, checkEqualTime, "S checks VA == VB");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
