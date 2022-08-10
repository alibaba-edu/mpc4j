package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.wykw21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.AbstractZ2BspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp.Z2BspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.Z2SspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * WYKW21-Z2-BSP-VOLE半诚实安全协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public class Wykw21ShZ2BspVoleSender extends AbstractZ2BspVoleSender {
    /**
     * COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * COT协议接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * 包含h层密钥数组，第i层包含2^(i + 1)个扩展密钥
     */
    private ArrayList<ArrayList<byte[][]>> treeKeysArrayList;

    public Wykw21ShZ2BspVoleSender(Rpc senderRpc, Party receiverParty, Wykw21ShZ2BspVoleConfig config) {
        super(Wykw21ShZ2BspVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
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
    public void init(int maxBatch, int maxNum) throws MpcAbortException {
        setInitInput(maxBatch, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotReceiver.init(maxH * maxBatch);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Z2BspVoleSenderOutput send(int[] alphaArray, int num) throws MpcAbortException {
        setPtoInput(alphaArray, num);
        return send();
    }

    @Override
    public Z2BspVoleSenderOutput send(int[] alphaArray, int num, Z2VoleSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(alphaArray, num, preSenderOutput);
        return send();
    }

    private Z2BspVoleSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // P_A and P_B calls F_OT, where P_A sends α_i^* ∈ {0,1} to F_OT
        boolean[] flattenNotAlphaBinaryArray = new boolean[h * batch];
        for (int index = 0; index < batch; index++) {
            System.arraycopy(notAlphaBinaryArray[index], 0, flattenNotAlphaBinaryArray, index * h, h);
        }
        cotReceiverOutput = coreCotReceiver.receive(flattenNotAlphaBinaryArray);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        // For i ∈ [h], P_A sends α_i^* ∈ {0, 1} to F_OT, which returns K_{α_i^*}^i to P_A.
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Wykw21ShZ2BspVolePtoDesc.PtoStep.RECEIVER_SEND_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();
        handleMessagePayload(messagePayload);
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        stopWatch.start();
        // P_B sends d := Σ_{i ∈ [0, n)} v[i] to P_A
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Wykw21ShZ2BspVolePtoDesc.PtoStep.RECEIVER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        Z2BspVoleSenderOutput senderOutput = handleCorrelatesPayload(correlatePayload);
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void handleMessagePayload(List<byte[]> messagePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(messagePayload.size() == 2 * h * batch);
        byte[][] messagesArray = messagePayload.toArray(new byte[0][]);
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        treeKeysArrayList = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> treeKeys = new ArrayList<>(h + 1);
                // 把一个空字节作为第0项占位
                treeKeys.add(new byte[0][]);
                int alphaPrefix = 0;
                // For each i ∈ {1,...,h}
                for (int i = 1; i <= h; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevelSeeds = new byte[1 << i][];
                    // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} β_i
                    boolean alphai = alphaBinaryArray[batchIndex][hIndex];
                    int alphaiInt = alphai ? 1 : 0;
                    boolean betai = notAlphaBinaryArray[batchIndex][hIndex];
                    int betaiInt = betai ? 1 : 0;
                    // Compute K_{β_i}^i = M_{β_i}^i ⊕ H(t_i, i || l)
                    byte[] kiNot = BytesUtils.clone(cotReceiverOutput.getRb(h * batchIndex + hIndex));
                    kiNot = crhf.hash(kiNot);
                    if (betai) {
                        BytesUtils.xori(kiNot, messagesArray[batchIndex * h * 2 + 2 * hIndex + 1]);
                    } else {
                        BytesUtils.xori(kiNot, messagesArray[batchIndex * h * 2 + 2 * hIndex]);
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
                return treeKeys;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cotReceiverOutput = null;
    }

    private Z2BspVoleSenderOutput handleCorrelatesPayload(List<byte[]> correlatePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(correlatePayload.size() == 1);
        byte[] correlateBytes = correlatePayload.remove(0);
        boolean[] correlateBinary = BinaryUtils.byteArrayToBinary(correlateBytes, batch);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        Z2SspVoleSenderOutput[] z2SspVoleSenderOutputs = batchIndexIntStream
            .mapToObj(batchIndex -> {
                boolean d = correlateBinary[batchIndex];
                // P_A defines w ∈ F_2^n as the vector with w[i] := v_i for i != α
                byte[][] vArray = treeKeysArrayList.get(batchIndex).get(h);
                byte[] w = new byte[byteNum];
                for (int i = 0; i < num; i++) {
                    if (i != alphaArray[batchIndex]) {
                        boolean ui = (vArray[i][0] % 2 == 1);
                        BinaryUtils.setBoolean(w, numOffset + i, ui);
                        d ^= ui;
                    }
                }
                // and computes w[α] = d + Σ_{i != α} w[i] (setting δ = 0, γ = Δ)
                BinaryUtils.setBoolean(w, numOffset + alphaArray[batchIndex], d);
                return Z2SspVoleSenderOutput.create(num, alphaArray[batchIndex], w);
            })
            .toArray(Z2SspVoleSenderOutput[]::new);
        treeKeysArrayList = null;
        return Z2BspVoleSenderOutput.create(z2SspVoleSenderOutputs);



    }
}
