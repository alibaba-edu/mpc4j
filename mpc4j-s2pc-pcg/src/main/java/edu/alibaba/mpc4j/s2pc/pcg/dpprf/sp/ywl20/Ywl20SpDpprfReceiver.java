package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.AbstractSpDpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.SpDpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp.ywl20.Ywl20SpDpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * YWL20-SP-DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Ywl20SpDpprfReceiver extends AbstractSpDpprfReceiver {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * pre-compute COT receiver
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * core COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmTree;

    public Ywl20SpDpprfReceiver(Rpc receiverRpc, Party senderParty, Ywl20SpDpprfConfig config) {
        super(Ywl20SpDpprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
    }

    @Override
    public void init(int maxAlphaBound) throws MpcAbortException {
        setInitInput(maxAlphaBound);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init(maxH);
        preCotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SpDpprfReceiverOutput puncture(int alpha, int alphaBound) throws MpcAbortException {
        setPtoInput(alpha, alphaBound);
        return puncture();
    }

    @Override
    public SpDpprfReceiverOutput puncture(int alpha, int alphaBound, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException {
        setPtoInput(alpha, alphaBound, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return puncture();
    }

    private SpDpprfReceiverOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // R send (extend, h) to F_COT, which returns (r_i, t_i) ∈ {0,1} × {0,1}^κ to R
        if (cotReceiverOutput == null) {
            // For each i ∈ {1,...,h}, R sends a bit b_i = r_i ⊕ α_i ⊕ 1 to S.
            // This is identical to choose the choice bits as !α_i.
            cotReceiverOutput = coreCotReceiver.receive(notBinaryAlpha);
        } else {
            cotReceiverOutput.reduce(h);
            // use pre-computed COT to correct the choice bits
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, notBinaryAlpha);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        stopWatch.start();
        DataPacketHeader messageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> messagePayload = rpc.receive(messageHeader).getPayload();
        handleMessagePayload(messagePayload);
        SpDpprfReceiverOutput receiverOutput = generateReceiverOutput();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, messageTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void handleMessagePayload(List<byte[]> messagePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(messagePayload.size() == 2 * h);
        byte[][] messages = messagePayload.toArray(new byte[0][]);
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        ggmTree = new ArrayList<>(h + 1);
        // place the level-0 key with an empty key
        ggmTree.add(new byte[0][]);
        int alphaPrefix = 0;
        // For each i ∈ {1,...,h}
        for (int i = 1; i <= h; i++) {
            int hIndex = i - 1;
            byte[][] currentLevelSeeds = new byte[1 << i][];
            // R defines an i-bit string α_i^* = α_1 ... α_{i − 1} β_i
            boolean alphai = binaryAlpha[hIndex];
            int alphaiInt = alphai ? 1 : 0;
            boolean betai = notBinaryAlpha[hIndex];
            int betaiInt = betai ? 1 : 0;
            // Compute K_{β_i}^i = M_{β_i}^i ⊕ H(t_i, i || l)
            byte[] kiNot = BytesUtils.clone(cotReceiverOutput.getRb(hIndex));
            kiNot = crhf.hash(kiNot);
            if (betai) {
                BytesUtils.xori(kiNot, messages[2 * hIndex + 1]);
            } else {
                BytesUtils.xori(kiNot, messages[2 * hIndex]);
            }
            if (i == 1) {
                // If i = 1, define s_{β_i}^i = K_{β_i}^i
                currentLevelSeeds[alphaiInt] = null;
                currentLevelSeeds[betaiInt] = kiNot;
            } else {
                // If i ≥ 2
                byte[][] lowLevelSeeds = ggmTree.get(i - 1);
                // for j ∈ [2^i − 1], j ≠ α_1...α_{i − 1}, compute (s_{2j}^i, s_{2j + 1}^i = G(s_ja^{i - 1}).
                for (int j = 0; j < (1 << (i - 1)); j++) {
                    if (j != alphaPrefix) {
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
                }
                // compute the remaining seeds
                int alphaStar = (alphaPrefix << 1) + betaiInt;
                currentLevelSeeds[alphaStar] = kiNot;
                for (int j = 0; j < (1 << (i - 1)); j++) {
                    if (j != alphaPrefix) {
                        BytesUtils.xori(currentLevelSeeds[alphaStar], currentLevelSeeds[2 * j + betaiInt]);
                    }
                }
            }
            // update α_1...α_{i − 1}
            alphaPrefix = (alphaPrefix << 1) + alphaiInt;
            ggmTree.add(currentLevelSeeds);
        }
        cotReceiverOutput = null;
    }

    private SpDpprfReceiverOutput generateReceiverOutput() {
        // R sets w[i] = s_i^h for i ∈ [n] \ {α}
        byte[][] pprfKeys = ggmTree.get(h);
        // number of key is 2^h, reduce the key num to alphaBound
        if (alphaBound < (1 << h)) {
            byte[][] reducePprfKeys = new byte[alphaBound][];
            System.arraycopy(pprfKeys, 0, reducePprfKeys, 0, alphaBound);
            pprfKeys = reducePprfKeys;
        }
        ggmTree = null;
        return new SpDpprfReceiverOutput(alphaBound, alpha, pprfKeys);
    }
}
