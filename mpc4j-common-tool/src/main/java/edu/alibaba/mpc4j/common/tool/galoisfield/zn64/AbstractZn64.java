package edu.alibaba.mpc4j.common.tool.galoisfield.zn64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * abstract Zn64.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
abstract class AbstractZn64 implements Zn64 {
    /**
     * the modulus n
     */
    protected final long n;
    /**
     * the n bit length
     */
    protected final int nBitLength;
    /**
     * the n byte length
     */
    protected final int nByteLength;
    /**
     * the l bit length
     */
    protected final int l;
    /**
     * the l byte length
     */
    protected final int byteL;
    /**
     * 2^l
     */
    protected final long rangeBound;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    AbstractZn64(EnvType envType, long n) {
        assert n > 1 : "n must be greater than 1: " + n;
        this.n = n;
        int log2N = LongUtils.ceilLog2(n);
        if (n == (1L << log2N)) {
            nBitLength = log2N + 1;
        } else {
            nBitLength = log2N;
        }
        nByteLength = CommonUtils.getByteLength(nBitLength);
        l = nBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    @Override
    public long getN() {
        return n;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getElementBitLength() {
        return nBitLength;
    }

    @Override
    public int getElementByteLength() {
        return nByteLength;
    }

    @Override
    public long getRangeBound() {
        return rangeBound;
    }

    @Override
    public long createZero() {
        return 0L;
    }

    @Override
    public long createOne() {
        return 1L;
    }

    @Override
    public boolean isZero(final long a) {
        assert validateElement(a);
        return a == 0L;
    }

    @Override
    public boolean isOne(final long a) {
        assert validateElement(a);
        return a == 1L;
    }

    @Override
    public long createRandom(SecureRandom secureRandom) {
        return LongUtils.randomNonNegative(n, secureRandom);
    }

    @Override
    public long createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return Math.abs(LongUtils.byteArrayToLong(elementByteArray) % n);
    }

    @Override
    public long createNonZeroRandom(SecureRandom secureRandom) {
        long random;
        do {
            random = createRandom(secureRandom);
        } while (random == 0L);
        return random;
    }

    @Override
    public long createNonZeroRandom(byte[] seed) {
        long random;
        byte[] key = BytesUtils.clone(seed);
        do {
            key = kdf.deriveKey(key);
            random = createRandom(key);
        } while (random == 0L);
        return random;
    }

    @Override
    public long createRangeRandom(SecureRandom secureRandom) {
        return LongUtils.randomNonNegative(rangeBound, secureRandom);
    }

    @Override
    public long createRangeRandom(byte[] seed) {
        return createRandom(seed) % rangeBound;
    }

    @Override
    public boolean validateElement(final long a) {
        return a >= 0 && a < n;
    }

    @Override
    public boolean validateNonZeroElement(final long a) {
        return a > 0 && a < n;
    }

    @Override
    public boolean validateRangeElement(final long a) {
        return a >= 0 && a < rangeBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractZn64 that = (AbstractZn64) o;
        // KDF and PRG can be different
        return this.n == that.n;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(n).hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (n = " + n + ")";
    }
}
