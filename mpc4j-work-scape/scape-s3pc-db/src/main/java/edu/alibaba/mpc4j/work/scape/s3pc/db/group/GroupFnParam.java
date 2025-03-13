package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

/**
 * input information of group function
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupFnParam {
    public enum GroupOp{
        /**
         * group extreme
         */
        EXTREME,
        /**
         * group sum
         */
        SUM,
        /**
         * get arithmetic group flag
         */
        A_GROUP_FLAG,
        /**
         * get binary group flag
         */
        B_GROUP_FLAG
    }

    /**
     * group operation
     */
    public GroupOp groupOp;
    /**
     * input dimension
     */
    public int inputDim;
    /**
     * input size
     */
    public int inputSize;

    /**
     * constructor
     *
     * @param inputDim input dimension
     * @param inputSize input size
     */
    public GroupFnParam(GroupOp groupOp, int inputDim, int inputSize) {
        this.groupOp = groupOp;
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
