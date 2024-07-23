package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * LCOT Z2 triple generation receiver.
 *
 * @author Liqiang Peng
 * @date 2024/5/27
 */
public class LcotZ2TripleGenReceiver extends AbstractZ2TripleGenParty {
    /**
     * LCOT receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * round num
     */
    private int roundNum;
    /**
     * hash
     */
    private Hash hash;

    public LcotZ2TripleGenReceiver(Rpc receiverRpc, Party senderParty, LcotZ2TripleGenConfig config) {
        super(LcotZ2TripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // we need to ensure round num is an even number
        if (expectTotalNum % 2 == 1) {
            roundNum = Math.min(config.defaultRoundNum(), expectTotalNum + 1);
        } else {
            roundNum = Math.min(config.defaultRoundNum(), expectTotalNum);
        }
        lcotReceiver.init(LcotZ2TripleGenPtoDesc.L);
        hash = HashFactory.createInstance(envType, 1);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // this.num stores the actual number of required triples, but we need to first generate even number of triples.
        int evenNum = (num % 2 == 1) ? num + 1 : num;
        Z2Triple triple = Z2Triple.createEmpty();
        int roundCount = 1;
        while (triple.getNum() < num) {
            Z2Triple eachTriple;
            if (evenNum - triple.getNum() < roundNum) {
                eachTriple = roundGenerate(evenNum - triple.getNum(), roundCount);
            } else {
                eachTriple = roundGenerate(roundNum, roundCount);
            }
            triple.merge(eachTriple);
        }
        if (num % 2 == 1) {
            triple.reduce(num);
        }

        logPhaseInfo(PtoState.PTO_END);
        return triple;
    }

    private Z2Triple roundGenerate(int num, int roundCount) throws MpcAbortException {
        stopWatch.start();
        // ensure num is an even number
        assert num % 2 == 0;
        int numDiv2 = num / 2;
        boolean[] a = BinaryUtils.randomBinary(num, secureRandom);
        boolean[] b = BinaryUtils.randomBinary(num, secureRandom);
        byte[][] choices = new byte[num / 2][1];
        IntStream intStream = parallel ? IntStream.range(0, numDiv2).parallel() : IntStream.range(0, numDiv2);
        intStream.forEach(i ->
            choices[i][0] = (byte) ((b[i * 2 + 1] ? 8 : 0) + (a[i * 2 + 1] ? 4 : 0) + (b[i * 2] ? 2 : 0) + (a[i * 2] ? 1 : 0))
        );
        LcotReceiverOutput lcotReceiverOutput = lcotReceiver.receive(choices);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 2, lcotTime);

        List<byte[]> encPayload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal());

        stopWatch.start();
        byte[][] encByteArray = encPayload.toArray(new byte[0][]);
        boolean[] c = new boolean[num];
        IntStream genTripleIntStream = parallel ? IntStream.range(0, numDiv2).parallel() : IntStream.range(0, numDiv2);
        genTripleIntStream.forEach(i -> {
            int ciphertext = hash.digestToBytes(lcotReceiverOutput.getRb(i))[0] & 0b11;
            int shift = (choices[i][0] & 0b11) << 1;
            int count = choices[i][0] >> 2;
            int enc = (encByteArray[i][count] >> shift) & 0b11;
            byte lotOutput = (byte) (ciphertext ^ enc);
            c[i * 2] = (lotOutput & 0b01) != 0;
            c[i * 2 + 1] = (lotOutput & 0b10) != 0;
        });
        stopWatch.stop();
        long genTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 2, 2, genTripleTime);

        return Z2Triple.create(
            num,
            BinaryUtils.binaryToRoundByteArray(a),
            BinaryUtils.binaryToRoundByteArray(b),
            BinaryUtils.binaryToRoundByteArray(c)
        );
    }
}
