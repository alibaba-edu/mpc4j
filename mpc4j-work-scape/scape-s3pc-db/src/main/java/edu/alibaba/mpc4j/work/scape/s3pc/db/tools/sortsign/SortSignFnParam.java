package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

/**
 * Invocation information of SortSign
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class SortSignFnParam {
    /**
     * whether the input is sorted by the join key
     */
    public boolean inputIsSorted;
    /**
     * length of key dimension
     */
    public int keyDim;
    /**
     * size of left table
     */
    public int leftTableLen;
    /**
     * size of right table
     */
    public int rightTableLen;

    /**
     * constructor
     *
     * @param keyDim        length of key dimension
     * @param leftTableLen  size of left table
     * @param rightTableLen size of right table
     */
    public SortSignFnParam(boolean inputIsSorted, int keyDim, int leftTableLen, int rightTableLen) {
        this.inputIsSorted = inputIsSorted;
        this.keyDim = keyDim;
        this.leftTableLen = leftTableLen;
        this.rightTableLen = rightTableLen;
    }

}
