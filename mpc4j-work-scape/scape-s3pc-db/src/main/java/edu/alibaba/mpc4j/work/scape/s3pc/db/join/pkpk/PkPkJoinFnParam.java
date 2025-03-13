package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

/**
 * input information of the PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class PkPkJoinFnParam {
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
     * payload dimension of the left table
     */
    public int leftValueDim;
    /**
     * payload dimension of the right table
     */
    public int rightValueDim;
    /**
     * is input sorted
     */
    public boolean isInputSorted;

    /**
     * Constructor
     *
     * @param leftDataNum   data size of the left table
     * @param rightDataNum  data size of the right table
     * @param keyDim        join key dimension
     * @param leftValueDim  payload dimension of the left table
     * @param rightValueDim payload dimension of the right table
     */
    public PkPkJoinFnParam(int leftDataNum, int rightDataNum, int keyDim,
                           int leftValueDim, int rightValueDim, boolean isInputSorted) {
        this.leftDataNum = leftDataNum;
        this.rightDataNum = rightDataNum;
        this.keyDim = keyDim;
        this.leftValueDim = leftValueDim;
        this.rightValueDim = rightValueDim;
        this.isInputSorted = isInputSorted;
    }
}
