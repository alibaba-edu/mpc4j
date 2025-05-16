package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.PheType;

/**
 * PHE key parameters.
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PheKeyParams extends PheParams {
    /**
     * Gets if the key is a secret/private key.
     *
     * @return true if the key is a secret/private key, false (the key is a public key) otherwise.
     */
    boolean isPrivate();

    /**
     * Gets the PHE scheme type. Here we use PHE public/secret key to identify which engine to use.
     *
     * @return the PHE scheme type.
     */
    PheType getPheType();
}
