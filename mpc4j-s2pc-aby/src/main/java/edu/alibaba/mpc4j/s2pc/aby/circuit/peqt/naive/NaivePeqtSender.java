package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.AbstractPeqtParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * naive private equality test sender.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtSender extends AbstractPeqtParty {
    /**
     * Boolean circuit sender
     */
    private final BcParty bcSender;
    /**
     * Z2 integer circuit
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public NaivePeqtSender(Rpc senderRpc, Party receiverParty, NaivePeqtConfig config) {
        super(NaivePeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcSender = BcFactory.createSender(senderRpc, receiverParty, config.getBcConfig());
        addSubPtos(bcSender);
        z2IntegerCircuit = new Z2IntegerCircuit(bcSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bcSender.init(maxNum * maxL, maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector peqt(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // transpose xs into bit vectors.
        ZlDatabase zlDatabase = ZlDatabase.create(l, xs);
        BitVector[] x = zlDatabase.bitPartition(envType, parallel);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 sends and gets the share
        SquareZ2Vector[] x0 = bcSender.shareOwn(x);
        int[] nums = new int[l];
        Arrays.fill(nums, num);
        SquareZ2Vector[] y0 = bcSender.shareOther(nums);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareTime);

        stopWatch.start();
        SquareZ2Vector eq0 = (SquareZ2Vector) z2IntegerCircuit.eq(x0, y0);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return eq0;
    }
}
