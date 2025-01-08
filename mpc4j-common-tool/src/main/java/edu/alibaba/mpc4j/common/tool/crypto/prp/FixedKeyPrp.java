package edu.alibaba.mpc4j.common.tool.crypto.prp;

/**
 * Fixed key pseudo-random permutation. This is used in client-preprocessing PIR.
 *
 * @author Weiran Liu
 * @date 2024/10/26
 */
public interface FixedKeyPrp {
    /**
     * Gets the random permutation.
     *
     * @param plaintext plaintext.
     * @return ciphertext.
     */
    byte[] prp(byte[] plaintext);
}
