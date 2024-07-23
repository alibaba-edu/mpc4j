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

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24.G24ZlExtensionPtoDesc.getInstance;

/**
 * G24 zl value signed extension sender.
 *
 * @author Li Peng
 * @date 2024/6/20
 */
public class G24ZlExtensionSender extends AbstractZlExtensionParty {
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
     * a0
     */
    private SquareZ2Vector a0;
    /**
     * d0
     */
    private SquareZ2Vector d0;

    public G24ZlExtensionSender(Z2cParty z2cSender, Party receiverParty, G24ZlExtensionConfig config) {
        super(getInstance(), z2cSender.getRpc(), receiverParty, config);
        b2aParty = ZlB2aFactory.createSender(z2cSender.getRpc(), receiverParty, config.getB2aConfig());
        addSubPto(b2aParty);
        this.z2cParty = z2cSender;
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
        assert xi.getZl().getL() > 2;
        logPhaseInfo(PtoState.PTO_BEGIN);

        if (inputZl.getL() == outputL) {
            return xi.copy();
        }

        stopWatch.start();
        step1();
        stopWatch.stop();
        long wrapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, wrapTime);

        stopWatch.start();
        SquareZlVector result = step2();
        stopWatch.stop();
        long b2aTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, b2aTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    private void step1() throws MpcAbortException {
        long threshold = 1L << inputZl.getL() - 2;
        // a0
        BitVector a0Vector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> a0Vector.set(i, input.getZlVector().getElement(i).longValue() < threshold));
        a0 = z2cParty.shareOwn(a0Vector);
        // a1
        SquareZ2Vector a1 = z2cParty.shareOther(num);
        // and
        a0 = z2cParty.and(a0, a1);

        // d0
        BitVector d0Vector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> d0Vector.set(i, input.getZlVector().getElement(i).longValue() > 3 * threshold));
        d0 = z2cParty.shareOwn(d0Vector);
        // d1
        SquareZ2Vector d1 = z2cParty.shareOther(num);
        // and
        d0 = z2cParty.and(d0, d1);
    }

    private SquareZlVector step2() throws MpcAbortException {
        SquareZlVector a0Arith = b2aParty.b2a(a0, outputZl);
        SquareZlVector d0Arith = b2aParty.b2a(d0, outputZl);
        // w
        ZlVector w = ZlVector.createOnes(outputZl, num).sub(a0Arith.getZlVector()).add(d0Arith.getZlVector());

        BigInteger[] newInputBigInt = input.getZlVector().getElements();
        BigInteger[] remaining = IntStream.range(0, num).mapToObj(i -> newInputBigInt[i].add(BigInteger.ONE.shiftLeft(outputZl.getL())
            .subtract(w.getElement(i).multiply(BigInteger.ONE.shiftLeft(inputZl.getL())))).mod(BigInteger.ONE.shiftLeft(outputZl.getL()))).toArray(BigInteger[]::new);
        return SquareZlVector.create(outputZl, remaining, false);
    }
}