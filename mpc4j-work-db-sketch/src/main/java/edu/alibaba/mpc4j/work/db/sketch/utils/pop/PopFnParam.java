package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

/**
 * pop one value out of the array
 */
public class PopFnParam {
    /**
     * Pop Operation with index
     */
    public boolean popFromIndex;
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
     * @param popFromIndex Pop Operation with index
     * @param inputDim      input dimension
     * @param inputSize     input size
     */
    public PopFnParam(boolean popFromIndex, int inputDim, int inputSize) {
        this.popFromIndex = popFromIndex;
        this.inputDim = inputDim;
        this.inputSize = inputSize;
    }
}
