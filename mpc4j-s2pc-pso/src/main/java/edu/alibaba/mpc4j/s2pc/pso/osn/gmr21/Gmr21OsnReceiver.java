package edu.alibaba.mpc4j.s2pc.pso.osn.gmr21;

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
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.osn.AbstractOsnReceiver;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnPartyOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GMR21-OSN协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
public class Gmr21OsnReceiver extends AbstractOsnReceiver {
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;
    /**
     * 接收方向量分享值
     */
    private Vector<byte[]> receiverShareVector;
    /**
     * 交换网络交换导线
     */
    private byte[][][] switchWireMasks;
    /**
     * 执行OSN所用的线程池
     */
    private ForkJoinPool osnForkJoinPool;

    public Gmr21OsnReceiver(Rpc receiverRpc, Party senderParty, Gmr21OsnConfig config) {
        super(Gmr21OsnPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        cotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        cotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        cotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        cotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxN) throws MpcAbortException {
        setInitInput(maxN);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        cotReceiver.init(maxWidth, maxSwitchNum);
        stopWatch.stop();
        long cotInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotInitTime);

        initialized = true;
        info("{}{} Redv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public OsnPartyOutput osn(int[] permutationMap, int byteLength) throws MpcAbortException {
        setPtoInput(permutationMap, byteLength);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader inputCorrectionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Gmr21OsnPtoDesc.PtoStep.SENDER_SEND_INPUT_CORRECTIONS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> inputCorrectionPayload = rpc.receive(inputCorrectionHeader).getPayload();
        handleInputCorrectionPayload(inputCorrectionPayload);
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), inputCorrectionTime);

