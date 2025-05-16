package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21;

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
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetwork;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractNetRosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21FlatNetRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21 flat Network Random OSN sender
 *
 * @author Feng Han
 * @date 2024/7/31
 */
public class Gmr21FlatNetRosnSender extends AbstractNetRosnSender {
    /**
     * width
     */
    private int width;
    /**
     * sender share vector
     */
    private byte[][] senderShareVector;
    /**
     * switch wire masks corresponding to 0
     */
    private byte[][] switchWireMask0;
    /**
     * switch wire masks corresponding to 1
     */
    private byte[][] switchWireMask1;
    /**
     * message
     */
    private byte[][] correctionsMask;
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

    public Gmr21FlatNetRosnSender(Rpc senderRpc, Party receiverParty, Gmr21FlatNetRosnConfig config) {
        super(Gmr21FlatNetRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
        int level = PermutationNetworkUtils.getLevel(num);
        width = PermutationNetworkUtils.getMaxWidth(num);
        CotSenderOutput[] cotSenderOutputs = new CotSenderOutput[level];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            cotSenderOutputs[levelIndex] = cotSender.send(width);
        }
        BenesNetwork<byte[]> benesNetwork = BenesNetworkFactory.createInstance(envType, IntStream.range(0, num).toArray());
        map2SwitchIndex = benesNetwork.getLayerSwitchIndexes();
        map2InputIndex = benesNetwork.getFixedLayerPermutations();
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime, "Sender runs COTs");

        stopWatch.start();
        if (2 * byteLength <= CommonConstants.BLOCK_BYTE_LENGTH) {
            // we only need to use more efficient CRHF instead of PRG
            crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        } else {
            // we need to use PRG
            prg = PrgFactory.createInstance(envType, byteLength * 2);
        }
        senderShareVector = BytesUtils.randomByteArrayVector(num, byteLength, secureRandom);
        byte[][] inputMask = BytesUtils.clone(senderShareVector);
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            // extend ot result
            handleCotSenderOutputs(cotSenderOutputs[levelIndex]);
            cotSenderOutputs[levelIndex] = null;
            List<byte[]> switchCorrectionPayload = generateSwitchCorrectionPayload(levelIndex);
            sendOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_SWITCH_CORRECTIONS.ordinal(), switchCorrectionPayload);
            // add one msg received from receiver to avoid too much msg stacked in RPC
            if (level >= 39 && ((levelIndex + 1) % 4 == 0 || levelIndex == level - 1)) {
                receiveOtherPartyPayload(PtoStep.SYNCHRONIZE_MSG.ordinal());
            }
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

    private void handleCotSenderOutputs(CotSenderOutput cotSenderOutputs) {
        int totalByteLen = byteLength * 2;
        switchWireMask0 = new byte[width][byteLength];
        switchWireMask1 = new byte[width][byteLength];
        correctionsMask = new byte[width][totalByteLen];
        // 要用width做并发，因为level数量太少了，并发效果不好
        IntStream widthIndexIntStream = parallel ? IntStream.range(0, width).parallel() : IntStream.range(0, width);
        if (totalByteLen <= CommonConstants.BLOCK_BYTE_LENGTH) {
            widthIndexIntStream.forEach(widthIndex -> {
                byte[] otR0 = cotSenderOutputs.getR0(widthIndex);
                otR0 = crhf.hash(otR0);
                switchWireMask0[widthIndex] = Arrays.copyOf(otR0, byteLength);
                switchWireMask1[widthIndex] = Arrays.copyOfRange(otR0, byteLength, totalByteLen);
                byte[] otR1 = cotSenderOutputs.getR1(widthIndex);
                correctionsMask[widthIndex] = Arrays.copyOf(crhf.hash(otR1), totalByteLen);
            });
        } else {
            widthIndexIntStream.forEach(widthIndex -> {
                byte[] otR0 = cotSenderOutputs.getR0(widthIndex);
                otR0 = prg.extendToBytes(otR0);
                switchWireMask0[widthIndex] = Arrays.copyOf(otR0, byteLength);
                switchWireMask1[widthIndex] = Arrays.copyOfRange(otR0, byteLength, totalByteLen);
                byte[] otR1 = cotSenderOutputs.getR1(widthIndex);
                correctionsMask[widthIndex] = prg.extendToBytes(otR1);
            });
        }
    }

    private List<byte[]> generateSwitchCorrectionPayload(int levelIndex) {
        // programming
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
                // M_(j, 0) = R_0
                byte[] outputMask0 = switchWireMask0[widthIndex];
                // M_(j, 1) = R_0 ⊕ R_1
                byte[] outputMask1 = switchWireMask1[widthIndex];
                setCorrection(inputMask0, inputMask1, outputMask0, outputMask1, widthIndex);
                senderShareVector[i - 1] = outputMask0;
                senderShareVector[i] = outputMask1;
            }
        });
        return Arrays.stream(correctionsMask).collect(Collectors.toList());
    }

    private void setCorrection(byte[] inputMask0, byte[] inputMask1, byte[] outputMask0, byte[] outputMask1, int widthIndex) {
        // compute the real mask, mask = G(R0) ⊕ inputMask
        BytesUtils.xori(outputMask0, inputMask0);
        BytesUtils.xori(outputMask1, inputMask1);
        // correctness = M_(i, 1) ⊕ M_(j, 0) || M_(i, 0) ⊕ M_(j, 1)
        byte[] message = new byte[byteLength * 2];
        System.arraycopy(BytesUtils.xor(inputMask1, outputMask0), 0, message, 0, byteLength);
        System.arraycopy(BytesUtils.xor(inputMask0, outputMask1), 0, message, byteLength, byteLength);
        BytesUtils.xori(correctionsMask[widthIndex], message);
    }
}
