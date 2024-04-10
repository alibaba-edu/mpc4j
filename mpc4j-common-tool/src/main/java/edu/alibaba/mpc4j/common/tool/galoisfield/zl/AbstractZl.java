package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * abstract Zl.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
abstract class AbstractZl implements Zl {
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
    protected final BigInteger rangeBound;
    /**
     * 2^l - 1
     */
    protected final BigInteger andRangeBound;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    AbstractZl(EnvType envType, int l) {
        assert l > 0 : "l must be greater than 0";
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        andRangeBound = rangeBound.subtract(BigInteger.ONE);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, byteL);
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
    public BigInteger getRangeBound() {
        return rangeBound;
    }

    @Override
    public BigInteger createZero() {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger createOne() {
        return BigInteger.ONE;
    }

    @Override
    public boolean isZero(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ZERO);
    }

    @Override
    public boolean isOne(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ONE);
    }

    @Override
    public BigInteger createRandom(SecureRandom secureRandom) {
        return new BigInteger(l, secureRandom);
    }

    @Override
    public BigInteger createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).and(andRangeBound);
    }

    @Override
    public BigInteger createNonZeroRandom(SecureRandom secureRandom) {
        BigInteger random;
        do {
            random = createRandom(secureRandom);
        } while (random.equals(BigInteger.ZERO));
        return random;
    }

    @Override
    public BigInteger createNonZeroRandom(byte[] seed) {
        BigInteger random;
        byte[] key = BytesUtils.clone(seed);
        do {
            key = kdf.deriveKey(key);
            random = createRandom(key);
        } while (random.equals(BigInteger.ZERO));
        return random;
    }

    @Override
    public BigInteger createRangeRandom(SecureRandom secureRandom) {
        return createRandom(secureRandom);
    }

    @Override
    public BigInteger createRangeRandom(byte[] seed) {
        return createRandom(seed);
    }

    @Override
    public boolean validateElement(final BigInteger a) {
        return a.signum() >= 0 && a.bitLength() <= l;
    }

    @Override
    public boolean validateNonZeroElement(final BigInteger a) {
        return a.signum() > 0 && a.bitLength() <= l;
    }

    @Override
    public boolean validateRangeElement(final BigInteger a) {
        return a.signum() >= 0 && a.bitLength() <= l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractZl that = (AbstractZl) o;
        // KDF and PRG can be different
        return this.l == that.l;
    }

    @Override
    public int hashCode() {
        return "Zl".hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (l = " + l + ")";
    }
}
