package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.PheEncodeException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * PHE public key. The public key includes:
 * <ul>
 *     <li>plaintext modulus</li>
 *     <li>ciphertext modulus</li>
 *     <li>plaintext encoder (the related methods are also accessible directly from the public key)</li>
 * </ul>
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PhePublicKey extends PheKeyParams {

    /**
     * Gets the plaintext modulus.
     *
     * @return the plaintext modulus.
     */
    BigInteger getPlaintextModulus();

    /**
     * Gets the ciphertext modulus.
     *
     * @return the ciphertext modulus.
     */
    BigInteger getCiphertextModulus();

    /**
     * Gets the plaintext encoder.
     *
     * @return the plaintext encoder.
     */
    PhePlaintextEncoder getPlaintextEncoder();

    /**
     * Gets base used for plaintext encoding.
     * Note that we encode the plaintext in the form <code>value * base<sup>exponent</sup></code>.
     *
     * @return base.
     */
    default int getBase() {
        return getPlaintextEncoder().getBase();
    }

    /**
     * Gets the precision.
     *
     * @return the precision.
     */
    default int getPrecision() {
        return getPlaintextEncoder().getPrecision();
    }

    /**
     * Returns if the encoding allows signed numbers.
     *
     * @return true if the encoding allows signed numbers, false otherwise.
     */
    default boolean isSigned() {
        return getPlaintextEncoder().isSigned();
    }

    /**
     * Gets the maximal encoded <code>value</code> for the encoding. That is, <code>value</code> in plaintext must be
     * less than or equal to <code>maxEncoded</code>. Given the plaintext <code>modulus</code>:
     * <ul>
     *     <li>For signed encoding, <code>maxEncoded</code> is the maximal <code>BigInteger</code> in the plaintext
     *     space where the first bit is 0.</li>
     *     <li>For unsigned encoding, <code>maxEncoded = modulus - 1.</code></li>
     * </ul>
     *
     * @return the maximal encoded <code>value</code> for the encoding.
     */
    default BigInteger getMaxEncoded() {
        return getPlaintextEncoder().getMaxEncoded();
    }

    /**
     * Gets the minimal encoded {@code value} for the encoding. That is, <code>value</code> in plaintext must be
     * greater than or equal to {@code minEncoded}.
     * <ul>
     *     <li>For signed encoding, <code>minEncoded = modulus - maxEncoded</code>.</li>
     *     <li>For unsigned encoding, <code>minEncoded = 1</code>.</li>
     * </ul>
     *
     * @return the minimal encoded {@code value} for the encoding.
     */
    default BigInteger getMinEncoded() {
        return getPlaintextEncoder().getMinEncoded();
    }

    /**
     * Gets the maximum plaintext that can be encoded.
     * <p>
     * - 对于有符号半同态加密明文，最大值为最大编码值。
     * - 对于无符号半同态加密明文，最大值为最大编码值。
     *
     * @return 公钥支持的最大明文值。
     */
    default BigInteger getMaxSignificand() {
        return getPlaintextEncoder().getMaxSignificand();
    }

    /**
     * 返回公钥支持编码的最小明文值。
     * <p>
     * - 对于有符号半同态加密明文，最小值为负最大编码值。
     * - 对于无符号半同态加密明文，最小值为0。
     *
     * @return 公钥支持编码的最小明文值。
     */
    default BigInteger getMinSignificand() {
        return getPlaintextEncoder().getMinSignificand();
    }

    /**
     * 返回公钥是否为全精度编码。
     *
     * @return 如果为全精度，返回true；否则返回false。
     */
    default boolean isFullPrecision() {
        return getPlaintextEncoder().isFullPrecision();
    }

    /**
     * 编码{@code BigInteger}。
     *
     * @param value 待编码的{@code BigInteger}。
     * @return 编码结果。
     */
    default PhePlaintext encode(BigInteger value) {
        return getPlaintextEncoder().encodeBigInteger(value);
    }

    /**
     * 编码{@code double}。
     *
     * @param value 待编码的{@code double}。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value) {
        return getPlaintextEncoder().encodeDouble(value);
    }

    /**
     * 在给定{@code maxExponent}下编码{@code double}。
     *
     * @param value       待编码的{@code double}。
     * @param maxExponent 最大幂指数。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value, int maxExponent) {
        return getPlaintextEncoder().encodeDouble(value, maxExponent);
    }

    /**
     * 在给定{@code precision}下编码{@code double}。
     *
     * @param value     待编码的{@code double}。
     * @param precision {@code value}与0的最大差值，{@code precision}的取值应在0到1之间。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value, double precision) {
        return getPlaintextEncoder().encodeDouble(value, precision);
    }

    /**
     * 编码{@code long}。
     *
     * @param value 待编码的{@code long}。
     * @return 编码结果。
     */
    default PhePlaintext encode(long value) {
        return getPlaintextEncoder().encodeLong(value);
    }

    /**
     * 编码{@code BigDecimal}。编码结果与实际值的最大误差小于10^-{@code precision}。
     *
     * @param value     待编码的{@code BigDecimal}。
     * @param precision 近似编码结果的最大相对误差。即编码再解码后的值与原始值之间的差小于10^-{@code precision}。
     * @return 编码结果。
     */
    default PhePlaintext encode(BigDecimal value, int precision) {
        return getPlaintextEncoder().encodeBigDecimal(value, precision);
    }

    /**
     * 编码{@code BigDecimal}。使用{@code BIG_DECIMAL_ENCODING_PRECISION}定义的默认精度。
     *
     * @param value 待编码的{@code BigDecimal}。
     * @return 编码结果。
     * @throws PheEncodeException 如果编码出现异常。
     */
    default PhePlaintext encode(BigDecimal value) throws PheEncodeException {
        return getPlaintextEncoder().encodeBigDecimal(value);
    }
}
