package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * abstract Zp64.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
abstract class AbstractZp64 implements Zp64 {
    /**
     * the prime
     */
    protected final long prime;
    /**
     * the prime bit length
     */
    protected final int primeBitLength;
    /**
     * the prime byte length
     */
    protected final int primeByteLength;
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

    AbstractZp64(EnvType envType, long prime) {
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH)
            : "input prime is not a prime: " + prime;
        this.prime = prime;
        primeBitLength = LongUtils.ceilLog2(prime);
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        l = primeBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    public AbstractZp64(EnvType envType, int l) {
        prime = Zp64Manager.getPrime(l);
        primeBitLength = LongUtils.ceilLog2(prime);
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    @Override
    public long getPrime() {
        return prime;
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
        return primeBitLength;
    }

    @Override
    public int getElementByteLength() {
        return primeByteLength;
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
        return LongUtils.randomNonNegative(prime, secureRandom);
    }

    @Override
    public long createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return Math.abs(LongUtils.byteArrayToLong(elementByteArray) % prime);
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
        return a >= 0 && a < prime;
    }

    @Override
    public boolean validateNonZeroElement(final long a) {
        return a > 0 && a < prime;
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
        AbstractZp64 that = (AbstractZp64) o;
        // KDF and PRG can be different
        return this.prime == that.prime;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(prime).hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (p = " + prime + ")";
    }
}
