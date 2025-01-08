package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetwork;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24FlatNetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LLL24 flat Network Random OSN receiver
 *
 * @author Feng Han
 * @date 2024/7/29
 */
public class Lll24FlatNetRosnReceiver extends AbstractNetRosnReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lll24FlatNetRosnReceiver.class);
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
     * receiver share vector
     */
    private byte[][] receiverShareVector;
    /**
     * switch wire masks in one specific layer
     */
    private byte[][] switchWireMask;
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

    public Lll24FlatNetRosnReceiver(Rpc receiverRpc, Party senderParty, Lll24FlatNetRosnConfig config) {
        super(Lll24FlatNetRosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        CotReceiverOutput[] cotReceiverOutputs = new CotReceiverOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            boolean[] choices = generateChoices(levelIndex);
            cotReceiverOutputs[levelIndex] = cotReceiver.receive(choices);
        }
        map2SwitchIndex = waksmanNetwork.getLayerSwitchIndexes();
        map2InputIndex = waksmanNetwork.getFixedLayerPermutations();
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Receiver runs COTs");

        stopWatch.start();
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // we only need to use more efficient CRHF instead of PRG
            crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        } else {
            // we need to use PRG
            prg = PrgFactory.createInstance(envType, byteLength);
        }
        receiverShareVector = new byte[num][byteLength];

        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            LOGGER.info("switching level: {}", levelIndex);
            sendOtherPartyEqualSizePayload(PtoStep.SYNCHRONIZE_MSG.ordinal(), Collections.singletonList(new byte[]{0}));

            StopWatch stopWatch2 = new StopWatch();
            stopWatch2.start();
            List<byte[]> switchCorrectionPayload = receiveOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(),
                waksmanNetwork.getWidth(levelIndex), byteLength);

            stopWatch2.stop();
            long receiveTime = stopWatch2.getTime(TimeUnit.MILLISECONDS);
            stopWatch2.reset();
            stopWatch2.start();
            handleCotReceiverOutputs(cotReceiverOutputs[levelIndex], levelIndex);
            cotReceiverOutputs[levelIndex] = null;
            handleSwitchCorrectionPayload(switchCorrectionPayload, levelIndex);
            stopWatch2.stop();
            long processTime = stopWatch2.getTime(TimeUnit.MILLISECONDS);
            stopWatch2.reset();
            LOGGER.info("receive time:{}, process time:{}, total time:{}", receiveTime, processTime, receiveTime + processTime);
        }
        RosnReceiverOutput receiverOutput = RosnReceiverOutput.create(pi, receiverShareVector);
        switchWireMask = null;
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

    private void handleCotReceiverOutputs(CotReceiverOutput cotReceiverOutputs, int levelIndex) {
        switchWireMask = new byte[maxWidth][];
        // padding to max width
        boolean[] paddingChoices = new boolean[maxWidth];
        byte[][] paddingRbArray = new byte[maxWidth][];
        int index = 0;
        for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
            byte[] gates = waksmanNetwork.getGates(levelIndex);
            if (gates[widthIndex] != 2) {
                paddingRbArray[widthIndex] = cotReceiverOutputs.getRb(index);
                paddingChoices[widthIndex] = cotReceiverOutputs.getChoice(index);
                index++;
            } else {
                paddingRbArray[widthIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                paddingChoices[widthIndex] = false;
            }
        }
        assert index == cotReceiverOutputs.getNum();
        CotReceiverOutput paddingCotReceiverOutputs = CotReceiverOutput.create(paddingChoices, paddingRbArray);
        // level = O(log(n)) but width = O(n), batch in width
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, maxWidth).parallel() : IntStream.range(0, maxWidth);
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            widthIndexIntStream.forEach(widthIndex -> {
                if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                    byte[] tmpSwitchWireMask = paddingCotReceiverOutputs.getRb(widthIndex);
                    switchWireMask[widthIndex] = Arrays.copyOf(crhf.hash(tmpSwitchWireMask), byteLength);
                }
            });
        } else {
            widthIndexIntStream.forEach(widthIndex -> {
                if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                    byte[] tmpSwitchWireMask = paddingCotReceiverOutputs.getRb(widthIndex);
                    switchWireMask[widthIndex] = prg.extendToBytes(tmpSwitchWireMask);
                }
            });
        }
    }

    private void handleSwitchCorrectionPayload(List<byte[]> switchCorrectionPayload, int levelIndex) throws MpcAbortException {
        int width = waksmanNetwork.getWidth(levelIndex);
        MpcAbortPreconditions.checkArgument(switchCorrectionPayload.size() == width);
        byte[] gates = waksmanNetwork.getGates(levelIndex);
        byte[][] corrections = new byte[maxWidth][byteLength];
        byte[][] reducedCorrection = switchCorrectionPayload.toArray(new byte[0][]);
        int index = 0;
        for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
            if (gates[widthIndex] != 2) {
                corrections[widthIndex] = reducedCorrection[index];
                index++;
            }
        }
        assert index == width;
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
                byte flag = waksmanNetwork.getGates(levelIndex)[widthIndex];
                int leftS = flag == 1 ? 1 : 0;
                if (flag != 2) {
                    // 计算输出导线遮蔽值，左侧Benes网络要交换输出导线的位置
                    byte[][] outputMasks = getOutputMasks(widthIndex, corrections, flag);
                    BytesUtils.xori(outputMasks[leftS], inputMask0);
                    BytesUtils.xori(outputMasks[1 - leftS], inputMask1);
                    receiverShareVector[i - 1] = outputMasks[0];
                    receiverShareVector[i] = outputMasks[1];
                } else {
                    receiverShareVector[i - 1] = inputMask0;
                    receiverShareVector[i] = inputMask1;
                }
            }
        });
    }


    private byte[][] getOutputMasks(int widthIndex, byte[][] corrections, byte flag) {
        byte[] choiceMessage = corrections[widthIndex];
        BytesUtils.xori(choiceMessage, switchWireMask[widthIndex]);
        byte[][] outputMasks = new byte[2][byteLength];
        int index = flag == 1 ? 1 : 0;
        outputMasks[index] = switchWireMask[widthIndex];
        outputMasks[1 - index] = choiceMessage;
        return outputMasks;
    }
}
