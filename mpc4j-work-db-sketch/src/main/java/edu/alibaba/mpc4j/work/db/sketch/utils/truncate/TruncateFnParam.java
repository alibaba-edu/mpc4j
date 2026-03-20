package edu.alibaba.mpc4j.work.db.sketch.utils.truncate;

/**
 * Function parameters for Truncate operation.
 * Defines the input characteristics for the truncate protocol.
 */
public class TruncateFnParam {
    /**
     * Input dimension - number of vectors in the input array
     */
    public int inputDim;
    /**
     * Input size - number of elements in each input vector
     */
    public int inputSize;

    /**
     * Constructor for Truncate function parameters
     *
     * @param inputDim  input dimension (number of vectors)
     * @param inputSize input size (elements per vector)
     */
    public TruncateFnParam(int inputDim, int inputSize) {
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
