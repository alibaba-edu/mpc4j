package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using Silent COT receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public class ScotConv32Receiver extends AbstractConv32Party {
    /**
     * no-choice COT receiver
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * max round num
     */
    private final int maxRoundNum;

    public ScotConv32Receiver(Rpc receiverRpc, Party senderParty, ScotConv32Config config) {
        super(ScotConv32PtoDesc.getInstance(), receiverRpc, senderParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, ncCotConfig);
        addSubPto(ncCotReceiver);
        // each conversion needs 2 COTs
        maxRoundNum = ncCotConfig.maxNum() / 2;
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        ncCotReceiver.init(roundNum * 2);
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
    public byte[] conv(byte[] w1) throws MpcAbortException {
        setPtoInput(w1);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // two parties generate (x_0, x_1, y_0, y_1), (x′_0, x′_1, y′_0, y'_1)
        stopWatch.start();
        // we follow f^{ab} of [ALSZ13], we need to do two rounds, here is round 1
        BitVector x1p = BitVectorFactory.createEmpty();
        BitVector y1p = BitVectorFactory.createEmpty();
        while (x1p.bitNum() < num * 2) {
            // R chooses b. S and R perform a ROT with b as input of R. Here we use Silent ROT.
            CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
            // R sets x_1 = R_b, y_1 = b
            int bitNum = cotReceiverOutput.getNum();
            BitVector b = BitVectorFactory.createZeros(bitNum);
            BitVector rb = BitVectorFactory.createZeros(bitNum);
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
            boolean[] choices = rotReceiverOutput.getChoices();
            byte[][] rbArray = rotReceiverOutput.getRbArray();
            IntStream.range(0, bitNum).forEach(i -> {
                b.set(i, choices[i]);
                rb.set(i, (rbArray[i][0] & 0b00000001) != 0);
            });
            x1p.merge(rb);
            y1p.merge(b);
        }
        BitVector x1 = x1p.split(num);
        BitVector y1 = y1p.split(num);
        x1p.reduce(num);
        y1p.reduce(num);
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate 1-out-of-2 bit ROT");

        // correct (x_0, x_1, y_0, y_1) to (x_0, x_1, y_0, u_{1,0})
        stopWatch.start();
        // P1 sends d = y_1 ⊕ u_{1,0}. P0 computes x_0 = x_0 ⊕ (y_0 · d)
        BitVector w10 = BitVectorFactory.createZeros(num);
        BitVector w11 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            w10.set(i, (w1[i] & 0b00000001) != 0);
            w11.set(i, (w1[i] & 0b00000010) != 0);
        });
        BitVector d = y1.xor(w10);
        List<byte[]> dPayload = Collections.singletonList(d.getBytes());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal(), dPayload);
        // P1 sends d' = y'_1 ⊕ u_{1,1}. P0 computes x'_0 = x'_0 ⊕ (y'_0 · d')
        BitVector dp = y1p.xor(w11);
        List<byte[]> dpPayload = Collections.singletonList(dp.getBytes());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_D_PRIME.ordinal(), dpPayload);
        stopWatch.stop();
        long correctTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, correctTime);

        stopWatch.start();
        // P0 sends (t_0, t_1) to P1.
        List<byte[]> t0t1Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_T0_T1.ordinal());
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == 2);
        BitVector t0 = BitVectorFactory.create(num, t0t1Payload.get(0));
        BitVector t1 = BitVectorFactory.create(num, t0t1Payload.get(1));
        // P1 compute v_1 = (w_{1,0} · t_0) ⊕ (w_{1,1} · t_1) ⊕ x_{1,0} ⊕ x_{1,1}
        BitVector v1 = w10.and(t0);
        v1.xori(w11.and(t1));
        v1.xori(x1);
        v1.xori(x1p);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, shareTime);

        logPhaseInfo(PtoState.PTO_END);
        // P1 outputs v1
        return v1.getBytes();
    }
}
