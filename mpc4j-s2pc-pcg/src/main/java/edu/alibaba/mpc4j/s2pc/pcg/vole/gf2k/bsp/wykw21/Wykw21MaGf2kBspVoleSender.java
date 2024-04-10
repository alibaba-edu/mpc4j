package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.AbstractGf2kBspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.wykw21.Wykw21MaGf2kBspVolePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;
import org.bouncycastle.crypto.Commitment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final BpDpprfReceiver bpDpprfReceiver;
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * random oracle
     */
    private final Prf randomOracle;
    /**
     * GF2K-VOLE sender output
     */
    private Gf2kVoleSenderOutput gf2kVoleSenderOutput;

    public Wykw21MaGf2kBspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21MaGf2kBspVoleConfig config) {
        super(Wykw21MaGf2kBspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        bpDpprfReceiver = BpDpprfFactory.createReceiver(senderRpc, receiverParty, config.getBpDpprfConfig());
        addSubPto(bpDpprfReceiver);
        commit = CommitFactory.createInstance(envType, secureRandom);
        randomOracle = PrfFactory.createInstance(envType, gf2k.getByteL());
    }

    @Override
    public void init(int maxBatchNum, int maxEachNum) throws MpcAbortException {
        setInitInput(maxBatchNum, maxEachNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPreVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, maxBatchNum, maxEachNum);
        gf2kCoreVoleSender.init(maxPreVoleNum);
        bpDpprfReceiver.init(maxBatchNum, maxEachNum);
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
        int preVoleNum = Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum);
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
        logStepInfo(PtoState.PTO_STEP, 1, 6, voleTime);

        stopWatch.start();
        // In the Extend phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S.
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R. Here we reuse β = a, δ = c. We reuse x as β.
        byte[][] betaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> gf2kVoleSenderOutput.getX(batchIndex))
            .peek(beta -> {
                assert !gf2k.isZero(beta);
            })
            .toArray(byte[][]::new);
        byte[][] littleDeltaArray = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> gf2kVoleSenderOutput.getT(batchIndex))
            .toArray(byte[][]::new);
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        BpDpprfReceiverOutput bpDpprfReceiverOutput = bpDpprfReceiver.puncture(alphaArray, eachNum);
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
                byte[][] ws = bpDpprfReceiverOutput.getSpDpprfReceiverOutput(batchIndex).getPprfKeys();
                ws[alpha] = d;
                for (int i = 0; i < eachNum; i++) {
                    if (i != alpha) {
                        gf2k.addi(ws[alpha], ws[i]);
                    }
                }
                gf2k.negi(ws[alpha]);
                gf2k.addi(ws[alpha], littleDelta);
                return Gf2kSspVoleSenderOutput.create(alpha, beta, ws);
            })
            .toArray(Gf2kSspVoleSenderOutput[]::new);
        Gf2kBspVoleSenderOutput senderOutput = Gf2kBspVoleSenderOutput.create(gf2kSspVoleSenderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        stopWatch.start();
        // In the Consistency check phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        byte[] x = gf2kVoleSenderOutput.getX(batchNum);
        byte[] z = gf2kVoleSenderOutput.getT(batchNum);
        gf2kVoleSenderOutput = null;
        // S samples χ_{i, j} for i ∈ [0, n), j ∈ [0, t), and extracts χ_{α, j}
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        randomOracle.setKey(seed);
        batchIntStream = IntStream.range(0, batchNum);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        byte[][] betaChiAlphas = new byte[batchNum][];
        byte[][][] chiArrays = batchIntStream
            .mapToObj(j -> {
                // generate χ_{i, j} for i ∈ [0, n)
                byte[][] chiArray = IntStream.range(0, eachNum)
                    .mapToObj(i -> {
                        byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + Integer.BYTES)
                            .putLong(extraInfo).putInt(j).putInt(i).array();
                        return randomOracle.getBytes(indexMessage);
                    })
                    .toArray(byte[][]::new);
                // compute β · χ_{{α, j}, j}
                byte[] beta = betaArray[j];
                int alpha = alphaArray[j];
                betaChiAlphas[j] = gf2k.mul(beta, chiArray[alpha]);
                return chiArray;
            })
            .toArray(byte[][][]::new);
        // S then computes x^* = Σ_{j ∈ [0, t)} {β · χ_{{α, j}, j}} - x
        byte[] xStar = gf2k.createZero();
        for (int j = 0; j < batchNum; j++) {
            gf2k.addi(xStar, betaChiAlphas[j]);
        }
        gf2k.subi(xStar, x);
        // S sends ({χ_{i, j}_{i ∈ [0, n), j ∈ [0, t)}, x^*) to R
        List<byte[]> xStarPayload = new LinkedList<>();
        xStarPayload.add(seed);
        xStarPayload.add(xStar);
        DataPacketHeader xStarHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED_X_STAR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xStarHeader, xStarPayload));
        // S computes V_A = Σ_{i ∈ [0, n)} (Σ_{j ∈ [0, t}} {χ_{i, j} · w_j[i]}) - z.
        IntStream indexIntStream = IntStream.range(0, eachNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] batchChiWs = indexIntStream
            .mapToObj(i -> {
                byte[] chiWs = gf2k.createZero();
                for (int j = 0; j < batchNum; j++) {
                    gf2k.addi(chiWs, gf2k.mul(chiArrays[j][i], gf2kSspVoleSenderOutputs[j].getT(i)));
                }
                return chiWs;
            })
            .toArray(byte[][]::new);
        byte[] va = gf2k.createZero();
        for (int i = 0; i < eachNum; i++) {
            gf2k.addi(va, batchChiWs[i]);
        }
        gf2k.subi(va, z);
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
