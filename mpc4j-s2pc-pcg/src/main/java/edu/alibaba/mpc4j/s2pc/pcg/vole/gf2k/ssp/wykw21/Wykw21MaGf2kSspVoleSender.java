package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.AbstractGf2kSspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21.Wykw21MaGf2kSspVolePtoDesc.PtoStep;
import org.bouncycastle.crypto.Commitment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final SpDpprfReceiver spDpprfReceiver;
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

    public Wykw21MaGf2kSspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21MaGf2kSspVoleConfig config) {
        super(Wykw21MaGf2kSspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleSender);
        spDpprfReceiver = SpDpprfFactory.createReceiver(senderRpc, receiverParty, config.getSpDpprfConfig());
        addSubPto(spDpprfReceiver);
        commit = CommitFactory.createInstance(envType, secureRandom);
        randomOracle = PrfFactory.createInstance(envType, gf2k.getByteL());
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
        // We need to invoke F_VOLE two times, one for the Extend phase, one for the Consistency check phase
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
        logStepInfo(PtoState.PTO_STEP, 1, 6, voleTime);

        stopWatch.start();
        // In the Extend phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S.
        // S sample β ∈ {0,1}^κ, sets δ = c, and sends a' = β - a to R. Here we reuse β = a, δ = c. We reuse x as β.
        byte[] beta = gf2kVoleSenderOutput.getX(0);
        assert !gf2k.isZero(beta);
        byte[] littleDelta = gf2kVoleSenderOutput.getT(0);
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // S runs GGM to obtain {v_j}_{j ≠ α)
        SpDpprfReceiverOutput spDpprfReceiverOutput = spDpprfReceiver.puncture(alpha, num);
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
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        stopWatch.start();
        // In the Consistency check phase, S send (extend, 1) to F_VOLE, which returns (x, t) ∈ {0,1}^κ × {0,1}^κ to S
        byte[] x = gf2kVoleSenderOutput.getX(1);
        byte[] z = gf2kVoleSenderOutput.getT(1);
        gf2kVoleSenderOutput = null;
        // S samples χ_i for i ∈ [0, n), and extracts χ_α
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        randomOracle.setKey(seed);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] chiArray = indexIntStream
            .mapToObj(i -> {
                byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                    .putLong(extraInfo).putInt(i).array();
                return randomOracle.getBytes(indexMessage);
            })
            .toArray(byte[][]::new);
        // S then computes x^* = β · χ_α - x
        byte[] xStar = gf2k.mul(beta, chiArray[alpha]);
        gf2k.subi(xStar, x);
        // S sends ({χ_i}_{i ∈ [0, n)}, x^*) to R
        List<byte[]> xStarPayload = new LinkedList<>();
        xStarPayload.add(seed);
        xStarPayload.add(xStar);
        DataPacketHeader xStarHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED_X_STAR.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(xStarHeader, xStarPayload));
        // S computes V_A = Σ_{i = 0}^{n - 1} {χ_i · w[i]} - z.
        indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] chiWs = indexIntStream
            .mapToObj(i -> gf2k.mul(chiArray[i], ws[i]))
            .toArray(byte[][]::new);
        byte[] va = gf2k.createZero();
        for (int i = 0; i < num; i++) {
            gf2k.addi(va, chiWs[i]);
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
