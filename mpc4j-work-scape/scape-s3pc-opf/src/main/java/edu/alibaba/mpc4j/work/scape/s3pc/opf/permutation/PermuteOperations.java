package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation;

import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Operation for three-party permutation
 *
 * @author Feng Han
 * @date 2024/03/01
 */
public class PermuteOperations {
    /**
     * type of 3p oblivious permutation operation
     */
    public enum PermuteOp {
        /**
         * compose arithmetic-shared permutation pai arithmetic shares sigma
         */
        COMPOSE_A_A,
        /**
         * compose binary-shared permutation pai binary shares sigma
         */
        COMPOSE_B_B,
        /**
         * apply the inverse of the arithmetic-shared permutation pai on arithmetic shares
         */
        APPLY_INV_A_A,
        /**
         * apply the inverse of the arithmetic-shared permutation pai on binary shares
         */
        APPLY_INV_A_B,
        /**
         * apply the inverse of the binary-shared permutation pai on binary shares
         */
        APPLY_INV_B_B,
    }

    /**
     * 3p oblivious permutation operation parameters
     */
    public static class PermuteFnParam{
        /**
         * operation name
         */
        public PermuteOp op;
        /**
         * size of data
         */
        public int dataNum;
        /**
         * input data dimension
         */
        public int dataDim;
        /**
         * dimension of permutation
         */
        public int paiDim;

        /**
         * constructor
         *
         * @param op      operation
         * @param dataNum input data size
         * @param dataDim input data dimension
         * @param paiDim  dimension of permutation
         */
        public PermuteFnParam(PermuteOp op, int dataNum, int dataDim, int paiDim){
            this.op = op;
            this.dataNum = dataNum;
            this.dataDim = dataDim;
            this.paiDim = paiDim;
        }
    }

    /**
     * shuffling result of binary values
     */
    public static class BcPermuteRes {
        /**
         * permutation
         */
        public int[] pai;
        /**
         * real input
         */
        public BitVector[] input;
        /**
         * real output
         */
        public BitVector[] output;

        /**
         * constructor
         *
         * @param pai     permutation
         * @param inputB  real input
         * @param outputB real output
         */
        public BcPermuteRes(int[] pai, BitVector[] inputB, BitVector[] outputB){
            this.pai = pai;
            input = inputB;
            output = outputB;
        }
    }

    /**
     * shuffling result of arithmetic values
     */
    public static class AcPermuteRes{
        /**
         * permutation
         */
        public int[] pai;
        /**
         * real input
         */
        public LongVector[] input;
        /**
         * real output
         */
        public LongVector[] output;

        /**
         * constructor
         *
         * @param pai     permutation
         * @param inputA  real input
         * @param outputA real output
         */
        public AcPermuteRes(int[] pai, LongVector[] inputA, LongVector[] outputA){
            this.pai = pai;
            input = inputA;
            output = outputA;
        }
    }
}
