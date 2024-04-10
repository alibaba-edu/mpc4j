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
    public enum ConvOp {
        A2B,
        B2A,
        BIT2A,
        A_MUL_B,
        BIT_EXTRACTION
    }

    /**
     * shuffling result of binary values
     *
     * @author Feng Han
     * @date 2024/02/02
     */
    public static class ConvRes{
        public int bitLen;
        public BitVector[][] bValues;
        public LongVector[] aValues;

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
