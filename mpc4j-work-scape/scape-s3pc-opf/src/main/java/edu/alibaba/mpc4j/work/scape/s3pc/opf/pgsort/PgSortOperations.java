package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

/**
 * 3p oblivious sorting operations
 *
 * @author Feng Han
 * @date 2024/03/08
 */
public class PgSortOperations {
    /**
     * 3p oblivious sorting operations
     *
     * @author Feng Han
     * @date 2024/03/08
     */
    public enum PgSortOp {
        /**
         * sort the arithmetic shares, and only need permutation
         */
        SORT_A,
        /**
         * sort the arithmetic shares. need permutation and sorted data
         */
        SORT_PERMUTE_A,
        /**
         * sort the binary shares, and only need permutation
         */
        SORT_B,
        /**
         * sort the binary shares. need permutation and sorted data
         */
        SORT_PERMUTE_B,
    }

    /**
     * 3p oblivious sorting operation parameters
     *
     * @author Feng Han
     * @date 2024/03/08
     */
    public static class PgSortFnParam{
        /**
         * operation
         */
        public PgSortOp op;
        /**
         * input data size
         */
        public int dataNum;
        /**
         * dimensions of input
         */
        public int[] dims;

        /**
         * constructor
         *
         * @param op      operation
         * @param dataNum input data size
         * @param dims    dimensions of input
         */
        public PgSortFnParam(PgSortOp op, int dataNum, int... dims){
            this.op = op;
            this.dataNum = dataNum;
            this.dims = dims;
        }
    }
}
