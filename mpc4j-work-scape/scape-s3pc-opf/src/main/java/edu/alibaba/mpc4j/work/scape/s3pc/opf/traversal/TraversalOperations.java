package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Operation for three-party traversal
 *
 * @author Feng Han
 * @date 2024/03/01
 */
public class TraversalOperations {
    public enum TraversalOp {
        /**
         * traverse on arithmetic shares
         */
        TRAVERSAL_A,
        /**
         * traverse on binary shares
         */
        TRAVERSAL_B,
    }

    /**
     * 3p oblivious traversal parameters
     *
     * @author Feng Han
     * @date 2024/03/08
     */
    public static class TraversalFnParam{
        /**
         * operation type
         */
        public TraversalOp op;
        /**
         * input size
         */
        public int dataNum;
        /**
         * input dimension
         */
        public int dim;

        public TraversalFnParam(TraversalOp op, int dataNum, int dim){
            this.op = op;
            this.dataNum = dataNum;
            this.dim = dim;
        }
    }

    /**
     * traversal result of binary values
     *
     * @author Feng Han
     * @date 2024/03/05
     */
    public static class BcTraversalRes{
        /**
         * traverse in an inverse order
         */
        public boolean isInv;
        /**
         * traverse flag
         */
        public BitVector flag;
        /**
         * input
         */
        public BitVector[] input;
        /**
         * output
         */
        public BitVector[] output;

        public BcTraversalRes(boolean isInv, BitVector flag, BitVector[] inputB, BitVector[] outputB){
            this.isInv = isInv;
            this.flag = flag;
            input = inputB;
            output = outputB;
        }
    }

    /**
     * traversal result of arithmetic values
     *
     * @author Feng Han
     * @date 2024/03/05
     */
    public static class AcTraversalRes{
        /**
         * traverse in an inverse order
         */
        public boolean isInv;
        /**
         * operation parameter
         */
        public boolean theta;
        /**
         * traverse flag
         */
        public LongVector flag;
        /**
         * input
         */
        public LongVector[] input;
        /**
         * output
         */
        public LongVector[] output;

        /**
         * constructor
         *
         * @param isInv   traverse in an inverse order
         * @param theta   operation parameter
         * @param flag    traverse flag
         * @param inputA  input
         * @param outputA output
         */
        public AcTraversalRes(boolean isInv, boolean theta, LongVector flag, LongVector[] inputA, LongVector[] outputA){
            this.isInv = isInv;
            this.theta = theta;
            this.flag = flag;
            input = inputA;
            output = outputA;
        }
    }
}
