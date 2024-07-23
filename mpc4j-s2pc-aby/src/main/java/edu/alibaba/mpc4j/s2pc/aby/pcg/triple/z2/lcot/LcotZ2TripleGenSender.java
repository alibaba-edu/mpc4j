package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LCOT Z2 triple generation sender.
 *
 * @author Liqiang Peng
 * @date 2024/5/27
 */
public class LcotZ2TripleGenSender extends AbstractZ2TripleGenParty {
    /**
     * LCOT sender
     */
    private final LcotSender lcotSender;
    /**
     * round num
     */
    private int roundNum;
    /**
     * hash
     */
    private Hash hash;

    public LcotZ2TripleGenSender(Rpc senderRpc, Party receiverParty, LcotZ2TripleGenConfig config) {
        super(LcotZ2TripleGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lcotSender = LcotFactory.createSender(senderRpc, receiverParty, config.getLcotConfig());
        addSubPto(lcotSender);
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
        lcotSender.init(LcotZ2TripleGenPtoDesc.L);
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
            roundCount++;
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
        LcotSenderOutput lcotSenderOutput = lcotSender.send(numDiv2);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, roundCount, 1, 2, lcotTime);

        stopWatch.start();
        boolean[] a = BinaryUtils.randomBinary(num, secureRandom);
        boolean[] b = BinaryUtils.randomBinary(num, secureRandom);
        boolean[] c = BinaryUtils.randomBinary(num, secureRandom);
        byte[][] lotInput = new byte[numDiv2][1 << (LcotZ2TripleGenPtoDesc.L - 2)];
        for (int j = 0; j < (1 << LcotZ2TripleGenPtoDesc.L); j++) {
            boolean j0 = (j & 0b0001) != 0;
            boolean j1 = (j & 0b0010) != 0;
            boolean j2 = (j & 0b0100) != 0;
            boolean j3 = (j & 0b1000) != 0;
            int shift = (j % 4) * 2;
            int count = j / 4;
            IntStream intStream = parallel ? IntStream.range(0, numDiv2).parallel() : IntStream.range(0, numDiv2);
            intStream.forEach(i -> {
                boolean t0 = (a[i * 2 + 1] ^ j2) & (b[i * 2 + 1] ^ j3) ^ c[i * 2 + 1];
                boolean t1 = (a[i * 2] ^ j0) & (b[i * 2] ^ j1) ^ c[i * 2];
                int value = (t0 ? 2 : 0) + (t1 ? 1 : 0);
                lotInput[i][count] ^= (byte) (value << shift);
            });
        }
        IntStream intStream = parallel ? IntStream.range(0, numDiv2).parallel() : IntStream.range(0, numDiv2);
        List<byte[]> encPayload = intStream.mapToObj(i -> {
            byte[] ciphertext = new byte[1 << (LcotZ2TripleGenPtoDesc.L - 2)];
            for (int j = 0; j < 1 << LcotZ2TripleGenPtoDesc.L; j++) {
                int digest =  hash.digestToBytes(lcotSenderOutput.getRb(i, new byte[]{(byte) j}))[0] & 0b11;
                int shift = (j & 0b11) << 1;
                int count = j >> 2;
                ciphertext[count] ^= (byte) (digest << shift);
            }
            BytesUtils.xori(ciphertext, lotInput[i]);
            return ciphertext;
        }).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), encPayload);
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