        stopWatch.start();
        CotReceiverOutput[] cotReceiverOutputs = new CotReceiverOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            cotReceiverOutputs[levelIndex] = cotReceiver.receive(benesNetwork.getNetworkLevel(levelIndex));
        }
        handleCotReceiverOutputs(cotReceiverOutputs);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        stopWatch.start();
        DataPacketHeader switchCorrectionHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Gmr21OsnPtoDesc.PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> switchCorrectionPayload = rpc.receive(switchCorrectionHeader).getPayload();
        handleSwitchCorrectionPayload(switchCorrectionPayload);
        OsnPartyOutput receiverOutput = new OsnPartyOutput(byteLength, receiverShareVector);
        switchWireMasks = null;
        receiverShareVector = null;
        benesNetwork = null;
        stopWatch.stop();
        long switchCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), switchCorrectionTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private void handleInputCorrectionPayload(List<byte[]> inputCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(inputCorrectionPayload.size() == n);
        receiverShareVector = new Vector<>(inputCorrectionPayload);
    }

    private void handleCotReceiverOutputs(CotReceiverOutput[] cotReceiverOutputs) {
        switchWireMasks = new byte[level][width][];
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    if (benesNetwork.getNetworkLevel(levelIndex)[widthIndex]) {
                        // 只需要扩展一半的密钥
                        byte[] switchWireMask = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                        switchWireMask = Arrays.copyOf(crhf.hash(switchWireMask), byteLength);
                        switchWireMasks[levelIndex][widthIndex] = switchWireMask;
                    }
                }
            });
        } else {
            // 字节长度大于128比特时，要使用PRG
            Prg prg = PrgFactory.createInstance(envType, byteLength);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    if (benesNetwork.getNetworkLevel(levelIndex)[widthIndex]) {
                        // 只需要扩展一半的密钥
                        byte[] switchWireMask = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                        switchWireMask = prg.extendToBytes(switchWireMask);
                        switchWireMasks[levelIndex][widthIndex] = switchWireMask;
                    }
                }
            });
        }
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == switchNum);
        byte[][] flattenCorrections = switchCorrectionPayload.toArray(new byte[0][]);
        byte[][][] corrections = new byte[level][width][byteLength * 2];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            System.arraycopy(flattenCorrections, levelIndex * width, corrections[levelIndex], 0, width);
        }
        int logN = (int) Math.ceil(DoubleUtils.log2(n));
        osnForkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        handleSwitchCorrection(logN, 0, 0, receiverShareVector, corrections);
    }

    private void handleSwitchCorrection(int subLogN, int levelIndex, int permIndex,
                                        Vector<byte[]> subShareInputs, byte[][][] corrections) {
        int subN = subShareInputs.size();
        if (subN == 2) {
            assert subLogN == 1 || subLogN == 2;
            handleSingleSwitchCorrection(subLogN, levelIndex, permIndex, subShareInputs, corrections);
        } else if (subN == 3) {
            handleTripleSwitchCorrection(levelIndex, permIndex, subShareInputs, corrections);
        } else {
            int subLevel = 2 * subLogN - 1;
            // 上方子Benes网络的输入导线遮蔽值，大小为Math.floor(n / 2)
            int subTopN = subN / 2;
            // 下方子Benes网络的输入导线遮蔽值，大小为Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            Vector<byte[]> subTopShareInputs = new Vector<>(subTopN);
            Vector<byte[]> subBottomShareInputs = new Vector<>(subBottomN);
            // 求解Benes网络左侧
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                int widthIndex = permIndex + i / 2;
                int leftS = benesNetwork.getNetworkLevel(levelIndex)[widthIndex] ? 1 : 0;
                byte[] inputMask0 = subShareInputs.elementAt(i);
                byte[] inputMask1 = subShareInputs.elementAt(i + 1);
                // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                byte[][] outputMasks = getOutputMasks(levelIndex, widthIndex, corrections);
                BytesUtils.xori(inputMask0, outputMasks[leftS]);
                BytesUtils.xori(inputMask1, outputMasks[1 - leftS]);
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ leftS, subLogN);
                    if (x < subN / 2) {
                        subTopShareInputs.add(subShareInputs.elementAt(i | j));
                    } else {
                        subBottomShareInputs.add(subShareInputs.elementAt(i | j));
                    }
                }
            }
            // 如果是奇数个输入，则下方子Benes网络需要再增加一个输入
            if (subN % 2 == 1) {
                subBottomShareInputs.add(subShareInputs.elementAt(subN - 1));
            }
            if (parallel) {
                // 参考https://github.com/dujiajun/PSU/blob/master/osn/OSNReceiver.cpp实现并发
                if (osnForkJoinPool.getParallelism() - osnForkJoinPool.getActiveThreadCount() > 0) {
                    ForkJoinTask<?> topTask = osnForkJoinPool.submit(() -> handleSwitchCorrection(
                        subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, corrections));
                    ForkJoinTask<?> subTask = osnForkJoinPool.submit(() -> handleSwitchCorrection(
                        subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, corrections)
                    );
                    topTask.join();
                    subTask.join();
                } else {
                    // 非并发处理
                    handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, corrections);
                    handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, corrections);
                }
            } else {
                handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex,
                    subTopShareInputs, corrections);
                handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                    subBottomShareInputs, corrections);
            }
            // 求解Benes网络右侧
            for (int i = 0; i < subN - 1; i += 2) {
                int rightLevelIndex = levelIndex + subLevel - 1;
                int widthIndex = permIndex + i / 2;
                int rightS = benesNetwork.getNetworkLevel(rightLevelIndex)[widthIndex] ? 1 : 0;
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ rightS, subLogN);
                    if (x < subN / 2) {
                        subShareInputs.set(i | j, subTopShareInputs.elementAt(x));
                    } else {
                        subShareInputs.set(i | j, subBottomShareInputs.elementAt(i / 2));
                    }
                }
                // 输入导线遮蔽值
                byte[] inputMask0 = subShareInputs.elementAt(i);
                byte[] inputMask1 = subShareInputs.elementAt(i + 1);
                // 输出导线遮蔽值，右侧Benes网络要交换输入导线遮蔽值的位置
                byte[][] outputMasks = getOutputMasks(rightLevelIndex, widthIndex, corrections);
                BytesUtils.xori(inputMask0, outputMasks[0]);
                BytesUtils.xori(inputMask1, outputMasks[1]);
            }
            // 如果是奇数个输入，则下方子Benes网络需要多替换一个输出导线遮蔽值
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subShareInputs.set(subN - 1, subBottomShareInputs.elementAt(idx - 1));
            }
        }
    }

    private void handleSingleSwitchCorrection(int subLogN, int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                              byte[][][] corrections) {
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        int s = benesNetwork.getNetworkLevel(singleLevelIndex)[permIndex] ? 1 : 0;
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs.elementAt(s);
        byte[] inputMask1 = subShareInputs.elementAt(1 - s);
        byte[][] outputMasks = getOutputMasks(singleLevelIndex, permIndex, corrections);
        BytesUtils.xori(outputMasks[0], inputMask0);
        BytesUtils.xori(outputMasks[1], inputMask1);
        subShareInputs.set(0, outputMasks[0]);
        subShareInputs.set(1, outputMasks[1]);
    }

    private void handleTripleSwitchCorrection(int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                              byte[][][] corrections) {
        // 第一组输出导线遮蔽值
        int s0 = benesNetwork.getNetworkLevel(levelIndex)[permIndex] ? 1 : 0;
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs.elementAt(s0);
        byte[] inputMask01 = subShareInputs.elementAt(1 - s0);
        byte[][] outputMasks0 = getOutputMasks(levelIndex, permIndex, corrections);
        BytesUtils.xori(outputMasks0[0], inputMask00);
        BytesUtils.xori(outputMasks0[1], inputMask01);
        subShareInputs.set(0, outputMasks0[0]);
        subShareInputs.set(1, outputMasks0[1]);

        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        int s1 = benesNetwork.getNetworkLevel(levelIndex1)[permIndex] ? 1 : 0;
        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs.elementAt(1 + s1);
        byte[] inputMask11 = subShareInputs.elementAt(2 - s1);
        byte[][] outputMasks1 = getOutputMasks(levelIndex1, permIndex, corrections);
        BytesUtils.xori(outputMasks1[0], inputMask10);
        BytesUtils.xori(outputMasks1[1], inputMask11);
        subShareInputs.set(1, outputMasks1[0]);
        subShareInputs.set(2, outputMasks1[1]);

        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        int s2 = benesNetwork.getNetworkLevel(levelIndex2)[permIndex] ? 1 : 0;
        byte[][] outputMasks2 = getOutputMasks(levelIndex2, permIndex, corrections);
        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs.elementAt(s2);
        byte[] inputMask21 = subShareInputs.elementAt(1 - s2);
        BytesUtils.xori(outputMasks2[0], inputMask20);
        BytesUtils.xori(outputMasks2[1], inputMask21);
        subShareInputs.set(0, outputMasks2[0]);
        subShareInputs.set(1, outputMasks2[1]);
    }

    private byte[][] getOutputMasks(int levelIndex, int widthIndex, byte[][][] corrections) {
        byte[] choiceMessage = corrections[levelIndex][widthIndex];
        byte[][] outputMasks = new byte[2][byteLength];
        if (benesNetwork.getNetworkLevel(levelIndex)[widthIndex]) {
            System.arraycopy(choiceMessage, 0, outputMasks[1], 0, byteLength);
            System.arraycopy(choiceMessage, byteLength, outputMasks[0], 0, byteLength);
            BytesUtils.xori(outputMasks[0], switchWireMasks[levelIndex][widthIndex]);
            BytesUtils.xori(outputMasks[1], switchWireMasks[levelIndex][widthIndex]);
        } else {
            System.arraycopy(choiceMessage, 0, outputMasks[0], 0, byteLength);
            System.arraycopy(choiceMessage, byteLength, outputMasks[1], 0, byteLength);
        }

        return outputMasks;
    }

    /**
     * 以n比特为单位，对数字i右循环移位。
     * 例如：n = 8，      i = 00010011
     * 则有：rightCycleShift(i, n) = 10001001
     *
     * @param i 整数i。
     * @param n 单位长度。
     * @return 以n为单位长度，将i右循环移位。
     */
    private int rightCycleShift(int i, int n) {
        return ((i & 1) << (n - 1)) | (i >> 1);
    }
}
