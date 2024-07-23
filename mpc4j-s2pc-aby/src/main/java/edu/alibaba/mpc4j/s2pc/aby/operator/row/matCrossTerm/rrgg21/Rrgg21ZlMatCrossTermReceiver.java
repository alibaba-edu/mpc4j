package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.AbstractZlMatCrossTermReceiver;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;

/**
 * RRGG21 Zl Matrix Cross Term Multiplication Receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
public class Rrgg21ZlMatCrossTermReceiver extends AbstractZlMatCrossTermReceiver {
    /**
     * cross term receiver
     */
    private final ZlCrossTermParty crossTermReceiver;

    public Rrgg21ZlMatCrossTermReceiver(Z2cParty z2cReceiver, Party senderParty, Rrgg21ZlMatCrossTermConfig config) {
        super(getInstance(), z2cReceiver.getRpc(), senderParty, config);
        crossTermReceiver = ZlCrossTermFactory.createReceiver(z2cReceiver, senderParty, config.getCrossTermConfig());
        addSubPto(crossTermReceiver);
    }

    @Override
    public void init(int maxM, int maxN, int maxD1, int maxD2, int maxD3) throws MpcAbortException {
        setInitInput(maxM, maxN, maxD1, maxD2, maxD3);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        crossTermReceiver.init(maxM, maxN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector matCrossTerm(SquareZlVector y, int d1, int d2, int d3, int m, int n) throws MpcAbortException {
        setPtoInput(y, d1, d2, d3, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int num = d1 * d3;
        BigInteger[] result = IntStream.range(0, num).mapToObj(i -> BigInteger.ZERO).toArray(BigInteger[]::new);
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d3; j++) {
                for (int l = 0; l < d2; l++) {
                    BigInteger t = crossTermReceiver.crossTerm(y.getZlVector().getElement(l * d3 + j), m, n);
                    result[i * d3  + j] = outputZl.add(t, result[i * d3  + j]);
                }
            }
        }
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(outputZl, result, false);
    }
}
