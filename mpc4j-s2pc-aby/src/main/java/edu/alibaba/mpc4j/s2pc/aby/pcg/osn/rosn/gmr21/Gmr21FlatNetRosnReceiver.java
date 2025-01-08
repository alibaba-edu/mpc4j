package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetwork;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21FlatNetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GMR21 flat Network Random OSN receiver
 *
 * @author Feng Han
 * @date 2024/7/31
 */
public class Gmr21FlatNetRosnReceiver extends AbstractNetRosnReceiver {
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
     * receiver share vector
     */
    private byte[][] receiverShareVector;
    /**
     * switch wire masks 0
     */
    private byte[][] switchWireMask0;
    /**
     * switch wire masks 1
     */
    private byte[][] switchWireMask1;
    /**
     * the index of switch, if the wire is directly linked, then the value is -1
     */
    private int[][] map2SwitchIndex;
    /**
     * the input index of each wire
     */
    private int[][] map2InputIndex;
    /**
     * Crhf
     */
    private Crhf crhf;
    /**
     * Prg
     */
    private Prg prg;

    public Gmr21FlatNetRosnReceiver(Rpc receiverRpc, Party senderParty, Gmr21FlatNetRosnConfig config) {
        super(Gmr21FlatNetRosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        map2SwitchIndex = benesNetwork.getLayerSwitchIndexes();
        map2InputIndex = benesNetwork.getFixedLayerPermutations();
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Receiver runs COTs");

        stopWatch.start();
        if (2 * byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // we only need to use more efficient CRHF instead of PRG
            crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        } else {
            // we need to use PRG
            prg = PrgFactory.createInstance(envType, byteLength * 2);
        }
        receiverShareVector = new byte[num][byteLength];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            List<byte[]> switchCorrectionPayload = receiveOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(),
                width, byteLength);
            handleCotReceiverOutputs(cotReceiverOutputs[levelIndex]);
            cotReceiverOutputs[levelIndex] = null;
            handleSwitchCorrectionPayload(switchCorrectionPayload, levelIndex);
        }
        RosnReceiverOutput receiverOutput = RosnReceiverOutput.create(pi, receiverShareVector);
        switchWireMask0 = null;
        switchWireMask1 = null;
        receiverShareVector = null;
        benesNetwork = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Receiver switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleCotReceiverOutputs(CotReceiverOutput cotReceiverOutputs) {
        int totalByteLen = byteLength * 2;
        switchWireMask0 = new byte[width][];
        switchWireMask1 = new byte[width][];
        // 要用width做并发，因为level数量太少了，并发效果不好
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, width).parallel() : IntStream.range(0, width);
        if (totalByteLen <= CommonConstants.BLOCK_BYTE_LENGTH) {
            widthIndexIntStream.forEach(widthIndex -> {
                byte[] switchWireMask = cotReceiverOutputs.getRb(widthIndex);
                switchWireMask = crhf.hash(switchWireMask);
                switchWireMask0[widthIndex] = Arrays.copyOf(switchWireMask, byteLength);
                switchWireMask1[widthIndex] = Arrays.copyOfRange(switchWireMask, byteLength, totalByteLen);
            });
        } else {
            widthIndexIntStream.forEach(widthIndex -> {
                byte[] switchWireMask = cotReceiverOutputs.getRb(widthIndex);
                switchWireMask = prg.extendToBytes(switchWireMask);
                switchWireMask0[widthIndex] = Arrays.copyOf(switchWireMask, byteLength);
                switchWireMask1[widthIndex] = Arrays.copyOfRange(switchWireMask, byteLength, totalByteLen);
            });
        }
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload, int levelIndex) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == width);
        byte[] gates = benesNetwork.getGates(levelIndex);
        byte[][] corrections = switchCorrectionPayload.toArray(new byte[0][]);
        // program
        int[] currentMap2InputIndex = map2InputIndex[levelIndex];
        int[] currentMap2WidthIndex = map2SwitchIndex[levelIndex];
        byte[][] beforeShareVector = BytesUtils.clone(receiverShareVector);
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        intStream.forEach(i -> {
            if (currentMap2WidthIndex[i] == -1) {
                // 如果是一个直接连线
                receiverShareVector[i] = beforeShareVector[currentMap2InputIndex[i]];
            } else if (i > 0 && currentMap2WidthIndex[i] == currentMap2WidthIndex[i - 1]) {
                int widthIndex = currentMap2WidthIndex[i];
                byte[] inputMask0 = beforeShareVector[currentMap2InputIndex[i - 1]];
                byte[] inputMask1 = beforeShareVector[currentMap2InputIndex[i]];
                byte flag = gates[widthIndex];
                int leftS = flag == 1 ? 1 : 0;
                // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                byte[][] outputMasks = getOutputMasks(widthIndex, corrections, flag);
                BytesUtils.xori(outputMasks[leftS], inputMask0);
                BytesUtils.xori(outputMasks[1 - leftS], inputMask1);
                receiverShareVector[i - 1] = outputMasks[0];
                receiverShareVector[i] = outputMasks[1];
            }
        });
    }

    private byte[][] getOutputMasks(int widthIndex, byte[][] corrections, byte flag) {
        byte[] choiceMessage = corrections[widthIndex];
        byte[][] outputMasks = new byte[2][byteLength];
        if (flag == 1) {
            System.arraycopy(choiceMessage, 0, outputMasks[0], 0, byteLength);
            System.arraycopy(choiceMessage, byteLength, outputMasks[1], 0, byteLength);
            BytesUtils.xori(outputMasks[0], switchWireMask0[widthIndex]);
            BytesUtils.xori(outputMasks[1], switchWireMask1[widthIndex]);
        } else {
            outputMasks[0] = switchWireMask0[widthIndex];
            outputMasks[1] = switchWireMask1[widthIndex];
        }
        return outputMasks;
    }

}
