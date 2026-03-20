package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Abstract base class for batch-optimized CMS implementations.
 * Uses buffering to batch updates for improved performance.
 */
public abstract class AbstractCMSBatchImpl implements CMS {

    protected final int[][] data;
    protected final BigInteger[] buffer;
    protected int n;
    protected final int bitLength;
    protected final int rowNum;
    protected final int columnNum;
    protected final int logSize;
    protected int bufferSize;

    /**
     * Constructs a batch CMS implementation
     *
     * @param d             number of rows in the sketch
     * @param t             number of columns (must be power of 2)
     * @param elementBitLen bit length of elements
     */
    public AbstractCMSBatchImpl(int d, int t, int elementBitLen) {
        assert (t > 0 && (t & (t - 1)) == 0) : "column size must be power of 2";

        this.rowNum = d;
        this.columnNum = t;
        this.logSize = LongUtils.ceilLog2(columnNum);
        this.bitLength = elementBitLen;

        this.buffer = new BigInteger[columnNum];
        this.bufferSize = 0;
        this.data = new int[rowNum][columnNum];
        for (int i = 0; i < rowNum; i++) {
            for (int j = 0; j < columnNum; j++) {
                data[i][j] = 0;
            }
        }
        this.n = 0;
    }

    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element);
        }
    }

    /**
     * Inserts a single element into the buffer
     * @param element element to insert
     */
    @Override
    public void input(BigInteger element) {
        buffer[bufferSize] = element;
        bufferSize++;
        n++;
        if (bufferSize == columnNum) {
            merge();
        }
    }

    /**
     * Merges buffered elements into the sketch
     */
    protected abstract void merge();

    /**
     * Hash function for mapping elements to sketch indices
     * @param element element to hash
     * @param index row index for hash function selection
     * @return column index in the sketch
     */
    protected abstract int hash(BigInteger element, int index);

    /**
     * Queries the estimated frequency of an element
     * @param element element to query
     * @return estimated frequency
     */
    @Override
    public int query(BigInteger element) {
        int result = n;
        if (bufferSize != 0) {
            merge();
        }
        for (int i = 0; i < rowNum; i++) {
            if (data[i][hash(element, i)] < result) {
                result = data[i][hash(element, i)];
            }
        }
        if (result == n) {
            return 0;
        }
        return result;
    }
    
    @Override
    public int[][] getTable() {
        return this.data;
    }
}
