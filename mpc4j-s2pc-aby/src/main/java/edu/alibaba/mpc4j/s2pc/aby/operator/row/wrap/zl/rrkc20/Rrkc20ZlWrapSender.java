package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.AbstractZlWrapParty;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * RRKC20 Zl wrap protocol sender.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrkc20ZlWrapSender extends AbstractZlWrapParty {
    /**
     * millionaire party
     */
    private final MillionaireParty millionaireParty;

    public Rrkc20ZlWrapSender(Z2cParty z2cSender, Party receiverParty, Rrkc20ZlWrapConfig config) {
        super(Rrkc20ZlWrapPtoDesc.getInstance(), z2cSender.getRpc(), receiverParty, config);
        millionaireParty = MillionaireFactory.createSender(z2cSender, receiverParty, config.getMillionaireConfig());
        addSubPto(millionaireParty);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        millionaireParty.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector wrap(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] millionaireInputs = prepareInput();
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, prepareTime);

        stopWatch.start();
        SquareZ2Vector z0 = millionaireParty.lt(l, millionaireInputs);
        stopWatch.stop();
        long compareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, compareTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private byte[][] prepareInput() {
        BigInteger z = BigInteger.ONE.shiftLeft(l);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(i -> {
            BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(inputs[i]);
            return BigIntegerUtils.nonNegBigIntegerToByteArray(z.subtract(input).subtract(BigInteger.ONE), byteL);
        }).toArray(byte[][]::new);
    }
}
