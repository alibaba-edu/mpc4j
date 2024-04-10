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
    public enum ShuffleOp {
        B_SHUFFLE_ROW,
        B_SHUFFLE_COLUMN,
        B_PERMUTE_NETWORK,
        B_SWITCH_NETWORK,
        B_DUPLICATE_NETWORK,
        A_SHUFFLE,
        A_SHUFFLE_OPEN,
        A_INV_SHUFFLE,
        A_PERMUTE_NETWORK,
        A_SWITCH_NETWORK,
        A_DUPLICATE_NETWORK
    }

    /**
     * shuffling result of binary values
     *
     * @author Feng Han
     * @date 2024/02/02
     */
    public static class BcShuffleRes{
        public boolean inRow;
        public int[] fun;
        public boolean[] flag;
        public BitVector[] input;
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
        public int[] fun;
        public boolean[] flag;
        public LongVector[] input;
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
