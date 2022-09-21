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
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.AbstractBspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.SspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20.Ywl20MaBspCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * YWL20-BSP-COT恶意安全协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaBspCotReceiver extends AbstractBspCotReceiver {
    /**
     * DPPRF协议配置项
     */
    private final DpprfConfig dpprfConfig;
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * DPPRF协议发送方
     */
    private final DpprfReceiver dpprfReceiver;
    /**
     * GF(2^128)运算接口
     */
    private final Gf2k gf2k;
    /**
     * H': F_{2^κ} → {0,1}^{2κ} modeled as a random oracle.
     */
    private final Hash hash;
    /**
     * COT协议接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * 验证COT协议接收方输出
     */
    private CotReceiverOutput checkCotReceiverOutput;
    /**
     * DPPRF接收方输出
     */
    private DpprfReceiverOutput dpprfReceiverOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;

    public Ywl20MaBspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20MaBspCotConfig config) {
        super(Ywl20MaBspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        dpprfConfig = config.getDpprfConfig();
        dpprfReceiver = DpprfFactory.createReceiver(receiverRpc, senderParty, dpprfConfig);
        dpprfReceiver.addLogLevel();
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // COT协议和DPPRF协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        coreCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        dpprfReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
        dpprfReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
        dpprfReceiver.addLogLevel();
    }

    @Override
    public void init(int maxBatchNum, int maxNum) throws MpcAbortException {
        setInitInput(maxBatchNum, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 协议执行过程要请求两次COT，一次用于DPPRF，一次是128个
        int maxCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, maxBatchNum, maxNum)
            + CommonConstants.BLOCK_BIT_LENGTH;
        coreCotReceiver.init(maxCotNum);
        dpprfReceiver.init(maxBatchNum, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        List<byte[]> randomOracleKeyPayload = new LinkedList<>();
        byte[] randomOracleKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomOracleKey);
        randomOracleKeyPayload.add(randomOracleKey);
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomOracleKeyHeader, randomOracleKeyPayload));
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKey);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int num) throws MpcAbortException {
        setPtoInput(alphaArray, num);
        return receive();
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private BspCotReceiverOutput receive() throws MpcAbortException {
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        int dpprfCotNum = DpprfFactory.getPrecomputeNum(dpprfConfig, batchNum, num);
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
        info("{}{} Recv. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        dpprfReceiverOutput = dpprfReceiver.puncture(alphaArray, num, extendCotReceiverOutput);
        stopWatch.stop();
        long dpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), dpprfTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        BspCotReceiverOutput receiverOutput = generateReceiverOutput(correlatePayload);
        dpprfReceiverOutput = null;
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        stopWatch.start();
        List<byte[]> checkChoicePayload = generateCheckChoicePayload();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(checkChoiceHeader, checkChoicePayload));
        // 先本地计算H'(w)，再接收对方的值
        byte[] expectHashValue = computeExpectHashValue(receiverOutput);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> actualHashValuePayload = rpc.receive(actualHashValueHeader).getPayload();
        MpcAbortPreconditions.checkArgument(actualHashValuePayload.size() == 1);
        byte[] actualHashValue = actualHashValuePayload.remove(0);
        MpcAbortPreconditions.checkArgument(Arrays.equals(expectHashValue, actualHashValue));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private BspCotReceiverOutput generateReceiverOutput(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == batchNum);
        byte[][] correlateByteArrays = correlatePayload.toArray(new byte[0][]);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        SspCotReceiverOutput[] sspCotReceiverOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[][] rbArray = dpprfReceiverOutput.getPprfOutputArray(batchIndex);
                // computes w[α]
                for (int i = 0; i < num; i++) {
                    if (i != alphaArray[batchIndex]) {
                        BytesUtils.xori(correlateByteArrays[batchIndex], rbArray[i]);
                    }
                }
                rbArray[alphaArray[batchIndex]] = correlateByteArrays[batchIndex];
                return SspCotReceiverOutput.create(alphaArray[batchIndex], rbArray);
            })
            .toArray(SspCotReceiverOutput[]::new);
        return BspCotReceiverOutput.create(sspCotReceiverOutputs);
    }

    private List<byte[]> generateCheckChoicePayload() {
        // R computes ϕ := Σ_{i ∈ [m]} χ_{α_l}^l ∈ F_{2^κ}
        byte[] phi = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int l = 0; l < batchNum; l++) {
            byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + Integer.BYTES)
                .putLong(extraInfo).putInt(l).putInt(alphaArray[l]).array();
            // Sample χ_α
            byte[] chiAlpha = randomOracle.getBytes(indexMessage);
            BytesUtils.xori(phi, chiAlpha);
        }
        // R sends x' := x + x^* ∈ F_2^κ to S
        byte[] xStar = BinaryUtils.binaryToByteArray(checkCotReceiverOutput.getChoices());
        BytesUtils.xori(phi, xStar);
        List<byte[]> checkChoicePayload = new LinkedList<>();
        checkChoicePayload.add(phi);
        return checkChoicePayload;
    }

    private byte[] computeExpectHashValue(BspCotReceiverOutput receiverOutput) {
        // R computes Z :=  Σ_{i ∈ [κ]} (z^*[i]·X^i) ∈ F_{2^κ}，常数次运算，不需要并发
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
        // R computes W := Σ_{l ∈ [m]}(Σ_{i ∈ [n]} (χ[i]·w[i])) + Z ∈ F_{2^κ}
        IntStream lIntStream = IntStream.range(0, batchNum);
        lIntStream = parallel ? lIntStream.parallel() : lIntStream;
        byte[][] ws = lIntStream
            .mapToObj(l -> {
                byte[] w = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int i = 0; i < num; i++) {
                    // samples uniform {χ_i}_{i ∈ [n]}
                    byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + Integer.BYTES)
                        .putLong(extraInfo).putInt(l).putInt(i).array();
                    byte[] chi = randomOracle.getBytes(indexMessage);
                    // χ[i]·w[i]
                    gf2k.muli(chi, receiverOutput.get(l).getRb(i));
                    // w += χ[i]·w[i]
                    BytesUtils.xori(w, chi);
                }
                return w;
            })
            .toArray(byte[][]::new);
        // W := Σ_{i ∈ [n]} (χ[i]·w[i]) + Z
        byte[] w = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int l = 0; l < batchNum; l++) {
            gf2k.addi(w, ws[l]);
        }
        gf2k.addi(w, z);
        // H'(w)
        return hash.digestToBytes(w);
    }
}
