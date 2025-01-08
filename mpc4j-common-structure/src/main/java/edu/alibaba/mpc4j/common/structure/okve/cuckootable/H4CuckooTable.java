package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

/**
 * Cuckoo Table with 4 hash num.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class H4CuckooTable<T> extends AbstractCuckooTable<T> {
    /**
     * 4 hash num
     */
    public static final int HASH_NUM = 4;

    /**
     * Creates a cuckoo table with 4 hash num.
     *
     * @param numOfVertices number of vertices.
     */
    public H4CuckooTable(int numOfVertices) {
        super(numOfVertices, HASH_NUM);
    }
}
