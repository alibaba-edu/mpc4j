package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.AbstractZlCorrParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrPtoDesc.*;

/**
 * GP23 Zl Corr Receiver.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Gp23ZlCorrReceiver extends AbstractZlCorrParty {
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * 1-out-of-n (with n = 2^l) ot receiver.
     */
    private final CotReceiver cotReceiver;
    /**
     * zl range bound
     */
    private BigInteger n;

    public Gp23ZlCorrReceiver(Rpc receiverRpc, Party senderParty, Gp23ZlCorrConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cReceiver.init(maxNum);
        cotReceiver.init(2 * maxL * maxNum);
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
        BitVector[] i1 = getIi(yi);
        MpcZ2Vector ai = generateK0Share(i1);
        MpcZ2Vector bi = generateK1Share(i1);
        bi.getBitVector().xori(BitVectorFactory.createOnes(num));
        stopWatch.stop();
        long genKiShareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genKiShareTime);

        stopWatch.start();
        int[] ts0 = booleanShareToArithShare(ai);
        int[] ts1 = booleanShareToArithShare(bi);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] corri = intStream
            .mapToObj(index -> {
                int bitValue = ai.getBitVector().get(index) ? 1 : 0;
                BigInteger t1 = BigInteger.valueOf(bitValue - 2L * ts0[index]).mod(n);
                bitValue = bi.getBitVector().get(index) ? 1 : 0;
                BigInteger t2 = BigInteger.valueOf(bitValue - 2L * ts1[index]).mod(n);
                return zl.sub(t2, t1);
            }).toArray(BigInteger[]::new);
        stopWatch.stop();
        long shareConvertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, shareConvertTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(zl, corri, false);
    }

    private MpcZ2Vector generateK0Share(BitVector[] i1) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(i1[0].and(i1[1].not()), false);
        return z2cReceiver.and(z0, z1);
    }

    private MpcZ2Vector generateK1Share(BitVector[] i1) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z1 = SquareZ2Vector.create((i1[0].not().and(i1[1])).xor(i1[0].and(i1[1].not())), false);
        MpcZ2Vector z2 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z3 = SquareZ2Vector.create((i1[0].not().and(i1[1].not())).xor(i1[0].not().and(i1[1])).xor(i1[0].and(i1[1].not())), false);
        MpcZ2Vector z4 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z5 = SquareZ2Vector.create((i1[0].not().and(i1[1].not())).xor(i1[0].not().and(i1[1])).xor(i1[0].and(i1[1].not())), false);
        MpcZ2Vector[] z = z2cReceiver.and(new MpcZ2Vector[]{z0, z2, z4}, new MpcZ2Vector[]{z1, z3, z5});
        return z2cReceiver.xor(z2cReceiver.xor(z[0], z[1]), z[2]);
    }

    private BitVector[] getIi(ZlVector yi) {
        BigInteger lowerBound = n.divide(BigInteger.valueOf(3));
        BigInteger upperBound = n.shiftLeft(1).divide(BigInteger.valueOf(3)).add(BigInteger.ONE);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        int[][] i1 = intStream.mapToObj(index -> {
            BigInteger x = yi.getElement(index);
            if (x.compareTo(lowerBound) <= 0) {
                return new int[]{0, 0};
            } else if (x.compareTo(upperBound) > 0) {
                return new int[]{1, 0};
            } else {
                return new int[]{0, 1};
            }
        }).toArray(int[][]::new);
        BitVector c = BitVectorFactory.createZeros(num);
        BitVector d = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            if (i1[index][0] == 1) {
                c.set(index, true);
            }
            if (i1[index][1] == 1) {
                d.set(index, true);
            }
        }
        return new BitVector[]{c, d};
    }

    private int[] booleanShareToArithShare(MpcZ2Vector k) throws MpcAbortException {
        // P_1 and P_2 engage in a OT_1^2, where P_2's selection bit is y_i.
        boolean[] ys = BinaryUtils.byteArrayToBinary(k.getBitVector().getBytes(), num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(ys);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_S.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderMessagePayload = rpc.receive(senderMessageHeader).getPayload();
        MpcAbortPreconditions.checkArgument(senderMessagePayload.size() == num * 2);
        byte[][] senderMessageFlattenArray = senderMessagePayload.toArray(new byte[0][]);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(num);
        return IntStream.range(0, num)
            .map(index -> {
                byte[] keyi = Arrays.copyOf(rotReceiverOutput.getRb(index), messageByteLength);
                byte[] choiceCiphertext = ys[index] ?
                    senderMessageFlattenArray[index * 2 + 1] : senderMessageFlattenArray[index * 2];
                BytesUtils.xori(choiceCiphertext, keyi);
                return IntUtils.fixedByteArrayToNonNegInt(choiceCiphertext);
            })
            .toArray();
    }
}
