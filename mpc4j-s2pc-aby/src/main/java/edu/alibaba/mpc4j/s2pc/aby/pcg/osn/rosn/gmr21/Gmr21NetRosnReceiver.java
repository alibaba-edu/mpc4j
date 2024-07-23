package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetwork;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GMR21 Network Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Gmr21NetRosnReceiver extends AbstractNetRosnReceiver {
    /**
     * Benes network
     */
    protected BenesNetwork<byte[]> benesNetwork;
    /**
     * level
     */
    protected int level;
    /**
     * width
     */
    protected int width;
    /**
     * COT num
     */
    private int cotNum;
    /**
     * receiver share vector
     */
    private byte[][] receiverShareVector;
    /**
     * switch wire masks 0
     */
    private byte[][][] switchWireMask0s;
    /**
     * switch wire masks 1
     */
    private byte[][][] switchWireMask1s;
    /**
     * thread pool
     */
    private ForkJoinPool forkJoinPool;

    public Gmr21NetRosnReceiver(Rpc receiverRpc, Party senderParty, Gmr21NetRosnConfig config) {
        super(Gmr21NetRosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init();
        preCotReceiver.init();
        stopWatch.stop();
        long cotInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cotInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnReceiverOutput rosn(int[] pi, int byteLength) throws MpcAbortException {
        setPtoInput(pi, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        benesNetwork = BenesNetworkFactory.createInstance(envType, pi);
        level = PermutationNetworkUtils.getLevel(num);
        width = PermutationNetworkUtils.getMaxWidth(num);
        cotNum = level * width;
        CotReceiverOutput[] cotReceiverOutputs = new CotReceiverOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            boolean[] binaryGates = new boolean[width];
            byte[] gates = benesNetwork.getGates(levelIndex);
            for (int widthIndex = 0; widthIndex < width; widthIndex++) {
                // we treat 2 as 0
                binaryGates[widthIndex] = (gates[widthIndex] == 1);
            }
            cotReceiverOutputs[levelIndex] = cotReceiver.receive(binaryGates);
        }
        handleCotReceiverOutputs(cotReceiverOutputs);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Receiver runs COTs");

        stopWatch.start();
        List<byte[]> switchCorrectionPayload = receiveOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), cotNum, byteLength * 2);
        receiverShareVector = new byte[num][byteLength];
        handleSwitchCorrectionPayload(switchCorrectionPayload);
        RosnReceiverOutput receiverOutput = RosnReceiverOutput.create(pi, receiverShareVector);
        switchWireMask0s = null;
        switchWireMask1s = null;
        receiverShareVector = null;
        benesNetwork = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Receiver switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleCotReceiverOutputs(CotReceiverOutput[] cotReceiverOutputs) {
        int totalByteLen = byteLength * 2;
        switchWireMask0s = new byte[level][width][];
        switchWireMask1s = new byte[level][width][];
        // 要用width做并发，因为level数量太少了，并发效果不好
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, width).parallel() : IntStream.range(0, width);
        if (totalByteLen <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] switchWireMask = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                    switchWireMask = crhf.hash(switchWireMask);
                    switchWireMask0s[levelIndex][widthIndex] = Arrays.copyOf(switchWireMask, byteLength);
                    switchWireMask1s[levelIndex][widthIndex] = Arrays.copyOfRange(switchWireMask, byteLength, totalByteLen);
                }
            });
        } else {
            // 字节长度大于128比特时，要使用PRG
            Prg prg = PrgFactory.createInstance(envType, totalByteLen);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] switchWireMask = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                    switchWireMask = prg.extendToBytes(switchWireMask);
                    switchWireMask0s[levelIndex][widthIndex] = Arrays.copyOf(switchWireMask, byteLength);
                    switchWireMask1s[levelIndex][widthIndex] = Arrays.copyOfRange(switchWireMask, byteLength, totalByteLen);
                }
            });
        }
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == cotNum);
        byte[][] flattenCorrections = switchCorrectionPayload.toArray(new byte[0][]);
        byte[][][] corrections = new byte[level][width][byteLength * 2];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            System.arraycopy(flattenCorrections, levelIndex * width, corrections[levelIndex], 0, width);
        }
        int logN = (int) Math.ceil(DoubleUtils.log2(num));
        forkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        handleSwitchCorrection(logN, 0, 0, receiverShareVector, corrections);
    }

    private void handleSwitchCorrection(int subLogN, int levelIndex, int permIndex,
                                        byte[][] subShareInputs, byte[][][] corrections) {
        int subN = subShareInputs.length;
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
            byte[][] subTopShareInputs = new byte[subTopN][];
            int subTopShareIndex = 0;
            byte[][] subBottomShareInputs = new byte[subBottomN][];
            int subBottomShareIndex = 0;
            // 求解Benes网络左侧
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                int widthIndex = permIndex + i / 2;
                int leftS = benesNetwork.getGates(levelIndex)[widthIndex] == 1 ? 1 : 0;
                byte[] inputMask0 = subShareInputs[i];
                byte[] inputMask1 = subShareInputs[i + 1];
                // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                byte[][] outputMasks = getOutputMasks(levelIndex, widthIndex, corrections);
                BytesUtils.xori(inputMask0, outputMasks[leftS]);
                BytesUtils.xori(inputMask1, outputMasks[1 - leftS]);
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ leftS, subLogN);
                    if (x < subN / 2) {
                        subTopShareInputs[subTopShareIndex] = subShareInputs[i | j];
                        subTopShareIndex++;
                    } else {
                        subBottomShareInputs[subBottomShareIndex] = subShareInputs[i | j];
                        subBottomShareIndex++;
                    }
                }
            }
            // 如果是奇数个输入，则下方子Benes网络需要再增加一个输入
            if (subN % 2 == 1) {
                subBottomShareInputs[subBottomShareIndex] = subShareInputs[subN - 1];
            }
            if (parallel) {
                // 参考https://github.com/dujiajun/PSU/blob/master/osn/OSNReceiver.cpp实现并发
                if (forkJoinPool.getParallelism() - forkJoinPool.getActiveThreadCount() > 0) {
                    ForkJoinTask<?> topTask = forkJoinPool.submit(() -> handleSwitchCorrection(
                        subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, corrections));
                    ForkJoinTask<?> subTask = forkJoinPool.submit(() -> handleSwitchCorrection(
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
                int rightS = benesNetwork.getGates(rightLevelIndex)[widthIndex] == 1 ? 1 : 0;
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ rightS, subLogN);
                    if (x < subN / 2) {
                        subShareInputs[i | j] = subTopShareInputs[x];
                    } else {
                        subShareInputs[i | j] = subBottomShareInputs[i / 2];
                    }
                }
                // 输入导线遮蔽值
                byte[] inputMask0 = subShareInputs[i];
                byte[] inputMask1 = subShareInputs[i + 1];
                // 输出导线遮蔽值，右侧Benes网络要交换输入导线遮蔽值的位置
                byte[][] outputMasks = getOutputMasks(rightLevelIndex, widthIndex, corrections);
                BytesUtils.xori(inputMask0, outputMasks[0]);
                BytesUtils.xori(inputMask1, outputMasks[1]);
            }
            // 如果是奇数个输入，则下方子Benes网络需要多替换一个输出导线遮蔽值
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subShareInputs[subN - 1] = subBottomShareInputs[idx - 1];
            }
        }
    }

    private void handleSingleSwitchCorrection(int subLogN, int levelIndex, int permIndex, byte[][] subShareInputs,
                                              byte[][][] corrections) {
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        int s = benesNetwork.getGates(singleLevelIndex)[permIndex] == 1 ? 1 : 0;
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs[s];
        byte[] inputMask1 = subShareInputs[1 - s];
        byte[][] outputMasks = getOutputMasks(singleLevelIndex, permIndex, corrections);
        BytesUtils.xori(outputMasks[0], inputMask0);
        BytesUtils.xori(outputMasks[1], inputMask1);
        subShareInputs[0] = outputMasks[0];
        subShareInputs[1] = outputMasks[1];
    }

    private void handleTripleSwitchCorrection(int levelIndex, int permIndex, byte[][] subShareInputs,
                                              byte[][][] corrections) {
        // 第一组输出导线遮蔽值
        int s0 = benesNetwork.getGates(levelIndex)[permIndex] == 1 ? 1 : 0;
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs[s0];
        byte[] inputMask01 = subShareInputs[1 - s0];
        byte[][] outputMasks0 = getOutputMasks(levelIndex, permIndex, corrections);
        BytesUtils.xori(outputMasks0[0], inputMask00);
        BytesUtils.xori(outputMasks0[1], inputMask01);
        subShareInputs[0] = outputMasks0[0];
        subShareInputs[1] = outputMasks0[1];

        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        int s1 = benesNetwork.getGates(levelIndex1)[permIndex] == 1 ? 1 : 0;
        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs[1 + s1];
        byte[] inputMask11 = subShareInputs[2 - s1];
        byte[][] outputMasks1 = getOutputMasks(levelIndex1, permIndex, corrections);
        BytesUtils.xori(outputMasks1[0], inputMask10);
        BytesUtils.xori(outputMasks1[1], inputMask11);
        subShareInputs[1] = outputMasks1[0];
        subShareInputs[2] = outputMasks1[1];

        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        int s2 = benesNetwork.getGates(levelIndex2)[permIndex] == 1 ? 1 : 0;
        byte[][] outputMasks2 = getOutputMasks(levelIndex2, permIndex, corrections);
        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs[s2];
        byte[] inputMask21 = subShareInputs[1 - s2];
        BytesUtils.xori(outputMasks2[0], inputMask20);
        BytesUtils.xori(outputMasks2[1], inputMask21);
        subShareInputs[0] = outputMasks2[0];
        subShareInputs[1] = outputMasks2[1];
    }

    private byte[][] getOutputMasks(int levelIndex, int widthIndex, byte[][][] corrections) {
        byte[] choiceMessage = corrections[levelIndex][widthIndex];
        byte[][] outputMasks = new byte[2][byteLength];
        if (benesNetwork.getGates(levelIndex)[widthIndex] == 1) {
            System.arraycopy(choiceMessage, 0, outputMasks[0], 0, byteLength);
            System.arraycopy(choiceMessage, byteLength, outputMasks[1], 0, byteLength);
            BytesUtils.xori(outputMasks[0], switchWireMask0s[levelIndex][widthIndex]);
            BytesUtils.xori(outputMasks[1], switchWireMask1s[levelIndex][widthIndex]);
        } else {
            outputMasks[0] = switchWireMask0s[levelIndex][widthIndex];
            outputMasks[1] = switchWireMask1s[levelIndex][widthIndex];
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
