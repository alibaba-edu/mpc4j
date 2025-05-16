package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeReceiver;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using Silent VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class SvodeConv32Sender extends AbstractConv32Party {
    /**
     * GF2K-NC-VODE receiver
     */
    private final Gf2kNcVodeReceiver gf2kNcVodeReceiver;
    /**
     * max round num
     */
    private final int maxRoundNum;
    /**
     * crhf
     */
    private final Crhf crhf;

    public SvodeConv32Sender(Rpc senderRpc, Party receiverParty, SvodeConv32Config config) {
        super(SvodeConv32PtoDesc.getInstance(), senderRpc, receiverParty, config);
        Gf2kNcVodeConfig gf2kNcVodeConfig = config.getGf2kNcVodeConfig();
        gf2kNcVodeReceiver = Gf2kNcVodeFactory.createReceiver(senderRpc, receiverParty, gf2kNcVodeConfig);
        addSubPto(gf2kNcVodeReceiver);
        // each conversion needs 1 Subfield VODE
        maxRoundNum = gf2kNcVodeConfig.maxNum();
        crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        gf2kNcVodeReceiver.init(2, delta, roundNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init() throws MpcAbortException {
        init(maxRoundNum);
    }

    @Override
    public byte[] conv(byte[] w0) throws MpcAbortException {
        setPtoInput(w0);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // The parties generate a random 1-out-of-4 bit OT with messages (m_0, m_1, m_2, m_3) ∈ F_2^4 held by P0 and
        // (c, m_c) ∈ F_4 × F_2 held by P1.
        stopWatch.start();
        BitVector[] ms = IntStream.range(0, 4).mapToObj(i -> BitVectorFactory.createEmpty()).toArray(BitVector[]::new);
        while (ms[0].bitNum() < num) {
            Gf2kVodeReceiverOutput gf2kVodeReceiverOutput = gf2kNcVodeReceiver.receive();
            int roundNum = gf2kVodeReceiverOutput.getNum();
            byte[] delta = gf2kVodeReceiverOutput.getDelta();
            byte[][] qs = gf2kVodeReceiverOutput.getQ();
            Dgf2k field = gf2kVodeReceiverOutput.getField();

            byte[][] prfRes = new byte[4][roundNum];
            for(int k = 0; k < 4; k++){
                byte[] xk = new byte[]{(byte) k};
                byte[] xkDelta = field.mixMul(xk, delta);
                byte[] tmpPrfRes = new byte[roundNum];
                IntStream intStream = parallel ? IntStream.range(0, roundNum).parallel() : IntStream.range(0, roundNum);
                intStream.forEach(i -> tmpPrfRes[i] = crhf.hash(field.add(xkDelta, qs[i]))[0]);
                prfRes[k] = tmpPrfRes;
            }
            IntStream intStream = parallel ? IntStream.range(0, 4).parallel() : IntStream.range(0, 4);
            intStream.forEach(k -> {
                byte[] tmpPrfRes = prfRes[k];
                BitVector roundMk = BitVectorFactory.createZeros(roundNum);
                IntStream.range(0, roundNum).forEach(i -> roundMk.set(i, (tmpPrfRes[i] & 0b00000001) != 0));
                ms[k].merge(roundMk);
            });
        }
        Arrays.stream(ms).forEach(m -> m.reduce(num));

        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate Subfield VODE");

        stopWatch.start();
        BitVector w00 = BitVectorFactory.createZeros(num);
        BitVector w01 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            w00.set(i, (w0[i] & 0b00000001) != 0);
            w01.set(i, (w0[i] & 0b00000010) != 0);
        });
        long decomposeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, decomposeTime);

        // According to the function "mod2OtF4" in AltModWPrfProto.cpp of secure-join [https://github.com/Visa-Research/secure-join],
        // the choice correction is fulfilled with bit operations.
        // P1 sends d0Diff = w1_0 ^c_0, d1Diff = w1_1 ^c_1 to P0.
        List<byte[]> dPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal());
        stopWatch.start();
        // d0Diff is LSB, d1Diff is MSB
        MpcAbortPreconditions.checkArgument(dPayload.size() == 2);
        BitVector lsb = BitVectorFactory.create(num, dPayload.get(0));
        BitVector msb = BitVectorFactory.create(num, dPayload.get(1));
        swapBit(ms, 0, 1, lsb);
        swapBit(ms, 2, 3, lsb);
        swapBit(ms, 0, 2, msb);
        swapBit(ms, 1, 3, msb);
        //            u
        //          0 1 2
        //         ________
        //      0 | 0 1 0
        //   v  1 | 1 0 0
        //      2 | 0 0 1
        // the shared value if u=0
        BitVector tv0 = w00;
        // the shared value if u=1
        BitVector tv1 = w00.xor(w01).not();
        // the shared value if u=2
        BitVector tv2 = w01;
        // outShare = T[v,0] ^ ot_0. They will have ot_1 which xors with this to T[v,0]
        BitVector v0 = tv0.xor(ms[0]);
        // t1 = Enc( T[v, 1] ^ outShare )
        BitVector t1 = tv1.xor(v0).xor(ms[1]);
        // t2 = Enc( T[u, 2] ^ outShare )
        BitVector t2 = tv2.xor(v0).xor(ms[2]);
        // P0 sends (t_1, t_2) to P1.
        List<byte[]> t0t1Payload = new LinkedList<>();
        t0t1Payload.add(t1.getBytes());
        t0t1Payload.add(t2.getBytes());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_T0_T1.ordinal(), t0t1Payload);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, shareTime);

        logPhaseInfo(PtoState.PTO_END);
        // P0 outputs v0
        return v0.getBytes();
    }

    /**
     * Swap elements in array according to the choice bits.
     */
    private static void swapBit(BitVector[] array, int i, int j, BitVector choice) {
        BitVector diff = array[i].xor(array[j]);
        array[i] = array[i].xor(choice.and(diff));
        array[j] = array[i].xor(diff);
    }
}
