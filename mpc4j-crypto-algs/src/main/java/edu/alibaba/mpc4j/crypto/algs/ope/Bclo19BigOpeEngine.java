package edu.alibaba.mpc4j.crypto.algs.ope;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.algs.distribution.BigHgd;
import edu.alibaba.mpc4j.crypto.algs.distribution.Coins;
import edu.alibaba.mpc4j.crypto.algs.range.BigValueRange;
import org.bouncycastle.crypto.CryptoException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;

/**
 * Order-Preserving Encryption (OPE) implemented using BigInteger. The scheme comes from the following paper:
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
public class Bclo19BigOpeEngine {
    /**
     * key
     */
    private byte[] key;
    /**
     * input range
     */
    private BigValueRange inputRange;
    /**
     * output range
     */
    private BigValueRange outputRange;
    /**
     * initialzied
     */
    private boolean initialized;

    /**
     * Creates a new OPE engine.
     */
    public Bclo19BigOpeEngine() {
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
    public void init(byte[] key, BigValueRange inputRange, BigValueRange outputRange) {
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
    public BigInteger encrypt(BigInteger plaintext) throws CryptoException {
        Preconditions.checkArgument(initialized, this.getClass().getSimpleName() + " is not initialized");
        Preconditions.checkArgument(
            inputRange.contains(plaintext),
            "Plaintext is not within the input range " + inputRange + ": " + plaintext
        );

        return lazySample(plaintext, inputRange, outputRange);
    }

    private BigInteger lazySample(BigInteger m, BigValueRange rangeD, BigValueRange rangeR) throws CryptoException {
        // LazySample(D, R, m)
        // M ← |D| ; N ← |R|
        BigInteger dSize = rangeD.size();
        BigInteger rSize = rangeR.size();
        // M <= N, otherwise there must exist a plaintext that does not have its corresponding ciphertext
        if (dSize.compareTo(rSize) > 0) {
            throw new CryptoException("rangeD (" + rangeD + ") is larger than rangeR (" + rangeR + ")");
        }

        // d ← min(D) − 1 ; r ← min(R) − 1
        BigInteger d = rangeD.getStart().subtract(BigInteger.ONE);
        BigInteger r = rangeR.getStart().subtract(BigInteger.ONE);
        // y ← r + ⌈N/2⌉
        BigInteger halfN = new BigDecimal(rSize)
            .divide(BigDecimalUtils.TWO, BigDecimalUtils.PRECISION, RoundingMode.CEILING)
            .toBigInteger();
        BigInteger y = r.add(halfN);

        // If |D| = 1
        if (dSize.compareTo(BigInteger.ONE) == 0) {
            // cc ← GetCoins(D, R, 1 || m), where D = 1
            Coins cc = new Coins(key, m.shiftLeft(1).add(BigInteger.ONE).toByteArray());
            // F[D, R, m] ← R and return F[D, R, m]
            return sampleUniform(rangeR, cc);
        }
        // cc ← GetCoins(D, R, 0 || y)
        Coins cc = new Coins(key, y.shiftLeft(1).toByteArray());
        // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
        BigInteger x = sampleHgd(rangeD, rangeR, y, cc);

        // If m ≤ x
        if (m.compareTo(x) <= 0) {
            // D ← {d + 1, ..., x}
            rangeD = new BigValueRange(d.add(BigInteger.ONE), x);
            // R ← {r + 1, ..., y}
            rangeR = new BigValueRange(r.add(BigInteger.ONE), y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new BigValueRange(x.add(BigInteger.ONE), d.add(dSize));
            // R ← {y + 1, ..., r + N}
            rangeR = new BigValueRange(y.add(BigInteger.ONE), r.add(rSize));
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
    public BigInteger decrypt(BigInteger ciphertext) throws CryptoException {
        Preconditions.checkArgument(initialized, this.getClass().getSimpleName() + " is not initialized");
        Preconditions.checkArgument(
            outputRange.contains(ciphertext),
            "Ciphertext is not within the input range " + outputRange + ": " + ciphertext
        );

        return lazySampleInv(ciphertext, inputRange, outputRange);
    }

    private BigInteger lazySampleInv(BigInteger c, BigValueRange rangeD, BigValueRange rangeR) throws CryptoException {
        // M ← |D|; N ← |R|
        BigInteger dSize = rangeD.size();
        BigInteger rSize = rangeR.size();
        // M <= N, otherwise there must exist a plaintext that does not have its corresponding ciphertext
        if (dSize.compareTo(rSize) > 0) {
            throw new CryptoException("rangeD (" + rangeD + ") is larger than rangeR (" + rangeR + ")");
        }

        // d ← min(D) − 1 ; r ← min(R) − 1
        BigInteger d = rangeD.getStart().subtract(BigInteger.ONE);
        BigInteger r = rangeR.getStart().subtract(BigInteger.ONE);
        // y ← r + ⌈N/2⌉
        BigInteger halfN = new BigDecimal(rSize)
            .divide(BigDecimalUtils.TWO, BigDecimalUtils.PRECISION, RoundingMode.CEILING)
            .toBigInteger();
        BigInteger y = r.add(halfN);

        // If |D| = 1
        if (rangeD.size().compareTo(BigInteger.ONE) == 0) {
            // m ← min(D)
            BigInteger plaintext = rangeD.getStart();
            Coins coins = new Coins(key, plaintext.shiftLeft(1).add(BigInteger.ONE).toByteArray());
            BigInteger sampledCiphertext = sampleUniform(rangeR, coins);
            if (sampledCiphertext.compareTo(c) == 0) {
                return plaintext;
            } else {
                // this means the ciphertext is not generated by the plaintext and the given key
                throw new CryptoException("Invalid ciphertext, the ciphertext is not generated by the given key");
            }
        }
        // cc ← GetCoins(D, R, 0 || y)
        Coins coins = new Coins(key, y.shiftLeft(1).toByteArray());
        // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
        BigInteger x = sampleHgd(rangeD, rangeR, y, coins);

        // if c ≤ y
        if (c.compareTo(y) <= 0) {
            // D ← {d + 1, ..., x}
            rangeD = new BigValueRange(d.add(BigInteger.ONE), x);
            // R ← {r + 1, ..., y}
            rangeR = new BigValueRange(r.add(BigInteger.ONE), y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new BigValueRange(x.add(BigInteger.ONE), d.add(dSize));
            // R ← {y + 1, ..., r + N}
            rangeR = new BigValueRange(y.add(BigInteger.ONE), r.add(rSize));
        }
        // Return LazySampleInv(D, R, c)
        return lazySampleInv(c, rangeD, rangeR);
    }

    private BigInteger sampleUniform(BigValueRange inRange, Coins coins) {
        BigValueRange curRange = new BigValueRange(inRange);
        assert curRange.size().compareTo(BigInteger.ZERO) != 0;
        // sample using binary search
        while (curRange.size().compareTo(BigInteger.ONE) > 0) {
            BigInteger mid = curRange.getStart().add(curRange.getEnd()).shiftRight(1);
            boolean bit = coins.next();
            if (!bit) {
                curRange.setEnd(mid);
            } else {
                curRange.setStart(mid.add(BigInteger.ONE));
            }
        }

        assert curRange.size().compareTo(BigInteger.ZERO) != 0;

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
    private BigInteger sampleHgd(BigValueRange rangeM, BigValueRange rangeN, BigInteger sample, Coins coins) {
        BigInteger mSize = rangeM.size();
        BigInteger nSize = rangeN.size();

        assert mSize.compareTo(BigInteger.ZERO) > 0 && nSize.compareTo(BigInteger.ZERO) > 0;
        assert mSize.compareTo(nSize) <= 0;
        assert rangeN.contains(sample);

        // k = n - r
        BigInteger k = sample.subtract(rangeN.getStart()).add(BigInteger.ONE);

        // input size == output size, one-to-one map.
        if (mSize.compareTo(nSize) == 0) {
            return rangeM.getStart().add(k).subtract(BigInteger.ONE);
        }

        BigInteger r = BigHgd.sample(k, mSize, nSize, coins);

        if (r.compareTo(BigInteger.ZERO) == 0) {
            return rangeM.getStart();
        } else if (r.compareTo(mSize) == 0) {
            return rangeM.getEnd();
        } else {
            BigInteger inSample = rangeM.getStart().add(r);
            assert rangeM.contains(inSample);
            return inSample;
        }
    }
}
