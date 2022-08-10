package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

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
     * COT发送方
     */
    private final CoreCotSender coreCotSender;
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
     * 扩展COT协议发送方输出
     */
    private CotSenderOutput extendCotSenderOutput;
    /**
     * 验证COT协议发送方输出
     */
    private CotSenderOutput checkCotSenderOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;
    /**
     * 包含h层密钥数组，第i层包含2^(i + 1)个扩展密钥
     */
    private ArrayList<ArrayList<byte[][]>> treeKeysArrayList;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][][] k0sArray;
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][][] k1sArray;

    public Ywl20MaBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20MaBspCotConfig config) {
        super(Ywl20MaBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
        gf2k = Gf2kFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxBatch, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatch, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 协议执行过程要请求两次COT，一次是h * m个，一次是128个
        coreCotSender.init(delta, maxH * maxBatch + CommonConstants.BLOCK_BIT_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.RECEIVER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
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
    public BspCotSenderOutput send(int batch, int num) throws MpcAbortException {
        setPtoInput(batch, num);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batch, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batch, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(h * batch + CommonConstants.BLOCK_BIT_LENGTH);
        } else {
            cotSenderOutput.reduce(h * batch + CommonConstants.BLOCK_BIT_LENGTH);
        }
        extendCotSenderOutput = cotSenderOutput.split(h * batch);
        checkCotSenderOutput = cotSenderOutput.split(CommonConstants.BLOCK_BIT_LENGTH);
        cotSenderOutput = null;
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        generatePprfKeys();
        stopWatch.stop();
        long pprfKeyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pprfKeyGenTime);

        stopWatch.start();
        DataPacketHeader binaryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> binaryPayload = rpc.receive(binaryHeader).getPayload();
        List<byte[]> messagePayload = generateMessagePayload(binaryPayload);
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        stopWatch.start();
        List<byte[]> correlatePayload = generateCorrelatePayload();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batch)
            .mapToObj(batchInde -> {
                // 得到的COT数量为2^h，要裁剪到num个
                byte[][] r0Array = treeKeysArrayList.get(batchInde).get(h);
                if (num < (1 << h)) {
                    byte[][] reduceR0Array = new byte[num][];
                    System.arraycopy(r0Array, 0, reduceR0Array, 0, num);
                    r0Array = reduceR0Array;
                }
                return SspCotSenderOutput.create(delta, r0Array);
            })
            .toArray(SspCotSenderOutput[]::new);
        treeKeysArrayList = null;
        stopWatch.stop();
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        stopWatch.start();
        DataPacketHeader checkChoiceHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.RECEIVER_SEND_CHECK_CHOICES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> checkChoicePayload = rpc.receive(checkChoiceHeader).getPayload();
        // 计算H'(V)
        List<byte[]> actualCheckValuePayload = handleCheckChoicePayload(senderOutputs, checkChoicePayload);
        DataPacketHeader actualHashValueHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20MaBspCotPtoDesc.PtoStep.SENDER_SEND_HASH_VALUE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(actualHashValueHeader, actualCheckValuePayload));
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), checkTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return BspCotSenderOutput.create(senderOutputs);
    }

    private void generatePprfKeys() {
        k0sArray = new byte[batch][h][];
        k1sArray = new byte[batch][h][];
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        treeKeysArrayList = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> treeKeys = new ArrayList<>(h + 1);
                // S picks a random s_0^0 ∈ {0, 1}^κ
                byte[][] s0 = new byte[1][CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(s0[0]);
                // 把s0作为第0项，从而方便后续迭代
                treeKeys.add(s0);
                // For each i ∈ {1,...,h}, j ∈ [2^{i − 1}], S computes (s_{2j}^i, s_{2j + 1}^i) = G(s_j^{i - 1})
                for (int i = 1; i <= h; i++) {
                    byte[][] lowLevelSeeds = treeKeys.get(i - 1);
                    byte[][] currentLevelSeeds = new byte[1 << i][];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
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
                    treeKeys.add(currentLevelSeeds);
                }
                // For each i ∈ {1,..., h}
                for (int i = 1; i <= h; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevelSeeds = treeKeys.get(i);
                    // S then computes K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
                    k0sArray[batchIndex][hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BytesUtils.xori(k0sArray[batchIndex][hIndex], currentLevelSeeds[2 * j]);
                    }
                    // and K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
                    k1sArray[batchIndex][hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BytesUtils.xori(k1sArray[batchIndex][hIndex], currentLevelSeeds[2 * j + 1]);
                    }
                }
                return treeKeys;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<byte[]> generateMessagePayload(List<byte[]> binaryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(binaryPayload.size() == batch);
        byte[][] bBytesArray = binaryPayload.toArray(new byte[0][]);
        int offset = CommonUtils.getByteLength(h) * Byte.SIZE - h;
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagePayload = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[] bBytes = bBytesArray[batchIndex];
                return IntStream.range(0, h)
                    .mapToObj(hIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l)
                        byte[] message0 = BytesUtils.clone(extendCotSenderOutput.getR0(batchIndex * h + hIndex));
                        if (BinaryUtils.getBoolean(bBytes, offset + hIndex)) {
                            BytesUtils.xori(message0, extendCotSenderOutput.getDelta());
                        }
                        message0 = crhf.hash(message0);
                        BytesUtils.xori(message0, k0sArray[batchIndex][hIndex]);
                        // and M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
                        byte[] message1 = BytesUtils.clone(extendCotSenderOutput.getR0(batchIndex * h + hIndex));
                        if (!BinaryUtils.getBoolean(bBytes, offset + hIndex)) {
                            BytesUtils.xori(message1, extendCotSenderOutput.getDelta());
                        }
                        message1 = crhf.hash(message1);
                        BytesUtils.xori(message1, k1sArray[batchIndex][hIndex]);
                        return new byte[][] {message0, message1};
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        k0sArray = null;
        k1sArray = null;
        extendCotSenderOutput = null;
        return messagePayload;
    }

    private List<byte[]> generateCorrelatePayload() {
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        return batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[] cBytes = BytesUtils.clone(delta);
                // S sets v = (s_0^h,...,s_{n - 1}^h)
                byte[][] vs = treeKeysArrayList.get(batchIndex).get(h);
                // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
                for (int i = 0; i < num; i++) {
                    BytesUtils.xori(cBytes, vs[i]);
                }
                return cBytes;
            })
            .collect(Collectors.toList());
    }

    private List<byte[]> handleCheckChoicePayload(SspCotSenderOutput[] senderOutputs, List<byte[]> checkChoicePayload)
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
        IntStream lIntStream = IntStream.range(0, batch);
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
                    gf2k.muli(chi, senderOutputs[l].getR0(i));
                    // v += χ[i]·v[i]
                    gf2k.addi(v, chi);
                }
                return v;
            })
            .toArray(byte[][]::new);
        // V := Σ_{l ∈ [m]} (χ[i]·v[i]) + Y
        byte[] v = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int l = 0; l < batch; l++) {
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
