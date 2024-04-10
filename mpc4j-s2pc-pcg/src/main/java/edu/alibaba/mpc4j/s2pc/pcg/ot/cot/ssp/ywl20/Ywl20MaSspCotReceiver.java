package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20.Ywl20MaSspCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * malicious YWL20-SSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Ywl20MaSspCotReceiver extends AbstractSspCotReceiver {
    /**
     * SP-DPPRF config
     */
    private final SpDpprfConfig spDpprfConfig;
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * SP-DPPRF
     */
    private final SpDpprfReceiver spDpprfReceiver;
    /**
     * GF(2^128) instance
     */
    private final Gf2k gf2k;
    /**
     * H': F_{2^κ} → {0,1}^{2κ} modeled as a random oracle.
     */
    private final Hash hash;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * check COT receiver output
     */
    private CotReceiverOutput checkCotReceiverOutput;
    /**
     * SP-DPPRF receiver output
     */
    private SpDpprfReceiverOutput spDpprfReceiverOutput;
    /**
     * random oracle
     */
    private Prf randomOracle;

    public Ywl20MaSspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20MaSspCotConfig config) {
        super(Ywl20MaSspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        spDpprfConfig = config.getSpDpprfConfig();
        spDpprfReceiver = SpDpprfFactory.createReceiver(receiverRpc, senderParty, spDpprfConfig);
        addSubPto(spDpprfReceiver);
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // we need to request COT two times, one for DPPRF, one for λ
        int maxCotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, maxNum) + CommonConstants.BLOCK_BIT_LENGTH;
        coreCotReceiver.init(maxCotNum);
        spDpprfReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        List<byte[]> randomOracleKeyPayload = new LinkedList<>();
        byte[] randomOracleKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomOracleKey);
        randomOracleKeyPayload.add(randomOracleKey);
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomOracleKeyHeader, randomOracleKeyPayload));
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKey);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return receive();
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alpha, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private SspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        int dpprfCotNum = SpDpprfFactory.getPrecomputeNum(spDpprfConfig, num);
        if (cotReceiverOutput == null) {
            boolean[] rs = new boolean[dpprfCotNum + CommonConstants.BLOCK_BIT_LENGTH];
            IntStream.range(0, dpprfCotNum + CommonConstants.BLOCK_BIT_LENGTH).forEach(index ->
                rs[index] = secureRandom.nextBoolean()
            );
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(dpprfCotNum + CommonConstants.BLOCK_BIT_LENGTH);
        }
        CotReceiverOutput extendCotReceiverOutput = cotReceiverOutput.split(dpprfCotNum);
        checkCotReceiverOutput = cotReceiverOutput.split(CommonConstants.BLOCK_BIT_LENGTH);
        cotReceiverOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

        stopWatch.start();
        spDpprfReceiverOutput = spDpprfReceiver.puncture(alpha, num, extendCotReceiverOutput);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, dpprfTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        SspCotReceiverOutput receiverOutput = generateReceiverOutput(correlatePayload);
        spDpprfReceiverOutput = null;
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, outputTime);

        stopWatch.start();
        List<byte[]> checkChoicePayload = generateCheckChoicePayload();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(checkChoiceHeader, checkChoicePayload));
        // locally compute H'(w), then receive H'(v)
        byte[] expectHashValue = computeExpectHashValue(receiverOutput);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> actualHashValuePayload = rpc.receive(actualHashValueHeader).getPayload();
        MpcAbortPreconditions.checkArgument(actualHashValuePayload.size() == 1);
        byte[] actualHashValue = actualHashValuePayload.remove(0);
        MpcAbortPreconditions.checkArgument(Arrays.equals(expectHashValue, actualHashValue));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private SspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == 1);
        byte[] correlateByteArray = correlatePayload.get(0);
        byte[][] rbArray = spDpprfReceiverOutput.getPprfKeys();
        // computes w[α]
        for (int i = 0; i < num; i++) {
            if (i != alpha) {
                BytesUtils.xori(correlateByteArray, rbArray[i]);
            }
        }
        rbArray[alpha] = correlateByteArray;
        return SspCotReceiverOutput.create(alpha, rbArray);
    }

    private List<byte[]> generateCheckChoicePayload() {
        // R computes ϕ := Σ_{i ∈ [m]} χ_{α_l}^l ∈ F_{2^κ}
        byte[] phi = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES).putLong(extraInfo).putInt(alpha).array();
        // Sample χ_α
        byte[] chiAlpha = randomOracle.getBytes(indexMessage);
        BytesUtils.xori(phi, chiAlpha);
        // R sends x' := x + x^* ∈ F_2^κ to S
        byte[] xStar = BinaryUtils.binaryToByteArray(checkCotReceiverOutput.getChoices());
        BytesUtils.xori(phi, xStar);
        return Collections.singletonList(phi);
    }

    private byte[] computeExpectHashValue(SspCotReceiverOutput receiverOutput) {
        // R computes Z :=  Σ_{i ∈ [κ]} (z^*[i]·X^i) ∈ F_{2^κ}
        byte[] z = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int checkIndex = 0; checkIndex < CommonConstants.BLOCK_BIT_LENGTH; checkIndex++) {
            byte[] zi = checkCotReceiverOutput.getRb(checkIndex);
            // z^*[i]·X^i
            byte[] xi = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            BinaryUtils.setBoolean(xi, checkIndex, true);
            gf2k.muli(zi, xi);
            // z += z^*[i]·X^i
            gf2k.addi(z, zi);
        }
        checkCotReceiverOutput = null;
        // R computes W := Σ_{i ∈ [n]} (χ[i]·w[i]) + Z ∈ F_{2^κ}
        byte[] w = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int i = 0; i < num; i++) {
            // samples uniform {χ_i}_{i ∈ [n]}
            byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES).putLong(extraInfo).putInt(i).array();
            byte[] chi = randomOracle.getBytes(indexMessage);
            // χ[i]·w[i]
            gf2k.muli(chi, receiverOutput.getRb(i));
            // w += χ[i]·w[i]
            BytesUtils.xori(w, chi);
        }
        // W := Σ_{i ∈ [n]} (χ[i]·w[i]) + Z
        gf2k.addi(w, z);
        // H'(w)
        return hash.digestToBytes(w);
    }
}
