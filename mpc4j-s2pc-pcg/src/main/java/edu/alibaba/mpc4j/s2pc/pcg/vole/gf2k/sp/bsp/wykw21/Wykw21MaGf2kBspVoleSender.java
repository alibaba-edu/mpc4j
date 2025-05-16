package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.AbstractGf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.Gf2kBspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp.wykw21.Wykw21MaGf2kBspVolePtoDesc.PtoStep;
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
 * malicious WYKW21-BSP-GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class Wykw21MaGf2kBspVoleSender extends AbstractGf2kBspVoleSender {
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender gf2kCoreVoleSender;
    /**
     * BP-DPPRF receiver
     */
    private final BpRdpprfReceiver bpRdpprfReceiver;
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21MaGf2kBspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21MaGf2kBspVoleConfig config) {
        super(Wykw21MaGf2kBspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        bpRdpprfReceiver = BpRdpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpDpprfConfig());
        addSubPto(bpRdpprfReceiver);
        commit = CommitFactory.createInstance(envType, secureRandom);
    }

    @Override
    public void init(int subfieldL) throws MpcAbortException {
        setInitInput(subfieldL);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        gf2kCoreVoleSender.init(subfieldL);
        bpRdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return send();
    }

    @Override
    public Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum, Gf2kVoleSenderOutput preSenderOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preSenderOutput);
        gf2kVoleSenderOutput = preSenderOutput;
        return send();
    }

    private Gf2kBspVoleSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // We need to invoke F_VOLE two times, one for the Extend phase, one for the Consistency check phase
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum);
        assert preVoleNum == batchNum + r;
        if (gf2kVoleSenderOutput == null) {
            byte[][] xs = IntStream.range(0, preVoleNum)
                .mapToObj(index -> subfield.createRandom(secureRandom))
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
        byte[][] aArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> gf2kVoleSenderOutput.getX(batchIndex))
            .toArray(byte[][]::new);
        byte[][] littleDeltaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> gf2kVoleSenderOutput.getT(batchIndex))
            .toArray(byte[][]::new);
        byte[][] betaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[] beta = subfield.createNonZeroRandom(secureRandom);
                assert subfield.validateNonZeroElement(beta);
                return beta;
            })
            .toArray(byte[][]::new);
        byte[][] aPrimeArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> subfield.sub(betaArray[batchIndex], aArray[batchIndex]))
            .toArray(byte[][]::new);
        List<byte[]> aPrimeArrayPayload = Arrays.stream(aPrimeArray).collect(Collectors.toList());
        DataPacketHeader aPrimesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_A_PRIME_ARRAY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(aPrimesHeader, aPrimeArrayPayload));
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        BpRdpprfReceiverOutput bpRdpprfReceiverOutput = bpRdpprfReceiver.puncture(alphaArray, eachNum);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, dpprfTime);

        DataPacketHeader dsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_DS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dsPayload = rpc.receive(dsHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(dsPayload.size() == batchNum);
        byte[][] ds = dsPayload.toArray(new byte[0][]);
        IntStream batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        Gf2kSspVoleSenderOutput[] gf2kSspVoleSenderOutputs = batchIntStream
            .mapToObj(batchIndex -> {
                // S defines w[i] = v_i for i ≠ α, and w[α] = δ - (d + Σ_{i ∈ [i ≠ α)} w[i])
                int alpha = alphaArray[batchIndex];
                byte[] d = ds[batchIndex];
                byte[] beta = betaArray[batchIndex];
                byte[] littleDelta = littleDeltaArray[batchIndex];
                byte[][] ws = bpRdpprfReceiverOutput.get(batchIndex).getV1Array();
                ws[alpha] = d;
                for (int i = 0; i < eachNum; i++) {
                    if (i != alpha) {
                        field.addi(ws[alpha], ws[i]);
                    }
                }
                field.negi(ws[alpha]);
                field.addi(ws[alpha], littleDelta);
                return Gf2kSspVoleSenderOutput.create(field, alpha, beta, ws);
            })
            .toArray(Gf2kSspVoleSenderOutput[]::new);
        Gf2kBspVoleSenderOutput senderOutput = new Gf2kBspVoleSenderOutput(gf2kSspVoleSenderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        stopWatch.start();
        // S samples χ_{i, j} for i ∈ [0, n), j ∈ [0, t), and extracts χ_{α, j}
        byte[] seed = BlockUtils.randomBlock(secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        DataPacketHeader seedHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedHeader, seedPayload));
        // In the Consistency check phase, S send (extend,r1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        byte[][] xs = IntStream.range(0, r)
            .mapToObj(h -> gf2kVoleSenderOutput.getX(batchNum + h))
            .toArray(byte[][]::new);
        byte[][] zs = IntStream.range(0, r)
            .mapToObj(h -> gf2kVoleSenderOutput.getT(batchNum + h))
            .toArray(byte[][]::new);
        gf2kVoleSenderOutput = null;
        batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][][] chiAlphaArray = new byte[batchNum][][];
        byte[][][] chiArrays = batchIntStream
            .mapToObj(j -> {
                // generate χ_{i, j} for i ∈ [0, n)
                byte[][] chiArray = IntStream.range(0, eachNum)
                    .mapToObj(i -> {
                        byte[] indexSeed = ByteBuffer
                            .allocate(CommonConstants.BLOCK_BYTE_LENGTH + Long.BYTES + Integer.BYTES + Integer.BYTES)
                            .put(seed)
                            .putLong(extraInfo).putInt(j).putInt(i)
                            .array();
                        return field.createRandom(indexSeed);
                    })
                    .toArray(byte[][]::new);
                int alpha = alphaArray[j];
                chiAlphaArray[j] = field.decomposite(chiArray[alpha]);
                return chiArray;
            })
            .toArray(byte[][][]::new);
        // S then computes x^* = Σ_{j ∈ [0, t)} {β · χ_{{α, j}, j}} - x
        byte[][] xStars = IntStream.range(0, r)
            .mapToObj(h -> {
                byte[] xStar = subfield.createZero();
                for (int j = 0; j < batchNum; j++) {
                    subfield.addi(xStar, subfield.mul(betaArray[j], chiAlphaArray[j][h]));
                }
                subfield.subi(xStar, xs[h]);
                return xStar;
            })
            .toArray(byte[][]::new);
        // S sends x^* to R
        List<byte[]> xStarsPayload = Arrays.stream(xStars).collect(Collectors.toList());
        DataPacketHeader xStarsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_X_STARS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xStarsHeader, xStarsPayload));
        // S computes V_A = Σ_{i ∈ [0, n)} (Σ_{j ∈ [0, t}} {χ_{i, j} · w_j[i]}) - z.
        IntStream indexIntStream = IntStream.range(0, eachNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] batchChiWs = indexIntStream
            .mapToObj(i -> {
                byte[] chiWs = field.createZero();
                for (int j = 0; j < batchNum; j++) {
                    field.addi(chiWs, field.mul(chiArrays[j][i], gf2kSspVoleSenderOutputs[j].getT(i)));
                }
                return chiWs;
            })
            .toArray(byte[][]::new);
        byte[] va = field.createZero();
        for (int i = 0; i < eachNum; i++) {
            field.addi(va, batchChiWs[i]);
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
