package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct.DirectZlTripleGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.AbstractZlTripleGenParty;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct DSZ15 OT-based Zl triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class DirectZlTripleGenReceiver extends AbstractZlTripleGenParty {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
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
    private BigInteger[] a1;
    /**
     * b1
     */
    private BigInteger[] b1;
    /**
     * c1
     */
    private BigInteger[] c1;
    /**
     * the receiver's correlation pairs (in the first COT round)
     */
    private BigInteger[][] receiverCorrelationPairs;
    /**
     * the receiver's choice (in the second COT round)
     */
    private boolean[] receiverChoices;
    /**
     * the sender's corrections (in the second COT round)
     */
    private BigInteger[][] senderCorrelations;

    public DirectZlTripleGenReceiver(Rpc receiverRpc, Party senderParty, DirectZlTripleGenConfig config) {
        super(DirectZlTripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(expectTotalNum, config.defaultRoundNum(maxL));
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        coreCotReceiver.init();
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
    public ZlTriple generate(Zl zl, int num) throws MpcAbortException {
        setPtoInput(zl, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        ZlTriple triple = ZlTriple.createEmpty(zl);
        int roundCount = 1;
        while (triple.getNum() < num) {
            int gapNum = num - triple.getNum();
            eachNum = Math.min(gapNum, roundNum);
            ZlTriple eachTriple = roundGenerate(roundCount);
            triple.merge(eachTriple);
            roundCount++;
        }

        logPhaseInfo(PtoState.PTO_END);
        return triple;
    }

    private ZlTriple roundGenerate(int roundCount) throws MpcAbortException {
        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 6, initParamTime);

        stopWatch.start();
        // the first COT round
        CotSenderOutput cotSenderOutput = coreCotSender.send(eachNum * l);
        stopWatch.stop();
        long firstCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 6, firstCotTime);

        stopWatch.start();
        // generate receiver's correlations
        List<byte[]> receiverCorrelationPayload = generateReceiverCorrelationPayload(cotSenderOutput);
        receiverCorrelationPairs = null;
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), receiverCorrelationPayload);
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 3, 6, sendTime);

        // the second COT round
        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(receiverChoices);
        receiverChoices = null;
        stopWatch.stop();
        long secondCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 4, 6, secondCotTime);

        List<byte[]> senderCorrelationPayload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_CORRELATION.ordinal());

        stopWatch.start();
        // handle sender's correlations
        handleSenderCorrelationPayload(cotReceiverOutput, senderCorrelationPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 5, 6, receiveTime);

        stopWatch.start();
        ZlTriple eachTriple = computeTriples();
        senderCorrelations = null;
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 6, 6, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return eachTriple;
    }

    private void initParams() {
        a1 = new BigInteger[eachNum];
        b1 = new BigInteger[eachNum];
        c1 = new BigInteger[eachNum];
        receiverChoices = new boolean[eachNum * l];
        receiverCorrelationPairs = new BigInteger[eachNum * l][2];
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index -> {
            // Let P_1 randomly generate <a>_1, <b>_1
            a1[index] = zl.createRandom(secureRandom);
            b1[index] = zl.createRandom(secureRandom);
            // The terms <a>_1 * <b>_1 can be computed locally by P_1
            c1[index] = zl.mul(a1[index], b1[index]);
            int offset = index * l;
            IntStream.range(0, l).forEach(i -> {
                // in the i-th COT, P_1 inputs the correlation function <a>_1 * 2^i - y mod 2^l
                BigInteger y = zl.shiftRight(zl.createRandom(secureRandom), l - 1 - i);
                // c_1 = c_1 - y * 2^{i}
                c1[index] = zl.sub(c1[index], zl.shiftLeft(y, l - 1 - i));
                // s_{i, 0} = y
                receiverCorrelationPairs[offset + i][0] = y;
                // s_{i, 1} = ((a_1 + y) << 2^i)
                receiverCorrelationPairs[offset + i][1]
                    = zl.shiftRight(zl.shiftLeft(zl.add(a1[index], y), l - 1 - i), l - 1 - i);
            });
            // In the i-th COT, P_1 inputs <b>_1[i] as choice bit.
            byte[] byteChoices = BigIntegerUtils.nonNegBigIntegerToByteArray(b1[index], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, receiverChoices, offset, l);
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
                        byte[] message0 = BigIntegerUtils.nonNegBigIntegerToByteArray(receiverCorrelationPairs[offset][0], shiftByteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prgs[i].extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = BigIntegerUtils.nonNegBigIntegerToByteArray(receiverCorrelationPairs[offset][1], shiftByteL);
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
                    .mapToObj(i -> {
                        int offset = index * l + i;
                        byte[] message = cotReceiverOutput.getRb(offset);
                        message = prgs[i].extendToBytes(message);
                        if (cotReceiverOutput.getChoice(offset)) {
                            BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                        } else {
                            BytesUtils.xori(message, messagePairArray[2 * offset]);
                        }
                        return BigIntegerUtils.byteArrayToNonNegBigInteger(message);
                    })
                    .toArray(BigInteger[]::new))
            .toArray(BigInteger[][]::new);
    }

    private ZlTriple computeTriples() {
        IntStream indexIntStream = parallel ? IntStream.range(0, eachNum).parallel() : IntStream.range(0, eachNum);
        indexIntStream.forEach(index ->
            IntStream.range(0, l).forEach(i -> {
                    // c_1 = c_1 + (s_{i, b} ⊕ H(k_{i, b}))
                    c1[index] = zl.add(c1[index], zl.shiftLeft(senderCorrelations[index][i], l - 1 - i));
                }
            ));
        return ZlTriple.create(zl, a1, b1, c1);
    }
}
