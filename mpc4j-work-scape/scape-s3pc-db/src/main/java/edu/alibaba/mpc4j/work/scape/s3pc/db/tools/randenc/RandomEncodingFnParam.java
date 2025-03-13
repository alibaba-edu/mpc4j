package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

/**
 * input information for random encoding
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class RandomEncodingFnParam {
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
     * require validFlag
     */
    public boolean withDummy;

    /**
     * constructor
     *
     * @param keyDim        length of key dimension
     * @param leftTableLen  size of left table
     * @param rightTableLen size of right table
     * @param withDummy     require validFlag
     */
    public RandomEncodingFnParam(int keyDim, int leftTableLen, int rightTableLen, boolean withDummy) {
        this.keyDim = keyDim;
        this.leftTableLen = leftTableLen;
        this.rightTableLen = rightTableLen;
        this.withDummy = withDummy;
    }
}
