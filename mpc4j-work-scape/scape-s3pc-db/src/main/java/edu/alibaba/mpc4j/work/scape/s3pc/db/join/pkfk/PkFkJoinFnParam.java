package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

/**
 * input information of the pkfk join protocol
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class PkFkJoinFnParam {
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    public boolean inputIsSorted;
    /**
     * data size of the left table
     */
    public int uTabNum;
    /**
     * data size of the right table
     */
    public int nuTabNum;
    /**
     * join key dimension
     */
    public int keyDim;
    /**
     * payload dimension of the left table
     */
    public int uTabValueDim;
    /**
     * payload dimension of the right table
     */
    public int nuTabValueDim;

    /**
     * Constructor
     *
     * @param uTabNum   data size of the left table
     * @param nuTabNum  data size of the right table
     * @param keyDim        join key dimension
     * @param uTabValueDim  payload dimension of the left table
     * @param nuTabValueDim payload dimension of the right table
     */
    public PkFkJoinFnParam( boolean inputIsSorted, int uTabNum, int nuTabNum, int keyDim,
                           int uTabValueDim, int nuTabValueDim) {
        this.inputIsSorted = inputIsSorted;
        this.uTabNum = uTabNum;
        this.nuTabNum = nuTabNum;
        this.keyDim = keyDim;
        this.uTabValueDim = uTabValueDim;
        this.nuTabValueDim = nuTabValueDim;
    }
}
