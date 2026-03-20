package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

/**
 * Three-party oblivious order select operations.
 * Defines the available operations and parameters for the order select protocol.
 */
public class OrderSelectOperations {
    /**
     * Three-party oblivious range select operations
     */
    public enum OrderSelectOp {
        /**
         * Select arithmetic shares - requires permutation and selected data
         */
        RANGE_SELECT_A,
        /**
         * Select binary shares in the target range - requires permutation and selected data
         */
        RANGE_SELECT_B,
    }

    /**
     * Three-party oblivious order select operation parameters
     */
    public static class OrderSelectFnParam {
        /**
         * The operation to perform
         */
        public OrderSelectOp op;
        /**
         * Input data size (number of elements)
         */
        public int dataNum;
        /**
         * Dimensions of input (number of vectors)
         */
        public int[] dims;
        /**
         * Required output range [from, to)
         */
        public int[] range;

        /**
         * Constructor for order select function parameters
         *
         * @param op      the operation to perform
         * @param dataNum input data size
         * @param range   required output range [from, to)
         * @param dims    dimensions of input
         */
        public OrderSelectFnParam(OrderSelectOp op, int dataNum, int[] range, int... dims) {
            this.op = op;
            this.dataNum = dataNum;
            this.range = range;
            this.dims = dims;
        }
    }
}
