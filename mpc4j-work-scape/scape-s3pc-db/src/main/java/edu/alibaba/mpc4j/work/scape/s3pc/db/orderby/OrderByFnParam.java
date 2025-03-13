package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

/**
 * input description for order-by operation
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class OrderByFnParam {
    /**
     * whether the input is in the binary form
     */
    public boolean inputInBinaryForm;
    /**
     * data size of the left table
     */
    public int inputSize;
    /**
     * join key dimension
     */
    public int keyDim;
    /**
     * total dimension
     */
    public int totalDim;

    /**
     * Constructor
     *
     * @param inputSize data size of the left table
     * @param keyDim    join key dimension
     */
    public OrderByFnParam(boolean inputInBinaryForm, int inputSize, int keyDim, int totalDim) {
        this.inputInBinaryForm = inputInBinaryForm;
        this.inputSize = inputSize;
        this.keyDim = keyDim;
        this.totalDim = totalDim;
    }
}
