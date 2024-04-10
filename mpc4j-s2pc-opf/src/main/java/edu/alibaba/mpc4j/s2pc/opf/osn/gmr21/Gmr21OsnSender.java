package edu.alibaba.mpc4j.s2pc.opf.osn.gmr21;

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
import edu.alibaba.mpc4j.s2pc.opf.osn.AbstractOsnSender;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-OSN协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
public class Gmr21OsnSender extends AbstractOsnSender {
    /**
     * COT协议发送方
     */
    private final CotSender cotSender;
    /**
     * 发送方向量分享值
     */
    private Vector<byte[]> senderShareVector;
    /**
     * 第0组交换网络交换导线
     */
    private byte[][][] switchWireMask0s;
    /**
     * 交换网络交换导线纠正值
     */
    private byte[][][] switchWireMask1s;
    /**
     * 执行OSN所用的线程池
     */
    private ForkJoinPool osnForkJoinPool;

    public Gmr21OsnSender(Rpc senderRpc, Party receiverParty, Gmr21OsnConfig config) {
        super(Gmr21OsnPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
            encodeTaskId, getPtoDesc().getPtoId(), Gmr21OsnPtoDesc.PtoStep.SENDER_SEND_INPUT_CORRECTIONS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(inputCorrectionHeader, inputCorrectionPayload));
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, inputCorrectionTime, "Sender computes input correlations");

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
            encodeTaskId, getPtoDesc().getPtoId(), Gmr21OsnPtoDesc.PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(switchCorrectionHeader, switchCorrectionPayload));
        OsnPartyOutput senderOutput = new OsnPartyOutput(byteLength, senderShareVector);
        switchWireMask0s = null;
        switchWireMask1s = null;
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
        // 先发送输入导线，再发送纠正消息，因为纠正过程中会把输入导线替换为输出导线
        return IntStream.range(0, n)
            .mapToObj(index -> BytesUtils.xor(senderShareVector.elementAt(index), inputVector.elementAt(index)))
            .collect(Collectors.toList());
    }

    private void handleCotSenderOutputs(CotSenderOutput[] cotSenderOutputs) {
        switchWireMask0s = new byte[level][width][byteLength];
        switchWireMask1s = new byte[level][width][byteLength];
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // 字节长度小于等于128比特时，只需要抗关联哈希函数
            Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
            // 要用width做并发，因为level数量太少了，并发效果不好
            IntStream widthIndexIntStream = IntStream.range(0, width);
            widthIndexIntStream = parallel ? widthIndexIntStream.parallel() : widthIndexIntStream;
            widthIndexIntStream.forEach(widthIndex -> {
                for (int levelIndex = 0; levelIndex < level; levelIndex++) {
                    byte[] switchWireMask0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    switchWireMask0 = Arrays.copyOf(crhf.hash(switchWireMask0), byteLength);
                    switchWireMask0s[levelIndex][widthIndex] = switchWireMask0;
                    byte[] switchWireMask1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    switchWireMask1 = Arrays.copyOf(crhf.hash(switchWireMask1), byteLength);
                    BytesUtils.xori(switchWireMask1, switchWireMask0s[levelIndex][widthIndex]);
                    switchWireMask1s[levelIndex][widthIndex] = switchWireMask1;
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
                    byte[] switchWireMask0 = cotSenderOutputs[levelIndex].getR0(widthIndex);
                    switchWireMask0 = prg.extendToBytes(switchWireMask0);
                    switchWireMask0s[levelIndex][widthIndex] = switchWireMask0;
                    byte[] switchWireMask1 = cotSenderOutputs[levelIndex].getR1(widthIndex);
                    switchWireMask1 = prg.extendToBytes(switchWireMask1);
                    BytesUtils.xori(switchWireMask1, switchWireMask0s[levelIndex][widthIndex]);
                    switchWireMask1s[levelIndex][widthIndex] = switchWireMask1;
                }
            });
        }
    }

    private List<byte[]> generateSwitchCorrectionPayload() {
        byte[][][] corrections = new byte[level][width][byteLength * 2];
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
                // M_(j, 0) = R_0
                byte[] outputMask0 = switchWireMask0s[levelIndex][widthIndex];
                // M_(j, 1) = R_0 ⊕ R_1
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
                // 参考https://github.com/dujiajun/PSU/blob/master/osn/OSNSender.cpp实现并发
                if (osnForkJoinPool.getParallelism() - osnForkJoinPool.getActiveThreadCount() > 0) {
                    ForkJoinTask<?> topTask = osnForkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex, subTopShareInputs, corrections)
                    );
                    ForkJoinTask<?> subTask = osnForkJoinPool.submit(() -> genSwitchCorrections(
                        subLogN - 1, levelIndex + 1, permIndex + subN / 4,
                        subBottomShareInputs, corrections)
                    );
                    topTask.join();
                    subTask.join();
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
                // M_(j, 0) = R_0
                byte[] outputMask0 = switchWireMask0s[rightLevelIndex][widthIndex];
                // M_(j, 1) = R_0 ⊕ R_1
                byte[] outputMask1 = switchWireMask1s[rightLevelIndex][widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, rightLevelIndex, widthIndex,
                    corrections);
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
        // M_(j, 0) = R_0
        byte[] outputMask0 = switchWireMask0s[singleLevelIndex][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
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
        // M_(j, 0) = R_0
        byte[] outputMask00 = switchWireMask0s[levelIndex][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask01 = switchWireMask1s[levelIndex][permIndex];
        setCorrection(inputMask00, inputMask01, outputMask00, outputMask01, levelIndex, permIndex, corrections);
        subShareInputs.set(0, outputMask00);
        subShareInputs.set(1, outputMask01);

        // 第二组输入导线遮蔽值
        byte[] inputMask10 = subShareInputs.elementAt(1);
        byte[] inputMask11 = subShareInputs.elementAt(2);
        // 第二组输出导线遮蔽值
        int levelIndex1 = levelIndex + 1;
        // M_(j, 0) = R_0
        byte[] outputMask10 = switchWireMask0s[levelIndex1][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask11 = switchWireMask1s[levelIndex1][permIndex];
        setCorrection(inputMask10, inputMask11, outputMask10, outputMask11, levelIndex1, permIndex, corrections);
        subShareInputs.set(1, outputMask10);
        subShareInputs.set(2, outputMask11);

        // 第三组输入导线遮蔽值
        byte[] inputMask20 = subShareInputs.elementAt(0);
        byte[] inputMask21 = subShareInputs.elementAt(1);
        // 第三组输出导线遮蔽值
        int levelIndex2 = levelIndex + 2;
        // M_(j, 0) = R_0
        byte[] outputMask20 = switchWireMask0s[levelIndex2][permIndex];
        // M_(j, 1) = R_0 ⊕ R_1
        byte[] outputMask21 = switchWireMask1s[levelIndex2][permIndex];
        setCorrection(inputMask20, inputMask21, outputMask20, outputMask21, levelIndex2, permIndex, corrections);
        subShareInputs.set(0, outputMask20);
        subShareInputs.set(1, outputMask21);
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1,
                               int levelIndex, int widthIndex, byte[][][] corrections) {
        // 消息 = 消息0 || 消息1
        byte[] message = new byte[byteLength * 2];
        // 消息0 = M_(i, 0) ⊕ R_0 = M_(i, 0) ⊕ M_(j, 0)
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask0), 0, message, 0, byteLength);
        // 消息1 = M_(i, 1) ⊕ R_0 ⊕ R_1 = M_(i, 1) ⊕ M_(j, 1)
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask1), 0, message, byteLength, byteLength);
        corrections[levelIndex][widthIndex] = message;
    }
}
