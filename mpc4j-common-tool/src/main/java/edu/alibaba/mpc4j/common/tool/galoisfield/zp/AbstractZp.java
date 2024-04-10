package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
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
 * abstract Zp.
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
abstract class AbstractZp implements Zp {
    /**
     * the prime
     */
    protected final BigInteger prime;
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

    AbstractZp(EnvType envType, BigInteger prime) {
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        this.prime = prime;
        primeBitLength = prime.bitLength();
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        l = primeBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        andRangeBound = rangeBound.subtract(BigInteger.ONE);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, primeByteLength);
    }

    AbstractZp(EnvType envType, int l) {
        prime = ZpManager.getPrime(l);
        primeBitLength = prime.bitLength();
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        andRangeBound = rangeBound.subtract(BigInteger.ONE);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, primeByteLength);
    }

    @Override
    public BigInteger getPrime() {
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
        return BigIntegerUtils.randomNonNegative(prime, secureRandom);
    }

    @Override
    public BigInteger createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).mod(prime);
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
        return BigIntegerUtils.greaterOrEqual(a, BigInteger.ZERO) && BigIntegerUtils.less(a, prime);
    }

    @Override
    public boolean validateNonZeroElement(final BigInteger a) {
        return BigIntegerUtils.greater(a, BigInteger.ZERO) && BigIntegerUtils.less(a, prime);
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
        AbstractZp that = (AbstractZp) o;
        // KDF and PRG can be different
        return this.prime.equals(that.prime);
    }

    @Override
    public int hashCode() {
        return prime.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (p = " + prime + ")";
    }
}
