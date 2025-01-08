package edu.alibaba.mpc4j.common.structure.filter;

import gnu.trove.set.TIntSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Cuckoo Filter. We use the description shown in Vacuum filters (defined in the following paper) for Cuckoo Filter.
 * <p>
 * Wang M, Zhou M, Shi S, et al. Vacuum filters: more space-efficient and faster replacement for bloom and cuckoo
 * filters[J]. Proceedings of the VLDB Endowment, 2019, 13(2): 197-210.
 * </p>
 * Cuckoo filters improves Bloom filters in two aspects.
 * <ul>
 * <li>First, in ideal cases, cuckoo filters cost smaller memory than the space-optimized Bloom filter when the target
 * false positive rate $ε < 3\%$. </li>
 * <li>Second, cuckoo filters support deletion operations without extra memory overhead.</li>
 * </ul>
 * A cuckoo filter is a table of $m$ buckets, each of which contains $4$ slots. Every slot stores an $l$-bit fingerprint
 * $f_x$ of an item $x$. For every item $x$, the cuckoo filter stores its fingerprint $f_x$ in one of two candidate
 * buckets with indices $B_1(x)$ and $B_2(x)$:
 * <ul>
 * <li>B_1(x) = H(x) mod m</li>
 * <li>B_2(x) = Alt(B_1(x), f_x)</li>
 * </ul>
 * where $H$ is a uniform hash function and function $Alt(B, f) = B ⊕ H'(f)$, where $H'$ is another uniform hash
 * function. It is easy to prove: $B_1(x) = Alt(B_2(x), f_x) which means, using $f_x$ and one of the two bucket indices
 * $B_1(x)$ and $B_2(x)$, we are able to compute the other index. To lookup an item $x$, we check whether the fingerprint
 * $f_x$ is stored in two buckets $B_1(x)$ and $B_2(x)$.
 * <p></p>
 * For each item $x$, the cuckoo filter stores its fingerprint $f_x$ in an empty slot in Bucket $B_1(x)$ or $B_2(x)$ if
 * there is an empty slot. If neither $B_1(x)$ nor $B_2(x)$ has an empty slot, the cuckoo filter performs the Eviction
 * process. It randomly chooses a non-empty slot in bucket $B$ ($B$ is one of $B_1(x)$ and $B_2(x)$). The fingerprint
 * $f'$ stored in the slot will be removed and replaced by $f_x$. Then $f'$ will be placed to a slot of the alternate
 * bucket $Alt(B, f')$ of $f'$. If the alternate bucket is also full, the cuckoo filter recursively evicts an existing
 * fingerprint $f''$ in Bucket $Alt(B, f')$ to place $f'$, and looks for an alternate slot for $f''$. When the number of
 * recursive evictions reaches a threshold, this insertion is failed and a reconstruction of the whole filter is required.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public interface CuckooFilter<T> extends Filter<T>, CuckooFilterPosition<T> {
    /**
     * Gets number of hash keys.
     *
     * @return number of hash keys.
     */
    static int getHashKeyNum() {
        return 2;
    }

    /**
     * Puts given data into Cuckoo Filter and traces all modified buckets.
     *
     * @param data data.
     * @return an int set containing all bucket indexes that are modified.
     * @throws IllegalArgumentException if inserting duplicated data into the Cuckoo Filter.
     */
    TIntSet modifyPut(T data);

    @Override
    default void put(T data) {
        modifyPut(data);
    }

    /**
     * Removes given data from Cuckoo Filter and returns the modified bucket index.
     *
     * @param data data.
     * @return the index of the modified bucket.
     * @throws IllegalArgumentException if removing data that is not contained in the Cuckoo Filter.
     */
    int modifyRemove(T data);

    /**
     * Gets the bucket.
     *
     * @param index index.
     * @return bucket.
     */
    ArrayList<ByteBuffer> getBucket(int index);
}
