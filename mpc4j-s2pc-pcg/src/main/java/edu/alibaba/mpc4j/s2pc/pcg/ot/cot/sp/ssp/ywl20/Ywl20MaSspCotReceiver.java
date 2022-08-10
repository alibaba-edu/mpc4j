package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * YWL20-SSP-COT恶意安全协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaSspCotReceiver extends AbstractSspCotReceiver {
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
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
     * 扩展COT协议接收方输出
     */
    private CotReceiverOutput extendCotReceiverOutput;
    /**
     * 验证COT协议接收方输出
     */
    private CotReceiverOutput checkCotReceiverOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;
    /**
     * 包含h层密钥数组，第i层包含2^(i + 1)个扩展密钥
     */
    private ArrayList<byte[][]> treeKeys;

    public Ywl20MaSspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20MaSspCotConfig config) {
        super(Ywl20MaSspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 协议执行过程要请求两次COT，一次是h个，一次是128个
        coreCotReceiver.init(maxH + CommonConstants.BLOCK_BIT_LENGTH);
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
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
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
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        if (cotReceiverOutput == null) {
            boolean[] rs = new boolean[h + CommonConstants.BLOCK_BIT_LENGTH];
            IntStream.range(0, h + CommonConstants.BLOCK_BIT_LENGTH).forEach(index ->
                rs[index] = secureRandom.nextBoolean()
            );
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(h + CommonConstants.BLOCK_BIT_LENGTH);
        }
        extendCotReceiverOutput = cotReceiverOutput.split(h);
        checkCotReceiverOutput = cotReceiverOutput.split(CommonConstants.BLOCK_BIT_LENGTH);
        cotReceiverOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        List<byte[]> binaryPayload = generateBinaryPayload();
        DataPacketHeader binaryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(binaryHeader, binaryPayload));
        stopWatch.stop();
        long binaryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), binaryTime);

        stopWatch.start();
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();
        handleMessagePayload(messagePayload);
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        SspCotReceiverOutput receiverOutput = handleCorrelatePayload(correlatePayload);
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        stopWatch.start();
        List<byte[]> checkChoicePayload = generateCheckChoicePayload();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(checkChoiceHeader, checkChoicePayload));
        // 先本地计算H'(w)，再接收对方的值
        byte[] expectHashValue = computeExpectHashValue(receiverOutput);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaSspCotPtoDesc.PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> actualHashValuePayload = rpc.receive(actualHashValueHeader).getPayload();
        MpcAbortPreconditions.checkArgument(actualHashValuePayload.size() == 1);
        byte[] actualHashValue = actualHashValuePayload.remove(0);
        MpcAbortPreconditions.checkArgument(Arrays.equals(expectHashValue, actualHashValue));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateBinaryPayload() {
        byte[] bBytes = new byte[CommonUtils.getByteLength(h)];
        int offset = bBytes.length * Byte.SIZE - h;
        // For each i ∈ {1,...,h}
        for (int i = 0; i < h; i++) {
            // R sends a bit b_i = r_i ⊕ α_i ⊕ 1 to S
            BinaryUtils.setBoolean(bBytes, offset + i, alphaBinary[i] == extendCotReceiverOutput.getChoice(i));
        }
        List<byte[]> bPayload = new LinkedList<>();
        bPayload.add(bBytes);

        return bPayload;
    }

    private void handleMessagePayload(List<byte[]> mPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(mPayload.size() == h * 2);
        byte[][] messages = mPayload.toArray(new byte[0][]);
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        treeKeys = new ArrayList<>(h + 1);
        // 把一个空字节作为第0项占位
        treeKeys.add(new byte[0][]);
        int alphaPrefix = 0;
        // For each i ∈ {1,...,h}
        for (int i = 1; i <= h; i++) {
            int index = i - 1;
            byte[][] currentLevelSeeds = new byte[1 << i][];
            // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} β_i
            boolean alphai = alphaBinary[index];
            int alphaiInt = alphai ? 1 : 0;
            boolean betai = notAlphaBinary[index];
            int betaiInt = betai ? 1 : 0;
            // Compute K_{β_i}^i = M_{β_i}^i ⊕ H(t_i, i || l)
            byte[] kiNot = BytesUtils.clone(extendCotReceiverOutput.getRb(index));
            kiNot = crhf.hash(kiNot);
            if (betai) {
                BytesUtils.xori(kiNot, messages[2 * index + 1]);
            } else {
                BytesUtils.xori(kiNot, messages[2 * index]);
            }
            if (i == 1) {
                // If i = 1, define s_{β_i}^i = K_{β_i}^i
                currentLevelSeeds[alphaiInt] = null;
                currentLevelSeeds[betaiInt] = kiNot;
            } else {
                // If i ≥ 2
                byte[][] lowLevelSeeds = treeKeys.get(i - 1);
                // for j ∈ [2^i − 1], j ≠ α_1...α_{i − 1}, compute (s_{2j}^i, s_{2j + 1}^i = G(s_ja^{i - 1}).
                for (int j = 0; j < (1 << (i - 1)); j++) {
                    if (j != alphaPrefix) {
                        byte[] extendSeeds = prg.extendToBytes(lowLevelSeeds[j]);
                        currentLevelSeeds[2 * j] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(
                            extendSeeds, 0,
                            currentLevelSeeds[2 * j], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        currentLevelSeeds[2 * j + 1] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(
                            extendSeeds, CommonConstants.BLOCK_BYTE_LENGTH,
                            currentLevelSeeds[2 * j + 1], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                    }
                }
                // 计算剩余缺失的种子
                int alphaStar = (alphaPrefix << 1) + betaiInt;
                currentLevelSeeds[alphaStar] = kiNot;
                for (int j = 0; j < (1 << (i - 1)); j++) {
                    if (j != alphaPrefix) {
                        BytesUtils.xori(currentLevelSeeds[alphaStar], currentLevelSeeds[2 * j + betaiInt]);
                    }
                }
            }
            // 更新α_1...α_{i − 1}
            alphaPrefix = (alphaPrefix << 1) + alphaiInt;
            treeKeys.add(currentLevelSeeds);
        }
        extendCotReceiverOutput = null;
    }

    private SspCotReceiverOutput handleCorrelatePayload(List<byte[]> cPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cPayload.size() == 1);
        byte[] cBytes = cPayload.remove(0);
        // R sets w[i] = s_i^h for i ∈ [n] \ {α}
        byte[][] rbArray = treeKeys.get(h);
        // and computes w[α]
        for (int i = 0; i < num; i++) {
            if (i != alpha) {
                BytesUtils.xori(cBytes, rbArray[i]);
            }
        }
        rbArray[alpha] = cBytes;
        // 得到的COT数量为2^h，要裁剪到num个
        if (num < (1 << h)) {
            byte[][] reduceWs = new byte[num][];
            System.arraycopy(rbArray, 0, reduceWs, 0, num);
            rbArray = reduceWs;
        }
        SspCotReceiverOutput receiverOutput = SspCotReceiverOutput.create(alpha, rbArray);
        treeKeys = null;

        return receiverOutput;
    }

    private List<byte[]> generateCheckChoicePayload() {
        byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
            .putLong(extraInfo).putInt(alpha).array();
        // Sample χ_α
        byte[] chiAlpha = randomOracle.getBytes(indexMessage);
        // R sends x' := x + x^* ∈ F_2^κ to S
        byte[] xStar = BinaryUtils.binaryToByteArray(checkCotReceiverOutput.getChoices());
        BytesUtils.xori(chiAlpha, xStar);
        List<byte[]> checkChoicePayload = new LinkedList<>();
        checkChoicePayload.add(chiAlpha);
        return checkChoicePayload;
    }

    private byte[] computeExpectHashValue(SspCotReceiverOutput receiverOutput) {
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
        // R computes W := Σ_{i ∈ [n]} (χ[i]·w[i]) + Z ∈ F_{2^κ}
        IntStream numIntStream = IntStream.range(0, num);
        numIntStream = parallel ? numIntStream.parallel() : numIntStream;
        byte[][] ws = numIntStream
            .mapToObj(i -> {
                // samples uniform {χ_i}_{i ∈ [n]}
                byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                    .putLong(extraInfo).putInt(i).array();
                byte[] chi = randomOracle.getBytes(indexMessage);
                // χ[i]·w[i]
                gf2k.muli(chi, receiverOutput.getRb(i));
                return chi;
            })
            .toArray(byte[][]::new);
        // W := Σ_{i ∈ [n]} (χ[i]·w[i]) + Z
        byte[] w = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int i = 0; i < num; i++) {
            gf2k.addi(w, ws[i]);
        }
        gf2k.addi(w, z);
        // H'(w)
        return hash.digestToBytes(w);
    }
}
