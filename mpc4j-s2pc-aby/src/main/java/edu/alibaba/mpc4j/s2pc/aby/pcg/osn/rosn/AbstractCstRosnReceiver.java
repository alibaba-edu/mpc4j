package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.network.PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
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
        if (num <= t) {
            logPhaseInfo(PtoState.PTO_BEGIN);

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
            logPhaseInfo(PtoState.PTO_BEGIN);

            // decompose
            stopWatch.start();
            CotReceiverOutput cotReceiverOutput = cotReceiver.receiveRandom(cstRosnConfig.getCotNum(num));
            // padding π to have 2^n permutation
            int paddingLogNum = LongUtils.ceilLog2(num);
            int paddingNum = (1 << paddingLogNum);
            int[] paddingPermutation = IntStream.range(0, paddingNum).toArray();
            System.arraycopy(pi, 0, paddingPermutation, 0, pi.length);
            // P_0 computes the (T, d)-sub-permutation representation π_1, ..., π_d of its input
            PermutationDecomposer decomposer = new PermutationDecomposer(paddingNum, t);
            int d = decomposer.getD();
            int g = decomposer.getG();
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
            int splitCotNum = BstFactory.getPrecomputeNum(cstRosnConfig.getBstConfig(), g, t);
            for (int i = 0; i < d; i++) {
                stopWatch.start();
                CotReceiverOutput splitCotReceiverOutput = cotReceiverOutput.split(splitCotNum);
                bstSenderOutputs[i] = bstSender.shareTranslate(subPermutations[i], byteLength, splitCotReceiverOutput);
                stopWatch.stop();
                long bstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, i + 2, 1, 2, bstTime);

                stopWatch.start();
                if (i > 0) {
                    int finalI = i - 1;
                    byte[][][] bdiGroups = IntStream.range(0, g)
                        .mapToObj(j -> bstSenderOutputs[finalI].get(j).getDeltas())
                        .toArray(byte[][][]::new);
                    byte[][] bdi = decomposer.combineGroups(bdiGroups, i - 1);
                    for (int index = 0; index < paddingNum; index++) {
                        BytesUtils.xori(delta[index], bdi[index]);
                    }
                    List<byte[]> diDataPacketPayload = receiveOtherPartyPayload(CstRosnPtoStep.RECEIVER_SEND_BST_LITTLE_DELTA.ordinal());
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
                    byte[][][] bddGroups = IntStream.range(0, g)
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
            List<byte[]> maskDataPacketPayload = receiveOtherPartyPayload(CstRosnPtoStep.RECEIVER_SEND_BST_MASK_OUTPUT.ordinal());
            MpcAbortPreconditions.checkArgument(maskDataPacketPayload.size() == 2 * paddingNum);
            byte[][] w = maskDataPacketPayload.toArray(new byte[0][]);
            byte[][] piX = PermutationNetworkUtils.permutation(pi, Arrays.copyOf(w, num));
            for (int index = 0; index < num; index++) {
                BytesUtils.xori(delta[index], w[index + paddingNum]);
                BytesUtils.xori(delta[index], piX[index]);
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
