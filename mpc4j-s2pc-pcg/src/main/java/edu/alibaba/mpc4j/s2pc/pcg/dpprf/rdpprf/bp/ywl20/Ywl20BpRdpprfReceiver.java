package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.AbstractBpRdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.BpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp.ywl20.Ywl20BpRdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YWL20-BP-RDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20BpRdpprfReceiver extends AbstractBpRdpprfReceiver {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * pre-compute COT receiver
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * the final level of the GGM trees. Each tree contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmResults;

    public Ywl20BpRdpprfReceiver(Rpc receiverRpc, Party senderParty, Ywl20BpRdpprfConfig config) {
        super(Ywl20BpRdpprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        preCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BpRdpprfReceiverOutput puncture(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return puncture();
    }

    @Override
    public BpRdpprfReceiverOutput puncture(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return puncture();
    }

    private BpRdpprfReceiverOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        int preCotNum = eachLogNum * batchNum;
        boolean[] rs = new boolean[preCotNum];
        for (int batchIndex = 0; batchIndex < batchNum; batchIndex++) {
            System.arraycopy(notAlphaBinaryArray[batchIndex], 0, rs, batchIndex * eachLogNum, eachLogNum);
        }
        if (cotReceiverOutput == null) {
            // For each i ∈ {1,...,h}, R sends a bit b_i = r_i ⊕ α_i ⊕ 1 to S
            // This is identical to choose the choice bits as !α_i.
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(preCotNum);
            // use pre-computed COT to correct the choice bits
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, rs);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        List<byte[]> messagePayload = receiveOtherPartyEqualSizePayload(
            PtoStep.SENDER_SEND_MESSAGE_ARRAY.ordinal(), 2 * eachLogNum * batchNum, CommonConstants.BLOCK_BYTE_LENGTH
        );

        stopWatch.start();
        handleMessagePayload(messagePayload);
        BpRdpprfReceiverOutput receiverOutput = generateReceiverOutput();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, messageTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleMessagePayload(List<byte[]> messagePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(messagePayload.size() == 2 * eachLogNum * batchNum);
        byte[][] messagesArray = messagePayload.toArray(new byte[0][]);
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        IntStream batchIndexIntStream = IntStream.range(0, batchNum);
        batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
        ggmResults = batchIndexIntStream
            .mapToObj(batchIndex -> {
                ArrayList<byte[][]> treeKeys = new ArrayList<>(eachLogNum + 1);
                // 把一个空字节作为第0项占位
                treeKeys.add(new byte[0][]);
                int alphaPrefix = 0;
                // For each i ∈ {1,...,h}
                for (int i = 1; i <= eachLogNum; i++) {
                    int hIndex = i - 1;
                    byte[][] currentLevelSeeds = new byte[1 << i][];
                    // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} β_i
                    boolean alphai = alphaBinaryArray[batchIndex][hIndex];
                    int alphaiInt = alphai ? 1 : 0;
                    boolean betai = notAlphaBinaryArray[batchIndex][hIndex];
                    int betaiInt = betai ? 1 : 0;
                    // Compute K_{β_i}^i = M_{β_i}^i ⊕ H(t_i, i || l)
                    byte[] kiNot = cotReceiverOutput.getRb(eachLogNum * batchIndex + hIndex);
                    kiNot = crhf.hash(kiNot);
                    if (betai) {
                        BlockUtils.xori(kiNot, messagesArray[batchIndex * eachLogNum * 2 + 2 * hIndex + 1]);
                    } else {
                        BlockUtils.xori(kiNot, messagesArray[batchIndex * eachLogNum * 2 + 2 * hIndex]);
                    }
                    if (i == 1) {
                        // If i = 1, define s_{β_i}^i = K_{β_i}^i
                        currentLevelSeeds[alphaiInt] = null;
                        currentLevelSeeds[betaiInt] = kiNot;
                    } else {
                        // If i ≥ 2
                        byte[][] lowLevelSeeds = treeKeys.get(i - 1);
                        // for j ∈ [2^i − 1], j ≠ α_1...α_{i − 1}, compute (s_{2j}^i, s_{2j + 1}^i = G(s_ja^{i - 1}).
                        for (int j = 0; j < (1 << (i - 1)); j++) {
                            if (j != alphaPrefix) {
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
                        }
                        // compute the remaining seeds
                        int alphaStar = (alphaPrefix << 1) + betaiInt;
                        currentLevelSeeds[alphaStar] = BlockUtils.zeroBlock();
                        BlockUtils.xori(currentLevelSeeds[alphaStar], kiNot);
                        for (int j = 0; j < (1 << (i - 1)); j++) {
                            if (j != alphaPrefix) {
                                BlockUtils.xori(currentLevelSeeds[alphaStar], currentLevelSeeds[2 * j + betaiInt]);
                            }
                        }
                    }
                    // update α_1...α_{i − 1}
                    alphaPrefix = (alphaPrefix << 1) + alphaiInt;
                    treeKeys.add(currentLevelSeeds);
                }
                return treeKeys.get(eachLogNum);
            })
            .collect(Collectors.toCollection(ArrayList::new));
        cotReceiverOutput = null;
    }

    private BpRdpprfReceiverOutput generateReceiverOutput() {
        SpRdpprfReceiverOutput[] receiverOutputs = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                // R sets w[i] = s_i^h for i ∈ [n] \ {α}
                byte[][] v1Array = ggmResults.get(batchIndex);
                // number of key is 2^h, reduce the key num to alphaBound
                if (eachNum < (1 << eachLogNum)) {
                    byte[][] reducePprfKeys = new byte[eachNum][];
                    System.arraycopy(v1Array, 0, reducePprfKeys, 0, eachNum);
                    v1Array = reducePprfKeys;
                }
                return new SpRdpprfReceiverOutput(alphaArray[batchIndex], v1Array);
            })
            .toArray(SpRdpprfReceiverOutput[]::new);
        return new BpRdpprfReceiverOutput(receiverOutputs);
    }
}
