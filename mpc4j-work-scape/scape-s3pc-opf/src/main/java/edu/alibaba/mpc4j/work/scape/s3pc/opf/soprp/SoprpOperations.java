package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

/**
 * 3p soprp operations
 *
 * @author Feng Han
 * @date 2024/03/08
 */
public class SoprpOperations {
    /**
     * type of operations
     */
    public enum PrpOp {
        /**
         * encryption
         */
        ENC,
        /**
         * decryption
         */
        DEC,
    }

    /**
     * 3p soprp operation parameters
     *
     * @author Feng Han
     * @date 2024/03/08
     */
    public static class PrpFnParam{
        /**
         * operation
         */
        public PrpOp op;
        /**
         * input data size
         */
        public int dataNum;
        /**
         * input data bit dimension
         */
        public int bitDim;

        /**
         * constructor
         *
         * @param op      operation
         * @param dataNum input data size
         * @param bitDim  input data bit dimension
         */
        public PrpFnParam(PrpOp op, int dataNum, int bitDim){
            this.op = op;
            this.dataNum = dataNum;
            this.bitDim = bitDim;
        }
    }
}
