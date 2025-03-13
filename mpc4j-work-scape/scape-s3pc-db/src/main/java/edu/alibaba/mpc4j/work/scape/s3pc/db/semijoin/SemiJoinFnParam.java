package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin;

/**
 * input information of the PkPk semi-join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class SemiJoinFnParam {
    /**
     * data size of the left table
     */
    public int leftDataNum;
    /**
     * data size of the right table
     */
    public int rightDataNum;
    /**
     * join key dimension
     */
    public int keyDim;
    /**
     * input is sorted
     */
    public boolean isInputSorted;

    /**
     * Constructor
     *
     * @param leftDataNum   data size of the left table
     * @param rightDataNum  data size of the right table
     * @param keyDim        join key dimension
     * @param isInputSorted input is sorted
     */
    public SemiJoinFnParam(int leftDataNum, int rightDataNum, int keyDim, boolean isInputSorted) {
        this.leftDataNum = leftDataNum;
        this.rightDataNum = rightDataNum;
        this.keyDim = keyDim;
        this.isInputSorted = isInputSorted;
    }
}
