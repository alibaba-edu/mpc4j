package edu.alibaba.mpc4j.common.tool.galoisfield.zn;

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
 * abstract Zn.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
abstract class AbstractZn implements Zn {
    /**
     * module n
     */
    protected final BigInteger n;
    /**
     * n bit length
     */
    protected final int nBitLength;
    /**
     * n byte length
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
    protected final BigInteger rangeBound;
    /**
     * 2^l - 1
     */
    private final BigInteger andRangeBound;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    AbstractZn(EnvType envType, BigInteger n) {
        assert BigIntegerUtils.greater(n, BigInteger.ONE) : "n must be greater than 1: " + n;
        this.n = n;
        nBitLength = n.bitLength();
        nByteLength = CommonUtils.getByteLength(nBitLength);
        l = nBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        andRangeBound = rangeBound.subtract(BigInteger.ONE);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, nByteLength);
    }

    @Override
    public BigInteger getN() {
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
        return BigIntegerUtils.randomNonNegative(n, secureRandom);
    }

    @Override
    public BigInteger createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).mod(n);
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
        return new BigInteger(l, secureRandom);
    }

    @Override
    public BigInteger createRangeRandom(byte[] seed) {
        return createRandom(seed).and(andRangeBound);
    }

    @Override
    public boolean validateElement(final BigInteger a) {
        return BigIntegerUtils.greaterOrEqual(a, BigInteger.ZERO) && BigIntegerUtils.less(a, n);
    }

    @Override
    public boolean validateNonZeroElement(final BigInteger a) {
        return BigIntegerUtils.greater(a, BigInteger.ZERO) && BigIntegerUtils.less(a, n);
    }

    @Override
    public boolean validateRangeElement(final BigInteger a) {
        return BigIntegerUtils.greaterOrEqual(a, BigInteger.ZERO) && BigIntegerUtils.less(a, rangeBound);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractZn that = (AbstractZn) o;
        // KDF and PRG can be different
        return this.n.equals(that.n);
    }

    @Override
    public int hashCode() {
        return n.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (n = " + n + ")";
    }
}
