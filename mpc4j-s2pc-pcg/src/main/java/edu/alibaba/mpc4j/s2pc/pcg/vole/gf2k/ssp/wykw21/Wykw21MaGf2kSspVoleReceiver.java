package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.commit.Commit;
import edu.alibaba.mpc4j.common.tool.crypto.commit.CommitFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.AbstractGf2kSspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.Gf2kSspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21.Wykw21MaGf2kSspVolePtoDesc.PtoStep;
import org.bouncycastle.crypto.Commitment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * malicious WYKW21-SSP-GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Wykw21MaGf2kSspVoleReceiver extends AbstractGf2kSspVoleReceiver {
    /**
     * core GF2K-VOLE receiver
     */
    private final Gf2kCoreVoleReceiver gf2kCoreVoleReceiver;
    /**
     * SP-DPPRF sender
     */
    private final SpDpprfSender spDpprfSender;
    /**
     * commitment scheme
     */
    private final Commit commit;
    /**
     * random oracle
     */
    private final Prf randomOracle;
    /**
     * GF2K-VOLE receiver output
     */
    private Gf2kVoleReceiverOutput gf2kVoleReceiverOutput;

    public Wykw21MaGf2kSspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21MaGf2kSspVoleConfig config) {
        super(Wykw21MaGf2kSspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getGf2kCoreVoleConfig());
        addSubPto(gf2kCoreVoleReceiver);
        spDpprfSender = SpDpprfFactory.createSender(receiverRpc, senderParty, config.getSpDpprfConfig());
        addSubPto(spDpprfSender);
        commit = CommitFactory.createInstance(envType, secureRandom);
        randomOracle = PrfFactory.createInstance(envType, gf2k.getByteL());
    }

    @Override
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPreVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, maxNum);
        gf2kCoreVoleReceiver.init(delta, maxPreVoleNum);
        spDpprfSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num) throws MpcAbortException {
        setPtoInput(num);
        return receive();
    }

    @Override
    public Gf2kSspVoleReceiverOutput receive(int num, Gf2kVoleReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(num, preReceiverOutput);
        gf2kVoleReceiverOutput = preReceiverOutput;
        return receive();
    }

    private Gf2kSspVoleReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // We need to invoke F_VOLE two times, one for the Extend phase, one for the Consistency check phase
        int preVoleNum = Gf2kSspVoleFactory.getPrecomputeNum(config, num);
        if (gf2kVoleReceiverOutput == null) {
            gf2kVoleReceiverOutput = gf2kCoreVoleReceiver.receive(preVoleNum);
        } else {
            gf2kVoleReceiverOutput.reduce(preVoleNum);
        }
        stopWatch.stop();
        long voleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, voleTime);

        stopWatch.start();
        // R send (extend, 1) to F_VOLE, which returns b ∈ {0,1}^κ to R
        // R computes γ = b - Δ · a'. Here we reuse γ = b
        byte[] gamma = gf2kVoleReceiverOutput.getQ(0);
        stopWatch.stop();
        long aPrimeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, aPrimeTime);

        stopWatch.start();
        // R runs GGM to obtain ({v_j}_{j ∈ [0, n}), {(K_0^i, K_1^i)}_{i ∈ [h]}), and sets v[j] = v_j for j ∈ [0, n}.
        SpDpprfSenderOutput spDpprfSenderOutput = spDpprfSender.puncture(num);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, dpprfTime);

        stopWatch.start();
        // R sends d = γ - Σ_{i ∈ [0, n)} v[i] to S
        byte[] d = gf2k.createZero();
        byte[][] vs = spDpprfSenderOutput.getPrfKeys();
        for (int i = 0; i < num; i++) {
            gf2k.addi(d, vs[i]);
        }
        gf2k.negi(d);
        gf2k.addi(d, gamma);
        List<byte[]> dPayload = Collections.singletonList(d);
        DataPacketHeader dHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_D.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dHeader, dPayload));
        Gf2kSspVoleReceiverOutput receiverOutput = Gf2kSspVoleReceiverOutput.create(delta, vs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, outputTime);

        DataPacketHeader xStarHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CHI_SEED_X_STAR.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> xStarPayload = rpc.receive(xStarHeader).getPayload();
        MpcAbortPreconditions.checkArgument(xStarPayload.size() == 2);

        stopWatch.start();
        // R generates χ_i for i ∈ [0, n)
        byte[] seed = xStarPayload.get(0);
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
        // R computes y = y^* - Δ · x^*
        byte[] y = BytesUtils.clone(gf2kVoleReceiverOutput.getQ(1));
        gf2k.subi(y, gf2k.mul(delta, xStarPayload.get(1)));
        gf2kVoleReceiverOutput = null;
        // R computes VB = Σ_{i = 0}^{n - 1} {χ_i · v[i]} - Y.
        indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        byte[][] chiVs = indexIntStream
            .mapToObj(i -> gf2k.mul(chiArray[i], vs[i]))
            .toArray(byte[][]::new);
        byte[] vb = gf2k.createZero();
        for (int i = 0; i < num; i++) {
            gf2k.addi(vb, chiVs[i]);
        }
        gf2k.subi(vb, y);
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
