package edu.alibaba.mpc4j.crypto.algs.ope;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.algs.distribution.Coins;
import edu.alibaba.mpc4j.crypto.algs.distribution.LongHgd;
import edu.alibaba.mpc4j.crypto.algs.range.LongValueRange;
import org.bouncycastle.crypto.CryptoException;

import java.security.SecureRandom;

/**
 * Order-Preserving Encryption (OPE) implemented using long. The scheme comes from the following paper:
 * <p></p>
 * Alexandra Boldyreva, Nathan Chenette, Younho Lee. Order-Preserving Symmetric Encryption. EUROCRYPT 2009, pp. 224-241.
 * <p></p>
 * The implementation is inspired by:
 * <p></p>
 * https://github.com/ssavvides/jope/blob/master/src/jope/OPE.java
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class Bclo19LongOpeEngine {
    /**
     * key
     */
    private byte[] key;
    /**
     * input range
     */
    private LongValueRange inputRange;
    /**
     * output range
     */
    private LongValueRange outputRange;
    /**
     * initialzied
     */
    private boolean initialized;

    /**
     * Creates a new OPE engine.
     */
    public Bclo19LongOpeEngine() {
        initialized = false;
    }

    /**
     * Generates a key.
     *
     * @param secureRandom the random state.
     * @return key.
     */
    public byte[] keyGen(SecureRandom secureRandom) {
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);

        return key;
    }

    /**
     * Initializes the OPE engine.
     *
     * @param key         key.
     * @param inputRange  input value range.
     * @param outputRange output value range.
     */
    public void init(byte[] key, LongValueRange inputRange, LongValueRange outputRange) {
        MathPreconditions.checkGreaterOrEqual("output range size", outputRange.size(), inputRange.size());
        this.inputRange = inputRange;
        this.outputRange = outputRange;
        MathPreconditions.checkEqual("key.length", "λ", key.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.key = BytesUtils.clone(key);
        initialized = true;
    }

    /**
     * Encrypts the plaintext.
     *
     * @param plaintext plaintext.
     * @return ciphertext.
     */
    public long encrypt(long plaintext) throws CryptoException {
        Preconditions.checkArgument(initialized, this.getClass().getSimpleName() + " is not initialized");
        Preconditions.checkArgument(
            inputRange.contains(plaintext),
            "Plaintext is not within the input range " + inputRange + ": " + plaintext
        );

        return lazySample(plaintext, inputRange, outputRange);
    }

    private long lazySample(long m, LongValueRange rangeD, LongValueRange rangeR) throws CryptoException {
        // LazySample(D, R, m)
        // M ← |D| ; N ← |R|
        long dSize = rangeD.size();
        long rSize = rangeR.size();
        // M <= N, otherwise there must exist a plaintext that does not have its corresponding ciphertext
        if (dSize > rSize) {
            throw new CryptoException("rangeD (" + rangeD + ") is larger than rangeR (" + rangeR + ")");
        }

        // d ← min(D) − 1 ; r ← min(R) − 1
        long d = rangeD.getStart() - 1;
        long r = rangeR.getStart() - 1;
        // y ← r + ⌈N/2⌉
        long halfN = (long) Math.ceil((double) rSize / 2.0);
        long y = r + halfN;

        // If |D| = 1
        if (dSize == 1) {
            // cc ← GetCoins(D, R, 1 || m), where D = 1
            Coins cc = new Coins(key, LongUtils.longToByteArray((m << 1) + 1));
            // F[D, R, m] ← R and return F[D, R, m]
            return sampleUniform(rangeR, cc);
        }
        // cc ← GetCoins(D, R, 0 || y)
        Coins cc = new Coins(key, LongUtils.longToByteArray(y << 1));
        // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
        long x = sampleHgd(rangeD, rangeR, y, cc);

        // If m ≤ x
        if (m <= x) {
            // D ← {d + 1, ..., x}
            rangeD = new LongValueRange(d + 1, x);
            // R ← {r + 1, ..., y}
            rangeR = new LongValueRange(r + 1, y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new LongValueRange(x + 1, d + dSize);
            // R ← {y + 1, ..., r + N}
            rangeR = new LongValueRange(y + 1, r + rSize);
        }
        // Return LazySample(D, R, m)
        return lazySample(m, rangeD, rangeR);
    }

    /**
     * Decrypts the ciphertext.
     *
     * @param ciphertext ciphertext.
     * @return plaintext.
     */
    public long decrypt(long ciphertext) throws CryptoException {
        Preconditions.checkArgument(initialized, this.getClass().getSimpleName() + " is not initialized");
        Preconditions.checkArgument(
            outputRange.contains(ciphertext),
            "Ciphertext is not within the input range " + outputRange + ": " + ciphertext
        );

        return lazySampleInv(ciphertext, inputRange, outputRange);
    }

    private long lazySampleInv(long c, LongValueRange rangeD, LongValueRange rangeR) throws CryptoException {
        // M ← |D|; N ← |R|
        long dSize = rangeD.size();
        long rSize = rangeR.size();
        // M <= N, otherwise there must exist a plaintext that does not have its corresponding ciphertext
        if (dSize > rSize) {
            throw new CryptoException("rangeD (" + rangeD + ") is larger than rangeR (" + rangeR + ")");
        }

        // d ← min(D) − 1 ; r ← min(R) − 1
        long d = rangeD.getStart() - 1;
        long r = rangeR.getStart() - 1;
        // y ← r + ⌈N/2⌉
        long halfN = (long) Math.ceil((double) rSize / 2.0);
        long y = r + halfN;

        // If |D| = 1
        if (rangeD.size() == 1) {
            // m ← min(D)
            long plaintext = rangeD.getStart();
            Coins coins = new Coins(key, LongUtils.longToByteArray((plaintext << 1) + 1));
            long sampledCiphertext = sampleUniform(rangeR, coins);
            if (sampledCiphertext == c) {
                return plaintext;
            } else {
                // this means the ciphertext is not generated by the plaintext and the given key
                throw new CryptoException("Invalid ciphertext, the ciphertext is not generated by the given key");
            }
        }
        // cc ← GetCoins(D, R, 0 || y)
        Coins coins = new Coins(key, LongUtils.longToByteArray(y << 1));
        // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
        long x = sampleHgd(rangeD, rangeR, y, coins);

        // if c ≤ y
        if (c <= y) {
            // D ← {d + 1, ..., x}
            rangeD = new LongValueRange(d + 1, x);
            // R ← {r + 1, ..., y}
            rangeR = new LongValueRange(r + 1, y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new LongValueRange(x + 1, d + dSize);
            // R ← {y + 1, ..., r + N}
            rangeR = new LongValueRange(y + 1, r + rSize);
        }
        // Return LazySampleInv(D, R, c)
        return lazySampleInv(c, rangeD, rangeR);
    }

    private long sampleUniform(LongValueRange inRange, Coins coins) {
        LongValueRange curRange = new LongValueRange(inRange);
        assert curRange.size() != 0;
        // sample using binary search
        while (curRange.size() > 1) {
            long mid = (curRange.getStart() + curRange.getEnd()) / 2;
            boolean bit = coins.next();
            if (!bit) {
                curRange.setEnd(mid);
            } else {
                curRange.setStart(mid + 1);
            }
        }

        assert curRange.size() != 0;

        return curRange.getStart();
    }

    /**
     * Samples HG(M, N, y - r; cc), where M is the plaintext space, N is the ciphertext space, y - r is the mid.
     *
     * @param rangeM plaintext space.
     * @param rangeN ciphertext space.
     * @param sample mid.
     * @param coins random coin.
     * @return sample result.
     */
    private long sampleHgd(LongValueRange rangeM, LongValueRange rangeN, long sample, Coins coins) {
        long mSize = rangeM.size();
        long nSize = rangeN.size();

        assert mSize > 0 && nSize > 0;
        assert mSize <= nSize;
        assert rangeN.contains(sample);

        // k = n - r
        long k = sample - rangeN.getStart() + 1;

        // input size == output size, one-to-one map.
        if (mSize == nSize) {
            return rangeM.getStart() + k - 1;
        }

        long r = LongHgd.sample(k, mSize, nSize, coins);

        if (r == 0) {
            return rangeM.getStart();
        } else if (r == mSize) {
            return rangeM.getEnd();
        } else {
            long inSample = rangeM.getStart() + r;
            assert rangeM.contains(inSample);
            return inSample;
        }
    }
}
