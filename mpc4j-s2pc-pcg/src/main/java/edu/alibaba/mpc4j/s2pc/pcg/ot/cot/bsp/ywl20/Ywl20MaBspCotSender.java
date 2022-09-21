package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
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
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotSenderOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YWL20-BSP-COT恶意安全协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaBspCotSender extends AbstractBspCotSender {
    /**
     * DPPRF协议配置项
     */
    private final DpprfConfig dpprfConfig;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * DPPRF协议发送方
     */
    private final DpprfSender dpprfSender;
    /**
     * GF(2^128)运算接口
     */
    private final Gf2k gf2k;
    /**
     * H': F_{2^κ} → {0,1}^{2κ} modeled as a random oracle.
     */
    private final Hash hash;
    /**
     * COT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 验证COT协议发送方输出
     */
    private CotSenderOutput checkCotSenderOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;

    public Ywl20MaBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20MaBspCotConfig config) {
        super(Ywl20MaBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        dpprfConfig = config.getDpprfConfig();
        dpprfSender = DpprfFactory.createSender(senderRpc, receiverParty, dpprfConfig);
        dpprfSender.addLogLevel();
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和DPPRF协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        dpprfSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotSender.setParallel(parallel);
        dpprfSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotSender.addLogLevel();
        dpprfSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxBatchNum, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatchNum, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 协议执行过程要请求两次COT，一次用于DPPRF，一次是128个
        int maxCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, maxBatchNum, maxNum)
            + CommonConstants.BLOCK_BIT_LENGTH;
        coreCotSender.init(delta, maxCotNum);
        dpprfSender.init(maxBatchNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomOracleKeyPayload = rpc.receive(randomOracleKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(randomOracleKeyPayload.size() == 1);
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKeyPayload.remove(0));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int num) throws MpcAbortException {
        setPtoInput(batchNum, num);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int dpprfCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, batchNum, num);
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
        info("{}{} Send. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        DpprfSenderOutput dpprfSenderOutput = dpprfSender.puncture(batchNum, num, extendCotSenderOutput);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), dpprfTime);

        stopWatch.start();
        byte[][] correlateByteArrays = new byte[batchNum][];
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                correlateByteArrays[batchIndex] = BytesUtils.clone(delta);
                // S sets v = (s_0^h,...,s_{n - 1}^h)
                byte[][] vs = dpprfSenderOutput.getPrfOutputArray(batchIndex);
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < num; i++) {
                    BytesUtils.xori(correlateByteArrays[batchIndex], vs[i]);
                }
                return SspCotSenderOutput.create(delta, vs);
            })
            .toArray(SspCotSenderOutput[]::new);
        List<byte[]> correlatePayload = Arrays.stream(correlateByteArrays).collect(Collectors.toList());
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        BspCotSenderOutput senderOutput = BspCotSenderOutput.create(senderOutputs);
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        stopWatch.start();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> checkChoicePayload = rpc.receive(checkChoiceHeader).getPayload();
        // 计算H'(V)
        List<byte[]> actualCheckValuePayload = handleCheckChoicePayload(senderOutput, checkChoicePayload);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(actualHashValueHeader, actualCheckValuePayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private List<byte[]> handleCheckChoicePayload(BspCotSenderOutput senderOutput, List<byte[]> checkChoicePayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(checkChoicePayload.size() == 1);
        byte[] xPrime = checkChoicePayload.remove(0);
        boolean[] xPrimeBinary = BinaryUtils.byteArrayToBinary(xPrime, CommonConstants.BLOCK_BIT_LENGTH);
        // S computes \vec{y} := \vec{y}^* + \vec{x}·∆, Y := Σ_{i ∈ [κ]} (y[i]·X^i) ∈ F_{2^κ}，常数次运算，不需要并发
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
                for (int i = 0; i < num; i++) {
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
