package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.AbstractZlCoreMtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15.Dsz15OtZlCoreMtgPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The receiver for DSZ15 OT-based Zl core multiplication triple generation protocol.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2023/2/20
 */
public class Dsz15OtZlCoreMtgReceiver extends AbstractZlCoreMtgParty {
    /**
     * the COT sender
     */
    private final CotSender cotSender;
    /**
     * the COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * the shift table
     */
    private final int[] shiftTable;
    /**
     * the pseudo-random generators
     */
    private final Prg[] prgs;
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
    private BigInteger[] senderCorrelations;

    public Dsz15OtZlCoreMtgReceiver(Rpc receiverRpc, Party senderParty, Dsz15OtZlCoreMtgConfig config) {
        super(Dsz15OtZlCoreMtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        cotSender = CotFactory.createSender(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotSender);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
        // each bit of a and b can be shifted to reduce the communication cost
        shiftTable = IntStream.range(0, l).map(i -> l - 1 - i).toArray();
        prgs = IntStream.range(0, l)
            .mapToObj(i -> {
                int shiftByteL = CommonUtils.getByteLength(i + 1);
                return PrgFactory.createInstance(envType, shiftByteL);
            })
            .toArray(Prg[]::new);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * l);
        cotReceiver.init(maxNum * l);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZlTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initParamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 6, initParamTime);

        stopWatch.start();
        // the first COT round
        CotSenderOutput cotSenderOutput = cotSender.send(num * l);
        stopWatch.stop();
        long firstCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, firstCotTime);

        stopWatch.start();
        // generate receiver's correlations
        List<byte[]> receiverCorrelationPayload = generateReceiverCorrelationPayload(cotSenderOutput);
        DataPacketHeader receiverCorrelationHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(receiverCorrelationHeader, receiverCorrelationPayload));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, sendTime);

        // the second COT round
        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(receiverChoices);
        stopWatch.stop();
        long secondCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, secondCotTime);

        DataPacketHeader senderCorrelationHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderCorrelationPayload = rpc.receive(senderCorrelationHeader).getPayload();

        stopWatch.start();
        // handle sender's correlations
        handleSenderCorrelationPayload(cotReceiverOutput, senderCorrelationPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, receiveTime);

        stopWatch.start();
        ZlTriple receiverOutput = computeTriples();
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private void initParams() {
        a1 = new BigInteger[num];
        b1 = new BigInteger[num];
        c1 = new BigInteger[num];
        receiverChoices = new boolean[num * l];
        receiverCorrelationPairs = new BigInteger[num * l][2];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // Let P_1 randomly generate <a>_1, <b>_1
            a1[arrayIndex] = new BigInteger(l, secureRandom);
            b1[arrayIndex] = new BigInteger(l, secureRandom);
            // The terms <a>_1 * <b>_1 can be computed locally by P_1
            c1[arrayIndex] = a1[arrayIndex].multiply(b1[arrayIndex]).and(mask);
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(i -> {
                // in the i-th COT, P_1 inputs the correlation function <a>_1 * 2^i - y mod 2^l
                BigInteger y = new BigInteger(i + 1, secureRandom);
                // c_1 = c_1 - y * 2^{i}
                c1[arrayIndex] = c1[arrayIndex].subtract(y.shiftLeft(shiftTable[i])).and(mask);
                // s_{i, 0} = y
                receiverCorrelationPairs[offset + i][0] = y;
                // s_{i, 1} = ((a_1 + y) << 2^i)
                receiverCorrelationPairs[offset + i][1] = a1[arrayIndex]
                    .add(y).shiftLeft(shiftTable[i]).and(mask)
                    .shiftRight(shiftTable[i]);
            });
            // In the i-th COT, P_1 inputs <b>_1[i] as choice bit.
            byte[] byteChoices = BigIntegerUtils.nonNegBigIntegerToByteArray(b1[arrayIndex], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, receiverChoices, offset, l);
        });
    }

    private List<byte[]> generateReceiverCorrelationPayload(CotSenderOutput cotSenderOutput) {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(i -> {
                        int offset = arrayIndex * l + i;
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
        MpcAbortPreconditions.checkArgument(senderMessagesPayload.size() == num * l * 2);
        byte[][] messagePairArray = senderMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        senderCorrelations = indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(i -> {
                        int offset = arrayIndex * l + i;
                        byte[] message = cotReceiverOutput.getRb(offset);
                        message = prgs[i].extendToBytes(message);
                        if (cotReceiverOutput.getChoice(offset)) {
                            BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                        } else {
                            BytesUtils.xori(message, messagePairArray[2 * offset]);
                        }
                        return BigIntegerUtils.byteArrayToNonNegBigInteger(message);
                    })
                    .collect(Collectors.toList())
            ).flatMap(Collection::stream)
            .toArray(BigInteger[]::new);
        receiverChoices = null;
    }

    private ZlTriple computeTriples() {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(i -> {
                    // c_1 = c_1 + (s_{i, b} ⊕ H(k_{i, b}))
                    c1[arrayIndex] = c1[arrayIndex].add(senderCorrelations[offset + i].shiftLeft(shiftTable[i])).and(mask);
                }
            );
        });
        ZlTriple zlTriple = ZlTriple.create(zl, num, a1, b1, c1);
        a1 = null;
        b1 = null;
        c1 = null;
        senderCorrelations = null;
        receiverCorrelationPairs = null;
        return zlTriple;
    }
}
