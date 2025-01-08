package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetwork;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24.Lll24FlatNetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LLL24 flat Network Random OSN sender
 *
 * @author Feng Han
 * @date 2024/7/29
 */
public class Lll24FlatNetRosnSender extends AbstractNetRosnSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(Lll24FlatNetRosnSender.class);
    /**
     * Waksman network
     */
    private WaksmanNetwork<byte[]> waksmanNetwork;
    /**
     * max width
     */
    private int maxWidth;
    /**
     * sender share vector
     */
    private byte[][] senderShareVector;
    /**
     * switch wire masks corresponding to 0 in one specific level
     */
    private byte[][] switchWireMask0;
    /**
     * switch wire masks corresponding to 1  in one specific level
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

    public Lll24FlatNetRosnSender(Rpc senderRpc, Party receiverParty, Lll24FlatNetRosnConfig config) {
        super(Lll24FlatNetRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
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
        // create an empty Waksman network, used only for locating empty switches.
        int[] pi = IntStream.range(0, num).toArray();
        waksmanNetwork = WaksmanNetworkFactory.createInstance(envType, pi);
        int level = waksmanNetwork.getLevel();
        maxWidth = waksmanNetwork.getMaxWidth();
        CotSenderOutput[] cotSenderOutputs = new CotSenderOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
//            LOGGER.info("cot level: {}", levelIndex);
            int width = waksmanNetwork.getWidth(levelIndex);
            cotSenderOutputs[levelIndex] = cotSender.send(width);
        }
        map2SwitchIndex = waksmanNetwork.getLayerSwitchIndexes();
        map2InputIndex = waksmanNetwork.getFixedLayerPermutations();
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Sender runs COTs");

        stopWatch.start();
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // we only need to use more efficient CRHF instead of PRG
            crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        } else {
            // we need to use PRG
            prg = PrgFactory.createInstance(envType, byteLength);
        }
        senderShareVector = BytesUtils.randomByteArrayVector(num, byteLength, secureRandom);
        // save the input vector
        byte[][] inputMask = BytesUtils.clone(senderShareVector);
        for (int levelIndex = 0; levelIndex < waksmanNetwork.getLevel(); levelIndex++) {
            // extend ot result
            LOGGER.info("switching level: {}", levelIndex);
            handleCotSenderOutputsInLayer(cotSenderOutputs[levelIndex], levelIndex);
            cotSenderOutputs[levelIndex] = null;
            List<byte[]> switchCorrectionPayload = generateSwitchCorrectionPayload(levelIndex);


            sendOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), switchCorrectionPayload);
            // add one msg received from receiver to avoid too much msg stacked in RPC
            receiveOtherPartyPayload(PtoStep.SYNCHRONIZE_MSG.ordinal());
        }
        RosnSenderOutput senderOutput = RosnSenderOutput.create(inputMask, senderShareVector);
        senderShareVector = null;
        stopWatch.stop();
        long inputCorrectionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, inputCorrectionTime, "Sender switches correlations");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleCotSenderOutputsInLayer(CotSenderOutput cotSenderOutputs, int levelIndex) {
        byte[] delta = cotSenderOutputs.getDelta();
        byte[][] paddingR0Array = new byte[maxWidth][];
        int index = 0;
        for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
            byte[] gates = waksmanNetwork.getGates(levelIndex);
            if (gates[widthIndex] != 2) {
                paddingR0Array[widthIndex] = cotSenderOutputs.getR0(index);
                index++;
            } else {
                paddingR0Array[widthIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            }
        }
        assert index == cotSenderOutputs.getNum();
        CotSenderOutput paddingCotSenderOutputs = CotSenderOutput.create(delta, paddingR0Array);
        switchWireMask0 = new byte[maxWidth][];
        switchWireMask1 = new byte[maxWidth][];
        // level = O(log(n)) but width = O(n), batch in width
        byte[] switchFlag = waksmanNetwork.getGates(levelIndex);
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, maxWidth).parallel() : IntStream.range(0, maxWidth);
        if (byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            widthIndexIntStream.forEach(widthIndex -> {
                if (switchFlag[widthIndex] != 2) {
                    byte[] otR0 = paddingCotSenderOutputs.getR0(widthIndex);
                    switchWireMask0[widthIndex] = Arrays.copyOf(crhf.hash(otR0), byteLength);
                    byte[] otR1 = paddingCotSenderOutputs.getR1(widthIndex);
                    switchWireMask1[widthIndex] = Arrays.copyOf(crhf.hash(otR1), byteLength);
                }
            });
        } else {
            widthIndexIntStream.forEach(widthIndex -> {
                if (switchFlag[widthIndex] != 2) {
                    byte[] otR0 = paddingCotSenderOutputs.getR0(widthIndex);
                    switchWireMask0[widthIndex] = prg.extendToBytes(otR0);
                    byte[] otR1 = paddingCotSenderOutputs.getR1(widthIndex);
                    switchWireMask1[widthIndex] = prg.extendToBytes(otR1);
                }
            });
        }
    }

    private List<byte[]> generateSwitchCorrectionPayload(int levelIndex) {
        // programming
        int width = waksmanNetwork.getWidth(levelIndex);
        byte[] gates = waksmanNetwork.getGates(levelIndex);
        int[] currentMap2InputIndex = map2InputIndex[levelIndex];
        int[] currentMap2WidthIndex = map2SwitchIndex[levelIndex];
        byte[][] beforeShareVector = BytesUtils.clone(senderShareVector);
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        intStream.forEach(i -> {
            if (currentMap2WidthIndex[i] == -1) {
                // 如果是一个直接连线
                senderShareVector[i] = beforeShareVector[currentMap2InputIndex[i]];
            } else if (i > 0 && currentMap2WidthIndex[i] == currentMap2WidthIndex[i - 1]) {
                int widthIndex = currentMap2WidthIndex[i];
                byte[] inputMask0 = beforeShareVector[currentMap2InputIndex[i - 1]];
                byte[] inputMask1 = beforeShareVector[currentMap2InputIndex[i]];
                if (waksmanNetwork.getGates(levelIndex)[widthIndex] != 2) {
                    // M_(j, 0) = R_0
                    senderShareVector[i - 1] = BytesUtils.clone(switchWireMask0[widthIndex]);
                    // M_(j, 1) = R_0 ⊕ R_1
                    senderShareVector[i] = BytesUtils.clone(switchWireMask1[widthIndex]);
                    setCorrection(inputMask0, inputMask1, senderShareVector[i - 1], senderShareVector[i], widthIndex);
                } else {
                    senderShareVector[i - 1] = inputMask0;
                    senderShareVector[i] = inputMask1;
                }
            }
        });
        // reducing corrections
        byte[][] reducedCorrections = new byte[width][];
        int index = 0;
        for (int widthIndex = 0; widthIndex < maxWidth; widthIndex++) {
            if (gates[widthIndex] != 2) {
                reducedCorrections[index++] = switchWireMask0[widthIndex];
            }
        }
        assert index == width;
        return Arrays.stream(reducedCorrections).collect(Collectors.toList());
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1, int widthIndex) {
        // compute the real mask
        BytesUtils.xori(outputMask0, inputMask0);
        BytesUtils.xori(outputMask1, inputMask0);
        // correctness = M_(i, 0) ⊕ M_(j, 0) ⊕ M_(i, 1) ⊕ M_(j, 1)
        BytesUtils.xori(switchWireMask0[widthIndex], inputMask1);
        BytesUtils.xori(switchWireMask0[widthIndex], outputMask1);
    }
}
