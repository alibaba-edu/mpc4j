package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion;

import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Operations for three-party conversion
 *
 * @author Feng Han
 * @date 2024/02/06
 */
public class ConvOperations {
    /**
     * Operations for three-party conversion
     */
    public enum ConvOp {
        /**
         * arithmetic share to binary share
         */
        A2B,
        /**
         * binary share to arithmetic share
         */
        B2A,
        /**
         * ont-bit share to arithmetic share
         */
        BIT2A,
        /**
         * do multiplication between an arithmetic share and a one-bit binary share
         */
        A_MUL_B,
        /**
         * extract a specific bit from an arithmetic share
         */
        BIT_EXTRACTION
    }

    /**
     * shuffling result of binary values
     *
     * @author Feng Han
     * @date 2024/02/02
     */
    public static class ConvRes{
        /**
         * valid bit length of the arithmetic share to be converted or the target bit index of the arithmetic share to be extracted
         */
        public int bitLen;
        /**
         * input or output binary value
         */
        public BitVector[][] bValues;
        /**
         * input or output arithmetic value
         */
        public LongVector[] aValues;
        /**
         * mul result of A_MUL_B
         */
        public LongVector[] mulRes;

        public ConvRes(int bitLen, BitVector[][] bValues, LongVector[] aValues){
            this.bitLen = bitLen;
            this.bValues = bValues;
            this.aValues = aValues;
        }

        public ConvRes(int bitLen, BitVector[][] bValues, LongVector[] aValues, LongVector[] mulRes){
            this.bitLen = bitLen;
            this.bValues = bValues;
            this.aValues = aValues;
            this.mulRes = mulRes;
        }
    }
}
