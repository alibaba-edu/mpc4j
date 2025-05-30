package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.AbstractZlTruncParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncPtoDesc.*;

/**
 * GP23 Zl Truncation Sender.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Gp23ZlTruncSender extends AbstractZlTruncParty {
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * cot sender
     */
    private final CotSender cotSender;
    /**
     * zl range bound
     */
    private BigInteger n;

    public Gp23ZlTruncSender(Rpc senderRpc, Party receiverParty, Gp23ZlTruncConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cSender.init(4 * maxNum);
        byte[] delta = BlockUtils.randomBlock(secureRandom);
        cotSender.init(delta, 2 * maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector trunc(SquareZlVector xi, int s) throws MpcAbortException {
        setPtoInput(xi, s);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        n = zl.getRangeBound();
        BitVector[] i0 = getIi(xi);
        MpcZ2Vector k0 = generateK0Share(i0);
        MpcZ2Vector k1 = generateK1Share(i0);
        stopWatch.stop();
        long kiShareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, kiShareTime);

        stopWatch.start();
        int[] rs0 = booleanShareToArithShare(k0);
        int[] rs1 = booleanShareToArithShare(k1);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] k = intStream.mapToObj(index -> {
            int t0 = (k0.getBitVector().get(index) ? 1 : 0) + rs0[index] * 2;
            int t1 = (k1.getBitVector().get(index) ? 1 : 0) + rs1[index] * 2;
            return BigInteger.valueOf(1 - t0 + t1).mod(n);
        }).toArray(BigInteger[]::new);
        ZlVector ki = ZlVector.create(zl, k);
        ki.setParallel(parallel);
        stopWatch.stop();
        long shareConvertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareConvertTime);

        stopWatch.start();
        ZlVector shift = ZlVector.create(
            zl, IntStream.range(0, num).mapToObj(i -> BigInteger.ONE.shiftLeft(l - s)).toArray(BigInteger[]::new)
        );
        ZlVector r = iDiv(xi.getZlVector().getElements(), s);
        r.setParallel(parallel);
        r.subi(ki.mul(shift));
        SquareZlVector result = SquareZlVector.create(r, false);
        stopWatch.stop();
        long genOutputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, genOutputTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private MpcZ2Vector generateK0Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].and(i0[1].not()).and(i0[2].not()), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        return z2cSender.and(z0, z1);
    }

    private MpcZ2Vector generateK1Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].not().and(i0[1].not()).and(i0[2]), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        return z2cSender.and(z0, z1);
    }

    private int[] booleanShareToArithShare(MpcZ2Vector k) throws MpcAbortException {
        // P_1 and P_2 engage in a OT_1^2, where P_1 acts as the sender, P_1's input is (r_i, r_i + x_i).
        CotSenderOutput cotSenderOutput = cotSender.send(num);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(num);
        int offset = CommonUtils.getByteLength(num) * Byte.SIZE - num;
        // P_1 generates n random values r_1, ... r_n \in Z_{n + 1} and computes r = Σ_{i = 1}^n t_i
        int[] rs = new int[num];
        IntStream.range(0, num).forEach(index -> rs[index] = secureRandom.nextInt(2));
        List<byte[]> senderMessagePayload = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] key0 = Arrays.copyOf(rotSenderOutput.getR0(index), messageByteLength);
                byte[] key1 = Arrays.copyOf(rotSenderOutput.getR1(index), messageByteLength);
                int rxi = BinaryUtils.getBoolean(k.getBitVector().getBytes(), index + offset) ? 1 : 0;
                int negRxi = (rxi + rs[index]);
                byte[][] ciphertexts = new byte[2][];
                ciphertexts[0] = IntUtils.nonNegIntToFixedByteArray(rs[index], messageByteLength);
                ciphertexts[1] = IntUtils.nonNegIntToFixedByteArray(negRxi, messageByteLength);
                BytesUtils.xori(ciphertexts[0], key0);
                BytesUtils.xori(ciphertexts[1], key1);
                return ciphertexts;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_S.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderMessageHeader, senderMessagePayload));
        return rs;
    }
}
