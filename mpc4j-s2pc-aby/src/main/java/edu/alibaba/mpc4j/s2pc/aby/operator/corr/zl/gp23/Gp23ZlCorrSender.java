package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.AbstractZlCorrParty;
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

import static edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrPtoDesc.*;

/**
 * GP23 Zl Corr Sender.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Gp23ZlCorrSender extends AbstractZlCorrParty {
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

    public Gp23ZlCorrSender(Rpc senderRpc, Party receiverParty, Gp23ZlCorrConfig config) {
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
        z2cSender.init(maxNum);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, 2 * maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector corr(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // set y_b = x_b + N/2 mod N
        n = zl.getRangeBound();
        BigInteger[] nPrime = IntStream.range(0, num)
            .mapToObj(i -> n.shiftRight(1))
            .toArray(BigInteger[]::new);
        ZlVector yi = xi.getZlVector().add(ZlVector.create(zl, nPrime));
        BitVector[] i0 = getIi(yi);
        MpcZ2Vector ai = generateK0Share(i0);
        MpcZ2Vector bi = generateK1Share(i0);
        stopWatch.stop();
        long genKiShareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genKiShareTime);

        stopWatch.start();
        int[] rs0 = booleanShareToArithShare(ai);
        int[] rs1 = booleanShareToArithShare(bi);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] corri = intStream
            .mapToObj(index -> {
                int bitValue = ai.getBitVector().get(index) ? 1 : 0;
                BigInteger t1 = BigInteger.valueOf(bitValue + 2L * rs0[index]).mod(n);
                bitValue = bi.getBitVector().get(index) ? 1 : 0;
                BigInteger t2 = BigInteger.valueOf(bitValue + 2L * rs1[index]).mod(n);
                return zl.sub(t2, t1);
            }).toArray(BigInteger[]::new);
        stopWatch.stop();
        long shareConvertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, shareConvertTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(zl, corri, false);
    }

    private BitVector[] getIi(ZlVector xi) {
        BigInteger lowerBound = n.divide(BigInteger.valueOf(3));
        BigInteger upperBound = n.shiftLeft(1).divide(BigInteger.valueOf(3)).add(BigInteger.ONE);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        int[][] i0 = intStream.mapToObj(index -> {
            BigInteger x = xi.getElement(index);
            if (x.compareTo(lowerBound) <= 0) {
                return new int[]{0, 0};
            } else if (x.compareTo(upperBound) > 0) {
                return new int[]{1, 0};
            } else {
                return new int[]{0, 1};
            }
        }).toArray(int[][]::new);
        BitVector a = BitVectorFactory.createZeros(num);
        BitVector b = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            if (i0[index][0] == 1) {
                a.set(index, true);
            }
            if (i0[index][1] == 1) {
                b.set(index, true);
            }
        }
        return new BitVector[]{a, b};
    }

    private MpcZ2Vector generateK0Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].and(i0[1].not()), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        return z2cSender.and(z0, z1);
    }

    private MpcZ2Vector generateK1Share(BitVector[] i0) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(i0[0].not().and(i0[1].not()), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z2 = SquareZ2Vector.create(i0[0].not().and(i0[1]), false);
        MpcZ2Vector z3 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z4 = SquareZ2Vector.create(i0[0].and(i0[1].not()), false);
        MpcZ2Vector z5 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector[] z = z2cSender.and(new MpcZ2Vector[]{z0, z2, z4}, new MpcZ2Vector[]{z1, z3, z5});
        return z2cSender.xor(z2cSender.xor(z[0], z[1]), z[2]);
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
