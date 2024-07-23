package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.ZlCrossTermParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.AbstractZlMatCrossTermSender;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.crossTerm.zl.rrgg21.Rrgg21ZlCrossTermPtoDesc.getInstance;

/**
 * RRGG21 Zl Matrix Cross Term Multiplication Sender.
 *
 * @author Liqiang Peng
 * @date 2024/6/12
 */
public class Rrgg21ZlMatCrossTermSender extends AbstractZlMatCrossTermSender {
    /**
     * cross term sender
     */
    private final ZlCrossTermParty crossTermSender;

    public Rrgg21ZlMatCrossTermSender(Z2cParty z2cSender, Party receiverParty, Rrgg21ZlMatCrossTermConfig config) {
        super(getInstance(), z2cSender.getRpc(), receiverParty, config);
        crossTermSender = ZlCrossTermFactory.createSender(z2cSender, receiverParty, config.getCrossTermConfig());
        addSubPto(crossTermSender);
    }

    @Override
    public void init(int maxM, int maxN, int maxD1, int maxD2, int maxD3) throws MpcAbortException {
        setInitInput(maxM, maxN, maxD1, maxD2, maxD3);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        crossTermSender.init(maxM, maxN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector matCrossTerm(SquareZlVector x, int d1, int d2, int d3, int m, int n) throws MpcAbortException {
        setPtoInput(x, d1, d2, d3, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int num = d1 * d3;
        BigInteger[] result = IntStream.range(0, num).mapToObj(i -> BigInteger.ZERO).toArray(BigInteger[]::new);
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d3; j++) {
                for (int l = 0; l < d2; l++) {
                    BigInteger t = crossTermSender.crossTerm(x.getZlVector().getElement(i * d2 + l), m, n);
                    result[i * d3  + j] = outputZl.add(t, result[i * d3  + j]);
                }
            }
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(outputZl, result, false);
    }
}