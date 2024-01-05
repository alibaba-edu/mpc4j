package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

/**
 * 2哈希-布谷鸟图。原始构造来自于：
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020.
 * Springer, Cham, 2020, pp. 739-767.
 *
 * @author Weiran Liu
 * @date 2021/09/08
 */
public class H2CuckooTable<T> extends AbstractCuckooTable<T> {
    /**
     * 2哈希-布谷鸟图的哈希数量
     */
    public static final int HASH_NUM = 2;

    /**
     * 2哈希-布谷鸟哈希图构造函数。
     *
     * @param numOfVertices 顶点总数量。
     */
    public H2CuckooTable(int numOfVertices) {
        super(numOfVertices, HASH_NUM);
    }
}
