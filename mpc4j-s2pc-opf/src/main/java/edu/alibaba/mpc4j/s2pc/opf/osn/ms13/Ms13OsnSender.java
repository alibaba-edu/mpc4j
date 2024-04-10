package edu.alibaba.mpc4j.s2pc.opf.osn.ms13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.AbstractOsnSender;
import edu.alibaba.mpc4j.s2pc.opf.osn.ms13.Ms13OsnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MS13-OSN协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class Ms13OsnSender extends AbstractOsnSender {
    /**
     * COT协议发送方
     */
    private final CotSender cotSender;
    /**
     * 发送方向量分享值
     */
    private Vector<byte[]> senderShareVector;
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

    public Ms13OsnSender(Rpc senderRpc, Party receiverParty, Ms13OsnConfig config) {
        super(Ms13OsnPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init(int maxN) throws MpcAbortException {
        setInitInput(maxN);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        // 每次调用maxWidth次COT协议，一共调用maxSwitchNum次
        cotSender.init(delta, maxSwitchNum);
        stopWatch.stop();
        long cotInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, cotInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public OsnPartyOutput osn(Vector<byte[]> inputVector, int byteLength) throws MpcAbortException {
        setPtoInput(inputVector, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> inputCorrectionPayload = generateInputCorrectionPayload();
        DataPacketHeader inputCorrectionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT_CORRECTIONS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(inputCorrectionHeader, inputCorrectionPayload));
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, inputCorrectionTime, "Sender computes input correlation");

        stopWatch.start();
        CotSenderOutput[] cotSenderOutputs = new CotSenderOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            cotSenderOutputs[levelIndex] = cotSender.send(width);
        }
        handleCotSenderOutputs(cotSenderOutputs);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, cotTime, "Sender runs COTs");

        stopWatch.start();
        List<byte[]> switchCorrectionPayload = generateSwitchCorrectionPayload();
        DataPacketHeader switchCorrectionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(switchCorrectionHeader, switchCorrectionPayload));
        OsnPartyOutput senderOutput = new OsnPartyOutput(byteLength, senderShareVector);
        switchWireExtendKey0s = null;
        switchWireExtendKey1s = null;
        senderShareVector = null;
        stopWatch.stop();
        long switchCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, switchCorrectionTime, "Sender switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    List<byte[]> generateInputCorrectionPayload() {
        // 生成输入导线遮蔽值
        senderShareVector = IntStream.range(0, n)
            .mapToObj(widthIndex -> {
                byte[] inputWireMask = new byte[byteLength];
                secureRandom.nextBytes(inputWireMask);
                return inputWireMask;
            })
            .collect(Collectors.toCollection(Vector::new));
        // 初始化交换导线遮蔽值
        switchWireMask0s = new byte[level][width][byteLength];
        switchWireMask1s = new byte[level][width][byteLength];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            for (int widthIndex = 0; widthIndex < width; widthIndex++) {
                secureRandom.nextBytes(switchWireMask0s[levelIndex][widthIndex]);
                secureRandom.nextBytes(switchWireMask1s[levelIndex][widthIndex]);
            }
        }
        // 先发送输入导线，再发送纠正消息，因为纠正过程中会把输入导线替换为输出导线
        return IntStream.range(0, n)
            .mapToObj(index -> BytesUtils.xor(senderShareVector.elementAt(index), inputVector.elementAt(index)))
            .collect(Collectors.toList());
    }

    private void handleCotSenderOutputs(CotSenderOutput[] cotSenderOutputs) {
        switchWireExtendKey0s = new byte[level][width][];
        switchWireExtendKey1s = new byte[level][width][];
        int extendByteLength = byteLength * 2;
        if (extendByteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度的2倍小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
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
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
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
        byte[][][] corrections = new byte[switchNum][2][byteLength * 2];
        int logN = (int) Math.ceil(DoubleUtils.log2(n));
        osnForkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        genSwitchCorrections(logN, 0, 0, senderShareVector, corrections);

        return Arrays.stream(corrections).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    private void genSwitchCorrections(int subLogN, int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                      byte[][][] corrections) {
        int subN = subShareInputs.size();
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
            Vector<byte[]> subTopShareInputs = new Vector<>(subTopN);
            Vector<byte[]> subBottomShareInputs = new Vector<>(subBottomN);
            // 构造Benes网络左侧的纠正值
            for (int i = 0; i < subN - 1; i += 2) {
                // 输入导线遮蔽值
                byte[] inputMask0 = subShareInputs.elementAt(i);
                byte[] inputMask1 = subShareInputs.elementAt(i ^ 1);
                int widthIndex = permIndex + i / 2;
                byte[] outputMask0 = switchWireMask0s[levelIndex][widthIndex];
                byte[] outputMask1 = switchWireMask1s[levelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, levelIndex, widthIndex, corrections);
                subTopShareInputs.add(outputMask0);
                subBottomShareInputs.add(outputMask1);
            }
            // 如果是奇数个输入，则下方子Benes网络需要再增加一个输入
            if (subN % 2 == 1) {
                subBottomShareInputs.add(subShareInputs.elementAt(subN - 1));
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
                byte[] inputMask0 = subTopShareInputs.elementAt(i / 2);
                byte[] inputMask1 = subBottomShareInputs.elementAt(i / 2);
                int rightLevelIndex = levelIndex + subLevel - 1;
                int widthIndex = permIndex + i / 2;
                byte[] outputMask0 = switchWireMask0s[rightLevelIndex][widthIndex];
                byte[] outputMask1 = switchWireMask1s[rightLevelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, rightLevelIndex, widthIndex, corrections);
                subShareInputs.set(i, outputMask0);
                subShareInputs.set(i ^ 1, outputMask1);
            }
            // 如果是奇数个输入，则下方子Benes网络需要多替换一个输出导线遮蔽值
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subShareInputs.set(subN - 1, subBottomShareInputs.elementAt(idx - 1));
            }
        }
    }

    private void genSingleSwitchCorrection(int subLogN, int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                           byte[][][] corrections) {
        // 输入导线遮蔽值
        byte[] inputMask0 = subShareInputs.elementAt(0);
        byte[] inputMask1 = subShareInputs.elementAt(1);
        // 输出导线遮蔽值
        int singleLevelIndex = (subLogN == 1) ? levelIndex : levelIndex + 1;
        byte[] outputMask0 = switchWireMask0s[singleLevelIndex][permIndex];
        byte[] outputMask1 = switchWireMask1s[singleLevelIndex][permIndex];
        setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, singleLevelIndex, permIndex, corrections);
        subShareInputs.set(0, outputMask0);
        subShareInputs.set(1, outputMask1);
    }

    private void genTripleSwitchCorrection(int levelIndex, int permIndex, Vector<byte[]> subShareInputs,
                                           byte[][][] corrections) {
        // 第一组输入导线遮蔽值
        byte[] inputMask00 = subShareInputs.elementAt(0);
        byte[] inputMask01 = subShareInputs.elementAt(1);
        // 第一组输出导线遮蔽值
        byte[] outputMask00 = switchWireMask0s[levelIndex][permIndex];
        byte[] outputMask01 = switchWireMask1s[levelIndex][permIndex];
        setCorrection(inputMask00, inputMask01, outputMask00, outputMask01, levelIndex, permIndex, corrections);
        subShareInputs.set(0, outputMask00);
        subShareInputs.set(1, outputMask01);

        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs.elementAt(1);
        byte[] inputMask11 = subShareInputs.elementAt(2);
        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        byte[] outputMask10 = switchWireMask0s[levelIndex1][permIndex];
        byte[] outputMask11 = switchWireMask1s[levelIndex1][permIndex];
        setCorrection(inputMask10, inputMask11, outputMask10, outputMask11, levelIndex1, permIndex, corrections);
        subShareInputs.set(1, outputMask10);
        subShareInputs.set(2, outputMask11);

        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs.elementAt(0);
        byte[] inputMask21 = subShareInputs.elementAt(1);
        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        byte[] outputMask20 = switchWireMask0s[levelIndex2][permIndex];
        byte[] outputMask21 = switchWireMask1s[levelIndex2][permIndex];
        setCorrection(inputMask20, inputMask21, outputMask20, outputMask21, levelIndex2, permIndex, corrections);
        subShareInputs.set(0, outputMask20);
        subShareInputs.set(1, outputMask21);
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1,
                               int levelIndex, int widthIndex, byte[][][] corrections) {
        // 消息0 = M_(i, 1) ⊕ M_(j, 1) || M_(i, 2) ⊕ M_(j, 2)
        byte[] message0 = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask0), 0, message0, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask1), 0, message0, byteLength, byteLength);
        BytesUtils.xori(message0, switchWireExtendKey0s[levelIndex][widthIndex]);
        corrections[levelIndex * width + widthIndex][0] = message0;
        // 消息1 = M_(i, 2) ⊕ M_(j, 1) || M_(i, 1) ⊕ M_(j, 2)
        byte[] message1 = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask0), 0, message1, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask1), 0, message1, byteLength, byteLength);
        BytesUtils.xori(message1, switchWireExtendKey1s[levelIndex][widthIndex]);
        corrections[levelIndex * width + widthIndex][1] = message1;
    }
}
