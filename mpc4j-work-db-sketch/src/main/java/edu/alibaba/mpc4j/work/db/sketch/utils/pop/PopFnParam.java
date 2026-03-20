package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

/**
 * Function parameters for Pop (Permute-and-Open) operation.
 * Defines the input characteristics and operation mode for the Pop protocol.
 */
public class PopFnParam {
    /**
     * Whether the pop operation uses index-based selection
     * If true, pop is performed based on a target index
     * If false, pop is performed based on a flag vector
     */
    public boolean popFromIndex;
    /**
     * Input dimension - number of vectors in the input array
     */
    public int inputDim;
    /**
     * Input size - number of elements in each input vector
     */
    public int inputSize;

    /**
     * Constructor for Pop function parameters
     *
     * @param popFromIndex whether pop operation uses index-based selection
     * @param inputDim      input dimension (number of vectors)
     * @param inputSize     input size (elements per vector)
     */
    public PopFnParam(boolean popFromIndex, int inputDim, int inputSize) {
        this.popFromIndex = popFromIndex;
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
