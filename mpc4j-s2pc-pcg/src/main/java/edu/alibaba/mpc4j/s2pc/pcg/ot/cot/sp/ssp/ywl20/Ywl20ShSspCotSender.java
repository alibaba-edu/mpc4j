package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

/**
 * YWL20-SSP-COT半诚实安全协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Ywl20ShSspCotSender extends AbstractSspCotSender {
    /**
     * COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * COT协议发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 包含h层密钥数组，第i层包含2^(i + 1)个扩展密钥
     */
    private ArrayList<byte[][]> treeKeys;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][] k0s;
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][] k1s;

    public Ywl20ShSspCotSender(Rpc senderRpc, Party receiverParty, Ywl20ShSspCotConfig config) {
        super(Ywl20ShSspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotSender.init(delta, maxH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
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
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(h);
        } else {
            cotSenderOutput.reduce(h);
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
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> binaryPayload = rpc.receive(binaryHeader).getPayload();
        List<byte[]> messagePayload = generateMessagePayload(binaryPayload);
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
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
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(correlateHeader, correlatePayload));
        // 得到的COT数量为2^h，要裁剪到num个
        byte[][] r0Array = treeKeys.get(h);
        if (num < (1 << h)) {
            byte[][] reduceR0Array = new byte[num][];
            System.arraycopy(r0Array, 0, reduceR0Array, 0, num);
            r0Array = reduceR0Array;
        }
        SspCotSenderOutput senderOutput = SspCotSenderOutput.create(delta, r0Array);
        treeKeys = null;
        cotSenderOutput = null;
        stopWatch.stop();
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void generatePprfKeys() {
        treeKeys = new ArrayList<>(h + 1);
        // S picks a random s_0^0 ∈ {0, 1}^κ
        byte[][] s0 = new byte[1][CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(s0[0]);
        // 把s0作为第0项，从而方便后续迭代
        treeKeys.add(s0);
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
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
        k0s = new byte[h][];
        k1s = new byte[h][];
        // For each i ∈ {1,..., h}
        for (int i = 1; i <= h; i++) {
            int index = i - 1;
            byte[][] currentLevelSeeds = treeKeys.get(i);
            // S then computes K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
            k0s[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BytesUtils.xori(k0s[index], currentLevelSeeds[2 * j]);
            }
            // and K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
            k1s[index] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BytesUtils.xori(k1s[index], currentLevelSeeds[2 * j + 1]);
            }
        }
    }

    private List<byte[]> generateMessagePayload(List<byte[]> bPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(bPayload.size() == 1);
        byte[] bBytes = bPayload.remove(0);
        int offset = bBytes.length * Byte.SIZE - h;
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        List<byte[]> mPayload = IntStream.range(0, h)
            .mapToObj(index -> {
                // S sends M_0^i = K_0^i ⊕ H(q_i ⊕ b_i ∆, i || l)
                byte[] message0 = BytesUtils.clone(cotSenderOutput.getR0(index));
                if (BinaryUtils.getBoolean(bBytes, offset + index)) {
                    BytesUtils.xori(message0, cotSenderOutput.getDelta());
                }
                message0 = crhf.hash(message0);
                BytesUtils.xori(message0, k0s[index]);
                // and M_1^i = K_1^i ⊕ H(q_i ⊕ \not b_i ∆, i || l)
                byte[] message1 = BytesUtils.clone(cotSenderOutput.getR0(index));
                if (!BinaryUtils.getBoolean(bBytes, offset + index)) {
                    BytesUtils.xori(message1, cotSenderOutput.getDelta());
                }
                message1 = crhf.hash(message1);
                BytesUtils.xori(message1, k1s[index]);
                return new byte[][] {message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        k0s = null;
        k1s = null;
        return mPayload;
    }

    private List<byte[]> generateCorrelatePayload() {
        List<byte[]> cPayload = new LinkedList<>();
        byte[] cBytes = BytesUtils.clone(delta);
        // S sets v = (s_0^h,...,s_{n - 1}^h)
        byte[][] vs = treeKeys.get(h);
        // and sends c = Δ + \sum_{i ∈ [n]} {v[i]}
        for (int i = 0; i < num; i++) {
            BytesUtils.xori(cBytes, vs[i]);
        }
        cPayload.add(cBytes);

        return cPayload;
    }
}
