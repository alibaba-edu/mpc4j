package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.Gf2kNcVodeReceiver;

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
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
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
        BitVector m0 = BitVectorFactory.createEmpty();
        BitVector m1 = BitVectorFactory.createEmpty();
        BitVector m2 = BitVectorFactory.createEmpty();
        BitVector m3 = BitVectorFactory.createEmpty();
        byte[] x0 = new byte[]{0b00000000};
        byte[] x1 = new byte[]{0b00000001};
        byte[] x2 = new byte[]{0b00000010};
        byte[] x3 = new byte[]{0b00000011};
        while (m0.bitNum() < num) {
            Gf2kVodeReceiverOutput gf2kVodeReceiverOutput = gf2kNcVodeReceiver.receive();
            int roundNum = gf2kVodeReceiverOutput.getNum();
            BitVector roundM0 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM1 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM2 = BitVectorFactory.createZeros(roundNum);
            BitVector roundM3 = BitVectorFactory.createZeros(roundNum);
            byte[] delta = gf2kVodeReceiverOutput.getDelta();
            byte[][] qs = gf2kVodeReceiverOutput.getQ();
            Dgf2k field = gf2kVodeReceiverOutput.getField();
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
