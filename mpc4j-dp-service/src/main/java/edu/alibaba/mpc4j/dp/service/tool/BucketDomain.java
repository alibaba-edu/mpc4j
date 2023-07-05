package edu.alibaba.mpc4j.dp.service.tool;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * bucket domain for Local Differential Privacy mechanism.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BucketDomain {
    /**
     * the universal domain.
     */
    private final Domain universalDomain;
    /**
     * budget num
     */
    private final int w;
    /**
     * domains in each bucket
     */
    private final Domain[] bucketDomains;
    /**
     * hash function
     */
    private final IntHash intHash;

    /**
     * the Constructor.
     *
     * @param universalDomainSet the universal domain set.
     * @param w                  the number of bucket.
     * @param minBucketSize      the minimal bucket size. Every bucket should contain at least that items.
     */
    public BucketDomain(Set<String> universalDomainSet, int w, int minBucketSize) {
        universalDomain = new Domain(universalDomainSet);
        MathPreconditions.checkPositive("w (# of buckets)", w);
        this.w = w;
        // init hash function
        intHash = IntHashFactory.fastestInstance();
        // init budget domain sets
        ArrayList<Set<String>> bucketDomainSets = IntStream.range(0, w)
            .mapToObj(budgetIndex -> new HashSet<String>())
            .collect(Collectors.toCollection(ArrayList::new));
        universalDomainSet.forEach(item -> {
            int bucketIndex = Math.abs(intHash.hash(ObjectUtils.objectToByteArray(item)) % w);
            bucketDomainSets.get(bucketIndex).add(item);
        });
        // init bucket domains
        bucketDomains = IntStream.range(0, w)
            .mapToObj(bucketIndex -> {
                Domain domain = new Domain(bucketDomainSets.get(bucketIndex));
                MathPreconditions.checkGreaterOrEqual("# in " + bucketIndex + "-th bucket", domain.getD(), minBucketSize);
                return domain;
            })
            .toArray(Domain[]::new);
    }

    /**
     * Gets the universal domain set.
     *
     * @return the universal domain set.
     */
    public Set<String> getUniversalDomainSet() {
        return universalDomain.getDomainSet();
    }

    /**
     * Returns if the domain contains the given item.
     *
     * @param item the item.
     * @return true if the domain contains the given item.
     */
    public boolean contains(String item) {
        return universalDomain.contains(item);
    }

    /**
     * Gets the universal index of the item.
     *
     * @param item the item.
     * @return the universal index.
     */
    public int getUniversalItemIndex(String item) {
        return universalDomain.getItemIndex(item);
    }

    /**
     * Gets the item of the universal index.
     *
     * @param universalIndex the universal index.
     * @return the item.
     */
    public String getUniversalIndexItem(int universalIndex) {
        return universalDomain.getIndexItem(universalIndex);
    }

    /**
     * Gets the universal domain size d, i.e., |Ω|.
     *
     * @return the domain size.
     */
    public int getUniversalD() {
        return universalDomain.getD();
    }

    /**
     * Gets the bucket num.
     *
     * @return the bucket num.
     */
    public int getW() {
        return w;
    }

    /**
     * the bucket index for the item.
     *
     * @param item the item.
     * @return the bucket index for the item.
     */
    public int getItemBucket(String item) {
        return HeavyGuardianUtils.getItemBucket(intHash, w, item);
    }

    /**
     * Gets the domain set for the specific bucket.
     *
     * @param bucketIndex the bucket index.
     * @return the domain set for the specific bucket.
     */
    public Set<String> getBucketDomainSet(int bucketIndex) {
        return bucketDomains[bucketIndex].getDomainSet();
    }

    /**
     * Gets the item index in the specific bucket.
     *
     * @param bucketIndex the bucket index.
     * @param item the item.
     * @return the item index in the specific bucket.
     */
    public int getBucketItemIndex(int bucketIndex, String item) {
        return bucketDomains[bucketIndex].getItemIndex(item);
    }

    /**
     * Gets the item of the index in the specific bucket.
     *
     * @param bucketIndex the bucket index.
     * @param index       the index.
     * @return the item of the index in the specific bucket.
     */
    public String getBucketIndexItem(int bucketIndex, int index) {
        return bucketDomains[bucketIndex].getIndexItem(index);
    }

    /**
     * Gets the domain size d, i.e., |Ω|, for the specific bucket.
     *
     * @param bucketIndex the bucket index.
     * @return the domain size for the specific bucket.
     */
    public int getD(int bucketIndex) {
        return bucketDomains[bucketIndex].getD();
    }
}
