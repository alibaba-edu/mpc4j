package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 应用JDK实现的Zp有限域。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
class JdkZp implements Zp {
    /**
     * 素数
     */
    private final BigInteger prime;
    /**
     * 素数比特长度
     */
    private final int primeBitLength;
    /**
     * 素数字节长度
     */
    private final int primeByteLength;
    /**
     * l比特长度
     */
    private final int l;
    /**
     * l字节长度
     */
    private final int byteL;
    /**
     * 最大有效元素：2^l
     */
    private final BigInteger rangeBound;
    /**
     * KDF
     */
    private final Kdf kdf;
    /**
     * 伪随机数生成器
     */
    private final Prg prg;

    /**
     * 素数为输入的构造函数。
     *
     * @param envType 环境类型。
     * @param prime   素数。
     */
    public JdkZp(EnvType envType, BigInteger prime) {
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        this.prime = prime;
        primeBitLength = prime.bitLength();
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        l = primeBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, primeByteLength);
    }

    /**
     * 有效比特为输入的构造函数。
     *
     * @param envType 环境类型。
     * @param l       有效比特长度。
     */
    public JdkZp(EnvType envType, int l) {
        prime = ZpManager.getPrime(l);
        primeBitLength = prime.bitLength();
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    @Override
    public ZpFactory.ZpType getZpType() {
        return ZpFactory.ZpType.JDK;
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
    public int getPrimeBitLength() {
        return primeBitLength;
    }

    @Override
    public int getPrimeByteLength() {
        return primeByteLength;
    }

    @Override
    public BigInteger getRangeBound() {
        return rangeBound;
    }

    @Override
    public BigInteger module(final BigInteger a) {
        return a.mod(prime);
    }

    @Override
    public BigInteger add(final BigInteger a, final BigInteger b) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        assert validateElement(b) : "b is not a valid element in Zp: " + b;
        return a.add(b).mod(prime);
    }

    @Override
    public BigInteger neg(final BigInteger a) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        if (a.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            return prime.subtract(a);
        }
    }

    @Override
    public BigInteger sub(final BigInteger a, final BigInteger b) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        assert validateElement(b) : "b is not a valid element in Zp: " + b;
        return a.subtract(b).mod(prime);
    }

    @Override
    public BigInteger mul(final BigInteger a, final BigInteger b) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        assert validateElement(b) : "b is not a valid element in Zp: " + b;
        return a.multiply(b).mod(prime);
    }

    @Override
    public BigInteger div(final BigInteger a, final BigInteger b) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        assert validateNonZeroElement(b) : "a is not a valid non-zero element in Zp: " + a;
        return a.multiply(BigIntegerUtils.modInverse(b, prime)).mod(prime);
    }

    @Override
    public BigInteger inv(final BigInteger a) {
        assert validateNonZeroElement(a) : "a is not a valid non-zero element in Zp: " + a;
        return BigIntegerUtils.modInverse(a, prime);
    }

    @Override
    public BigInteger mulPow(final BigInteger a, final BigInteger b) {
        assert validateElement(a) : "a is not a valid element in Zp: " + a;
        assert validateElement(b) : "b is not a valid element in Zp: " + b;
        return BigIntegerUtils.modPow(a, b, prime);
    }

    @Override
    public BigInteger innerProduct(final BigInteger[] zpVector, final boolean[] binaryVector) {
        assert zpVector.length == binaryVector.length
            : "Zp vector length must be equal to binary vector length = " + binaryVector.length + ": " + zpVector.length;
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < zpVector.length; i++) {
            if (binaryVector[i]) {
                value = add(value, zpVector[i]);
            }
        }
        return value;
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
        BigInteger random = BigInteger.ZERO;
        while (random.equals(BigInteger.ZERO)) {
            random = BigIntegerUtils.randomPositive(prime, secureRandom);
        }
        return random;
    }

    @Override
    public BigInteger createNonZeroRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        BigInteger random = BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).mod(prime);
        while (random.equals(BigInteger.ZERO)) {
            // 如果恰巧为0，则迭代种子
            key = kdf.deriveKey(key);
            elementByteArray = prg.extendToBytes(key);
            random = BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).mod(prime);
        }
        return random;
    }

    @Override
    public BigInteger createRangeRandom(SecureRandom secureRandom) {
        return BigIntegerUtils.randomNonNegative(rangeBound, secureRandom);
    }

    @Override
    public BigInteger createRangeRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).mod(rangeBound);
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
}
