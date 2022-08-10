package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.AbstractZ2BspVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.Z2BspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * WYKW21-Z2-BSP-VOLE半诚实安全协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/6/23
 */
public class Wykw21ShZ2BspVoleReceiver extends AbstractZ2BspVoleReceiver {
    /**
     * COT发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * COT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
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
    /**
     * 输出值v
     */
    private byte[][] v;

    public Wykw21ShZ2BspVoleReceiver(Rpc receiverRpc, Party senderParty, Wykw21ShZ2BspVoleConfig config) {
        super(Wykw21ShZ2BspVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
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
    public void init(boolean delta, int maxBatch, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatch, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] cotDelta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(cotDelta);
        coreCotSender.init(cotDelta, maxH * maxBatch);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2BspVoleReceiverOutput receive(int batch, int num) throws MpcAbortException {
        setPtoInput(batch, num);
        return receive();
    }

    @Override
    public Z2BspVoleReceiverOutput receive(int batch, int num, Z2VoleReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(batch, num, preReceiverOutput);
        return receive();
    }

    private Z2BspVoleReceiverOutput receive() throws MpcAbortException {
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // P_A and P_B calls F_OT
        cotSenderOutput = coreCotSender.send(h * batch);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        // P_B samples s ← {0,1}^κ, runs GGM(1^n, s) to obtain ({v_j}_{j ∈ [0, n)}, {(K_0^i, K_1^i}_{i ∈ [h]})
        generatePprfKeys();
        // P_B sends (K_0^i, K_1^i) to F_OT.
        List<byte[]> messagePayload = generateMessagePayload();
        cotSenderOutput = null;
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Wykw21ShZ2BspVolePtoDesc.PtoStep.RECEIVER_SEND_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        stopWatch.stop();
        long messagesTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messagesTime);

        stopWatch.start();
        // P_B sets v[j] := v_j for j ∈ [0, n), and sends d := Σ_{i ∈ [0, n)} v[i] to P_A
        List<byte[]> correlatePayload = generateCorrelatePayload();
        treeKeysArrayList = null;
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Wykw21ShZ2BspVolePtoDesc.PtoStep.RECEIVER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        Z2SspVoleReceiverOutput[] z2SspVoleReceiverOutputs = IntStream.range(0, batch)
            .mapToObj(batchIndex -> Z2SspVoleReceiverOutput.create(num, delta, v[batchIndex]))
            .toArray(Z2SspVoleReceiverOutput[]::new);
        Z2BspVoleReceiverOutput receiverOutput = Z2BspVoleReceiverOutput.create(z2SspVoleReceiverOutputs);
        stopWatch.stop();
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
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

    private List<byte[]> generateMessagePayload() {
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagesPayload = batchIndexIntStream
            .mapToObj(batchIndex ->
                IntStream.range(0, h)
                    .mapToObj(hIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(R_0)
                        byte[] message0 = BytesUtils.clone(cotSenderOutput.getR0(h * batchIndex + hIndex));
                        message0 = crhf.hash(message0);
                        BytesUtils.xori(message0, k0sArray[batchIndex][hIndex]);
                        // and M_1^i = K_1^i ⊕ H(R_1)
                        byte[] message1 = BytesUtils.clone(cotSenderOutput.getR1(h * batchIndex + hIndex));
                        message1 = crhf.hash(message1);
                        BytesUtils.xori(message1, k1sArray[batchIndex][hIndex]);
                        return new byte[][]{message0, message1};
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        k0sArray = null;
        k1sArray = null;
        return messagesPayload;
    }

    private List<byte[]> generateCorrelatePayload() {
        v = new byte[batch][byteNum];
        boolean[] correlatesBinary = new boolean[batch];
        IntStream batchIndexStream = IntStream.range(0, batch);
        batchIndexStream = parallel ? batchIndexStream.parallel() : batchIndexStream;
        batchIndexStream.forEach(batchIndex -> {
            boolean d = delta;
            // P_B sets v[j] := v_j for j ∈ [0, n), and sends d := Σ_{i ∈ [0, n)} v[i] to P_A (setting δ = 0, γ = Δ)
            byte[][] vArray = treeKeysArrayList.get(batchIndex).get(h);
            // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
            for (int i = 0; i < num; i++) {
                boolean vi = (vArray[i][0] % 2 == 1);
                BinaryUtils.setBoolean(v[batchIndex], numOffset + i, vi);
                d ^= vi;
            }
            correlatesBinary[batchIndex] = d;
        });
        List<byte[]> correlatesPayload = new LinkedList<>();
        correlatesPayload.add(BinaryUtils.binaryToRoundByteArray(correlatesBinary));
        return correlatesPayload;
    }
}
