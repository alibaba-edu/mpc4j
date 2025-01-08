package edu.alibaba.mpc4j.crypto.algs.smprp;

import edu.alibaba.mpc4j.crypto.algs.smprp.SmallDomainPrpFactory.SmallDomainPrpType;

/**
 * small-domain PRP, which is a PRP for [n] → [n] where n is up to 2^32 - 1.
 *
 * @author Weiran Liu
 * @date 2024/8/22
 */
public interface SmallDomainPrp {
    /**
     * Gets type.
     *
     * @return type.
     */
    SmallDomainPrpType getType();

    /**
     * Initialize.
     *
     * @param range range.
     * @param key   key.
     */
    void init(int range, byte[] key);

    /**
     * Computes pseudo-random permutation.
     *
     * @param plaintext plaintext in [0, range).
     * @return ciphertext in [0, range).
     */
    int prp(int plaintext);

    /**
     * Computes inverse pseudo-random permutation.
     *
     * @param ciphertext ciphertext in [0, range).
     * @return plaintext in [0, range).
     */
    int invPrp(int ciphertext);
}
