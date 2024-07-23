package edu.alibaba.mpc4j.crypto.algs.ope;

import edu.alibaba.mpc4j.crypto.algs.restriction.LongEmptyRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import org.bouncycastle.crypto.CryptoException;

import java.security.SecureRandom;

/**
 * Order-Preserving Encryption (OPE) implemented using long. The scheme comes from the following paper:
 * <p></p>
 * Alexandra Boldyreva, Nathan Chenette, Younho Lee, Adam O'Neill. Order-Preserving Symmetric Encryption. EUROCRYPT
 * 2009, pp. 224-241.
 * <p></p>
 * The implementation is inspired by:
 * <p></p>
 * https://github.com/ssavvides/jope/blob/master/src/jope/OPE.java
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class Bclo09LongOpeEngine {
    /**
     * restricted OPE
     */
    private final Zlp24LongRopeEngine ropeEngine;

    /**
     * Creates a new OPE engine.
     */
    public Bclo09LongOpeEngine() {
        this(HgdType.FAST);
    }

    /**
     * Creates a new OPE engine.
     *
     * @param hgdType HGD type.
     */
    public Bclo09LongOpeEngine(HgdType hgdType) {
        ropeEngine = new Zlp24LongRopeEngine(hgdType);
    }

    /**
     * Generates a key.
     *
     * @param secureRandom the random state.
     * @return key.
     */
    public byte[] keyGen(SecureRandom secureRandom) {
        return ropeEngine.keyGen(secureRandom);
    }

    /**
     * Initializes the OPE engine.
     *
     * @param key         key.
     * @param inputRange  input value range.
     * @param outputRange output value range.
     */
    public void init(byte[] key, LongRange inputRange, LongRange outputRange) {
        ropeEngine.init(key, new LongEmptyRestriction(inputRange, outputRange));
    }

    /**
     * Encrypts the plaintext.
     *
     * @param plaintext plaintext.
     * @return ciphertext.
     */
    public long encrypt(long plaintext) throws CryptoException {
        return ropeEngine.encrypt(plaintext);
    }

    /**
     * Decrypts the ciphertext.
     *
     * @param ciphertext ciphertext.
     * @return plaintext.
     */
    public long decrypt(long ciphertext) throws CryptoException {
        return ropeEngine.decrypt(ciphertext);
    }
}
