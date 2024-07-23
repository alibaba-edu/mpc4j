package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetwork;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24NetRosnPtoDesc.PtoStep;
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
 * LLL24 Network Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/10
 */
public class Lll24NetRosnReceiver extends AbstractNetRosnReceiver {
    /**
     * Waksman network
     */
    protected WaksmanNetwork<byte[]> waksmanNetwork;
    /**
     * level
     */
    protected int level;
    /**
     * width
     */
    protected int maxWidth;
    /**
     * COT num
     */
    private int cotNum;
    /**
     * receiver share vector
     */
    private byte[][] receiverShareVector;
    /**
     * switch wire masks
     */
    private byte[][][] switchWireMasks;
    /**
     * thread pool
     */
    private ForkJoinPool forkJoinPool;

    public Lll24NetRosnReceiver(Rpc receiverRpc, Party senderParty, Lll24NetRosnConfig config) {
        super(Lll24NetRosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        waksmanNetwork = WaksmanNetworkFactory.createInstance(envType, pi);
        level = waksmanNetwork.getLevel();
        maxWidth = waksmanNetwork.getMaxWidth();
        cotNum = WaksmanNetworkFactory.getSwitchCount(num);
        CotReceiverOutput[] cotReceiverOutputs = new CotReceiverOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            boolean[] choices = generateChoices(levelIndex);
            cotReceiverOutputs[levelIndex] = cotReceiver.receive(choices);
        }
        handleCotReceiverOutputs(cotReceiverOutputs);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Receiver runs COTs");

        stopWatch.start();
        List<byte[]> switchCorrectionPayload = receiveOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), cotNum, byteLength);
        receiverShareVector = new byte[num][byteLength];
        handleSwitchCorrectionPayload(switchCorrectionPayload);
        RosnReceiverOutput receiverOutput = RosnReceiverOutput.create(pi, receiverShareVector);
        switchWireMasks = null;
        receiverShareVector = null;
        waksmanNetwork = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Receiver computes input correlation");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private boolean[] generateChoices(int levelIndex) {
        int width = waksmanNetwork.getWidth(levelIndex);
        byte[] gates = waksmanNetwork.getGates(levelIndex);
        boolean[] binaryGates = new boolean[width];
        int index = 0;
        for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
            if (gates[widthIndex] != 2) {
                binaryGates[index] = (gates[widthIndex] == 1);
                index++;
            }
        }
        assert index == width;
        return binaryGates;
    }

    private void handleCotReceiverOutputs(CotReceiverOutput[] cotReceiverOutputs) {
        switchWireMasks = new byte[level][maxWidth][];
        // padding to max width
        CotReceiverOutput[] paddingCotReceiverOutputs = new CotReceiverOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            boolean[] paddingChoices = new boolean[maxWidth];
            byte[][] paddingRbArray = new byte[maxWidth][];
            int index = 0;
            for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
                byte[] gates = waksmanNetwork.getGates(levelIndex);
                if (gates[widthIndex] != 2) {
                    paddingRbArray[widthIndex] = cotReceiverOutputs[levelIndex].getRb(index);
                    paddingChoices[widthIndex] = cotReceiverOutputs[levelIndex].getChoice(index);
                    index++;
                } else {
                    paddingRbArray[widthIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    paddingChoices[widthIndex] = false;
                }
            }
            assert index == cotReceiverOutputs[levelIndex].getNum();
            paddingCotReceiverOutputs[levelIndex] = CotReceiverOutput.create(paddingChoices, paddingRbArray);
        }
        // level = O(log(n)) but width = O(n), batch in width
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, maxWidth).parallel() : IntStream.range(0, maxWidth);
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // we only need to use more efficient CRHF instead of PRG
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                        byte[] switchWireMask = paddingCotReceiverOutputs[levelIndex].getRb(widthIndex);
                        switchWireMasks[levelIndex][widthIndex] = Arrays.copyOf(crhf.hash(switchWireMask), byteLength);
                    }
                }
            });
        } else {
            // we need to use PRG
            Prg prg = PrgFactory.createInstance(envType, byteLength);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                        byte[] switchWireMask = paddingCotReceiverOutputs[levelIndex].getRb(widthIndex);
                        switchWireMasks[levelIndex][widthIndex] = prg.extendToBytes(switchWireMask);
                    }
                }
            });
        }
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == cotNum);
        byte[][] flattenReducedCorrections = switchCorrectionPayload.toArray(new byte[0][]);
        byte[][][] corrections = new byte[level][maxWidth][byteLength];
        int offset = 0;
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            int width = waksmanNetwork.getWidth(levelIndex);
            byte[] gates = waksmanNetwork.getGates(levelIndex);
            byte[][] reducedCorrection = new byte[width][];
            System.arraycopy(flattenReducedCorrections, offset, reducedCorrection, 0, width);
            offset += width;
            int index = 0;
            for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
                if (gates[widthIndex] != 2) {
                    corrections[levelIndex][widthIndex] = reducedCorrection[index];
                    index++;
                }
            }
            assert index == width;
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
                int leftS = waksmanNetwork.getGates(levelIndex)[widthIndex] == 1 ? 1 : 0;
                if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                    byte[] inputMask0 = subShareInputs[i];
                    byte[] inputMask1 = subShareInputs[i + 1];
                    // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                    byte[][] outputMasks = getOutputMasks(levelIndex, widthIndex, corrections);
                    BytesUtils.xori(inputMask0, outputMasks[leftS]);
                    BytesUtils.xori(inputMask1, outputMasks[1 - leftS]);
                }
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
                int rightS = waksmanNetwork.getGates(rightLevelIndex)[widthIndex] == 1 ? 1 : 0;
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ rightS, subLogN);
                    if (x < subN / 2) {
                        subShareInputs[i | j] = subTopShareInputs[x];
                    } else {
                        subShareInputs[i | j] = subBottomShareInputs[i / 2];
                    }
                }
                if (waksmanNetwork.getGates(rightLevelIndex)[widthIndex] != 2) {
                    // 输入导线遮蔽值
                    byte[] inputMask0 = subShareInputs[i];
                    byte[] inputMask1 = subShareInputs[i + 1];
                    // 输出导线遮蔽值，右侧Benes网络要交换输入导线遮蔽值的位置
                    byte[][] outputMasks = getOutputMasks(rightLevelIndex, widthIndex, corrections);
                    BytesUtils.xori(inputMask0, outputMasks[0]);
                    BytesUtils.xori(inputMask1, outputMasks[1]);
                }
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
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        if (waksmanNetwork.getGates(singleLevelIndex)[permIndex] != 2) {
            // 输出导线遮蔽值
            int s = waksmanNetwork.getGates(singleLevelIndex)[permIndex] == 1 ? 1 : 0;
            // 输入导线遮蔽值
            byte[] inputMask0 = subShareInputs[s];
            byte[] inputMask1 = subShareInputs[1 - s];
            byte[][] outputMasks = getOutputMasks(singleLevelIndex, permIndex, corrections);
            BytesUtils.xori(outputMasks[0], inputMask0);
            BytesUtils.xori(outputMasks[1], inputMask1);
            subShareInputs[0] = outputMasks[0];
            subShareInputs[1] = outputMasks[1];
        }
    }

    private void handleTripleSwitchCorrection(int levelIndex, int permIndex, byte[][] subShareInputs,
                                              byte[][][] corrections) {
        if (waksmanNetwork.getGates(levelIndex)[permIndex] != 2) {
            // 第一组输出导线遮蔽值
            int s0 = waksmanNetwork.getGates(levelIndex)[permIndex] == 1 ? 1 : 0;
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
            int s1 = waksmanNetwork.getGates(levelIndex1)[permIndex] == 1 ? 1 : 0;
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
            int s2 = waksmanNetwork.getGates(levelIndex2)[permIndex] == 1 ? 1 : 0;
            byte[][] outputMasks2 = getOutputMasks(levelIndex2, permIndex, corrections);
            // 第三组输入导线遮蔽值
            byte[] inputMask20 = subShareInputs[s2];
            byte[] inputMask21 = subShareInputs[1 - s2];
            BytesUtils.xori(outputMasks2[0], inputMask20);
            BytesUtils.xori(outputMasks2[1], inputMask21);
            subShareInputs[0] = outputMasks2[0];
            subShareInputs[1] = outputMasks2[1];
        }
    }

    private byte[][] getOutputMasks(int levelIndex, int widthIndex, byte[][][] corrections) {
        byte[] choiceMessage = corrections[levelIndex][widthIndex];
        BytesUtils.xori(choiceMessage, switchWireMasks[levelIndex][widthIndex]);
        byte[][] outputMasks = new byte[2][byteLength];
        int index = waksmanNetwork.getGates(levelIndex)[widthIndex] == 1 ? 1 : 0;
        outputMasks[index] = switchWireMasks[levelIndex][widthIndex];
        outputMasks[1 - index] = choiceMessage;
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
