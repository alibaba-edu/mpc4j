package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

/**
 * 3p oblivious order select operations
 */
public class OrderSelectOperations {
    /**
     * 3p oblivious range select operations
     */
    public enum OrderSelectOp {
        /**
         * select the arithmetic shares. need permutation and selected data
         */
        RANGE_SELECT_A,
        /**
         * select the binary shares in the target range. need permutation and selected data
         */
        RANGE_SELECT_B,
    }

    /**
     * 3p oblivious order select operation parameters
     */
    public static class OrderSelectFnParam {
        /**
         * operation
         */
        public OrderSelectOp op;
        /**
         * input data size
         */
        public int dataNum;
        /**
         * dimensions of input
         */
        public int[] dims;
        /**
         * required output range
         */
        public int[] range;

        /**
         * constructor
         *
         * @param op      operation
         * @param dataNum input data size
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
