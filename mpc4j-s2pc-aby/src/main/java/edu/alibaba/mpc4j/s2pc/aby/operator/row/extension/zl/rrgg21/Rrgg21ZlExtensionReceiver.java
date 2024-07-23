package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.ZlB2aParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.AbstractZlExtensionParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.ZlWrapParty;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionPtoDesc.getInstance;

/**
 * RRGG21 Zl Value Extension Receiver.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlExtensionReceiver extends AbstractZlExtensionParty {
    /**
     * b2a party
     */
    private final ZlB2aParty b2aParty;
    /**
     * wrap party
     */
    private final ZlWrapParty wrapParty;

    public Rrgg21ZlExtensionReceiver(Z2cParty z2cReceiver, Party senderParty, Rrgg21ZlExtensionConfig config) {
        super(getInstance(), z2cReceiver.getRpc(), senderParty, config);
        b2aParty = ZlB2aFactory.createReceiver(z2cReceiver.getRpc(), senderParty, config.getB2aConfig());
        addSubPto(b2aParty);
        wrapParty = ZlWrapFactory.createReceiver(z2cReceiver, senderParty, config.getZlWrapConfig());
        addSubPto(wrapParty);
    }

    @Override
    public void init(int maxInputL, int maxOutputL, int maxNum) throws MpcAbortException {
        setInitInput(maxInputL, maxOutputL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        b2aParty.init(maxOutputL, maxNum);
        wrapParty.init(maxInputL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector zExtend(SquareZlVector xi, int outputL, boolean inputMsb) throws MpcAbortException {
        setPtoInput(xi, outputL);
        logPhaseInfo(PtoState.PTO_BEGIN);

        if (inputZl.getL() == outputL) {
            return xi.copy();
        }

        stopWatch.start();
        MpcZ2Vector z1 = wrapParty.wrap(inputZl.getL(), xs);
        stopWatch.stop();
        long wrapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, wrapTime);

        stopWatch.start();
        MpcZlVector as = b2aParty.b2a(z1, ZlFactory.createInstance(envType, outputL - inputZl.getL()));
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] y = intStream
            .mapToObj(i -> xi.getZlVector().getElement(i)
                .subtract(as.getZlVector().getElement(i).multiply(inputZl.getRangeBound()))
                .mod(outputZl.getRangeBound()))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        long b2aTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, b2aTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(outputZl, y, false);
    }
}
