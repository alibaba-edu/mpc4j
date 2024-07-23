package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.AbstractZl64TripleGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct.DirectZl64TripleGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * direct Zl64 triple generation sender.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
public class DirectZl64TripleGenSender extends AbstractZl64TripleGenParty {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
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
     * a0
     */
    private long[] a0;
    /**
     * b0
     */
    private long[] b0;
    /**
     * c0
     */
    private long[] c0;
    /**
     * the sender's choices (in the first COT round)
     */
    private boolean[] senderChoices;
    /**
     * the receiver's correlations (in the first COT round)
     */
    private long[][] receiverCorrelations;
    /**
     * the sender's correlation pairs (in the second COT round)
     */
    private long[][] senderMessagesArray;

    public DirectZl64TripleGenSender(Rpc senderRpc, Party receiverParty, DirectZl64TripleGenConfig config) {
        super(DirectZl64TripleGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(expectTotalNum, config.defaultRoundNum(maxL));
        coreCotReceiver.init();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
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
        initParams();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 6, initTime);

        stopWatch.start();
        // the first COT round
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(senderChoices);
        senderChoices = null;
        stopWatch.stop();
        long firstCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 6, firstCotTime);

        // receive messages in the first COT round
        List<byte[]> receiverCorrelationPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal());

        stopWatch.start();
        // handle the receiver's correlations
        handleReceiverCorrelationPayload(cotReceiverOutput, receiverCorrelationPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 3, 6, receiveTime);

        stopWatch.start();
        // the second COT round
        CotSenderOutput cotSenderOutput = coreCotSender.send(eachNum * l);
        stopWatch.stop();
        long secondCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 4, 6, secondCotTime);

        stopWatch.start();
        // generate the sender's correlations
        List<byte[]> senderCorrelationPayload = generateSenderCorrelationPayload(cotSenderOutput);
        senderMessagesArray = null;
        sendOtherPartyPayload(PtoStep.SENDER_SEND_CORRELATION.ordinal(), senderCorrelationPayload);
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 5, 6, sendTime);

        stopWatch.start();
        Zl64Triple eachTriple = computeTriples();
        receiverCorrelations = null;
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 6, 6, tripleTime);
        return eachTriple;
    }

    private void initParams() {
        a0 = new long[eachNum];
        b0 = new long[eachNum];
        c0 = new long[eachNum];
        senderChoices = new boolean[eachNum * l];
        senderMessagesArray = new long[eachNum * l][2];
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index -> {
            // Let P_0 randomly generate <a>_0, <b>_0
            a0[index] = zl64.createRandom(secureRandom);
            b0[index] = zl64.createRandom(secureRandom);
            // The terms <a>_0 * <b>_0 can be computed locally by P_0
            c0[index] = zl64.mul(a0[index], b0[index]);
            // in the i-th COT, P_0 inputs the correlation function <a>_0 * 2^i - x mod 2^l
            int offset = index * l;
            IntStream.range(0, l).forEach(i -> {
                long x = zl64.shiftRight(zl64.createRandom(secureRandom), l - 1 - i);
                // c_0 = c_0 - x * 2^{i}
                c0[index] = zl64.sub(c0[index], zl64.shiftLeft(x, l - 1 - i));
                // s_{i, 0} = x
                senderMessagesArray[offset + i][0] = x;
                // s_{i, 1} = ((a_0 + x) << 2^i)
                senderMessagesArray[offset + i][1]
                    = zl64.shiftRight(zl64.shiftLeft(zl64.add(a0[index], x), l - 1 - i), l - 1 - i);
            });
            // In the i-th COT, P_0 inputs <b>_0[i] as choice bit.
            byte[] byteChoices = LongUtils.longToFixedByteArray(b0[index], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, senderChoices, offset, l);
        });
    }

    private void handleReceiverCorrelationPayload(CotReceiverOutput cotReceiverOutput, List<byte[]> receiverMessagesPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverMessagesPayload.size() == eachNum * l * 2);
        byte[][] messagePairArray = receiverMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        receiverCorrelations = indexIntStream
            .mapToObj(index -> IntStream.range(0, l)
                .mapToLong(bitIndex -> {
                    int offset = index * l + bitIndex;
                    byte[] message = cotReceiverOutput.getRb(offset);
                    // s_{i, b} ⊕ H(k_{i, b})
                    message = prgs[bitIndex].extendToBytes(message);
                    if (cotReceiverOutput.getChoice(offset)) {
                        BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                    } else {
                        BytesUtils.xori(message, messagePairArray[2 * offset]);
                    }
                    return LongUtils.fixedByteArrayToLong(message);
                })
                .toArray())
            .toArray(long[][]::new);
    }

    private List<byte[]> generateSenderCorrelationPayload(CotSenderOutput cotSenderOutput) {
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        return indexIntStream
            .mapToObj(index ->
                IntStream.range(0, l)
                    .mapToObj(i -> {
                        int offset = index * l + i;
                        int shiftByteL = prgs[i].getOutputByteLength();
                        byte[][] ciphertexts = new byte[2][];
                        ciphertexts[0] = prgs[i].extendToBytes(cotSenderOutput.getR0(offset));
                        byte[] message0 = LongUtils.longToFixedByteArray(senderMessagesArray[offset][0], shiftByteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prgs[i].extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = LongUtils.longToFixedByteArray(senderMessagesArray[offset][1], shiftByteL);
                        BytesUtils.xori(ciphertexts[1], message1);
                        return ciphertexts;
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private Zl64Triple computeTriples() {
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index ->
            IntStream.range(0, l).forEach(i -> {
                    // c_0 = c_0 + (s_{i, b} ⊕ H(k_{i, b}))
                    c0[index] = zl64.add(c0[index], zl64.shiftLeft(receiverCorrelations[index][i], l - 1 - i));
                }
            ));
        return Zl64Triple.create(zl64, a0, b0, c0);
    }
}
