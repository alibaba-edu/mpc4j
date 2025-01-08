package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposer;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst.SstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract CST Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public abstract class AbstractCstRosnSender extends AbstractRosnSender implements CstRosnSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCstRosnSender.class);
    /**
     * config
     */
    private final CstRosnConfig cstRosnConfig;
    /**
     * PST
     */
    private final PstReceiver pstReceiver;
    /**
     * BST
     */
    private final BstReceiver bstReceiver;
    /**
     * SST
     */
    private final SstReceiver sstReceiver;
    /**
     * COT
     */
    private final CotSender cotSender;
    /**
     * T
     */
    private final int t;

    public AbstractCstRosnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, CstRosnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        cstRosnConfig = config;
        pstReceiver = PstFactory.createReceiver(senderRpc, receiverParty, config.getPstConfig());
        addSubPto(pstReceiver);
        bstReceiver = BstFactory.createReceiver(senderRpc, receiverParty, config.getBstConfig());
        addSubPto(bstReceiver);
        sstReceiver = SstFactory.createReceiver(senderRpc, receiverParty, config.getSstConfig());
        addSubPto(sstReceiver);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
        t = config.getT();
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pstReceiver.init();
        bstReceiver.init();
        sstReceiver.init();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        cotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnSenderOutput rosn(int num, int byteLength) throws MpcAbortException {
        setPtoInput(num, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);
        if (num <= t) {
            stopWatch.start();
            CotSenderOutput cotSenderOutput = cotSender.sendRandom(cstRosnConfig.getCotNum(num));
            // P0 and P1 execute a Share Translation protocol, where P0 holds input π, receives output ∆,
            // and P1 receives output a, b.
            SstReceiverOutput sstReceiverOutput = sstReceiver.shareTranslate(num, byteLength, cotSenderOutput);
            stopWatch.stop();
            long sstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, sstTime);

            logPhaseInfo(PtoState.PTO_END);
            return RosnSenderOutput.create(sstReceiverOutput.getAs(), sstReceiverOutput.getBs());
        } else {
            // decompose
            stopWatch.start();
            CotSenderOutput cotSenderOutput = cotSender.sendRandom(cstRosnConfig.getCotNum(num));
            // padding π to have 2^n permutation
            int paddingLogNum = LongUtils.ceilLog2(num);
            int paddingNum = (1 << paddingLogNum);
            // P_0 computes the (T, d)-sub-permutation representation π_1, ..., π_d of its input
            PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(cstRosnConfig.getDecomposerType(), paddingNum, t);
            int d = decomposer.getD();
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2 + d, initTime);

            // For each layer i, the parties run N / T instances of ShareTrans_T, with P_0 providing as input the N / T
            // permutations making up π_i. (Note that all of these instances and layers can be run in parallel.)
            BstReceiverOutput[] bstReceiverOutputs = new BstReceiverOutput[d];

            byte[][][] asGroups;
            byte[][] as = new byte[0][];
            // we can do it in huge batch, but very easy to run out of memory since the cost is T^2 * d * g
            for (int i = 0; i < d; i++) {
                stopWatch.start();
                int splitCotNum = i == d / 2
                    ? BstFactory.getPrecomputeNum(cstRosnConfig.getBstConfig(), decomposer.getG(i), decomposer.getT(i))
                    : PstFactory.getPrecomputeNum(cstRosnConfig.getPstConfig(), decomposer.getG(i), decomposer.getT(i));
                CotSenderOutput splitCotSenderOutput = cotSenderOutput.split(splitCotNum);

                LOGGER.info("T:{}, G:{}, total:{}", decomposer.getT(i), decomposer.getG(i), ((long) decomposer.getT(i)) * decomposer.getT(i) * decomposer.getG(i));
                LOGGER.info("storage for one batch: {}B", ((long) decomposer.getT(i)) * decomposer.getT(i) * byteLength);
                boolean exceedMaxNt = ((long) decomposer.getT(i)) * decomposer.getT(i) * decomposer.getG(i) > cstRosnConfig.getMaxNt4Batch();
                boolean exceedMaxCache = ((long) decomposer.getT(i)) * decomposer.getT(i) * decomposer.getG(i) * byteLength > cstRosnConfig.getMaxCache4Batch();
                if (exceedMaxNt || exceedMaxCache) {
                    LOGGER.info("into small batch process");
                    int groupNumByNtThreshold = cstRosnConfig.getMaxNt4Batch() / decomposer.getT(i) / decomposer.getT(i);
                    int groupNumByCacheThreshold = (int) (cstRosnConfig.getMaxCache4Batch() / byteLength / decomposer.getT(i) / decomposer.getT(i));
                    int singleBatchNum = Math.max(1, Math.min(groupNumByNtThreshold, groupNumByCacheThreshold));
                    SstReceiverOutput[] tmpBst = new SstReceiverOutput[decomposer.getG(i)];
                    for (int currentBatchIndex = 0; currentBatchIndex < decomposer.getG(i); ) {
                        int smallBatchNum = Math.min(singleBatchNum, decomposer.getG(i) - currentBatchIndex);
                        int smallBatchCotNum = (int) (((long) splitCotNum) * smallBatchNum / decomposer.getG(i));
                        LOGGER.info("batch:{} - {} / {}", currentBatchIndex, currentBatchIndex + smallBatchNum, decomposer.getG(i));

                        CotSenderOutput smallBatchCotSenderOutput = splitCotSenderOutput.split(smallBatchCotNum);
                        BstReceiverOutput tmpRes = i == d / 2
                            ? bstReceiver.shareTranslate(smallBatchNum, decomposer.getT(i), byteLength, smallBatchCotSenderOutput)
                            : pstReceiver.shareTranslate(smallBatchNum, decomposer.getT(i), byteLength, smallBatchCotSenderOutput, i < d / 2);
                        int finalCurrentBatchIndex = currentBatchIndex;
                        IntStream.range(0, smallBatchNum).forEach(index -> tmpBst[index + finalCurrentBatchIndex] = tmpRes.get(index));
                        currentBatchIndex += smallBatchNum;
                    }
                    bstReceiverOutputs[i] = new BstReceiverOutput(tmpBst);
                } else {
                    bstReceiverOutputs[i] = i == d / 2
                        ? bstReceiver.shareTranslate(decomposer.getG(i), decomposer.getT(i), byteLength, splitCotSenderOutput)
                        : pstReceiver.shareTranslate(decomposer.getG(i), decomposer.getT(i), byteLength, splitCotSenderOutput, i < d / 2);
                }

                stopWatch.stop();
                long bstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, i + 2, 1, 2, bstTime);

                stopWatch.start();
                if (i == 0) {
                    // P_1 sets a = a^(1)
                    asGroups = IntStream.range(0, decomposer.getG(i))
                        .mapToObj(j -> bstReceiverOutputs[0].get(j).getAs())
                        .toArray(byte[][][]::new);
                    as = decomposer.combineGroups(asGroups, 0);
                } else {
                    int finalI = i;
                    byte[][][] aiGroups = IntStream.range(0, decomposer.getG(i))
                        .mapToObj(j -> bstReceiverOutputs[finalI].get(j).getAs())
                        .toArray(byte[][][]::new);
                    byte[][] ai = decomposer.combineGroups(aiGroups, i);
                    byte[][][] biGroups = IntStream.range(0, decomposer.getG(i - 1))
                        .mapToObj(j -> bstReceiverOutputs[finalI - 1].get(j).getBs())
                        .toArray(byte[][][]::new);
                    byte[][] bi = decomposer.combineGroups(biGroups, i - 1);
                    // δ^(i) = a^(i+1) − b^(i)
                    byte[][] di = new byte[paddingNum][byteLength];
                    for (int index = 0; index < paddingNum; index++) {
                        di[index] = BytesUtils.xor(ai[index], bi[index]);
                    }
                    List<byte[]> diDataPacketPayload = Arrays.stream(di).collect(Collectors.toList());
                    sendOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_LITTLE_DELTA.ordinal(), diDataPacketPayload);
                    // set bstReceiverOutputs[i - 1] = null
                    bstReceiverOutputs[i - 1] = null;
                }
                stopWatch.stop();
                long permuteTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                logSubStepInfo(PtoState.PTO_STEP, i + 2, 2, 2, permuteTime);
            }

            stopWatch.start();

            byte[][][] bdGroups = IntStream.range(0, decomposer.getG(d - 1))
                .mapToObj(j -> bstReceiverOutputs[d - 1].get(j).getBs())
                .toArray(byte[][][]::new);
            bstReceiverOutputs[d - 1] = null;
            // P1 samples and sends random x, y
            byte[][] x = BytesUtils.randomByteArrayVector(paddingNum, byteLength, secureRandom);
            LOGGER.info("ready to send mask x");
            sendOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_MASK_OUTPUT.ordinal(), Arrays.stream(x).collect(Collectors.toList()));
            byte[][] y = BytesUtils.randomByteArrayVector(paddingNum, byteLength, secureRandom);
            LOGGER.info("ready to send mask y");
            sendOtherPartyEqualSizePayload(CstRosnPtoStep.RECEIVER_SEND_BST_MASK_OUTPUT.ordinal(), Arrays.stream(y).collect(Collectors.toList()));
            LOGGER.info("finish send mask");
            // P1 outputs a = x + a, b = y + b^(d)
            byte[][] bs = decomposer.combineGroups(bdGroups, d - 1);
            for (int index = 0; index < num; index++) {
                BytesUtils.xori(as[index], x[index]);
                BytesUtils.xori(bs[index], y[index]);
            }
            // remove padding
            byte[][] reduceAs = Arrays.copyOfRange(as, 0, num);
            byte[][] reduceBs = Arrays.copyOfRange(bs, 0, num);
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2 + d, 2 + d, outputTime);

            logPhaseInfo(PtoState.PTO_END);
            return RosnSenderOutput.create(reduceAs, reduceBs);
        }
    }
}
