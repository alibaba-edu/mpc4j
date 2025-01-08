package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * thread for shuffle sender
 *
 * @author Feng Han
 * @date 2024/9/27
 */
public class ShuffleSenderThread extends Thread {
    /**
     * the sender
     */
    private final ShuffleParty sender;
    /**
     * dataNum
     */
    private final int dataNum;
    /**
     * dimNum
     */
    private final int dimNum;
    /**
     * input arrays
     */
    private final MpcZ2Vector[] inputVectors;
    /**
     * z0
     */
    private SquareZ2Vector[] resultVectors;

    ShuffleSenderThread(ShuffleParty sender, int dataNum, int dimNum, MpcZ2Vector[] inputVectors) {
        this.sender = sender;
        this.dataNum = dataNum;
        this.dimNum = dimNum;
        this.inputVectors = inputVectors;
    }

    public SquareZ2Vector[] getRes() {
        return resultVectors;
    }

    @Override
    public void run() {
        try {
            sender.init();
            sender.getRpc().reset();
            resultVectors = sender.shuffle(inputVectors, dataNum, dimNum);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
