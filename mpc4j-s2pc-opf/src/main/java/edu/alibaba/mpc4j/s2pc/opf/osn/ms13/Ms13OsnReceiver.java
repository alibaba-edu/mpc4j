package edu.alibaba.mpc4j.s2pc.opf.osn.ms13;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.s2pc.opf.osn.AbstractOsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * MS13-OSN协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class Ms13OsnReceiver extends AbstractOsnReceiver {
    /**
     * COT协议接收方
     */
    private final CotReceiver cotReceiver;
    /**
     * 接收方向量分享值
     */
    private Vector<byte[]> receiverShareVector;
    /**
     * 交换网络交换导线加密密钥，共有level组，每组width个加密密钥
     */
    private byte[][][] switchWireExtendKeys;
    /**
     * 执行OSN所用的线程池
     */
    private ForkJoinPool osnForkJoinPool;

    public Ms13OsnReceiver(Rpc receiverRpc, Party senderParty, Ms13OsnConfig config) {
        super(Ms13OsnPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int maxN) throws MpcAbortException {
        setInitInput(maxN);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxSwitchNum);
        stopWatch.stop();
        long cotInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cotInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OsnPartyOutput osn(int[] permutationMap, int byteLength) throws MpcAbortException {
        setPtoInput(permutationMap, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader inputCorrectionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_CORRECTIONS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> inputCorrectionPayload = rpc.receive(inputCorrectionHeader).getPayload();

        stopWatch.start();
        handleInputCorrectionPayload(inputCorrectionPayload);
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, inputCorrectionTime, "Receiver computes input correlation");

        stopWatch.start();
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
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Receiver runs COTs");

        stopWatch.start();
        DataPacketHeader switchCorrectionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> switchCorrectionPayload = rpc.receive(switchCorrectionHeader).getPayload();
        handleSwitchCorrectionPayload(switchCorrectionPayload);
        OsnPartyOutput receiverOutput = new OsnPartyOutput(byteLength, receiverShareVector);
        switchWireExtendKeys = null;
        receiverShareVector = null;
        benesNetwork = null;
        stopWatch.stop();
        long switchCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, switchCorrectionTime, "Receiver switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleInputCorrectionPayload(List<byte[]> inputCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(inputCorrectionPayload.size() == n);
        receiverShareVector = new Vector<>(inputCorrectionPayload);
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == switchNum * 2);
        byte[][] flattenCorrections = switchCorrectionPayload.toArray(new byte[0][]);
        byte[][][] correction0s = new byte[level][width][];
        byte[][][] correction1s = new byte[level][width][];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            for (int widthIndex = 0; widthIndex < width; widthIndex++) {
                int switchWireIndex = levelIndex * width + widthIndex;
                correction0s[levelIndex][widthIndex] = flattenCorrections[2 * switchWireIndex];
                correction1s[levelIndex][widthIndex] = flattenCorrections[2 * switchWireIndex + 1];
            }
        }
        int logN = (int) Math.ceil(DoubleUtils.log2(n));
        osnForkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        handleSwitchCorrection(logN, 0, 0, receiverShareVector, correction0s, correction1s);
    }

    private void handleCotReceiverOutputs(CotReceiverOutput[] cotReceiverOutputs) {
        switchWireExtendKeys = new byte[level][width][];
        int extendByteLength = byteLength * 2;
        if (extendByteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度的2倍小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] extendKey = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                    extendKey = Arrays.copyOf(crhf.hash(extendKey), extendByteLength);
                    switchWireExtendKeys[levelIndex][widthIndex] = extendKey;
                }
            });
        } else {
            // 字节长度的2倍大于128比特时，要使用PRG
            Prg prg = PrgFactory.createInstance(envType, extendByteLength);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] extendKey = cotReceiverOutputs[levelIndex].getRb(widthIndex);
                    extendKey = prg.extendToBytes(extendKey);
                    switchWireExtendKeys[levelIndex][widthIndex] = extendKey;
                }
            });
        }


    }

    private void handleSwitchCorrection(int subLogN, int levelIndex, int permIndex,
                                        Vector<byte[]> subShareInputs, byte[][][] correction0s, byte[][][] correction1s) {
        int subN = subShareInputs.size();
        if (subN == 2) {
            assert subLogN == 1 || subLogN == 2;
            handleSingleSwitchCorrection(subLogN, levelIndex, permIndex, subShareInputs, correction0s, correction1s);
        } else if (subN == 3) {
            handleTripleSwitchCorrection(levelIndex, permIndex, subShareInputs, correction0s, correction1s);
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
                int leftS = benesNetwork.getGates(levelIndex)[widthIndex] == 1 ? 1 : 0;
                byte[] inputMask0 = subShareInputs.elementAt(i);
                byte[] inputMask1 = subShareInputs.elementAt(i + 1);
                // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                byte[][] outputMasks = getOutputMasks(levelIndex, widthIndex, correction0s, correction1s);
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
                        subTopShareInputs, correction0s, correction1s)
                    );
                    ForkJoinTask<?> bottomTask = osnForkJoinPool.submit(() -> handleSwitchCorrection(
                        subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, correction0s, correction1s)
                    );
                    topTask.join();
                    bottomTask.join();
                } else {
                    // 非并发处理
                    handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, correction0s, correction1s);
                    handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, correction0s, correction1s);
                }
            } else {
                handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex,
                    subTopShareInputs, correction0s, correction1s);
                handleSwitchCorrection(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                    subBottomShareInputs, correction0s, correction1s);
            }
            // 求解Benes网络右侧
            for (int i = 0; i < subN - 1; i += 2) {
                int widthIndex = permIndex + i / 2;
                int rightS = benesNetwork.getGates(levelIndex + subLevel - 1)[widthIndex] == 1 ? 1 : 0;
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
                int rightLevelIndex = levelIndex + subLevel - 1;
                // 输出导线遮蔽值，右侧Benes网络要交换输入导线遮蔽值的位置
                byte[][] outputMasks = getOutputMasks(rightLevelIndex, widthIndex, correction0s, correction1s);
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
                                              byte[][][] correction0s, byte[][][] corrections1s) {
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        int s = benesNetwork.getGates(singleLevelIndex)[permIndex] == 1 ? 1 : 0;
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs.elementAt(s);
        byte[] inputMask1 = subShareInputs.elementAt(1 - s);
        byte[][] outputMasks = getOutputMasks(singleLevelIndex, permIndex, correction0s, corrections1s);
        BytesUtils.xori(outputMasks[0], inputMask0);
        BytesUtils.xori(outputMasks[1], inputMask1);
        subShareInputs.set(0, outputMasks[0]);
        subShareInputs.set(1, outputMasks[1]);
    }

    private void handleTripleSwitchCorrection(int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                              byte[][][] correction0s, byte[][][] corrections1s) {
        // 第一组输出导线遮蔽值
        int s0 = benesNetwork.getGates(levelIndex)[permIndex] == 1 ? 1 : 0;
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs.elementAt(s0);
        byte[] inputMask01 = subShareInputs.elementAt(1 - s0);
        byte[][] outputMasks0 = getOutputMasks(levelIndex, permIndex, correction0s, corrections1s);
        BytesUtils.xori(outputMasks0[0], inputMask00);
        BytesUtils.xori(outputMasks0[1], inputMask01);
        subShareInputs.set(0, outputMasks0[0]);
        subShareInputs.set(1, outputMasks0[1]);

        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        int s1 = benesNetwork.getGates(levelIndex1)[permIndex] == 1 ? 1 : 0;
        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs.elementAt(1 + s1);
        byte[] inputMask11 = subShareInputs.elementAt(2 - s1);
        byte[][] outputMasks1 = getOutputMasks(levelIndex1, permIndex, correction0s, corrections1s);
        BytesUtils.xori(outputMasks1[0], inputMask10);
        BytesUtils.xori(outputMasks1[1], inputMask11);
        subShareInputs.set(1, outputMasks1[0]);
        subShareInputs.set(2, outputMasks1[1]);

        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        int s2 = benesNetwork.getGates(levelIndex2)[permIndex] == 1 ? 1 : 0;
        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs.elementAt(s2);
        byte[] inputMask21 = subShareInputs.elementAt(1 - s2);
        byte[][] outputMasks2 = getOutputMasks(levelIndex2, permIndex, correction0s, corrections1s);
        BytesUtils.xori(outputMasks2[0], inputMask20);
        BytesUtils.xori(outputMasks2[1], inputMask21);
        subShareInputs.set(0, outputMasks2[0]);
        subShareInputs.set(1, outputMasks2[1]);
    }

    private byte[][] getOutputMasks(int levelIndex, int widthIndex, byte[][][] correction0s, byte[][][] correction1s) {
        byte[] choiceCorrection = benesNetwork.getGates(levelIndex)[widthIndex] == 1 ?
            correction1s[levelIndex][widthIndex] : correction0s[levelIndex][widthIndex];
        byte[] choiceMessage = switchWireExtendKeys[levelIndex][widthIndex];
        BytesUtils.xori(choiceMessage, choiceCorrection);
        byte[][] outputMasks = new byte[2][byteLength];
        System.arraycopy(choiceMessage, 0, outputMasks[0], 0, byteLength);
        System.arraycopy(choiceMessage, byteLength, outputMasks[1], 0, byteLength);

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
