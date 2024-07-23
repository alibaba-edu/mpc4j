package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using Silent COT sender.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public class ScotConv32Sender extends AbstractConv32Party {
    /**
     * NC-COT sender
     */
    private final NcCotSender ncCotSender;
    /**
     * max round num
     */
    private final int maxRoundNum;

    public ScotConv32Sender(Rpc senderRpc, Party receiverParty, ScotConv32Config config) {
        super(ScotConv32PtoDesc.getInstance(), senderRpc, receiverParty, config);
        NcCotConfig ncCotConfig = config.getNcCotConfig();
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, ncCotConfig);
        addSubPto(ncCotSender);
        // each conversion needs 2 COTs
        maxRoundNum = ncCotConfig.maxNum() / 2;
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        ncCotSender.init(delta, roundNum * 2);
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

        // two parties generate (x_0, x_1, y_0, y_1), (x′_0, x′_1, y′_0, y'_1)
        stopWatch.start();
        // we follow f^{ab} of [ALSZ13]
        BitVector x0p = BitVectorFactory.createEmpty();
        BitVector y0p = BitVectorFactory.createEmpty();
        while (x0p.bitNum() < num * 2) {
            // R chooses b. S and R perform a ROT with b as input of R. Here we use Silent ROT.
            CotSenderOutput cotSenderOutput = ncCotSender.send();
            // S sets x_0 = R_0 ⊕ R_1, y_0 = R_0
            int bitNum = cotSenderOutput.getNum();
            BitVector r0 = BitVectorFactory.createZeros(bitNum);
            BitVector r1 = BitVectorFactory.createZeros(bitNum);
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
            byte[][] r0Array = rotSenderOutput.getR0Array();
            byte[][] r1Array = rotSenderOutput.getR1Array();
            IntStream.range(0, bitNum).forEach(i -> {
                r0.set(i, (r0Array[i][0] & 0b00000001) != 0);
                r1.set(i, (r1Array[i][0] & 0b00000001) != 0);
            });
            // x_0 = R_0, y_0 = R_0 ⊕ R_1
            r1.xori(r0);
            x0p.merge(r0);
            y0p.merge(r1);
        }
        BitVector x0 = x0p.split(num);
        BitVector y0 = y0p.split(num);
        x0p.reduce(num);
        y0p.reduce(num);
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate 1-out-of-2 bit ROT");

        // correct (x_0, x_1, y_0, y_1) to (x_0, x_1, y_0, u_{1,0})
        stopWatch.start();
        // we can ahead of time decompose w0
        BitVector w00 = BitVectorFactory.createZeros(num);
        BitVector w01 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            w00.set(i, (w0[i] & 0b00000001) != 0);
            w01.set(i, (w0[i] & 0b00000010) != 0);
        });
        // P1 sends d = y_1 ⊕ u_{1,0}. P0 computes x_0 = x_0 ⊕ (y_0 · d)
        List<byte[]> dPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal());
        MpcAbortPreconditions.checkArgument(dPayload.size() == 1);
        BitVector d = BitVectorFactory.create(num, dPayload.get(0));
        x0.xori(y0.and(d));
        // P1 sends d' = y'_1 ⊕ u_{1,1}. P0 computes x'_0 = x'_0 ⊕ (y'_0 · d')
        List<byte[]> dpPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_D_PRIME.ordinal());
        MpcAbortPreconditions.checkArgument(dpPayload.size() == 1);
        BitVector dp = BitVectorFactory.create(num, dpPayload.get(0));
        x0p.xori(y0p.and(dp));
        // m_0 = x_0 ⊕ x′_0
        BitVector m0 = x0.xor(x0p);
        // m_1 = x_0 ⊕ y_0 ⊕ x′_0
        BitVector m1 = m0.xor(y0);
        // m_2 = x_0 ⊕ x′_0 ⊕ y′_0
        BitVector m2 = m0.xor(y0p);
        stopWatch.stop();
        long correctTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, correctTime);

        stopWatch.start();
        // P0 computes v_0 = w_{0,0} ⊕ m_0.
        BitVector v0 = w00.xor(m0);
        // P0 computes t_0 = v_0 ⊕ m_1 ⊕ w_{0,0} ⊕ w_{0,1} ⊕ 1
        BitVector t0 = v0.xor(m1);
        t0.xori(w00);
        t0.xori(w01);
        t0.noti();
        // P0 computes t_1 = v_0 ⊕ m_2 ⊕ w_{0,1}
        BitVector t1 = v0.xor(m2);
        t1.xori(w01);
        // P0 sends (t_0, t_1) to P1.
        List<byte[]> t0t1Payload = new LinkedList<>();
        t0t1Payload.add(t0.getBytes());
        t0t1Payload.add(t1.getBytes());
        sendOtherPartyPayload(PtoStep.SENDER_SEND_T0_T1.ordinal(), t0t1Payload);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, shareTime);

        logPhaseInfo(PtoState.PTO_END);
        // P0 outputs v0
        return v0.getBytes();
    }
}
