package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.AbstractPeqtParty;

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
     * Z2 circuit receiver
     */
    private final Z2cParty z2cReceiver;
    /**
     * Z2 integer circuit
     */
    private final Z2IntegerCircuit z2IntegerCircuit;

    public NaivePeqtReceiver(Rpc receiverRpc, Party senderParty, NaivePeqtConfig config) {
        super(NaivePeqtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
        z2IntegerCircuit = new Z2IntegerCircuit(z2cReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2cReceiver.init(maxNum * maxL);
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
        SquareZ2Vector[] x1 = z2cReceiver.shareOther(nums);
        SquareZ2Vector[] y1 = z2cReceiver.shareOwn(y);
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
