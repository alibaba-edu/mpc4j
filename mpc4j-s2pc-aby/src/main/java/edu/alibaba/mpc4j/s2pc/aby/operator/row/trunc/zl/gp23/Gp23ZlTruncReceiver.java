package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23;

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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.AbstractZlTruncParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncPtoDesc.*;

/**
 * GP23 Zl Truncation Receiver.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Gp23ZlTruncReceiver extends AbstractZlTruncParty {
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;
    /**
     * 1-out-of-n (with n = 2^l) ot receiver.
     */
    private final CotReceiver cotReceiver;


    public Gp23ZlTruncReceiver(Rpc receiverRpc, Party senderParty, Gp23ZlTruncConfig config) {
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
        z2cReceiver.init(4 * maxNum);
        cotReceiver.init(2 * maxL * maxNum);
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
        BitVector[] i1 = getIi(xi);
        MpcZ2Vector k0 = generateK0Share(i1);
        MpcZ2Vector k1 = generateK1Share(i1);
        stopWatch.stop();
        long kiShareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, kiShareTime);

        stopWatch.start();
        int[] ts0 = booleanShareToArithShare(k0);
        int[] ts1 = booleanShareToArithShare(k1);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] k = intStream.mapToObj(index -> {
            int t0 = (k0.getBitVector().get(index) ? 1 : 0) - ts0[index] * 2;
            int t1 = (k1.getBitVector().get(index) ? 1 : 0) - ts1[index] * 2;
            return BigInteger.valueOf(-t0 + t1).mod(n);
        }).toArray(BigInteger[]::new);
        ZlVector ki = ZlVector.create(zl, k);
        ki.setParallel(parallel);
        stopWatch.stop();
        long shareConTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareConTime);

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

    private MpcZ2Vector generateK0Share(BitVector[] i1) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(i1[0].and(i1[1].not()).and(i1[2].not()), false);
        return z2cReceiver.and(z0, z1);
    }

    private MpcZ2Vector generateK1Share(BitVector[] i1) throws MpcAbortException {
        MpcZ2Vector z0 = SquareZ2Vector.create(BitVectorFactory.createZeros(num), false);
        MpcZ2Vector z1 = SquareZ2Vector.create(i1[0].not().and(i1[1].not()).and(i1[2]), false);
        return z2cReceiver.and(z0, z1);
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
