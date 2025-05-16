package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

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
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21 Network Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Gmr21NetRosnSender extends AbstractNetRosnSender {
    /**
     * level
     */
    private int level;
    /**
     * width
     */
    private int width;
    /**
     * sender share vector
     */
    private byte[][] senderShareVector;
    /**
     * mask for input
     */
    private byte[][] inputMask;
    /**
     * switch wire masks corresponding to 0
     */
    private byte[][][] switchWireMask0s;
    /**
     * switch wire masks corresponding to 1
     */
    private byte[][][] switchWireMask1s;
    /**
     * message
     */
    private byte[][][] correctionsMask;
    /**
     * thread pool
     */
    private ForkJoinPool forkJoinPool;

    public Gmr21NetRosnSender(Rpc senderRpc, Party receiverParty, Gmr21NetRosnConfig config) {
        super(Gmr21NetRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        switchWireMask0s = null;
        switchWireMask1s = null;
        senderShareVector = null;
        inputMask = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Sender switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleCotSenderOutputs(CotSenderOutput[] cotSenderOutputs) {
        int totalByteLen = byteLength * 2;
        switchWireMask0s = new byte[level][width][byteLength];
        switchWireMask1s = new byte[level][width][byteLength];
        correctionsMask = new byte[level][width][totalByteLen];
        // 要用width做并发，因为level数量太少了，并发效果不好
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, width).parallel() : IntStream.range(0, width);
        if (totalByteLen <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] otR0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    otR0 = crhf.hash(otR0);
                    switchWireMask0s[levelIndex][widthIndex] = Arrays.copyOf(otR0, byteLength);
                    switchWireMask1s[levelIndex][widthIndex] = Arrays.copyOfRange(otR0, byteLength, totalByteLen);
                    byte[] otR1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    correctionsMask[levelIndex][widthIndex] = Arrays.copyOf(crhf.hash(otR1), totalByteLen);
                }
            });
        } else {
            // 字节长度大于128比特时，要使用PRG
            Prg prg = PrgFactory.createInstance(envType, totalByteLen);
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] otR0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    otR0 = prg.extendToBytes(otR0);
                    switchWireMask0s[levelIndex][widthIndex] = Arrays.copyOf(otR0, byteLength);
                    switchWireMask1s[levelIndex][widthIndex] = Arrays.copyOfRange(otR0, byteLength, totalByteLen);
                    byte[] otR1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    correctionsMask[levelIndex][widthIndex] = prg.extendToBytes(otR1);
                }
            });
        }
    }

    private List<byte[]> generateSwitchCorrectionPayload() {
        senderShareVector = BytesUtils.randomByteArrayVector(num, byteLength, secureRandom);
        inputMask = BytesUtils.clone(senderShareVector);
        int logN = (int) Math.ceil(DoubleUtils.log2(num));
        forkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        genSwitchCorrections(logN, 0, 0, senderShareVector);
        return Arrays.stream(correctionsMask).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    private void genSwitchCorrections(int subLogN, int levelIndex, int permIndex, byte[][] subShareInputs) {
        int subN = subShareInputs.length;
        if (subN == 2) {
            assert subLogN == 1 || subLogN == 2;
            genSingleSwitchCorrection(subLogN, levelIndex, permIndex, subShareInputs);
        } else if (subN == 3) {
            genTripleSwitchCorrection(levelIndex, permIndex, subShareInputs);
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
                // M_(j, 0) = R_0
                byte[] outputMask0 = switchWireMask0s[levelIndex][widthIndex];
                // M_(j, 1) = R_0 ⊕ R_1
                byte[] outputMask1 = switchWireMask1s[levelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, levelIndex, widthIndex);
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
                // 参考https://github.com/dujiajun/PSU/blob/master/osn/OSNSender.cpp实现并发
                if (forkJoinPool.getParallelism() - forkJoinPool.getActiveThreadCount() > 0) {
                    ForkJoinTask<?> topTask = forkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex, subTopShareInputs)
                    );
                    ForkJoinTask<?> subTask = forkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomShareInputs)
                    );
                    topTask.join();
                    subTask.join();
                } else {
                    // 非并发处理
                    genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex, subTopShareInputs);
                    genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomShareInputs);
                }
            } else {
                genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex, subTopShareInputs);
                genSwitchCorrections(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomShareInputs);
            }
            // 构造Benes网络右侧的纠正值
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                byte[] inputMask0 = subTopShareInputs[i / 2];
                byte[] inputMask1 = subBottomShareInputs[i / 2];
                int rightLevelIndex = levelIndex + subLevel - 1;
                int widthIndex = permIndex + i / 2;
                // M_(j, 0) = R_0
                byte[] outputMask0 = switchWireMask0s[rightLevelIndex][widthIndex];
                // M_(j, 1) = R_0 ⊕ R_1
                byte[] outputMask1 = switchWireMask1s[rightLevelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, rightLevelIndex, widthIndex);
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

    private void genSingleSwitchCorrection(int subLogN, int levelIndex, int permIndex, byte[][] subShareInputs) {
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs[0];
        byte[] inputMask1 = subShareInputs[1];
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        // M_(j, 0) = R_0
        byte[] outputMask0 = switchWireMask0s[singleLevelIndex][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask1 = switchWireMask1s[singleLevelIndex][permIndex];
        setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, singleLevelIndex, permIndex);
        subShareInputs[0] = outputMask0;
        subShareInputs[1] = outputMask1;
    }

    private void genTripleSwitchCorrection(int levelIndex, int permIndex, byte[][] subShareInputs) {
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs[0];
        byte[] inputMask01 = subShareInputs[1];
        // 第一组输出导线遮蔽值
        // M_(j, 0) = R_0
        byte[] outputMask00 = switchWireMask0s[levelIndex][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask01 = switchWireMask1s[levelIndex][permIndex];
        setCorrection(inputMask00, inputMask01, outputMask00, outputMask01, levelIndex, permIndex);
        subShareInputs[0] = outputMask00;
        subShareInputs[1] = outputMask01;

        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs[1];
        byte[] inputMask11 = subShareInputs[2];
        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        // M_(j, 0) = R_0
        byte[] outputMask10 = switchWireMask0s[levelIndex1][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask11 = switchWireMask1s[levelIndex1][permIndex];
        setCorrection(inputMask10, inputMask11, outputMask10, outputMask11, levelIndex1, permIndex);
        subShareInputs[1] = outputMask10;
        subShareInputs[2] = outputMask11;

        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs[0];
        byte[] inputMask21 = subShareInputs[1];
        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        // M_(j, 0) = R_0
        byte[] outputMask20 = switchWireMask0s[levelIndex2][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask21 = switchWireMask1s[levelIndex2][permIndex];
        setCorrection(inputMask20, inputMask21, outputMask20, outputMask21, levelIndex2, permIndex);
        subShareInputs[0] = outputMask20;
        subShareInputs[1] = outputMask21;
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1,
                               int levelIndex, int widthIndex) {
        // compute the real mask, mask = G(R0) ⊕ inputMask
        BytesUtils.xori(outputMask0, inputMask0);
        BytesUtils.xori(outputMask1, inputMask1);
        // correctness = M_(i, 1) ⊕ M_(j, 0) || M_(i, 0) ⊕ M_(j, 1)
        byte[] message = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask0), 0, message, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask1), 0, message, byteLength, byteLength);
        BytesUtils.xori(correctionsMask[levelIndex][widthIndex], message);
    }
}

