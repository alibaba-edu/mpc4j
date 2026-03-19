package edu.alibaba.mpc4j.work.db.sketch.utils.agg;

/**
 * input formation for aggregate functions
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
     * information of extreme
     */
    public enum ExtremeInfo {
        /**
         * index
         */
        INDEX,
        /**
         * indicator flag
         */
        INDICATOR,
    }

    /**
     * is binary input
     */
    public boolean isBinaryInput;
    /**
     * information of extreme
     */
    public ExtremeInfo extremeInfo;
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
        extremeInfo = null;
    }

    /**
     * constructor
     *
     * @param extremeInfo information of extreme
     * @param aggOp       aggregation type
     * @param inputDim    input dimension
     * @param inputSize   input size
     */
    public AggFnParam(AggOp aggOp, int inputDim, int inputSize, ExtremeInfo extremeInfo) {
        this.extremeInfo = extremeInfo;
        this.isBinaryInput = true;
        this.aggOp = aggOp;
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
