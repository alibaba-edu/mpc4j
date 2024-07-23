package edu.alibaba.mpc4j.crypto.algs.ope;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.Coins;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.Hgd;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import org.bouncycastle.crypto.CryptoException;

import java.security.SecureRandom;

/**
 * Restricted Order-Preserving Encryption (ROPE) implemented using long.
 *
 * @author Liqiang Peng
 * @date 2024/5/10
 */
public class Zlp24LongRopeEngine {
    /**
     * HGD
     */
    private final Hgd hgd;
    /**
     * key
     */
    private byte[] key;
    /**
     * input range
     */
    private LongRange inputRange;
    /**
     * output range
     */
    private LongRange outputRange;
    /**
     * initialized
     */
    private boolean initialized;
    /**
     * restricted function
     */
    private LongRestriction restriction;

    /**
     * Creates a new ROPE engine.
     */
    public Zlp24LongRopeEngine() {
        this(HgdType.FAST);
    }

    /**
     * Creates a new ROPE engine.
     *
     * @param hgdType HGD type.
     */
    public Zlp24LongRopeEngine(HgdType hgdType) {
        hgd = HgdFactory.getInstance(hgdType);
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
     * Initializes the ROPE engine.
     *
     * @param key                key.
     * @param restriction restricted function.
     */
    public void init(byte[] key, LongRestriction restriction) {
        inputRange = restriction.getInputRange();
        outputRange = restriction.getOutputRange();
        MathPreconditions.checkGreaterOrEqual("output range size", outputRange.size(), inputRange.size());
        MathPreconditions.checkEqual("key.length", "λ", key.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.key = BytesUtils.clone(key);
        this.restriction = restriction;
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

    private long lazySample(long m, LongRange rangeD, LongRange rangeR) throws CryptoException {
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

        // If |D| = 1, uniform sample
        if (dSize == 1) {
            boolean uniformSuccess = false;
            long uniformY = 0L;
            long uniformCounter = 0L;
            while (!uniformSuccess) {
                uniformCounter++;
                // cc ← GetCoins(D, R, 1 || m), where D = 1
                Coins cc = new Coins(key, LongUtils.longToByteArray((m << 1) + 1), uniformCounter);
                // F[D, R, m] ← R
                uniformY = sampleUniform(rangeR, cc);
                // g(m, F[D, R, m]) = 1
                uniformSuccess = restriction.restriction(m, uniformY);
            }
            // return F[D, R, m]
            return uniformY;
        }
        // |D| > 1, HGD sample
        boolean hgdSuccess = false;
        long x = 0L;
        long hgdCounter = 0L;
        while (!hgdSuccess) {
            hgdCounter++;
            // cc ← GetCoins(D, R, 0 || y)
            Coins cc = new Coins(key, LongUtils.longToByteArray(y << 1), hgdCounter);
            // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
            x = sampleHgd(rangeD, rangeR, y, cc);
            hgdSuccess = restriction.restriction(x, y);
        }
        // If m ≤ x
        if (m <= x) {
            // D ← {d + 1, ..., x}
            rangeD = new LongRange(d + 1, x);
            // R ← {r + 1, ..., y}
            rangeR = new LongRange(r + 1, y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new LongRange(x + 1, d + dSize);
            // R ← {y + 1, ..., r + N}
            rangeR = new LongRange(y + 1, r + rSize);
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

    private long lazySampleInv(long c, LongRange rangeD, LongRange rangeR) throws CryptoException {
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

        // If |D| = 1, uniform sample
        if (rangeD.size() == 1) {
            boolean uniformSuccess = false;
            // m ← min(D)
            long uniformPlaintext = rangeD.getStart();
            long uniformCounter = 0L;
            long uniformY = 0L;
            while (!uniformSuccess) {
                uniformCounter++;
                // cc ← GetCoins(D, R, 1 || m), where D = 1
                Coins cc = new Coins(key, LongUtils.longToByteArray((uniformPlaintext << 1) + 1), uniformCounter);
                // F[D, R, m] ← R
                uniformY = sampleUniform(rangeR, cc);
                // g(m, F[D, R, m]) = 1
                uniformSuccess = restriction.restriction(uniformPlaintext, uniformY);
            }
            if (uniformY == c) {
                return uniformPlaintext;
            } else {
                // this means the ciphertext is not generated by the plaintext and the given key
                throw new CryptoException("Invalid ciphertext, the ciphertext is not generated by the given key");
            }
        }
        // |D| > 1, HGD sample
        boolean hgdSuccess = false;
        long x = 0L;
        long hgdCounter = 0L;
        while (!hgdSuccess) {
            hgdCounter++;
            // cc ← GetCoins(D, R, 0 || y)
            Coins coins = new Coins(key, LongUtils.longToByteArray(y << 1), hgdCounter);
            // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
            x = sampleHgd(rangeD, rangeR, y, coins);
            hgdSuccess = restriction.restriction(x, y);
        }
        // if c ≤ y
        if (c <= y) {
            // D ← {d + 1, ..., x}
            rangeD = new LongRange(d + 1, x);
            // R ← {r + 1, ..., y}
            rangeR = new LongRange(r + 1, y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new LongRange(x + 1, d + dSize);
            // R ← {y + 1, ..., r + N}
            rangeR = new LongRange(y + 1, r + rSize);
        }
        // Return LazySampleInv(D, R, c)
        return lazySampleInv(c, rangeD, rangeR);
    }

    private long sampleUniform(LongRange inRange, Coins coins) {
        LongRange curRange = new LongRange(inRange);
        assert curRange.size() != 0;
        // sample using binary search
        while (curRange.size() > 1) {
            // we use shift right instead of "/ 2" to avoid bugs for negative numbers.
            long mid = ((curRange.getStart() + curRange.getEnd()) >> 1);
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
    private long sampleHgd(LongRange rangeM, LongRange rangeN, long sample, Coins coins) {
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

        long r = hgd.sample(k, mSize, nSize - mSize, coins);

        if (r == 0) {
            return rangeM.getStart();
        } else if (r == mSize) {
            return rangeM.getEnd();
        } else {
            // x ← min(D) − 1 + I[D, R, y]
            return rangeM.getStart() + r - 1;
        }
    }
}
