package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

/**
 * permutation completion operations
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class FillPerOperations {
    /**
     * permutation completion operations
     */
    public enum FillPerOp {
        /**
         * fill one permutation
         */
        FILL_ONE_PER_A,
        /**
         * fill two permutation
         */
        FILL_TWO_PER_A,
    }

    public static class FillPerFnParam{
        /**
         * permutation completion operations
         */
        public FillPerOp op;
        /**
         * input size
         */
        public int[] inputLen;
        /**
         * target permutation size
         */
        public int outputLen;

        /**
         * constructor
         *
         * @param op        permutation completion operations
         * @param outputLen target permutation size
         * @param inputLen  input size
         */
        public FillPerFnParam(FillPerOp op, int outputLen, int... inputLen){
            this.op = op;
            this.outputLen = outputLen;
            this.inputLen = inputLen;
        }
    }
}
