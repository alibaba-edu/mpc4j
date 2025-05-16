package edu.alibaba.mpc4j.crypto.phe.params;

/**
 * PHE private key, in which the public key is also included.
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PhePrivateKey extends PheKeyParams {
    /**
     * Gets the public key.
     *
     * @return the public key.
     */
    PhePublicKey getPublicKey();
}
