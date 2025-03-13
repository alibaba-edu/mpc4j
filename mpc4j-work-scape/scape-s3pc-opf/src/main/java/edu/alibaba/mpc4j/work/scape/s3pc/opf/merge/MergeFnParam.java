package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

/**
 * input information of merge operation
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class MergeFnParam {
    /**
     * size of left table
     */
    public int leftDataNum;
    /**
     * size of right table
     */
    public int rightDataNum;
    /**
     * input dimension
     */
    public int dim;

    /**
     * constructor
     *
     * @param leftDataNum  size of left table
     * @param rightDataNum size of right table
     * @param dim          input dimension
     */
    public MergeFnParam(int leftDataNum, int rightDataNum, int dim) {
        this.leftDataNum = leftDataNum;
        this.rightDataNum = rightDataNum;
        this.dim = dim;
    }
}
