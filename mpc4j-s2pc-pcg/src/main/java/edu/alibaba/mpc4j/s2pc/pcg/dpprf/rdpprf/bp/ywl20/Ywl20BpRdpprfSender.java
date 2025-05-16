package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.AbstractBpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfPtoDesc.PtoStep;
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
 * YWL20-BP-RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20BpRdpprfSender extends AbstractBpRdpprfSender {
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
     * the final level of the GGM trees. Each tree contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmResults;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][][] k0sArray;
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][][] k1sArray;

    public Ywl20BpRdpprfSender(Rpc senderRpc, Party receiverParty, Ywl20BpRdpprfConfig config) {
        super(Ywl20BpRdpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        coreCotSender.init(delta);
        preCotSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BpRdpprfSenderOutput puncture(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return puncture();
    }

    @Override
    public BpRdpprfSenderOutput puncture(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private BpRdpprfSenderOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        int preCotNum = eachLogNum * batchNum;
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
        sendOtherPartyEqualSizePayload(PtoStep.SENDER_SEND_MESSAGE_ARRAY.ordinal(), messagePayload);
        BpRdpprfSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, messageTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void generatePprfKeys() {
        k0sArray = new byte[batchNum][eachLogNum][];
        k1sArray = new byte[batchNum][eachLogNum][];
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmResults = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> treeKeys = new ArrayList<>(eachLogNum + 1);
                // S picks a random s_0^0 ∈ {0, 1}^κ
                byte[][] s0 = BlockUtils.zeroBlocks(1);
                secureRandom.nextBytes(s0[0]);
                // 把s0作为第0项，从而方便后续迭代
                treeKeys.add(s0);
                // For each i ∈ {1,...,h}, j ∈ [2^{i − 1}], S computes (s_{2j}^i, s_{2j + 1}^i) = G(s_j^{i - 1})
                for (int i = 1; i <= eachLogNum; i++) {
                    byte[][] lowLevelSeeds = treeKeys.get(i - 1);
                    byte[][] currentLevelSeeds = new byte[1 << i][];
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        byte[] extendSeeds = prg.extendToBytes(lowLevelSeeds[j]);
                        currentLevelSeeds[2 * j] = BlockUtils.zeroBlock();
                        System.arraycopy(
                            extendSeeds, 0, currentLevelSeeds[2 * j], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        currentLevelSeeds[2 * j + 1] = BlockUtils.zeroBlock();
                        System.arraycopy(
                            extendSeeds, CommonConstants.BLOCK_BYTE_LENGTH, currentLevelSeeds[2 * j + 1], 0,
                            CommonConstants.BLOCK_BYTE_LENGTH
                        );
                    }
                    treeKeys.add(currentLevelSeeds);
                }
                // For each i ∈ {1,..., h}
                for (int i = 1; i <= eachLogNum; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevelSeeds = treeKeys.get(i);
                    // S then computes K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
                    k0sArray[batchIndex][hIndex] = BlockUtils.zeroBlock();
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BlockUtils.xori(k0sArray[batchIndex][hIndex], currentLevelSeeds[2 * j]);
                    }
                    // and K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
                    k1sArray[batchIndex][hIndex] = BlockUtils.zeroBlock();
                    for (int j = 0; j < (1 << (i - 1)); j++) {
                        BlockUtils.xori(k1sArray[batchIndex][hIndex], currentLevelSeeds[2 * j + 1]);
                    }
                }
                return treeKeys.get(eachLogNum);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<byte[]> generateMessagePayload() {
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        List<byte[]> messagePayload = batchIndexIntStream
            .mapToObj(batchIndex ->
                IntStream.range(0, eachLogNum)
                    .mapToObj(lIndex -> {
                        // S sends M_0^i = K_0^i ⊕ H(q_i, i || l)
                        byte[] message0 = cotSenderOutput.getR0(batchIndex * eachLogNum + lIndex);
                        message0 = crhf.hash(message0);
                        BlockUtils.xori(message0, k0sArray[batchIndex][lIndex]);
                        // and M_1^i = K_1^i ⊕ H(q_i ⊕ ∆, i || l)
                        byte[] message1 = cotSenderOutput.getR1(batchIndex * eachLogNum + lIndex);
                        message1 = crhf.hash(message1);
                        BlockUtils.xori(message1, k1sArray[batchIndex][lIndex]);
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

    private BpRdpprfSenderOutput generateSenderOutput() {
        SpRdpprfSenderOutput[] senderOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                // number of key is 2^h, reduce the key num to alphaBound
                byte[][] v0Array = ggmResults.get(batchIndex);
                if (eachNum < (1 << eachLogNum)) {
                    byte[][] reducePrfKeys = new byte[eachNum][];
                    System.arraycopy(v0Array, 0, reducePrfKeys, 0, eachNum);
                    v0Array = reducePrfKeys;
                }
                return new SpRdpprfSenderOutput(v0Array);
            })
            .toArray(SpRdpprfSenderOutput[]::new);
        return new BpRdpprfSenderOutput(senderOutputs);
    }
}
