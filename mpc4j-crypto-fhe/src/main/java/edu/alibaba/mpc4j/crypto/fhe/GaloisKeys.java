package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;

/**
 * Class to store Galois keys.
 * <p></p>
 * <p>Slot Rotations</p>
 * Galois keys are certain types of public keys that are needed to perform encrypted
 * vector rotation operations on batched ciphertexts. Batched ciphertexts encrypt
 * a 2-by-(N/2) matrix of modular integers in the BFV scheme, or an N/2-dimensional
 * vector of complex numbers in the CKKS scheme, where N denotes the degree of the
 * polynomial modulus. In the BFV scheme Galois keys can enable both cyclic rotations
 * of the encrypted matrix rows, as well as row swaps (column rotations). In the CKKS
 * scheme Galois keys can enable cyclic vector rotations, as well as a complex
 * conjugation operation.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/galoiskeys.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class GaloisKeys extends KswitchKeys {
    /**
     * Creates an empty GaloisKeys.
     */
    public GaloisKeys() {
        super();
    }

    /**
     * Returns the index of a Galois key in the backing key switching keys instance that corresponds to
     * the given Galois element, assuming that it exists in the backing key switching keys.
     *
     * @param galoisElt the Galois element.
     * @return the index of a Galois key.
     */
    public static int getIndex(int galoisElt) {
        return GaloisTool.getIndexFromElt(galoisElt);
    }

    /**
     * Returns whether a Galois key corresponding to a given Galois element exists.
     *
     * @param galoisElt the Galois element.
     * @return true if the Galois key exists; false otherwise.
     */
    public boolean hasKey(int galoisElt) {
        int index = getIndex(galoisElt);
        return data().length > index && data()[index].length > 0;
    }

    /**
     * Returns a reference to a Galois key. The returned Galois key corresponds
     * to the given Galois element.
     *
     * @param galoisElt the Galois element.
     * @return a reference to a Galois key.
     */
    public PublicKey[] key(int galoisElt) {
        return data(getIndex(galoisElt));
    }
}
