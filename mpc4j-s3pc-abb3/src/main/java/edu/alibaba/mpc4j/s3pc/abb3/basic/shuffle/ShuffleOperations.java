package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle;

import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Operations for three-party shuffling
 *
 * @author Feng Han
 * @date 2024/02/02
 */
public class ShuffleOperations {
    /**
     * Operations for three-party shuffle
     */
    public enum ShuffleOp {
        /**
         * shuffle binary shares in row
         */
        B_SHUFFLE_ROW,
        /**
         * shuffle binary shares in column
         */
        B_SHUFFLE_COLUMN,
        /**
         * inverse shuffle binary shares in column
         */
        B_INV_SHUFFLE_COLUMN,
        /**
         * permute binary shares, the permutation is given by the programmer
         */
        B_PERMUTE_NETWORK,
        /**
         * switch binary shares, the map function is given by the programmer
         */
        B_SWITCH_NETWORK,
        /**
         * duplicate binary shares, the control bits are given by the programmer
         */
        B_DUPLICATE_NETWORK,
        /**
         * shuffle arithmetic shares
         */
        A_SHUFFLE,
        /**
         * shuffle and open the arithmetic shares
         */
        A_SHUFFLE_OPEN,
        /**
         * inverse shuffle arithmetic shares
         */
        A_INV_SHUFFLE,
        /**
         * permute arithmetic shares, the permutation is given by the programmer
         */
        A_PERMUTE_NETWORK,
        /**
         * switch arithmetic shares, the map function is given by the programmer
         */
        A_SWITCH_NETWORK,
        /**
         * duplicate arithmetic shares, the control bits are given by the programmer
         */
        A_DUPLICATE_NETWORK
    }

    /**
     * shuffling result of binary values
     *
     * @author Feng Han
     * @date 2024/02/02
     */
    public static class BcShuffleRes{
        /**
         * binary shuffle in row or column
         */
        public boolean inRow;
        /**
         * permutation or a map function
         */
        public int[] fun;
        /**
         * duplicate flag
         */
        public boolean[] flag;
        /**
         * input
         */
        public BitVector[] input;
        /**
         * output
         */
        public BitVector[] output;

        public BcShuffleRes(boolean inRow, int[] fun, BitVector[] input, BitVector[] output){
            this.inRow = inRow;
            this.fun = fun;
            this.input = input;
            this.output = output;
        }
        public BcShuffleRes(boolean[] flag, BitVector[] input, BitVector[] output){
            this.flag = flag;
            this.input = input;
            this.output = output;
        }
    }

    /**
     * shuffling result of arithmetic values
     *
     * @author Feng Han
     * @date 2024/02/02
     */
    public static class AcShuffleRes{
        /**
         * permutation or a map function
         */
        public int[] fun;
        /**
         * duplicate flag
         */
        public boolean[] flag;
        /**
         * input
         */
        public LongVector[] input;
        /**
         * output
         */
        public LongVector[] output;

        public AcShuffleRes(int[] fun, LongVector[] input, LongVector[] output){
            this.fun = fun;
            this.input = input;
            this.output = output;
        }
        public AcShuffleRes(boolean[] flag, LongVector[] input, LongVector[] output){
            this.flag = flag;
            this.input = input;
            this.output = output;
        }
    }
}
