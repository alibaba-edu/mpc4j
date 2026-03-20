package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Naive implementation of Count-Min Sketch (CMS).
 * Directly updates the sketch table without batching.
 */
public class CMSNaiveImpl implements CMS{
    private final int[][] data;
    private int n;
    private final int bitLength;
    private final int rowNum;
    private final int columnNum;
    private final BigInteger[][] hashParameter;
    private final int logSize;

    /**
     * Constructs a naive CMS implementation
     * @param d number of rows in the sketch
     * @param t number of columns
     * @param hashParameter hash parameters [a][b] for each row
     * @param elementBitLen bit length of elements
     */
    public CMSNaiveImpl(int d, int t, BigInteger[][] hashParameter, int elementBitLen) {
        this.rowNum = d;
        this.columnNum = t;
        this.bitLength = elementBitLen;
        this.data = new int[rowNum][columnNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < columnNum; j++) {
                data[i][j] = 0;
            }
        }
        this.n = 0;
        this.logSize = LongUtils.ceilLog2(columnNum);

        assert (2 == hashParameter.length) : "two rows, first row is a and second row is b";
        assert (d == hashParameter[0].length) : "row size must be equal to hash parameter length";
        this.hashParameter = hashParameter.clone();
    }

    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element);
        }
    }

    /**
     * Inserts a single element into the sketch
     * @param element element to insert
     */
    @Override
    public void input(BigInteger element) {
        for (int i = 0; i < rowNum; i++) {
            int index = hash(element, i);
            data[i][index] = data[i][index] + 1;
        }
    }
    
    /**
     * Hash function using double hashing scheme
     * @param element element to hash
     * @param index row index for hash function selection
     * @return column index in the sketch
     */
    private int hash(BigInteger element, int index) {
        BigInteger res = BigInteger.valueOf(1).shiftLeft(2 * bitLength).subtract(BigInteger.valueOf(1)).and
                        (element.multiply(hashParameter[0][index]).add(hashParameter[1][index]))
                .shiftRight(2 * bitLength - logSize);
        return res.intValue();
    }

    @Override
    public int query(BigInteger element) {
        return 0;
    }
    
    @Override
    public int[][] getTable(){
        return this.data.clone();
    }
}
