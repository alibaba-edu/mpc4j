package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * abstract CST Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class AbstractCstRosnReceiver extends AbstractRosnReceiver implements CstRosnReceiver {
    /**
     * config
     */
    private final CstRosnConfig cstRosnConfig;
    /**
     * PST
     */
    private final PstSender pstSender;
    /**
     * BST
     */
    private final BstSender bstSender;
    /**
     * SST
     */
    private final SstSender sstSender;
    /**
     * COT
     */
    private final CotReceiver cotReceiver;
    /**
     * T
     */
    private final int t;

    public AbstractCstRosnReceiver(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, CstRosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        cstRosnConfig = config;
        pstSender = PstFactory.createSender(senderRpc, receiverParty, config.getPstConfig());
        addSubPto(pstSender);
        bstSender = BstFactory.createSender(senderRpc, receiverParty, config.getBstConfig());
        addSubPto(bstSender);
        sstSender = SstFactory.createSender(senderRpc, receiverParty, config.getSstConfig());
        addSubPto(sstSender);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotReceiver);
        t = config.getT();
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pstSender.init();
        bstSender.init();
        sstSender.init();
        cotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnReceiverOutput rosn(int[] pi, int byteLength) throws MpcAbortException {
        setPtoInput(pi, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        if (num <= t) {
            stopWatch.start();
            CotReceiverOutput cotReceiverOutput = cotReceiver.receiveRandom(cstRosnConfig.getCotNum(num));
            // P0 and P1 execute a Share Translation protocol, where P0 holds input π, receives output ∆,
            // and P1 receives output a, b.
            SstSenderOutput sstSenderOutput = sstSender.shareTranslate(pi, byteLength, cotReceiverOutput);
            stopWatch.stop();
            long sstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, sstTime);

            logPhaseInfo(PtoState.PTO_END);
            return RosnReceiverOutput.create(sstSenderOutput.getPi(), sstSenderOutput.getDeltas());
        } else {
            // decompose
            stopWatch.start();
            CotReceiverOutput cotReceiverOutput = cotReceiver.receiveRandom(cstRosnConfig.getCotNum(num));
            // padding π to have 2^n permutation
            int paddingLogNum = LongUtils.ceilLog2(num);
            int paddingNum = (1 << paddingLogNum);
            int[] paddingPermutation = IntStream.range(0, paddingNum).toArray();
            System.arraycopy(pi, 0, paddingPermutation, 0, pi.length);
            // P_0 computes the (T, d)-sub-permutation representation π_1, ..., π_d of its input
            PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(cstRosnConfig.getDecomposerType(), paddingNum, t);
            int d = decomposer.getD();
            decomposer.setPermutation(paddingPermutation);
            int[][][] subPermutations = decomposer.getSubPermutations();
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, d + 2, initTime);

            // For each layer i, the parties run N / T instances of ShareTrans_T, with P_0 providing as input the N / T
            // permutations making up π_i. (Note that all of these instances and layers can be run in parallel.)
            BstSenderOutput[] bstSenderOutputs = new BstSenderOutput[d];
            byte[][] delta = new byte[paddingNum][byteLength];
            // we can do it in huge batch, but very easy to run out of memory since the cost is T^2 * d * g
            for (int i = 0; i < d; i++) {
                stopWatch.start();
                int splitCotNum = i == d / 2
                    ? BstFactory.getPrecomputeNum(cstRosnConfig.getBstConfig(), decomposer.getG(i), decomposer.getT(i))
                    : PstFactory.getPrecomputeNum(cstRosnConfig.getPstConfig(), decomposer.getG(i), decomposer.getT(i));
                CotReceiverOutput splitCotReceiverOutput = cotReceiverOutput.split(splitCotNum);

                boolean exceedMaxNt = ((long) decomposer.getT(i)) * decomposer.getT(i) * decomposer.getG(i) > cstRosnConfig.getMaxNt4Batch();
                boolean exceedMaxCache = ((long) decomposer.getT(i)) * decomposer.getT(i) * decomposer.getG(i) * byteLength > cstRosnConfig.getMaxCache4Batch();
                if (exceedMaxNt || exceedMaxCache) {
                    int groupNumByNtThreshold = cstRosnConfig.getMaxNt4Batch() / decomposer.getT(i) / decomposer.getT(i);
                    int groupNumByCacheThreshold = (int) (cstRosnConfig.getMaxCache4Batch() / byteLength / decomposer.getT(i) / decomposer.getT(i));
                    int singleBatchNum = Math.max(1, Math.min(groupNumByNtThreshold, groupNumByCacheThreshold));
                    SstSenderOutput[] tmpBst = new SstSenderOutput[decomposer.getG(i)];
                    for (int currentBatchIndex = 0; currentBatchIndex < decomposer.getG(i); ) {
                        int smallBatchNum = Math.min(singleBatchNum, decomposer.getG(i) - currentBatchIndex);
                        int smallBatchCotNum = (int) (((long) splitCotNum) * smallBatchNum / decomposer.getG(i));
                        CotReceiverOutput smallBatchCotReceiverOutput = splitCotReceiverOutput.split(smallBatchCotNum);
                        int[][] smallBatchPermutations = Arrays.copyOfRange(subPermutations[i], currentBatchIndex, currentBatchIndex + smallBatchNum);
                        BstSenderOutput tmpRes = i == d / 2
                            ? bstSender.shareTranslate(smallBatchPermutations, byteLength, smallBatchCotReceiverOutput)
                            : pstSender.shareTranslate(smallBatchPermutations, byteLength, smallBatchCotReceiverOutput, i < d / 2);
                        int finalCurrentBatchIndex = currentBatchIndex;
                        IntStream.range(0, smallBatchNum).forEach(index -> tmpBst[index + finalCurrentBatchIndex] = tmpRes.get(index));
                        currentBatchIndex += smallBatchNum;
                    }
                    bstSenderOutputs[i] = new BstSenderOutput(tmpBst);
                } else {
                    bstSenderOutputs[i] = i == d / 2
                        ? bstSender.shareTranslate(subPermutations[i], byteLength, splitCotReceiverOutput)
                        : pstSender.shareTranslate(subPermutations[i], byteLength, splitCotReceiverOutput, i < d / 2);
                }

                stopWatch.stop();
                long bstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, i + 2, 1, 2, bstTime);

                stopWatch.start();
                if (i > 0) {
                    int finalI = i - 1;
                    byte[][][] bdiGroups = IntStream.range(0, decomposer.getG(i - 1))
                        .mapToObj(j -> bstSenderOutputs[finalI].get(j).getDeltas())
                        .toArray(byte[][][]::new);
                    byte[][] bdi = decomposer.combineGroups(bdiGroups, i - 1);
                    for (int index = 0; index < paddingNum; index++) {
                        BytesUtils.xori(delta[index], bdi[index]);
                    }
                    List<byte[]> diDataPacketPayload = receiveOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_LITTLE_DELTA.ordinal(), paddingNum, delta[0].length);
                    MpcAbortPreconditions.checkArgument(diDataPacketPayload.size() == paddingNum);
                    byte[][] di = diDataPacketPayload.toArray(new byte[0][]);
                    for (int index = 0; index < paddingNum; index++) {
                        BytesUtils.xori(delta[index], di[index]);
                    }
                    delta = decomposer.permutation(delta, i);
                    bstSenderOutputs[i - 1] = null;
                }
                if (i == d - 1) {
                    // last round
                    byte[][][] bddGroups = IntStream.range(0, decomposer.getG(i))
                        .mapToObj(j -> bstSenderOutputs[d - 1].get(j).getDeltas())
                        .toArray(byte[][][]::new);
                    byte[][] bdd = decomposer.combineGroups(bddGroups, d - 1);
                    for (int index = 0; index < paddingNum; index++) {
                        BytesUtils.xori(delta[index], bdd[index]);
                    }
                    bstSenderOutputs[d - 1] = null;
                }
                stopWatch.stop();
                long permuteTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, i + 2, 2, 2, permuteTime);
            }

            stopWatch.start();
            // P1 samples and sends random x, y
            List<byte[]> xPacketPayload = receiveOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_MASK_OUTPUT.ordinal(), paddingNum, delta[0].length);
            List<byte[]> yPacketPayload = receiveOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_MASK_OUTPUT.ordinal(), paddingNum, delta[0].length);
            byte[][] x = xPacketPayload.toArray(new byte[0][]);
            byte[][] y = yPacketPayload.toArray(new byte[0][]);
            for (int index = 0; index < num; index++) {
                BytesUtils.xori(delta[index], y[index]);
                BytesUtils.xori(delta[index], x[pi[index]]);
            }
            // remove padding
            byte[][] reduceDelta = Arrays.copyOfRange(delta, 0, num);
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, d + 2, d + 2, outputTime);

            logPhaseInfo(PtoState.PTO_END);
            return RosnReceiverOutput.create(pi, reduceDelta);
        }
    }
}
