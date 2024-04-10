package edu.alibaba.mpc4j.common.structure.okve.cuckootable;

/**
 * 3哈希-布谷鸟图。原始构造来自于下述论文第4.1节：OKVS based on a 3-Hash Garbled Cuckoo Table。
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 *
 * 注意，虽然原始论文要求3哈希-布谷鸟图中每条边对应的顶点应不一致，但如果每条边对应的定点有重复，影响的只是2-core图中边的数量。因此，实际实现
 * 中我们不要求3哈希-布谷鸟图中每条边对应的定点必须不同，但利用3哈希-布谷鸟图构造DOKVS时会要求这一点。
 *
 * 原始论文边对应定点不一致的要求来自于第4.1节的脚注4：
 * The hyperedge is sampled uniformly at random from all subsets of 3 different nodes in the graph. We simplify the
 * notation by referring to hash functions h_1, h_2, h_3, but these functions are invoked together under the constraint
 * that the outputs of the three hash functions are distinct from each other.
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
public class H3CuckooTable<T> extends AbstractCuckooTable<T> {
    /**
     * 3哈希-布谷鸟图的哈希数量
     */
    public static final int HASH_NUM = 3;

    /**
     * 3哈希-布谷鸟哈希图构造函数。
     *
     * @param numOfVertices 顶点总数量。
     */
    public H3CuckooTable(int numOfVertices) {
        super(numOfVertices, HASH_NUM);
    }
}
