package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

public class TruncateFnParam {
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
     * @param inputDim  input dimension
     * @param inputSize input size
     */
    public TruncateFnParam(int inputDim, int inputSize) {
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
