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
 * naive private equality test receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtReceiver extends AbstractPeqtParty {
    /**
     * Boolean circuit receiver
     */
    private final BcParty bcReceiver;
    /**
     * Z2 integer circuit
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public NaivePeqtReceiver(Rpc receiverRpc, Party senderParty, NaivePeqtConfig config) {
        super(NaivePeqtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bcReceiver = BcFactory.createReceiver(receiverRpc, senderParty, config.getBcConfig());
        addSubPtos(bcReceiver);
        z2IntegerCircuit = new Z2IntegerCircuit(bcReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bcReceiver.init(maxNum * maxL, maxNum * maxL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector peqt(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // transpose ys into bit vectors.
        ZlDatabase zlDatabase = ZlDatabase.create(l, ys);
        BitVector[] y = zlDatabase.bitPartition(envType, parallel);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P1 gets and sends the share
        int[] nums = new int[l];
        Arrays.fill(nums, num);
        SquareZ2Vector[] x1 = bcReceiver.shareOther(nums);
        SquareZ2Vector[] y1 = bcReceiver.shareOwn(y);
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, shareTime);

        stopWatch.start();
        SquareZ2Vector eq1 = (SquareZ2Vector) z2IntegerCircuit.eq(x1, y1);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return eq1;
    }
}
