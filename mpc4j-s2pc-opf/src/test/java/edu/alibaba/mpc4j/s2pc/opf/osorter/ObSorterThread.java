package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * @author Feng Han
 * @date 2024/9/30
 */
public class ObSorterThread extends Thread{

    /**
     * the sorter
     */
    private final ObSorter sorter;
    /**
     * input arrays
     */
    private final SquareZ2Vector[] inputVectors;
    /**
     * input arrays
     */
    private final SquareZ2Vector[] payload;
    /**
     * need permutation?
     */
    private final boolean needPermutation;
    /**
     * need stable?
     */
    private final boolean needStable;
    /**
     * z0
     */
    private SquareZ2Vector[] resultVectors;

    ObSorterThread(ObSorter sorter, SquareZ2Vector[] inputVectors, SquareZ2Vector[] payload, boolean needPermutation, boolean needStable) {
        this.sorter = sorter;
        this.inputVectors = inputVectors;
        this.payload = payload;
        this.needPermutation = needPermutation;
        this.needStable = needStable;
    }

    public SquareZ2Vector[] getRes() {
        return resultVectors;
    }

    @Override
    public void run() {
        try {
            sorter.init();
            sorter.getRpc().reset();
            if(payload != null) {
                resultVectors = sorter.unSignSort(inputVectors, payload, needPermutation, needStable);
            }else{
                resultVectors = sorter.unSignSort(inputVectors, needPermutation, needStable);
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
