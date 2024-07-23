package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.vector.ByteVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.AbstractConv32Party;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32PtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleSender;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F_3 -> F_2 modulus conversion using Silent VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class SvoleConv32Receiver extends AbstractConv32Party {
    /**
     * NC-GF2K-VOLE sender
     */
    private final Gf2kNcVoleSender gf2kNcVoleSender;
    /**
     * max round num
     */
    private final int maxRoundNum;
    /**
     * crhf
     */
    private final Crhf crhf;
    /**
     * Z_{2^2}, used for merging c
     */
    private final Zl64 zl2;

    public SvoleConv32Receiver(Rpc receiverRpc, Party senderParty, SvoleConv32Config config) {
        super(SvoleConv32PtoDesc.getInstance(), receiverRpc, senderParty, config);
        Gf2kNcVoleConfig gf2kNcVoleConfig = config.getGf2kNcVoleConfig();
        gf2kNcVoleSender = Gf2kNcVoleFactory.createSender(receiverRpc, senderParty, gf2kNcVoleConfig);
        addSubPto(gf2kNcVoleSender);
        // each conversion needs 1 Subfield VOLE
        maxRoundNum = gf2kNcVoleConfig.maxNum();
        crhf = CrhfFactory.createInstance(envType, CrhfType.MMO);
        zl2 = Zl64Factory.createInstance(envType, 2);
    }

    @Override
    public void init(int expectNum) throws MpcAbortException {
        setInitInput(expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int roundNum = Math.min(maxRoundNum, expectNum);
        gf2kNcVoleSender.init(2, roundNum);
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

        // The parties generate a random 1-out-of-4 bit OT with messages (m_0, m_1, m_2, m_3) ∈ F_2^4 held by P0 and
        // (c, m_c) ∈ F_4 × F_2 held by P1.
        stopWatch.start();
        ByteVector c = ByteVector.createEmpty();
        BitVector mc = BitVectorFactory.createEmpty();
        while (mc.bitNum() < num) {
            Gf2kVoleSenderOutput gf2kVoleSenderOutput = gf2kNcVoleSender.send();
            int roundNum = gf2kVoleSenderOutput.getNum();
            ByteVector roundC = ByteVector.createZeros(roundNum);
            BitVector roundMc = BitVectorFactory.createZeros(roundNum);
            byte[][] xs = gf2kVoleSenderOutput.getX();
            byte[][] ts = gf2kVoleSenderOutput.getT();
            IntStream.range(0, roundNum).forEach(i -> {
                roundC.setElement(i, xs[i][0]);
                roundMc.set(i, (crhf.hash(ts[i])[0] & 0b00000001) != 0);
            });
            c.merge(roundC);
            mc.merge(roundMc);
        }
        c.reduce(num);
        mc.reduce(num);
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, roundTime, "Parties generate 1-out-of-4 bit ROT");

        stopWatch.start();
        // P1 sends d = w1 − c mod p to P0.
        byte[] ds = new byte[num];
        for (int i = 0; i < num; i++) {
            ds[i] = (byte) zl2.sub(w1[i], c.getElement(i));
        }
        byte[] dsCompress = SerializeUtils.compressL2(ds);
        List<byte[]> dPayload = Collections.singletonList(dsCompress);
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_D.ordinal(), dPayload);
        stopWatch.stop();
        long correctTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, correctTime);

        stopWatch.start();
        // Let w_{1,0}, w_{1,1} ∈ F_2 be the bit decomposition of w_1, i.e. w_1 = w_{1,0} + 2 * w_{1,1}.
        BitVector w10 = BitVectorFactory.createZeros(num);
        BitVector w11 = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            w10.set(i, (w1[i] & 0b00000001) != 0);
            w11.set(i, (w1[i] & 0b00000010) != 0);
        });
        // P0 sends (t_0, t_1) to P1.
        List<byte[]> t0t1Payload = receiveOtherPartyPayload(PtoStep.SENDER_SEND_T0_T1.ordinal());
        MpcAbortPreconditions.checkArgument(t0t1Payload.size() == 2);
        BitVector t0 = BitVectorFactory.create(num, t0t1Payload.get(0));
        BitVector t1 = BitVectorFactory.create(num, t0t1Payload.get(1));
        // P1 compute v_1 = (w_{1,0} · t_0) ⊕ (w_{1,1} · t_1) ⊕ m_c
        BitVector v1 = w10.and(t0);
        v1.xori(w11.and(t1));
        v1.xori(mc);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, shareTime);

        logPhaseInfo(PtoState.PTO_END);
        // P1 outputs v1
        return v1.getBytes();
    }
}
