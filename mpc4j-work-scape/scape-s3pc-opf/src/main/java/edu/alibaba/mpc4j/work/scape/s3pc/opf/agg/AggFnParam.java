package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

/**
 * input formation for aggregate functions
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class AggFnParam {
    /**
     * aggregation type
     */
    public enum AggOp {
        /**
         * max
         */
        MAX,
        /**
         * min
         */
        MIN,
        /**
         * sum
         */
        SUM
    }

    /**
     * is binary input
     */
    public boolean isBinaryInput;
    /**
     * group operation
     */
    public AggOp aggOp;
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
     * @param isBinaryInput is binary input
     * @param aggOp   aggregation type
     * @param inputDim  input dimension
     * @param inputSize input size
     */
    public AggFnParam(boolean isBinaryInput, AggOp aggOp, int inputDim, int inputSize) {
        this.isBinaryInput = isBinaryInput;
        this.aggOp = aggOp;
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
