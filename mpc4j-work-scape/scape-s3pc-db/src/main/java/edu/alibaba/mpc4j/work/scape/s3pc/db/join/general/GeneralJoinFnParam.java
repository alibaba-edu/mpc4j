package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

/**
 * input information of the general join protocol
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class GeneralJoinFnParam {
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    public boolean inputIsSorted;
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
     * the upper bound of the join result size
     */
    public int resultUpperBound;

    /**
     * Constructor
     *
     * @param leftDataNum      data size of the left table
     * @param rightDataNum     data size of the right table
     * @param keyDim           join key dimension
     * @param leftValueDim     payload dimension of the left table
     * @param rightValueDim    payload dimension of the right table
     * @param resultUpperBound the upper bound of the join result size
     */
    public GeneralJoinFnParam(boolean inputIsSorted, int leftDataNum, int rightDataNum, int keyDim,
                              int leftValueDim, int rightValueDim, int resultUpperBound) {
        this.inputIsSorted = inputIsSorted;
        this.leftDataNum = leftDataNum;
        this.rightDataNum = rightDataNum;
        this.keyDim = keyDim;
        this.leftValueDim = leftValueDim;
        this.rightValueDim = rightValueDim;
        this.resultUpperBound = resultUpperBound;
    }
}
