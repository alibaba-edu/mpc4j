package edu.alibaba.mpc4j.crypto.algs.utils.distribution;

import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;

/**
 * Hypergeometric distribution.
 *
 * @author Weiran Liu
 * @date 2024/5/14
 */
public interface Hgd {
    /**
     * Samples from the hypergeometric distribution.
     *
     * @param k     sample k of the items.
     * @param n1    n_1 of the items have a particular attribute (good).
     * @param n2    n_2 of the items do not have a particular attribute (bad).
     * @param coins random coins.
     * @return number of items that has the particular attribute.
     */
    long sample(long k, long n1, long n2, Coins coins);

    /**
     * Gets type.
     *
     * @return type.
     */
    HgdType getType();
}
