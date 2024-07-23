package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.AbstractZlExtensionParty;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionPtoDesc.getInstance;

/**
 * G24 zl value signed extension receiver.
 *
 * @author Li Peng
 * @date 2024/6/20
 */
public class G24ZlExtensionReceiver extends AbstractZlExtensionParty {
    /**
     * b2a party
     */
    private final ZlB2aParty b2aParty;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cParty;
    /**
     * input values.
     */
    private SquareZlVector input;
    /**
     * a1
     */
    private SquareZ2Vector a1;
    /**
     * d1
     */
    private SquareZ2Vector d1;

    public G24ZlExtensionReceiver(Z2cParty z2cReceiver, Party senderParty, G24ZlExtensionConfig config) {
        super(getInstance(), z2cReceiver.getRpc(), senderParty, config);
        b2aParty = ZlB2aFactory.createReceiver(z2cReceiver.getRpc(), senderParty, config.getB2aConfig());
        addSubPto(b2aParty);
        this.z2cParty = z2cReceiver;
    }

    @Override
    public void init(int maxInputL, int maxOutputL, int maxNum) throws MpcAbortException {
        setInitInput(maxInputL, maxOutputL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        b2aParty.init(maxOutputL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector zExtend(SquareZlVector xi, int outputL, boolean inputMsb) throws MpcAbortException {
        setPtoInput(xi, outputL);
        input = xi;
        logPhaseInfo(PtoState.PTO_BEGIN);

        if (inputZl.getL() == outputL) {
            return xi.copy();
        }
        stopWatch.start();
        step1();
        stopWatch.stop();
        long step1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, step1Time);

        stopWatch.start();
        SquareZlVector result = step2();
        stopWatch.stop();
        long step2Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, step2Time);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private void step1() throws MpcAbortException {
        long threshold = 1L << inputZl.getL() - 2;
        // a0
        SquareZ2Vector a0 = z2cParty.shareOther(num);
        // a1
        BitVector a1Vector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> a1Vector.set(i, input.getZlVector().getElement(i).longValue() < threshold));
        a1 = z2cParty.shareOwn(a1Vector);
        // and
        a1 = z2cParty.and(a0, a1);

        // d0
        SquareZ2Vector d0 = z2cParty.shareOther(num);
        // d1
        BitVector d0Vector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> d0Vector.set(i, input.getZlVector().getElement(i).longValue() > 3 * threshold));
        d1 = z2cParty.shareOwn(d0Vector);
        // and
        this.d1 = z2cParty.and(d0, d1);
    }

    private SquareZlVector step2() throws MpcAbortException {
        SquareZlVector a1Arith = b2aParty.b2a(a1, outputZl);
        SquareZlVector d1Arith = b2aParty.b2a(d1, outputZl);
        // w
        ZlVector w = ZlVector.createZeros(outputZl, num).sub(a1Arith.getZlVector()).add(d1Arith.getZlVector());

        BigInteger[] newInputBigInt = input.getZlVector().getElements();
        BigInteger[] remaining = IntStream.range(0, num).mapToObj(i -> newInputBigInt[i]
            .subtract(w.getElement(i).multiply(BigInteger.ONE.shiftLeft(inputZl.getL())))
            .mod(BigInteger.ONE.shiftLeft(outputZl.getL()))).toArray(BigInteger[]::new);
        return SquareZlVector.create(outputZl, remaining, false);
    }
}
