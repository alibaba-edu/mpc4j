package edu.alibaba.mpc4j.crypto.fhe;

/**
 * Class to store relinearization keys.
 * <p></p>
 * Freshly encrypted ciphertexts have a size of 2, and multiplying ciphertexts
 * of sizes K and L results in a ciphertext of size K+L-1. Unfortunately, this
 * growth in size slows down further multiplications and increases noise growth.
 * Relinearization is an operation that has no semantic meaning, but it reduces
 * the size of ciphertexts back to 2. Microsoft SEAL can only relinearize size 3
 * ciphertexts back to size 2, so if the ciphertexts grow larger than size 3,
 * there is no way to reduce their size. Relinearization requires an instance of
 * RelinKeys to be created by the secret key owner and to be shared with the
 * evaluator. Note that plain multiplication is fundamentally different from
 * normal multiplication and does not result in ciphertext size growth.
 * <p></p>
 * Typically, one should always relinearize after each multiplications. However,
 * in some cases relinearization should be postponed as late as possible due to
 * its computational cost. For example, suppose the computation involves several
 * homomorphic multiplications followed by a sum of the results. In this case it
 * makes sense to not relinearize each product, but instead add them first and
 * only then relinearize the sum. This is particularly important when using the
 * CKKS scheme, where relinearization is much more computationally costly than
 * multiplications and additions.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/relinkeys.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class RelinKeys extends KswitchKeys {
    private static final long serialVersionUID = -8735811624759084213L;

    /**
     * Creates an empty RelinKeys.
     */
    public RelinKeys() {
        super();
    }

    /**
     * Returns the index of a relinearization key in the backing KSwitchKeys
     * instance that corresponds to the given secret key power, assuming that
     * it exists in the backing KSwitchKeys.
     *
     * @param keyPower the power of the secret key.
     * @return the index of a relinearization key.
     */
    public static int getIndex(int keyPower) {
        if (keyPower < 2) {
            throw new IllegalArgumentException("keyPower con not be less than 2");
        }
        return keyPower - 2;
    }

    /**
     * Returns whether a relinearization key corresponding to a given power of
     * the secret key exists.
     *
     * @param keyPower the power of the secret key.
     * @return true if a relinearization key corresponding to a given power of
     * the secret key exists; false otherwise.
     */
    public boolean hasKey(int keyPower) {
        int index = getIndex(keyPower);
        return data().length > index && data(index).length > 0;
    }

    /**
     * Returns a reference to a relinearization key. The returned
     * relinearization key corresponds to the given power of the secret key.
     *
     * @param keyPower the power of the secret key.
     * @return a reference to a relinearization key.
     */
    public PublicKey[] key(int keyPower) {
        return data(getIndex(keyPower));
    }
}
