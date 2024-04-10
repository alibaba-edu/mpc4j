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
 * The sender for DSZ15 OT-based Zl core multiplication triple generation protocol.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2023/2/20
 */
public class Dsz15OtZlCoreMtgSender extends AbstractZlCoreMtgParty {
    /**
     * the COT receiver
     */
    private final CotReceiver cotReceiver;
    /**
     * the COT sender
     */
    private final CotSender cotSender;
    /**
     * the shift table
     */
    private final int[] shiftTable;
    /**
     * the pseudo-random generators
     */
    private final Prg[] prgs;
    /**
     * a0
     */
    private BigInteger[] a0;
    /**
     * b0
     */
    private BigInteger[] b0;
    /**
     * c0
     */
    private BigInteger[] c0;
    /**
     * the sender's choices (in the first COT round)
     */
    private boolean[] senderChoices;
    /**
     * the receiver's correlations (in the first COT round)
     */
    private BigInteger[] receiverCorrelations;
    /**
     * the sender's correlation pairs (in the second COT round)
     */
    private BigInteger[][] senderMessagesArray;

    public Dsz15OtZlCoreMtgSender(Rpc senderRpc, Party receiverParty, Dsz15OtZlCoreMtgConfig config) {
        super(Dsz15OtZlCoreMtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotReceiver = CotFactory.createReceiver(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotReceiver);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
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
        cotReceiver.init(maxNum * l);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxNum * l);
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
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(senderChoices);
        stopWatch.stop();
        long firstCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 6, firstCotTime);

        // receive messages in the first COT round
        DataPacketHeader receiverCorrelationHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CORRELATION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> receiverCorrelationPayload = rpc.receive(receiverCorrelationHeader).getPayload();

        stopWatch.start();
        // handle the receiver's correlations
        handleReceiverCorrelationPayload(cotReceiverOutput, receiverCorrelationPayload);
        stopWatch.stop();
        long receiveTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 6, receiveTime);

        stopWatch.start();
        // the second COT round
        CotSenderOutput cotSenderOutput = cotSender.send(num * l);
        stopWatch.stop();
        long secondCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 6, secondCotTime);

        stopWatch.start();
        // generate the sender's correlations
        List<byte[]> senderCorrelationPayload = generateSenderCorrelationPayload(cotSenderOutput);
        DataPacketHeader senderCorrelationHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderCorrelationHeader, senderCorrelationPayload));
        stopWatch.stop();
        long sendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 6, sendTime);

        stopWatch.start();
        ZlTriple senderOutput = computeTriples();
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 6, 6, tripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void initParams() {
        a0 = new BigInteger[num];
        b0 = new BigInteger[num];
        c0 = new BigInteger[num];
        senderChoices = new boolean[num * l];
        senderMessagesArray = new BigInteger[num * l][2];
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            // Let P_0 randomly generate <a>_0, <b>_0
            a0[arrayIndex] = new BigInteger(l, secureRandom);
            b0[arrayIndex] = new BigInteger(l, secureRandom);
            // The terms <a>_0 * <b>_0 can be computed locally by P_0
            c0[arrayIndex] = a0[arrayIndex].multiply(b0[arrayIndex]).and(mask);
            // in the i-th COT, P_0 inputs the correlation function <a>_0 * 2^i - x mod 2^l
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(i -> {
                BigInteger x = new BigInteger(i + 1, secureRandom);
                // c_0 = c_0 - x * 2^{i}
                c0[arrayIndex] = c0[arrayIndex].subtract(x.shiftLeft(shiftTable[i])).and(mask);
                // s_{i, 0} = x
                senderMessagesArray[offset + i][0] = x;
                // s_{i, 1} = ((a_0 + x) << 2^i)
                senderMessagesArray[offset + i][1] = a0[arrayIndex]
                    .add(x).shiftLeft(shiftTable[i]).and(mask)
                    .shiftRight(shiftTable[i]);
            });
            // In the i-th COT, P_0 inputs <b>_0[i] as choice bit.
            byte[] byteChoices = BigIntegerUtils.nonNegBigIntegerToByteArray(b0[arrayIndex], byteL);
            boolean[] binaryChoices = BinaryUtils.byteArrayToBinary(byteChoices, l);
            System.arraycopy(binaryChoices, 0, senderChoices, offset, l);
        });
    }

    private void handleReceiverCorrelationPayload(CotReceiverOutput cotReceiverOutput, List<byte[]> receiverMessagesPayload)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(receiverMessagesPayload.size() == num * l * 2);
        byte[][] messagePairArray = receiverMessagesPayload.toArray(new byte[0][]);
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        receiverCorrelations = indexIntStream
            .mapToObj(arrayIndex ->
                IntStream.range(0, l)
                    .mapToObj(bitIndex -> {
                        int offset = arrayIndex * l + bitIndex;
                        byte[] message = cotReceiverOutput.getRb(offset);
                        // s_{i, b} ⊕ H(k_{i, b})
                        message = prgs[bitIndex].extendToBytes(message);
                        if (cotReceiverOutput.getChoice(offset)) {
                            BytesUtils.xori(message, messagePairArray[2 * offset + 1]);
                        } else {
                            BytesUtils.xori(message, messagePairArray[2 * offset]);
                        }
                        return BigIntegerUtils.byteArrayToNonNegBigInteger(message);
                    })
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .toArray(BigInteger[]::new);
        senderChoices = null;
    }

    private List<byte[]> generateSenderCorrelationPayload(CotSenderOutput cotSenderOutput) {
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
                        byte[] message0 = BigIntegerUtils.nonNegBigIntegerToByteArray(senderMessagesArray[offset][0], shiftByteL);
                        BytesUtils.xori(ciphertexts[0], message0);
                        ciphertexts[1] = prgs[i].extendToBytes(cotSenderOutput.getR1(offset));
                        byte[] message1 = BigIntegerUtils.nonNegBigIntegerToByteArray(senderMessagesArray[offset][1], shiftByteL);
                        BytesUtils.xori(ciphertexts[1], message1);
                        return ciphertexts;
                    })
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private ZlTriple computeTriples() {
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(arrayIndex -> {
            int offset = arrayIndex * l;
            IntStream.range(0, l).forEach(i -> {
                    // c_0 = c_0 + (s_{i, b} ⊕ H(k_{i, b}))
                c0[arrayIndex] = c0[arrayIndex].add(receiverCorrelations[offset + i].shiftLeft(shiftTable[i])).and(mask);
                }
            );
        });
        ZlTriple zlTriple = ZlTriple.create(zl, num, a0, b0, c0);
        a0 = null;
        b0 = null;
        c0 = null;
        receiverCorrelations = null;
        senderMessagesArray = null;
        return zlTriple;
    }
}
