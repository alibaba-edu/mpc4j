package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.AbstractBpDpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.ywl20.Ywl20BpDpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YWL20-BP-DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20BpDpprfSender extends AbstractBpDpprfSender {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT sender
     */
    private final PreCotSender preCotSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * the GGM trees. Each tree contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<ArrayList<byte[][]>> ggmTree;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][][] k0sArray;
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][][] k1sArray;

    public Ywl20BpDpprfSender(Rpc senderRpc, Party receiverParty, Ywl20BpDpprfConfig config) {
        super(Ywl20BpDpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
    }

    @Override
    public void init(int maxBatchNum, int maxAlphaBound) throws MpcAbortException {
        setInitInput(maxBatchNum, maxAlphaBound);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        int maxPreCotNum = maxH * maxBatchNum;
        coreCotSender.init(delta, maxPreCotNum);
        preCotSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BpDpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException {
        setPtoInput(batchNum, alphaBound);
        return puncture();
    }

    @Override
    public BpDpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, alphaBound, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private BpDpprfSenderOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int preCotNum = h * batchNum;
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(preCotNum);
        } else {
            cotSenderOutput.reduce(preCotNum);
            // use pre-computed COT to correct the choice bits
            cotSenderOutput = preCotSender.send(cotSenderOutput);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        stopWatch.start();
        generatePprfKeys();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, keyGenTime);

        stopWatch.start();
        List<byte[]> messagePayload = generateMessagePayload();
        DataPacketHeader messageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE_ARRAY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        BpDpprfSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, messageTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void generatePprfKeys() {
        k0sArray = new byte[batchNum][h][];
        k1sArray = new byte[batchNum][h][];
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmTree = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> treeKeys = new ArrayList<>(h + 1);
                // S picks a random s_0^0 ∈ {0, 1}^κ
                byte[][] s0 = new byte[1][CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(s0[0]);
                // 把s0作为第0项，从而方便后续迭代
                treeKeys.add(s0);
                // For each i ∈ {1,...,h}, j ∈ [2^{i − 1}], S computes (s_{2j}^i, s_{2j + 1}^i) = G(s_j^{i - 1})
                for (int i = 1; i <= h; i++) {
                    byte[][] lowLevelSeeds = treeKeys.get(i - 1);
                    byte[][] currentLevelSeeds = new byte[1 << i][];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        byte[] extendSeeds = prg.extendToBytes(lowLevelSeeds[j]);
                        currentLevelSeeds[2 * j] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(
                            extendSeeds, 0,
                            currentLevelSeeds[2 * j], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        currentLevelSeeds[2 * j + 1] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(
                            extendSeeds, CommonConstants.BLOCK_BYTE_LENGTH,
                            currentLevelSeeds[2 * j + 1], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                    }
                    treeKeys.add(currentLevelSeeds);
                }
                // For each i ∈ {1,..., h}
                for (int i = 1; i <= h; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevelSeeds = treeKeys.get(i);
                    // S then computes K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
                    k0sArray[batchIndex][hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BytesUtils.xori(k0sArray[batchIndex][hIndex], currentLevelSeeds[2 * j]);
                    }
                    // and K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
                    k1sArray[batchIndex][hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BytesUtils.xori(k1sArray[batchIndex][hIndex], currentLevelSeeds[2 * j + 1]);
                    }
                }
                return treeKeys;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<byte[]> generateMessagePayload() {
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagePayload = batchIndexIntStream
            .mapToObj(batchIndex ->
                IntStream.range(0, h)
                    .mapToObj(lIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(q_i, i || l)
                        byte[] message0 = BytesUtils.clone(cotSenderOutput.getR0(batchIndex * h + lIndex));
                        message0 = crhf.hash(message0);
                        BytesUtils.xori(message0, k0sArray[batchIndex][lIndex]);
                        // and M_1^i = K_1^i ⊕ H(q_i ⊕ ∆, i || l)
                        byte[] message1 = BytesUtils.clone(cotSenderOutput.getR1(batchIndex * h + lIndex));
                        message1 = crhf.hash(message1);
                        BytesUtils.xori(message1, k1sArray[batchIndex][lIndex]);
                        return new byte[][]{message0, message1};
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        k0sArray = null;
        k1sArray = null;
        cotSenderOutput = null;
        return messagePayload;
    }

    private BpDpprfSenderOutput generateSenderOutput() {
        SpDpprfSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                // number of key is 2^h, reduce the key num to alphaBound
                byte[][] prfkeys = ggmTree.get(batchIndex).get(h);
                if (alphaBound < (1 << h)) {
                    byte[][] reducePrfKeys = new byte[alphaBound][];
                    System.arraycopy(prfkeys, 0, reducePrfKeys, 0, alphaBound);
                    prfkeys = reducePrfKeys;
                }
                return new SpDpprfSenderOutput(alphaBound, prfkeys);
            })
            .toArray(SpDpprfSenderOutput[]::new);
        ggmTree = null;
        return new BpDpprfSenderOutput(senderOutputs);
    }
}
