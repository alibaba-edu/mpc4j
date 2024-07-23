package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * abstract Zl64.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
abstract class AbstractZl64 implements Zl64 {
    /**
     * the l bit length
     */
    protected final int l;
    /**
     * the l byte length
     */
    protected final int byteL;
    /**
     * mask
     */
    protected final long mask;
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

    AbstractZl64(EnvType envType, int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, Long.SIZE);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        if (l == Long.SIZE) {
            mask = 0xFFFFFFFFFFFFFFFFL;
        } else if (l == Long.SIZE - 1) {
            mask = 0x7FFFFFFFFFFFFFFFL;
        } else {
            mask = (1L << l) - 1;
        }
        // when l = 64, range_bound becomes 0
        rangeBound = (l == Long.SIZE) ? 0 : (1L << l);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
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
        return l;
    }

    @Override
    public int getElementByteLength() {
        return byteL;
    }

    @Override
    public long getRangeBound() {
        return rangeBound;
    }

    @Override
    public long getMask() {
        return mask;
    }

    @Override
    public long module(final long a) {
        return a & mask;
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
        return secureRandom.nextLong() & mask;
    }

    @Override
    public long createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return LongUtils.byteArrayToLong(elementByteArray) & mask;
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
        return createRandom(secureRandom);
    }

    @Override
    public long createRangeRandom(byte[] seed) {
        return createRandom(seed);
    }

    @Override
    public boolean validateElement(final long a) {
        if (l == Long.SIZE) {
            return true;
        } else {
            return a >= 0 && Long.compareUnsigned(a, rangeBound) < 0;
        }
    }

    @Override
    public boolean validateNonZeroElement(final long a) {
        if (l == Long.SIZE) {
            return a != 0;
        } else {
            return a > 0 && Long.compareUnsigned(a, rangeBound) < 0;
        }
    }

    @Override
    public boolean validateRangeElement(final long a) {
        return validateElement(a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractZl64 that = (AbstractZl64) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.getL() == that.getL();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(Zl64.class.getSimpleName()).append(l).hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (l = " + l + ")";
    }
}
