package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.ms13.Ms13NetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MS13 Network Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Ms13NetRosnSender extends AbstractNetRosnSender {
    /**
     * level
     */
    private int level;
    /**
     * width
     */
    private int width;
    /**
     * COT num
     */
    private int cotNum;
    /**
     * mask for input
     */
    private byte[][] inputMask;
    /**
     * 发送方向量分享值
     */
    private byte[][] senderShareVector;
    /**
     * 交换网络交换导线的遮蔽值0，共有level组，每组width个遮蔽值
     */
    private byte[][][] switchWireMask0s;
    /**
     * 交换网络交换导线的遮蔽值1，共有level组，每组width个遮蔽值
     */
    private byte[][][] switchWireMask1s;
    /**
     * 交换网络交换导线第0组加密密钥，共有level组，每组width个加密密钥
     */
    private byte[][][] switchWireExtendKey0s;
    /**
     * 交换网络交换导线第1组加密密钥，共有level组，每组width个加密密钥
     */
    private byte[][][] switchWireExtendKey1s;
    /**
     * 执行OSN所用的线程池
     */
    private ForkJoinPool osnForkJoinPool;

    public Ms13NetRosnSender(Rpc senderRpc, Party receiverParty, Ms13NetRosnConfig config) {
        super(Ms13NetRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        cotSender.init(delta);
        preCotSender.init();
        stopWatch.stop();
        long cotInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cotInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnSenderOutput rosn(int num, int byteLength) throws MpcAbortException {
        setPtoInput(num, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        level = PermutationNetworkUtils.getLevel(num);
        width = PermutationNetworkUtils.getMaxWidth(num);
        cotNum = level * width;
        CotSenderOutput[] cotSenderOutputs = new CotSenderOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            cotSenderOutputs[levelIndex] = cotSender.send(width);
        }
        handleCotSenderOutputs(cotSenderOutputs);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Sender runs COTs");

        stopWatch.start();
        List<byte[]> switchCorrectionPayload = generateSwitchCorrectionPayload();
        sendOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), switchCorrectionPayload);
        RosnSenderOutput senderOutput = RosnSenderOutput.create(inputMask, senderShareVector);
        switchWireExtendKey0s = null;
        switchWireExtendKey1s = null;
        senderShareVector = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Sender switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleCotSenderOutputs(CotSenderOutput[] cotSenderOutputs) {
        switchWireExtendKey0s = new byte[level][width][];
        switchWireExtendKey1s = new byte[level][width][];
        int extendByteLength = byteLength * 2;
        // 要用width做并发，因为level数量太少了，并发效果不好
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, width).parallel() : IntStream.range(0, width);
        if (extendByteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度的2倍小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] extendKey0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    extendKey0 = Arrays.copyOf(crhf.hash(extendKey0), extendByteLength);
                    switchWireExtendKey0s[levelIndex][widthIndex] = extendKey0;
                    byte[] extendKey1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    extendKey1 = Arrays.copyOf(crhf.hash(extendKey1), extendByteLength);
                    switchWireExtendKey1s[levelIndex][widthIndex] = extendKey1;
                }
            });
        } else {
            // 字节长度的2倍大于128比特时，要使用PRG
            Prg prg = PrgFactory.createInstance(envType, extendByteLength);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] extendKey0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    extendKey0 = prg.extendToBytes(extendKey0);
                    switchWireExtendKey0s[levelIndex][widthIndex] = extendKey0;
                    byte[] extendKey1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    extendKey1 = prg.extendToBytes(extendKey1);
                    switchWireExtendKey1s[levelIndex][widthIndex] = extendKey1;
                }
            });
        }
    }

    private List<byte[]> generateSwitchCorrectionPayload() {
        // 生成输入导线遮蔽值
        senderShareVector = BytesUtils.randomByteArrayVector(num, byteLength, secureRandom);
        inputMask = BytesUtils.clone(senderShareVector);
        // 初始化交换导线遮蔽值
        switchWireMask0s = new byte[level][width][byteLength];
        switchWireMask1s = new byte[level][width][byteLength];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            for (int widthIndex = 0; widthIndex < width; widthIndex++) {
                secureRandom.nextBytes(switchWireMask0s[levelIndex][widthIndex]);
                secureRandom.nextBytes(switchWireMask1s[levelIndex][widthIndex]);
            }
        }
        byte[][][] corrections = new byte[cotNum][2][byteLength * 2];
        int logN = (int) Math.ceil(DoubleUtils.log2(num));
        osnForkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        genSwitchCorrections(logN, 0, 0, senderShareVector, corrections);

        return Arrays.stream(corrections).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    private void genSwitchCorrections(int subLogN, int levelIndex, int permIndex, byte[][] subShareInputs,
                                      byte[][][] corrections) {
        int subN = subShareInputs.length;
        if (subN == 2) {
            assert subLogN == 1 || subLogN == 2;
            genSingleSwitchCorrection(subLogN, levelIndex, permIndex, subShareInputs, corrections);
        } else if (subN == 3) {
            genTripleSwitchCorrection(levelIndex, permIndex, subShareInputs, corrections);
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
            // 构造Benes网络左侧的纠正值
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                byte[] inputMask0 = subShareInputs[i];
                byte[] inputMask1 = subShareInputs[i ^ 1];
                int widthIndex = permIndex + i / 2;
                byte[] outputMask0 = switchWireMask0s[levelIndex][widthIndex];
                byte[] outputMask1 = switchWireMask1s[levelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, levelIndex, widthIndex, corrections);
                subTopShareInputs[subTopShareIndex] = outputMask0;
                subTopShareIndex++;
                subBottomShareInputs[subBottomShareIndex] = outputMask1;
                subBottomShareIndex++;
            }
            // 如果是奇数个输入，则下方子Benes网络需要再增加一个输入
            if (subN % 2 == 1) {
                subBottomShareInputs[subBottomShareIndex] = subShareInputs[subN - 1];
            }
            if (parallel) {
                // 参考https://github.com/dujiajun/PSU/blob/master/osn/OSNReceiver.cpp实现并发
                if (osnForkJoinPool.getParallelism() - osnForkJoinPool.getActiveThreadCount() > 0) {
                    ForkJoinTask<?> topTask = osnForkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, corrections)
                    );
                    ForkJoinTask<?> bottomTask = osnForkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, corrections)
                    );
                    topTask.join();
                    bottomTask.join();
                } else {
                    // 非并发处理
                    genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex,
                        subTopShareInputs, corrections);
                    genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, corrections);
                }
            } else {
                genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex,
                    subTopShareInputs, corrections);
                genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                    subBottomShareInputs, corrections);
            }
            // 构造Benes网络右侧的纠正值
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                byte[] inputMask0 = subTopShareInputs[i / 2];
                byte[] inputMask1 = subBottomShareInputs[i / 2];
                int rightLevelIndex = levelIndex + subLevel - 1;
                int widthIndex = permIndex + i / 2;
                byte[] outputMask0 = switchWireMask0s[rightLevelIndex][widthIndex];
                byte[] outputMask1 = switchWireMask1s[rightLevelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, rightLevelIndex, widthIndex, corrections);
                subShareInputs[i] = outputMask0;
                subShareInputs[i ^ 1] = outputMask1;
            }
            // 如果是奇数个输入，则下方子Benes网络需要多替换一个输出导线遮蔽值
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subShareInputs[subN - 1] = subBottomShareInputs[idx - 1];
            }
        }
    }

    private void genSingleSwitchCorrection(int subLogN, int levelIndex, int permIndex, byte[][] subShareInputs,
                                           byte[][][] corrections) {
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs[0];
        byte[] inputMask1 = subShareInputs[1];
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        byte[] outputMask0 = switchWireMask0s[singleLevelIndex][permIndex];
        byte[] outputMask1 = switchWireMask1s[singleLevelIndex][permIndex];
        setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, singleLevelIndex, permIndex, corrections);
        subShareInputs[0] = outputMask0;
        subShareInputs[1] = outputMask1;
    }

    private void genTripleSwitchCorrection(int levelIndex, int permIndex, byte[][] subShareInputs,
                                           byte[][][] corrections) {
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs[0];
        byte[] inputMask01 = subShareInputs[1];
        // 第一组输出导线遮蔽值
        byte[] outputMask00 = switchWireMask0s[levelIndex][permIndex];
        byte[] outputMask01 = switchWireMask1s[levelIndex][permIndex];
        setCorrection(inputMask00, inputMask01, outputMask00, outputMask01, levelIndex, permIndex, corrections);
        subShareInputs[0] = outputMask00;
        subShareInputs[1] = outputMask01;

        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs[1];
        byte[] inputMask11 = subShareInputs[2];
        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        byte[] outputMask10 = switchWireMask0s[levelIndex1][permIndex];
        byte[] outputMask11 = switchWireMask1s[levelIndex1][permIndex];
        setCorrection(inputMask10, inputMask11, outputMask10, outputMask11, levelIndex1, permIndex, corrections);
        subShareInputs[1] = outputMask10;
        subShareInputs[2] = outputMask11;

        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs[0];
        byte[] inputMask21 = subShareInputs[1];
        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        byte[] outputMask20 = switchWireMask0s[levelIndex2][permIndex];
        byte[] outputMask21 = switchWireMask1s[levelIndex2][permIndex];
        setCorrection(inputMask20, inputMask21, outputMask20, outputMask21, levelIndex2, permIndex, corrections);
        subShareInputs[0] = outputMask20;
        subShareInputs[1] = outputMask21;
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1,
                               int levelIndex, int widthIndex, byte[][][] corrections) {
        // 消息0 = M_(i, 1) ⊕ M_(j, 1) || M_(i, 2) ⊕ M_(j, 2)
        byte[] message0 = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask0), 0, message0, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask1), 0, message0, byteLength, byteLength);
        BytesUtils.xori(switchWireExtendKey0s[levelIndex][widthIndex], message0);
        corrections[levelIndex * width + widthIndex][0] = switchWireExtendKey0s[levelIndex][widthIndex];
        // 消息1 = M_(i, 2) ⊕ M_(j, 1) || M_(i, 1) ⊕ M_(j, 2)
        byte[] message1 = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask0), 0, message1, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask1), 0, message1, byteLength, byteLength);
        BytesUtils.xori(switchWireExtendKey1s[levelIndex][widthIndex], message1);
        corrections[levelIndex * width + widthIndex][1] = switchWireExtendKey1s[levelIndex][widthIndex];
    }
}
