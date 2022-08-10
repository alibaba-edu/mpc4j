package edu.alibaba.mpc4j.crypto.phe.params;

import edu.alibaba.mpc4j.crypto.phe.CryptoEncodeException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 半同态加密公钥。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PhePublicKey extends PheKeyParams {
    /**
     * 返回模数编码方案。
     *
     * @return 模数编码方案。
     */
    PhePlaintextEncoder getPlaintextEncoder();

    /**
     * 返回明文模数。
     *
     * @return 明文模数。
     */
    BigInteger getModulus();

    /**
     * 返回密文模数。
     *
     * @return 密文模数。
     */
    BigInteger getCiphertextModulus();

    /**
     * 返回底数（base），即编码结果以value * base^(exponent)的形式表示。
     *
     * @return 底数。
     */
    default int getBase() {
        return getPlaintextEncoder().getBase();
    }

    /**
     * 返回编码精度。
     *
     * @return 编码精度。
     */
    default int getPrecision() {
        return getPlaintextEncoder().getPrecision();
    }

    /**
     * 返回公钥是否支持有符号数运算。
     *
     * @return 如果支持有符号数运算，则返回true；否则返回false。
     */
    default boolean isSigned() {
        return getPlaintextEncoder().isSigned();
    }

    /**
     * 返回半同态加密明文中{@code value}的最大值{@code maxEncoded}。
     * 即：编码得到的半同态加密明文中，{@code value}必须小于等于{@code maxEncoded}。
     * - 对于有符号半同态加密明文，最大值为明文空间首位为0时所能表示的最大BigInteger。
     * - 对于无符号半同态加密明文，最大值为明文空间的最大值。
     * <p>
     * {@code modulus}下，编码结果{@code EncodedNumber}中{@code value}的最大值。
     *
     * @return 半同态加密明文中{@code value}的最大值{@code maxEncoded}.
     */
    default BigInteger getMaxEncoded() {
        return getPlaintextEncoder().getMaxEncoded();
    }

    /**
     * 返回半同态加密明文中{@code value}的最小值{@code minEncoded}。
     * 即：编码得到的半同态加密明文中，{@code value}必须大于等于{@code minEncoded}。
     * <p>
     * - 对于有符号半同态加密明文，最小值为{@code modulus} - {@code maxEncoded}。
     * - 对于无符号半同态加密明文，最小值为0。
     *
     * @return {@code modulus}下，{@code PhePlaintext}中{@code value}的最小值。
     */
    default BigInteger getMinEncoded() {
        return getPlaintextEncoder().getMinEncoded();
    }

    /**
     * 返回公钥支持的最大明文值。
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
        return getPlaintextEncoder().encode(value);
    }

    /**
     * 编码{@code double}。
     *
     * @param value 待编码的{@code double}。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value) {
        return getPlaintextEncoder().encode(value);
    }

    /**
     * 在给定{@code maxExponent}下编码{@code double}。
     *
     * @param value       待编码的{@code double}。
     * @param maxExponent 最大幂指数。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value, int maxExponent) {
        return getPlaintextEncoder().encode(value, maxExponent);
    }

    /**
     * 在给定{@code precision}下编码{@code double}。
     *
     * @param value     待编码的{@code double}。
     * @param precision {@code value}与0的最大差值，{@code precision}的取值应在0到1之间。
     * @return 编码结果。
     */
    default PhePlaintext encode(double value, double precision) {
        return getPlaintextEncoder().encode(value, precision);
    }

    /**
     * 编码{@code long}。
     *
     * @param value 待编码的{@code long}。
     * @return 编码结果。
     */
    default PhePlaintext encode(long value) {
        return getPlaintextEncoder().encode(value);
    }

    /**
     * 编码{@code BigDecimal}。编码结果与实际值的最大误差小于10^-{@code precision}。
     *
     * @param value     待编码的{@code BigDecimal}。
     * @param precision 近似编码结果的最大相对误差。即编码再解码后的值与原始值之间的差小于10^-{@code precision}。
     * @return 编码结果。
     */
    default PhePlaintext encode(BigDecimal value, int precision) {
        return getPlaintextEncoder().encode(value, precision);
    }

    /**
     * 编码{@code BigDecimal}。使用{@code BIG_DECIMAL_ENCODING_PRECISION}定义的默认精度。
     *
     * @param value 待编码的{@code BigDecimal}。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果编码出现异常。
     */
    default PhePlaintext encode(BigDecimal value) throws CryptoEncodeException {
        return getPlaintextEncoder().encode(value);
    }
}
