package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20.Ywl20ShBspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

/**
 * YWL20-BSP-COT半诚实安全协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotSender extends AbstractBspCotSender {
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

    public Ywl20ShBspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShBspCotConfig config) {
        super(Ywl20ShBspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
    public void init(byte[] delta, int maxBatch, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxBatch, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotSender.init(delta, maxH * maxBatch);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

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
            cotSenderOutput = coreCotSender.send(h * batch);
        } else {
            cotSenderOutput.reduce(h * batch);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        generatePprfKeys();
        stopWatch.stop();
        long pprfKeyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pprfKeyGenTime);

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
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        stopWatch.start();
        List<byte[]> correlatePayload = generateCorrelatePayload();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        SspCotSenderOutput[] senderOutputs = IntStream.range(0, batch)
            .mapToObj(batchIndex -> {
                // 得到的COT数量为2^h，要裁剪到num个
                byte[][] r0Array = treeKeysArrayList.get(batchIndex).get(h);
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
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

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
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batch);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagePayload = batchIndexIntStream
            .mapToObj(batchIndex -> {
                byte[] bBytes = bBytesArray[batchIndex];
                return IntStream.range(0, h)
                    .mapToObj(hIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l)
                        byte[] message0 = BytesUtils.clone(cotSenderOutput.getR0(batchIndex * h + hIndex));
                        if (BinaryUtils.getBoolean(bBytes, offset + hIndex)) {
                            BytesUtils.xori(message0, cotSenderOutput.getDelta());
                        }
                        message0 = crhf.hash(message0);
                        BytesUtils.xori(message0, k0sArray[batchIndex][hIndex]);
                        // and M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
                        byte[] message1 = BytesUtils.clone(cotSenderOutput.getR0(batchIndex * h + hIndex));
                        if (!BinaryUtils.getBoolean(bBytes, offset + hIndex)) {
                            BytesUtils.xori(message1, cotSenderOutput.getDelta());
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
        cotSenderOutput = null;
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
}
