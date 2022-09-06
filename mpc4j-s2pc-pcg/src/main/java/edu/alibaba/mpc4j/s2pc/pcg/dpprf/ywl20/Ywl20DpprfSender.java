package edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.AbstractDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.ywl20.Ywl20DpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YWL20-DPPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20DpprfSender extends AbstractDpprfSender {
    /**
     * 核COT发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * COT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 批处理数量个PRF密钥，每个密钥包含l + 1层密钥数组，第i层包含2^i个扩展密钥
     */
    private ArrayList<ArrayList<byte[][]>> ggmTree;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][][] k0sArray;
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][][] k1sArray;

    public Ywl20DpprfSender(Rpc senderRpc, Party receiverParty, Ywl20DpprfConfig config) {
        super(Ywl20DpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotSender.addLogLevel();
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
    public void init(int maxBatchNum, int maxAlphaBound) throws MpcAbortException {
        setInitInput(maxBatchNum, maxAlphaBound);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // DPPRF使用的是Random OT，所以可以随机选择Δ
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxH * maxBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public DpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException {
        setPtoInput(batchNum, alphaBound);
        return puncture();
    }

    @Override
    public DpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, alphaBound, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private DpprfSenderOutput puncture() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(h * batchNum);
        } else {
            cotSenderOutput.reduce(h * batchNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        generatePprfKeys();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        stopWatch.start();
        DataPacketHeader binaryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> binaryPayload = rpc.receive(binaryHeader).getPayload();
        List<byte[]> messagePayload = generateMessagePayload(binaryPayload);
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        DpprfSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void generatePprfKeys() {
        k0sArray = new byte[batchNum][h][];
        k1sArray = new byte[batchNum][h][];
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmTree = batchIndexIntStream
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
        MpcAbortPreconditions.checkArgument(binaryPayload.size() == batchNum);
        byte[][] bBytesArray = binaryPayload.toArray(new byte[0][]);
        int offset = CommonUtils.getByteLength(h) * Byte.SIZE - h;
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagePayload = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[] bBytes = bBytesArray[batchIndex];
                return IntStream.range(0, h)
                    .mapToObj(lIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l)
                        byte[] message0 = BytesUtils.clone(cotSenderOutput.getR0(batchIndex * h + lIndex));
                        if (BinaryUtils.getBoolean(bBytes, offset + lIndex)) {
                            BytesUtils.xori(message0, cotSenderOutput.getDelta());
                        }
                        message0 = crhf.hash(message0);
                        BytesUtils.xori(message0, k0sArray[batchIndex][lIndex]);
                        // and M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
                        byte[] message1 = BytesUtils.clone(cotSenderOutput.getR0(batchIndex * h + lIndex));
                        if (!BinaryUtils.getBoolean(bBytes, offset + lIndex)) {
                            BytesUtils.xori(message1, cotSenderOutput.getDelta());
                        }
                        message1 = crhf.hash(message1);
                        BytesUtils.xori(message1, k1sArray[batchIndex][lIndex]);
                        return new byte[][] {message0, message1};
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        k0sArray = null;
        k1sArray = null;
        cotSenderOutput = null;
        return messagePayload;
    }

    private DpprfSenderOutput generateSenderOutput() {
        byte[][][] prfOutputArrays = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                // 得到的PRF密钥数量为2^h，要裁剪到alphaBound个
                byte[][] prfOutputArray = ggmTree.get(batchIndex).get(h);
                if (alphaBound < (1 << h)) {
                    byte[][] reducePrfOutputArray = new byte[alphaBound][];
                    System.arraycopy(prfOutputArray, 0, reducePrfOutputArray, 0, alphaBound);
                    prfOutputArray = reducePrfOutputArray;
                }
                return prfOutputArray;
            })
            .toArray(byte[][][]::new);
        ggmTree = null;
        return new DpprfSenderOutput(alphaBound, prfOutputArrays);
    }
}
