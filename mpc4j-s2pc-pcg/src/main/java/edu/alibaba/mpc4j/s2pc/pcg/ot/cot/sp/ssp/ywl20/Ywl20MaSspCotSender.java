package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

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
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20.Ywl20MaSspCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * malicious YWL20-SSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
public class Ywl20MaSspCotSender extends AbstractSspCotSender {
    /**
     * SP-DPPRF config
     */
    private final SpRdpprfConfig spRdpprfConfig;
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * SP-DPPRF
     */
    private final SpRdpprfSender spRdpprfSender;
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

    public Ywl20MaSspCotSender(Rpc senderRpc, Party receiverParty, Ywl20MaSspCotConfig config) {
        super(Ywl20MaSspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        spRdpprfConfig = config.getSpDpprfConfig();
        spRdpprfSender = SpRdpprfFactory.createSender(senderRpc, receiverParty, spRdpprfConfig);
        addSubPto(spRdpprfSender);
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        spRdpprfSender.init();
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
    public SspCotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        return send();
    }

    @Override
    public SspCotSenderOutput send(int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private SspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
        } else {
            cotSenderOutput.reduce(cotNum);
        }
        int dpprfCotNum = SpRdpprfFactory.getPrecomputeNum(spRdpprfConfig, num);
        CotSenderOutput extendCotSenderOutput = cotSenderOutput.split(dpprfCotNum);
        checkCotSenderOutput = cotSenderOutput.split(CommonConstants.BLOCK_BIT_LENGTH);
        cotSenderOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, cotTime);

        stopWatch.start();
        SpRdpprfSenderOutput spRdpprfSenderOutput = spRdpprfSender.puncture(num, extendCotSenderOutput);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, dpprfTime);

        stopWatch.start();
        byte[] correlateByteArray = BlockUtils.zeroBlock();
        BlockUtils.xori(correlateByteArray, delta);
        // S sets v = (s_0^h,...,s_{n - 1}^h)
        byte[][] vs = spRdpprfSenderOutput.getV0Array();
        // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
        for (int i = 0; i < num; i++) {
            BlockUtils.xori(correlateByteArray, vs[i]);
        }
        SspCotSenderOutput senderOutput = SspCotSenderOutput.create(delta, vs);
        List<byte[]> correlatePayload = Collections.singletonList(correlateByteArray);
        DataPacketHeader correlateHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
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

    private List<byte[]> handleCheckChoicePayload(SspCotSenderOutput senderOutput, List<byte[]> checkChoicePayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(checkChoicePayload.size() == 1);
        byte[] xPrime = checkChoicePayload.get(0);
        boolean[] xPrimeBinary = BinaryUtils.byteArrayToBinary(xPrime, CommonConstants.BLOCK_BIT_LENGTH);
        // S computes \vec{y} := \vec{y}^* + \vec{x}·∆, Y := Σ_{i ∈ [κ]} (y[i]·X^i) ∈ F_{2^κ}
        byte[] y = BlockUtils.zeroBlock();
        for (int checkIndex = 0; checkIndex < CommonConstants.BLOCK_BIT_LENGTH; checkIndex++) {
            // y[i] = y[i]^* + x[i]·∆
            byte[] yi = checkCotSenderOutput.getR0(checkIndex);
            if (xPrimeBinary[checkIndex]) {
                BlockUtils.xori(yi, delta);
            }
            // y[i]·X^i
            byte[] xi = BlockUtils.zeroBlock();
            BinaryUtils.setBoolean(xi, checkIndex, true);
            gf2k.muli(yi, xi);
            // y += y[i]·X^i
            gf2k.addi(y, yi);
        }
        checkCotSenderOutput = null;
        // S computes V := Σ_{i ∈ [n]} (χ[i]·v[i]) + Y ∈ F_{2^κ}
        byte[] v = BlockUtils.zeroBlock();
        for (int i = 0; i < num; i++) {
            // samples uniform {χ_i}_{i ∈ [n]}
            byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES).putLong(extraInfo).putInt(i).array();
            byte[] chi = randomOracle.getBytes(indexMessage);
            // χ[i]·v[i]
            gf2k.muli(chi, senderOutput.getR0(i));
            // v += χ[i]·v[i]
            gf2k.addi(v, chi);
        }
        // V := v + Y
        gf2k.addi(v, y);
        // H'(v)
        v = hash.digestToBytes(v);
        return Collections.singletonList(v);
    }
}
