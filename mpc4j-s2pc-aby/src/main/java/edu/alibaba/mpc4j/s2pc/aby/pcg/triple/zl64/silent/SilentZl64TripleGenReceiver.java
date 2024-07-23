package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.AbstractZl64TripleGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent.SilentZl64TripleGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * silent Zl64 triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/7/1
 */
public class SilentZl64TripleGenReceiver extends AbstractZl64TripleGenParty {
    /**
     * NC-COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * NC-COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * PRG
     */
    private Prg[] prgs;
    /**
     * round num
     */
    private int roundNum;
    /**
     * each num
     */
    private int eachNum;
    /**
     * a1
     */
    private long[] a1;
    /**
     * b1
     */
    private long[] b1;
    /**
     * c1
     */
    private long[] c1;
    /**
     * the receiver's correlation pairs (in the first COT round)
     */
    private long[][] receiverCorrelationPairs;
    /**
     * the sender's corrections (in the second COT round)
     */
    private long[][] senderCorrelations;

    public SilentZl64TripleGenReceiver(Rpc receiverRpc, Party senderParty, SilentZl64TripleGenConfig config) {
        super(SilentZl64TripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotSender = NcCotFactory.createSender(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotSender);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotReceiver);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(expectTotalNum, config.defaultRoundNum(l));
        int ncCotNum = SilentZl64TripleGenConfig.maxNcCotNum(roundNum, maxL);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        ncCotSender.init(delta, ncCotNum);
        ncCotReceiver.init(ncCotNum);
        // each bit of a and b can be shifted to reduce the communication cost
        prgs = IntStream.range(0, maxL)
            .mapToObj(i -> {
                int shiftByteL = CommonUtils.getByteLength(i + 1);
                return PrgFactory.createInstance(envType, shiftByteL);
            })
            .toArray(Prg[]::new);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Zl64Triple generate(Zl64 zl64, int num) throws MpcAbortException {
        setPtoInput(zl64, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        Zl64Triple triple = Zl64Triple.createEmpty(zl64);
        int roundCount = 1;
        while (triple.getNum() < num) {
            int gapNum = num - triple.getNum();
            eachNum = Math.min(gapNum, roundNum);
            Zl64Triple eachTriple = roundGenerate(roundCount);
            triple.merge(eachTriple);
            roundCount++;
        }

        logPhaseInfo(PtoState.PTO_END);
        return triple;
    }

    private Zl64Triple roundGenerate(int roundCount) throws MpcAbortException {
        stopWatch.start();
        // the first COT round
        CotSenderOutput cotSenderOutput = ncCotSender.send();
        cotSenderOutput.reduce(eachNum * l);
        stopWatch.stop();
        long firstCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 6, firstCotTime);

        // the second COT round
        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
        cotReceiverOutput.reduce(eachNum * l);
        stopWatch.stop();
        long secondCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 6, secondCotTime);

        stopWatch.start();
        initParams(cotReceiverOutput);
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 6, initParamTime);

        stopWatch.start();
        // generate receiver's correlations
        List<byte[]> receiverCorrelationPayload = generateReceiverCorrelationPayload(cotSenderOutput);
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), receiverCorrelationPayload);
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 3, 6, sendTime);

        List<byte[]> senderCorrelationPayload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_CORRELATION.ordinal());

        stopWatch.start();
        // handle sender's correlations
        handleSenderCorrelationPayload(cotReceiverOutput, senderCorrelationPayload);
        receiverCorrelationPairs = null;
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 5, 6, receiveTime);

        stopWatch.start();
        Zl64Triple eachTriple = computeTriples();
        senderCorrelations = null;
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 6, 6, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return eachTriple;
    }

    private void initParams(CotReceiverOutput cotReceiverOutput) {
        a1 = new long[eachNum];
        b1 = new long[eachNum];
        c1 = new long[eachNum];
        boolean[] receiverChoices = cotReceiverOutput.getChoices();
        receiverCorrelationPairs = new long[eachNum * l][2];
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index -> {
            int offset = index * l;
            // Let P_1 randomly generate <a>_1, <b>_1
            a1[index] = zl64.createRandom(secureRandom);
            // In the i-th COT, P_1 inputs <b>_1[i] as choice bit, here we use choice bits generated by silent OT.
            boolean[] binaryChoices = new boolean[l];
            System.arraycopy(receiverChoices, offset, binaryChoices, 0, l);
            byte[] byteChoices = BinaryUtils.binaryToRoundByteArray(binaryChoices);
            b1[index] = LongUtils.fixedByteArrayToLong(byteChoices);
            // The terms <a>_1 * <b>_1 can be computed locally by P_1
            c1[index] = zl64.mul(a1[index], b1[index]);
            IntStream.range(0, l).forEach(i -> {
                // in the i-th COT, P_1 inputs the correlation function <a>_1 * 2^i - y mod 2^l
                long y = zl64.shiftRight(zl64.createRandom(secureRandom), l - 1 - i);
                // c_1 = c_1 - y * 2^{i}
                c1[index] = zl64.sub(c1[index], zl64.shiftLeft(y, l - 1 - i));
                // s_{i, 0} = y
                receiverCorrelationPairs[offset + i][0] = y;
                // s_{i, 1} = ((a_1 + y) << 2^i)
                receiverCorrelationPairs[offset + i][1]
                    = zl64.shiftRight(zl64.shiftLeft(zl64.add(a1[index], y), l - 1 - i), l - 1 - i);
            });
        });
    }

    private List<byte[]> generateReceiverCorrelationPayload(CotSenderOutput cotSenderOutput) {
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        return indexIntStream
            .mapToObj(index ->
                IntStream.range(0, l)
                    .mapToObj(i -> {
                        int offset = index * l + i;
                        int shiftByteL = prgs[i].getOutputByteLength();
                        byte[][] ciphertexts = new byte[2][];
                        ciphertexts[0] = prgs[i].extendToBytes(cotSenderOutput.getR0(offset));
                        byte[] message0 = LongUtils.longToFixedByteArray(receiverCorrelationPairs[offset][0], shiftByteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prgs[i].extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = LongUtils.longToFixedByteArray(receiverCorrelationPairs[offset][1], shiftByteL);
                        BytesUtils.xori(ciphertexts[1], message1);
                        return ciphertexts;
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private void handleSenderCorrelationPayload(CotReceiverOutput cotReceiverOutput, List<byte[]> senderMessagesPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(senderMessagesPayload.size() == eachNum * l * 2);
        byte[][] messagePairArray = senderMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        senderCorrelations = indexIntStream
            .mapToObj(index ->
                IntStream.range(0, l)
                    .mapToLong(i -> {
                        int offset = index * l + i;
                        byte[] message = cotReceiverOutput.getRb(offset);
                        message = prgs[i].extendToBytes(message);
                        if (cotReceiverOutput.getChoice(offset)) {
                            BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                        } else {
                            BytesUtils.xori(message, messagePairArray[2 * offset]);
                        }
                        return LongUtils.fixedByteArrayToLong(message);
                    })
                    .toArray()
            )
            .toArray(long[][]::new);
    }

    private Zl64Triple computeTriples() {
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index ->
            IntStream.range(0, l).forEach(i -> {
                    // c_1 = c_1 + (s_{i, b} ⊕ H(k_{i, b}))
                    c1[index] = zl64.add(c1[index], zl64.shiftLeft(senderCorrelations[index][i], l - 1 - i));
                }
            ));
        return Zl64Triple.create(zl64, a1, b1, c1);
    }
}
