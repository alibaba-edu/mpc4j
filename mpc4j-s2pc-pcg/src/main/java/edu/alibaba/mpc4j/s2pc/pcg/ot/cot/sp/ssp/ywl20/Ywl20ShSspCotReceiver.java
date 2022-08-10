package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.ywl20;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;

/**
 * YWL20-SSP-COT半诚实安全协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Ywl20ShSspCotReceiver extends AbstractSspCotReceiver {
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
    private ArrayList<byte[][]> treeKeys;

    public Ywl20ShSspCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20ShSspCotConfig config) {
        super(Ywl20ShSspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
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
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        coreCotReceiver.init(maxH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

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
            boolean[] rs = new boolean[h];
            IntStream.range(0, h).forEach(index -> rs[index] = secureRandom.nextBoolean());
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(h);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        List<byte[]> binaryPayload = generateBinaryPayload();
        DataPacketHeader binaryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.RECEIVER_SEND_BINARY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(binaryHeader, binaryPayload));
        stopWatch.stop();
        long binaryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), binaryTime);

        stopWatch.start();
        DataPacketHeader messageHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();
        handleMessagePayload(messagePayload);
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), messageTime);

        stopWatch.start();
        DataPacketHeader correlateHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20ShSspCotPtoDesc.PtoStep.SENDER_SEND_CORRELATE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlatePayload = rpc.receive(correlateHeader).getPayload();
        SspCotReceiverOutput receiverOutput = handleCorrelatePayload(correlatePayload);
        long correlateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), correlateTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateBinaryPayload() {
        byte[] bBytes = new byte[CommonUtils.getByteLength(h)];
        int offset = bBytes.length * Byte.SIZE - h;
        // For each i ∈ {1,...,h}
        for (int i = 0; i < h; i++) {
            // R sends a bit b_i = r_i ⊕ α_i ⊕ 1 to S
            BinaryUtils.setBoolean(bBytes, offset + i, alphaBinary[i] == cotReceiverOutput.getChoice(i));
        }
        List<byte[]> bPayload = new LinkedList<>();
        bPayload.add(bBytes);

        return bPayload;
    }

    private void handleMessagePayload(List<byte[]> mPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(mPayload.size() == h * 2);
        byte[][] messages = mPayload.toArray(new byte[0][]);
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
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
            byte[] kiNot = BytesUtils.clone(cotReceiverOutput.getRb(index));
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
        cotReceiverOutput = null;
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
}
