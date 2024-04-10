package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

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
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotSenderOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * malicious YWL20-BSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaBspCotSender extends AbstractBspCotSender {
    /**
     * BP-DPPRF config
     */
    private final BpDpprfConfig bpDpprfConfig;
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * BP-DPPRF
     */
    private final BpDpprfSender bpDpprfSender;
    /**
     * GF(2^128) instance
     */
    private final Gf2k gf2k;
    /**
     * H': F_{2^κ} → {0,1}^{2κ} modeled as a random oracle.
     */
    private final Hash hash;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * check COT sender output
     */
    private CotSenderOutput checkCotSenderOutput;
    /**
     * random oracle
     */
    private Prf randomOracle;

    public Ywl20MaBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20MaBspCotConfig config) {
        super(Ywl20MaBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        bpDpprfConfig = config.getBpDpprfConfig();
        bpDpprfSender = BpDpprfFactory.createSender(senderRpc, receiverParty, bpDpprfConfig);
        addSubPto(bpDpprfSender);
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(byte[] delta, int maxBatchNum, int maxEachNum) throws MpcAbortException {
        setInitInput(delta, maxBatchNum, maxEachNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // we need to request COT two times, one for DPPRF, one for λ
        int maxCotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, maxBatchNum, maxEachNum)
            + CommonConstants.BLOCK_BIT_LENGTH;
        coreCotSender.init(delta, maxCotNum);
        bpDpprfSender.init(maxBatchNum, maxEachNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKeyPayload.remove(0));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int dpprfCotNum = BpDpprfFactory.getPrecomputeNum(bpDpprfConfig, batchNum, eachNum);
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(dpprfCotNum + CommonConstants.BLOCK_BIT_LENGTH);
        } else {
            cotSenderOutput.reduce(dpprfCotNum + CommonConstants.BLOCK_BIT_LENGTH);
        }
        CotSenderOutput extendCotSenderOutput = cotSenderOutput.split(dpprfCotNum);
        checkCotSenderOutput = cotSenderOutput.split(CommonConstants.BLOCK_BIT_LENGTH);
        cotSenderOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

        stopWatch.start();
        BpDpprfSenderOutput bpDpprfSenderOutput = bpDpprfSender.puncture(batchNum, eachNum, extendCotSenderOutput);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, dpprfTime);

        stopWatch.start();
        byte[][] correlateByteArrays = new byte[batchNum][];
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                correlateByteArrays[batchIndex] = BytesUtils.clone(delta);
                // S sets v = (s_0^h,...,s_{n - 1}^h)
                byte[][] vs = bpDpprfSenderOutput.getSpDpprfSenderOutput(batchIndex).getPrfKeys();
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < eachNum; i++) {
                    BytesUtils.xori(correlateByteArrays[batchIndex], vs[i]);
                }
                return SspCotSenderOutput.create(delta, vs);
            })
            .toArray(SspCotSenderOutput[]::new);
        List<byte[]> correlatePayload = Arrays.stream(correlateByteArrays).collect(Collectors.toList());
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        BspCotSenderOutput senderOutput = BspCotSenderOutput.create(senderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, outputTime);

        stopWatch.start();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> checkChoicePayload = rpc.receive(checkChoiceHeader).getPayload();
        // compute H'(V)
        List<byte[]> actualCheckValuePayload = handleCheckChoicePayload(senderOutput, checkChoicePayload);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(actualHashValueHeader, actualCheckValuePayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private List<byte[]> handleCheckChoicePayload(BspCotSenderOutput senderOutput, List<byte[]> checkChoicePayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(checkChoicePayload.size() == 1);
        byte[] xPrime = checkChoicePayload.remove(0);
        boolean[] xPrimeBinary = BinaryUtils.byteArrayToBinary(xPrime, CommonConstants.BLOCK_BIT_LENGTH);
        // S computes \vec{y} := \vec{y}^* + \vec{x}·∆, Y := Σ_{i ∈ [κ]} (y[i]·X^i) ∈ F_{2^κ}
        byte[] y = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int checkIndex = 0; checkIndex < CommonConstants.BLOCK_BIT_LENGTH; checkIndex++) {
            // y[i] = y[i]^* + x[i]·∆
            byte[] yi = checkCotSenderOutput.getR0(checkIndex);
            if (xPrimeBinary[checkIndex]) {
                BytesUtils.xori(yi, delta);
            }
            // y[i]·X^i
            byte[] xi = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            BinaryUtils.setBoolean(xi, checkIndex, true);
            gf2k.muli(yi, xi);
            // y += y[i]·X^i
            gf2k.addi(y, yi);
        }
        checkCotSenderOutput = null;
        // S computes V := Σ_{l ∈ [m]}(Σ_{i ∈ [n]} (χ[i]·v[i])) + Y ∈ F_{2^κ}
        IntStream lIntStream = IntStream.range(0, batchNum);
        lIntStream = parallel ? lIntStream.parallel() : lIntStream;
        byte[][] vs = lIntStream
            .mapToObj(l -> {
                byte[] v = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int i = 0; i < eachNum; i++) {
                    // samples uniform {χ_i}_{i ∈ [n]}
                    byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + Integer.BYTES)
                        .putLong(extraInfo).putInt(l).putInt(i).array();
                    byte[] chi = randomOracle.getBytes(indexMessage);
                    // χ[i]·v[i]
                    gf2k.muli(chi, senderOutput.get(l).getR0(i));
                    // v += χ[i]·v[i]
                    gf2k.addi(v, chi);
                }
                return v;
            })
            .toArray(byte[][]::new);
        // V := Σ_{l ∈ [m]} (χ[i]·v[i]) + Y
        byte[] v = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int l = 0; l < batchNum; l++) {
            gf2k.addi(v, vs[l]);
        }
        gf2k.addi(v, y);
        // H'(v)
        v = hash.digestToBytes(v);
        List<byte[]> hashValuePayload = new LinkedList<>();
        hashValuePayload.add(v);
        return hashValuePayload;
    }
}
