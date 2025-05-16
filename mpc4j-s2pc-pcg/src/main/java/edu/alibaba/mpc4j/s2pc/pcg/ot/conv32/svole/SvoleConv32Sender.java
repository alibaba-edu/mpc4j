package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleReceiver;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using Silent VOLE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class SvoleConv32Sender extends AbstractConv32Party {
    /**
     * NC-GF2K-VOLE receiver
     */
    private final Gf2kNcVoleReceiver gf2kNcVoleReceiver;
    /**
     * max round num
     */
    private final int maxRoundNum;
    /**
     * crhf
     */
    private final Crhf crhf;

    public SvoleConv32Sender(Rpc senderRpc, Party receiverParty, SvoleConv32Config config) {
        super(SvoleConv32PtoDesc.getInstance(), senderRpc, receiverParty, config);
        Gf2kNcVoleConfig gf2kNcVoleConfig = config.getGf2kNcVoleConfig();
        gf2kNcVoleReceiver = Gf2kNcVoleFactory.createReceiver(senderRpc, receiverParty, gf2kNcVoleConfig);
        addSubPto(gf2kNcVoleReceiver);
        // each conversion needs 1 Subfield VOLE
        maxRoundNum = gf2kNcVoleConfig.maxNum();
        crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        gf2kNcVoleReceiver.init(2, delta, roundNum);
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
        BitVector m0 = BitVectorFactory.createEmpty();
        BitVector m1 = BitVectorFactory.createEmpty();
        BitVector m2 = BitVectorFactory.createEmpty();
        BitVector m3 = BitVectorFactory.createEmpty();
        byte[] x0 = new byte[]{0b00000000};
        byte[] x1 = new byte[]{0b00000001};
        byte[] x2 = new byte[]{0b00000010};
        byte[] x3 = new byte[]{0b00000011};
        while (m0.bitNum() < num) {
            Gf2kVoleReceiverOutput gf2kVoleReceiverOutput = gf2kNcVoleReceiver.receive();
            int roundNum = gf2kVoleReceiverOutput.getNum();
            BitVector roundM0 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM1 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM2 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM3 = BitVectorFactory.createZeros(roundNum);
            byte[] delta = gf2kVoleReceiverOutput.getDelta();
            byte[][] qs = gf2kVoleReceiverOutput.getQ();
            Sgf2k field = gf2kVoleReceiverOutput.getField();
            IntStream.range(0, roundNum).forEach(i -> {
                roundM0.set(i, (crhf.hash(field.add(field.mixMul(x0, delta), qs[i]))[0] & 0b00000001) != 0);
                roundM1.set(i, (crhf.hash(field.add(field.mixMul(x1, delta), qs[i]))[0] & 0b00000001) != 0);
                roundM2.set(i, (crhf.hash(field.add(field.mixMul(x2, delta), qs[i]))[0] & 0b00000001) != 0);
                roundM3.set(i, (crhf.hash(field.add(field.mixMul(x3, delta), qs[i]))[0] & 0b00000001) != 0);
            });
            m0.merge(roundM0);
            m1.merge(roundM1);
            m2.merge(roundM2);
            m3.merge(roundM3);
        }
        m0.reduce(num);
        m1.reduce(num);
        m2.reduce(num);
        m3.reduce(num);
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate 1-out-of-4 bit ROT");

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

        // P1 sends d = w1 − c mod p to P0.
        List<byte[]> dPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(dPayload.size() == 1);
        byte[] ds = SerializeUtils.decompressL2(dPayload.get(0), num);
        // P0 computes v_0 = w_{0,0} ⊕ m_d, t_0 = v_0 ⊕ m_{d + 1} ⊕ w_{0,0} ⊕ w_{0,1} ⊕ 1, t_1 = v_0 ⊕ m_{d + 2} ⊕ w_{0,1}
        // but the correct is v_0 = w_{0,0} ⊕ m_d, t_0 = v_0 ⊕ m_{d - 1} ⊕ w_{0,0} ⊕ w_{0,1} ⊕ 1, t_1 = v_0 ⊕ m_{d - 2} ⊕ w_{0,1}
        BitVector md0 = BitVectorFactory.createZeros(num);
        BitVector md1 = BitVectorFactory.createZeros(num);
        BitVector md2 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            int d = ds[i];
            if (d == 0) {
                md0.set(i, m0.get(i));
                md1.set(i, m1.get(i));
                md2.set(i, m2.get(i));
            } else if (d == 1) {
                md0.set(i, m3.get(i));
                md1.set(i, m0.get(i));
                md2.set(i, m1.get(i));
            } else if (d == 2) {
                md0.set(i, m2.get(i));
                md1.set(i, m3.get(i));
                md2.set(i, m0.get(i));
            } else {
                assert d == 3;
                md0.set(i, m1.get(i));
                md1.set(i, m2.get(i));
                md2.set(i, m3.get(i));
            }
        });
        // v_0 = w_{0,0} ⊕ m_d
        BitVector v0 = w00.xor(md0);
        // t_0 = v_0 ⊕ m_{d - 1} ⊕ w_{0,0} ⊕ w_{0,1} ⊕ 1
        BitVector t0 = v0.xor(md1);
        t0.xori(w00);
        t0.xori(w01);
        t0.noti();
        // t_0 = v_0 ⊕ m_{d - 2} ⊕ w_{0,0} ⊕ w_{0,1} ⊕ 1
        BitVector t1 = v0.xor(md2);
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
