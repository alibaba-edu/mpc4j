package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.AbstractPdsmReceiver;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 OPPRF-based PDSM receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22OpprfPdsmReceiver extends AbstractPdsmReceiver {
    /**
     * batched OPPRF receiver
     */
    private final BopprfReceiver bopprfReceiver;
    /**
     * PEQT receiver
     */
    private final PeqtParty peqtReceiver;

    public Cgs22OpprfPdsmReceiver(Rpc receiverRpc, Party senderParty, Cgs22OpprfPdsmConfig config) {
        super(Cgs22OpprfPdsmPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bopprfReceiver = BopprfFactory.createReceiver(receiverRpc, senderParty, config.getBopprfConfig());
        addSubPto(bopprfReceiver);
        peqtReceiver = PeqtFactory.createReceiver(receiverRpc, senderParty, config.getPeqtConfig());
        addSubPto(peqtReceiver);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bopprfReceiver.init(maxNum, maxNum * maxD);
        peqtReceiver.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pdsm(int l, int d, byte[][] inputArray) throws MpcAbortException {
        setPtoInput(l, d, inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P0 and P1 invoke F_{OPPRF} in which P1 plays the role of receiver with a as the input query.
        byte[][] targetArray = bopprfReceiver.opprf(l, inputArray, num * d);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, opprfTime);

        stopWatch.start();
        // P0 and P1 call F_{eq} with inputs t and w and receive bits y0 and y1 respectively
        SquareZ2Vector z1 = peqtReceiver.peqt(l, targetArray);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }
}
