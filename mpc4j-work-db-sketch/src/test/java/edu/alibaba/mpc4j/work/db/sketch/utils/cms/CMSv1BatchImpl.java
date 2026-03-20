package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import java.math.BigInteger;

/**
 * Batch-optimized CMS implementation using double hashing.
 * Batches updates for improved performance while using simple hash functions.
 */
public class CMSv1BatchImpl extends AbstractCMSBatchImpl implements CMS {

    /**
     * Constructs a batch CMS implementation with double hashing
     * @param d number of rows in the sketch
     * @param t number of columns
     * @param hashParameter hash parameters [a][b] for each row
     * @param elementBitLen bit length of elements
     */
    public CMSv1BatchImpl(int d, int t, BigInteger[][] hashParameter, int elementBitLen) {
        super(d, t, elementBitLen);
        assert (2 == hashParameter.length) : "two rows, first row is a and second row is b";
        assert (d == hashParameter[0].length) : "row size must be equal to hash parameter length";
        this.hashParameter = hashParameter.clone();
    }

    /**
     * Merges buffered elements into the sketch
     */
    @Override
    protected void merge() {
        for (int j = 0; j < bufferSize; j++) {
            BigInteger element = buffer[j];
            for (int i = 0; i < rowNum; i++) {
                int index = hash(element, i);
                data[i][index] = data[i][index] + 1;
            }
        }
        bufferSize = 0;
    }
    
    /**
     * Hash function using double hashing scheme
     * @param element element to hash
     * @param index row index for hash function selection
     * @return column index in the sketch
     */
    protected int hash(BigInteger element, int index) {
        BigInteger res = BigInteger.valueOf(1).shiftLeft(2 * bitLength).subtract(BigInteger.valueOf(1)).and
                (element.multiply(hashParameter[0][index]).add(hashParameter[1][index]))
                .shiftRight(2 * bitLength - logSize);
        return res.intValue();
    }
    private final BigInteger[][] hashParameter;
}
