package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.egk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.AbstractZlDaBitGenParty;
import edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl.SquareZlDaBitVector;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * EGK+20 semi-honest Zl (MAC) daBit generation receiver.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class Egk20MacZlDaBitGenReceiver extends AbstractZlDaBitGenParty {
    /**
     * Zl circuit receiver
     */
    private final ZlcParty zlcReceiver;
    /**
     * Z2 circuit receiver
     */
    private final Z2cParty z2cReceiver;

    public Egk20MacZlDaBitGenReceiver(Rpc receiverPpc, Party senderParty, Egk20MacZlDaBitGenConfig config) {
        super(Egk20MacZlDaBitGenPtoDesc.getInstance(), receiverPpc, senderParty, config);
        zlcReceiver = ZlcFactory.createReceiver(receiverPpc, senderParty, config.getZlcConfig());
        addSubPto(zlcReceiver);
        z2cReceiver = Z2cFactory.createReceiver(receiverPpc, senderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
    }

    public Egk20MacZlDaBitGenReceiver(Rpc receiverPpc, Party senderParty, Party aiderParty, Egk20MacZlDaBitGenConfig config) {
        super(Egk20MacZlDaBitGenPtoDesc.getInstance(), receiverPpc, senderParty, config);
        zlcReceiver = ZlcFactory.createReceiver(receiverPpc, senderParty, aiderParty, config.getZlcConfig());
        addSubPto(zlcReceiver);
        z2cReceiver = Z2cFactory.createReceiver(receiverPpc, senderParty, aiderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
    }

    @Override
    public void init(int maxNum) throws MpcAbortException {
        setInitInput(maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlcReceiver.init(maxNum);
        z2cReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlDaBitVector generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // The parties generate a random bit [b]_{2^k} in the arithmetic part of F_{ABB}.
        BigInteger[] zlArray = IntStream.range(0, num)
            .mapToObj(index -> {
                boolean b = secureRandom.nextBoolean();
                return b ? zl.createOne() : zl.createZero();
            })
            .toArray(BigInteger[]::new);
        ZlVector randomZlVector = ZlVector.create(zl, zlArray);
        BigInteger[] twoArray = IntStream.range(0, num)
            .mapToObj(index -> zl.module(BigIntegerUtils.BIGINT_2))
            .toArray(BigInteger[]::new);
        ZlVector twoZlVector = ZlVector.create(zl, twoArray);
        SquareZlVector squareZlVector0 = zlcReceiver.shareOther(num);
        SquareZlVector squareZlVector1 = zlcReceiver.shareOwn(randomZlVector);
        SquareZlVector squareAddAb = zlcReceiver.add(squareZlVector0, squareZlVector1);
        SquareZlVector squareMul2Ab = zlcReceiver.mul(squareZlVector0, squareZlVector1);
        squareMul2Ab = zlcReceiver.mul(squareMul2Ab, zlcReceiver.create(twoZlVector));
        SquareZlVector squareZlVector = zlcReceiver.sub(squareAddAb, squareMul2Ab);
        stopWatch.stop();
        long zlTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, zlTime);

        stopWatch.start();
        ZlVector zlVector = squareZlVector.getZlVector();
        // P1 computes [b_i mod 2]_2. In order to have the MAC, we must explicitly share the Z2 vector.
        BitVector randomZ2Vector = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            randomZ2Vector.set(index, zlVector.getElement(index).and(BigInteger.ONE).equals(BigInteger.ONE));
        }
        SquareZ2Vector squareZ2Vector0 = z2cReceiver.shareOther(num);
        SquareZ2Vector squareZ2Vector1 = z2cReceiver.shareOwn(randomZ2Vector);
        SquareZ2Vector squareZ2Vector = z2cReceiver.xor(squareZ2Vector0, squareZ2Vector1);
        SquareZlDaBitVector senderOutput = SquareZlDaBitVector.create(squareZlVector, squareZ2Vector);
        stopWatch.stop();
        long z2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, z2Time);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
