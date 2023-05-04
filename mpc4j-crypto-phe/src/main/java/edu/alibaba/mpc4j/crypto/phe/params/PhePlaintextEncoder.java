/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Generalize the code to support other PheEngine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.alibaba.mpc4j.crypto.phe.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.phe.CryptoDecodeException;
import edu.alibaba.mpc4j.crypto.phe.CryptoEncodeException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 明文空间为[1, modulus)的编码方案。原始实现参考：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/StandardEncodingScheme.java
 * </p>
 * 编码标准参见：
 * <p>
 * http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2.3
 * </p>
 * 编码结果表示为value * base^{exponent}，但与科学计数法不同，这里0 <= value < module。
 *
 * @author Dongyao Wu, mpnd, Weiran Liu
 * @date 2017/09/21
 */
public class PhePlaintextEncoder implements PheParams {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhePlaintextEncoder.class);
    /**
     * BigDecimal的编码精度。BigDecimal编码精度永远小于10^-BIG_DECIMAL_ENCODING_PRECISION
     */
    public static final int BIG_DECIMAL_ENCODING_PRECISION = 34;
    /**
     * 尾数长度，即：用2进制表示的小数点后的数字，总共有52位，加上首位的1 <= fraction < 2，共53位
     */
    private static final int DOUBLE_MANTISSA_BITS = 53;
    /**
     * log<sub>2</sub>底数
     */
    private double log2Base;
    /**
     * 编码结果是否为有符号数字
     */
    private boolean signed;
    /**
     * 编码精度，即在给定模数下表示有效数字的比特数量
     */
    private int precision;
    /**
     * 给定模数下所能加密的最大编码值{@code value}
     */
    private BigInteger maxEncoded;
    /**
     * 给定模数下所能加密的最小编码值{@code value}
     */
    private BigInteger minEncoded;
    /**
     * 给定模数下支持编码的最大明文值
     */
    private BigInteger maxSignificand;

    /**
     * 给定模数下支持编码的最小明文值
     */
    private BigInteger minSignificand;
    /**
     * 计算编码值所需的底数
     */
    private int base;
    /**
     * 用大整数表示的底数
     */
    private BigInteger bigIntegerBase;
    /**
     * 模数
     */
    private BigInteger modulus;

    public static PhePlaintextEncoder fromParams(BigInteger modulus, boolean signed, int precision, int base) {
        return PhePlaintextEncoder.create(modulus, signed, precision, base);
    }

    public static PhePlaintextEncoder fromByteArrayList(List<byte[]> byteArrayList) {
        // 解析模数
        BigInteger modulus = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        // 解析符号位
        byte[] signedBytes = byteArrayList.remove(0);
        Preconditions.checkArgument(signedBytes.length == 1);
        byte signedByte = signedBytes[0];
        Preconditions.checkArgument(signedByte == (byte) 0 || signedByte == (byte) 1);
        boolean signed = (signedByte == 1);
        // 解析准确度
        int precision = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 解析底数
        int base = IntUtils.byteArrayToInt(byteArrayList.remove(0));

        return PhePlaintextEncoder.create(modulus, signed, precision, base);
    }

    private static PhePlaintextEncoder create(BigInteger modulus, boolean signed, int precision, int base) {
        PhePlaintextEncoder plaintextEncoder = new PhePlaintextEncoder();
        Preconditions.checkArgument(
            modulus.compareTo(BigInteger.ONE) > 0, "module must be at least equals to 1."
        );
        plaintextEncoder.modulus = modulus;
        plaintextEncoder.signed = signed;
        Preconditions.checkArgument(base >= 2, "Base must be at least equals to 2.");
        plaintextEncoder.base = base;
        plaintextEncoder.bigIntegerBase = BigInteger.valueOf(plaintextEncoder.base);
        plaintextEncoder.log2Base = Math.log(base) / Math.log(2.0);
        Preconditions.checkArgument(
            modulus.bitLength() >= precision && precision >= 1,
            "Precision must be greater than zero and less than or equal to the number of bits in the modulus"
        );
        Preconditions.checkArgument(
            !signed || precision >= 2, "Precision must be greater than 1 when signed is true"
        );
        plaintextEncoder.precision = precision;
        // 已要求modulus.bitLength() >= precision，如果相等，则明文空间为模数；否则明文空间为2^{precision}
        BigInteger encSpace = modulus.bitLength() == precision ? modulus : BigInteger.ONE.shiftLeft(precision);
        if (signed) {
            // 有符号表示时，最大编码值为明文空间最大比特长度首位为0时的编码值，最小编码值为模数减最大编码值
            plaintextEncoder.maxEncoded = encSpace.add(BigInteger.ONE).shiftRight(1).subtract(BigInteger.ONE);
            plaintextEncoder.minEncoded = modulus.subtract(plaintextEncoder.maxEncoded);
            // 有符号表示时，最大明文值为最大编码值，最小明文值为最大编码值的逆
            plaintextEncoder.maxSignificand = plaintextEncoder.maxEncoded;
            plaintextEncoder.minSignificand = plaintextEncoder.maxEncoded.negate();
        } else {
            // 无符号表示时，最大编码值为明文空间减1，最小编码值为0
            plaintextEncoder.maxEncoded = encSpace.subtract(BigInteger.ONE);
            plaintextEncoder.minEncoded = BigInteger.ZERO;
            // 无符号表示时，最大明文值为最大编码值，最小明文值为0
            plaintextEncoder.maxSignificand = plaintextEncoder.maxEncoded;
            plaintextEncoder.minSignificand = BigInteger.ZERO;
        }
        return plaintextEncoder;
    }

    private PhePlaintextEncoder() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(ObjectUtils.objectToByteArray(modulus));
        byte signedByte = signed ? (byte) 1 : (byte) 0;
        byteArrayList.add(new byte[]{signedByte});
        byteArrayList.add(IntUtils.intToByteArray(precision));
        byteArrayList.add(IntUtils.intToByteArray(base));

        return byteArrayList;
    }

    /**
     * 编码{@code BigInteger}。
     *
     * @param value 待编码的{@code BigInteger}。
     * @return 编码结果。
     */
    public PhePlaintext encode(BigInteger value) throws CryptoEncodeException {
        if (value == null) {
            throw new CryptoEncodeException("cannot encode 'null'");
        }
        if (BigIntegerUtils.less(value, BigInteger.ZERO) && !isSigned()) {
            throw new CryptoEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        int exponent = 0;
        // 只有当value mod base == 0时才会做除法
        if (!value.equals(BigInteger.ZERO)) {
            while (value.mod(bigIntegerBase).equals(BigInteger.ZERO)) {
                value = value.divide(bigIntegerBase);
                exponent++;
            }
        }
        if (!isValid(value)) {
            throw new CryptoEncodeException("Input value cannot be encoded.");
        }
        if (value.signum() < 0) {
            value = value.add(modulus);
        }
        return PhePlaintext.fromParams(this, value, exponent);
    }

    /**
     * 把多个短的明文数据按照m_0, m_1, ..., m_t按照m = m_0 || m_1 ||...|| m_t的形式编码时，所能支持的最大编码数量。
     *
     * @param maxBitLength 明文最大比特长度。
     * @return 最大编码数量。
     */
    public int getMaxPacketSize(int maxBitLength) {
        // 打包数量上线依赖于最大有效位
        return (getMaxSignificand().bitLength() - 1) / maxBitLength;
    }

    /**
     * 把多个短的明文数据按照m_0, m_1, ..., m_t按照m = m_0 || m_1 ||...|| m_t的形式编码。
     *
     * @param values       待编码的{@code BigInteger}数组。
     * @param maxBitLength 明文最大比特长度。
     * @return 编码结果。
     */
    public PhePlaintext encode(BigInteger[] values, int maxBitLength) {
        Preconditions.checkArgument(maxBitLength > 0, "最大比特长度 = %s，要求最大比特长度 > 0", maxBitLength);
        Arrays.stream(values).forEach(value -> {
            Preconditions.checkArgument(
                value.compareTo(BigInteger.ZERO) >= 0, "明文值 = %s，要求明文值 >= 0", value
            );
            Preconditions.checkArgument(
                value.bitLength() <= maxBitLength, "明文比特长度 = %s，要求明文比特长度 <= %s",
                value.bitLength(), maxBitLength
            );
        });
        int maxPacketSize = getMaxPacketSize(maxBitLength);
        if (values.length > maxPacketSize) {
            throw new CryptoEncodeException(String.format(
                "打包明文数量 = %s，超过了最大打包数量 = %s", values.length, maxPacketSize
            ));
        }
        // 从数组中第一个元素开始操作
        BigInteger encoded = BigInteger.ZERO.add(values[0]);
        for (int index = 1; index < values.length; index++) {
            encoded = encoded.shiftLeft(maxBitLength).add(values[index]);
        }
        return encode(encoded);
    }

    /**
     * 编码{@code double}。
     *
     * @param value 待编码的{@code double}。
     * @return 编码结果。
     */
    public PhePlaintext encode(double value) throws CryptoEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new CryptoEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new CryptoEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        int exponent = Precision.equals(value, 0, Double.MIN_VALUE) ? 0 : getDoublePrecExponent(value);
        return PhePlaintext.fromParams(this, innerEncode(new BigDecimal(value), exponent), exponent);
    }

    /**
     * 在给定{@code maxExponent}下编码{@code double}。
     *
     * @param value       待编码的{@code double}。
     * @param maxExponent 最大幂指数。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果{@code value}和/或{@code maxExponent}无效。
     */
    public PhePlaintext encode(double value, int maxExponent) throws CryptoEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new CryptoEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new CryptoEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        int exponent = getExponent(getDoublePrecExponent(value), maxExponent);
        return PhePlaintext.fromParams(this, innerEncode(new BigDecimal(value),
            getExponent(getDoublePrecExponent(value), maxExponent)), exponent);
    }

    /**
     * 返回{@code double}值的指数。
     *
     * @param value 待编码的{@code double}值。
     * @return 输入{@code double}值的指数。
     */
    private int getDoublePrecExponent(double value) {
        int binFltExponent = Math.getExponent(value) + 1;
        int binLsbExponent = binFltExponent - DOUBLE_MANTISSA_BITS;
        return (int) Math.floor((double) binLsbExponent / log2Base);
    }

    /**
     * 在给定{@code precision}下编码{@code double}。
     *
     * @param value     待编码的{@code double}。
     * @param precision {@code value}与0的最大差值，{@code precision}的取值应在0到1之间。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果{@code value}和/或{@code maxExponent}无效。
     */
    public PhePlaintext encode(double value, double precision) throws CryptoEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new CryptoEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new CryptoEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        if (precision > 1 || precision <= 0) {
            throw new CryptoEncodeException("Precision must be 10^-i where i > 0.");
        }
        int exponent = getPrecExponent(precision);
        return PhePlaintext.fromParams(this, innerEncode(new BigDecimal(value), exponent), exponent);
    }

    /**
     * 编码{@code long}。
     *
     * @param value 待编码的{@code long}。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果值{@code value}无效。
     */
    public PhePlaintext encode(long value) throws CryptoEncodeException {
        return encode(BigInteger.valueOf(value));
    }

    /**
     * 编码{@code BigDecimal}。编码结果与实际值的最大误差小于10^-{@code precision}。
     *
     * @param value     待编码的{@code BigDecimal}。
     * @param precision 近似编码结果的最大相对误差。即编码再解码后的值与原始值之间的差小于10^-{@code precision}。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果值{@code value}无效。
     */
    public PhePlaintext encode(BigDecimal value, int precision) throws CryptoEncodeException {
        if (value == null) {
            throw new CryptoEncodeException("cannot encode 'null'");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 && !isSigned()) {
            throw new CryptoEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        if (base == 10) {
            BigInteger significant;
            int exp = -value.scale();
            if (value.scale() > 0) {
                significant = value.scaleByPowerOfTen(value.scale()).toBigInteger();
            } else {
                significant = value.unscaledValue();
            }
            if (BigIntegerUtils.greater(significant, maxSignificand)
                || BigIntegerUtils.less(significant, minSignificand)) {
                throw new CryptoEncodeException("Input value cannot be encoded.");
            }
            if (significant.signum() < 0) {
                significant = modulus.add(significant);
            }
            return PhePlaintext.fromParams(this, significant, exp);
        } else {
            if (value.scale() > 0) {
                // we've got a fractional part, epsilon is the max relative error we are willing to accept
                BigDecimal epsilon = new BigDecimal(BigInteger.ONE, precision);
                MathContext mc = new MathContext(precision + 1, RoundingMode.HALF_EVEN);
                int newExponent = (int) Math.floor(
                    BigDecimalUtils.log(value.multiply(epsilon, mc), base)
                );
                BigDecimal newValue = newExponent < 0 ?
                    value.multiply(BigDecimal.valueOf(base).pow(-newExponent, mc), mc)
                    : value.divide(BigDecimal.valueOf(base).pow(newExponent, mc), mc);
                BigInteger significant = newValue.setScale(0, RoundingMode.HALF_EVEN).unscaledValue();
                if (BigIntegerUtils.greater(significant, maxSignificand)
                    || BigIntegerUtils.less(significant, minSignificand)) {
                    throw new CryptoEncodeException("Input value cannot be encoded.");
                }
                if (significant.signum() < 0) {
                    significant = modulus.add(significant);
                }
                return PhePlaintext.fromParams(this, significant, newExponent);
            } else {
                // so we can turn it into a BigInteger without precision loss
                return encode(value.toBigInteger());
            }
        }
    }

    /**
     * 编码{@code BigDecimal}。使用{@code BIG_DECIMAL_ENCODING_PRECISION}定义的默认精度。
     *
     * @param value 待编码的{@code BigDecimal}。
     * @return 编码结果。
     * @throws CryptoEncodeException 如果值{@code value}无效。
     */
    public PhePlaintext encode(BigDecimal value) throws CryptoEncodeException {
        return encode(value, BIG_DECIMAL_ENCODING_PRECISION);
    }

    /**
     * 检查此编码方案是否为有符号数编码。
     *
     * @return 如果此编码方案为有符号数编码，则返回true；否则返回false。
     */
    public boolean isSigned() {
        return signed;
    }

    /**
     * 返回用整数({@code BigInteger})表示的浮点数。
     * - 对于非负数，整数表示为<code>value * base<sup>exponent</sup></code>。
     * - 对于负数，整数表示为<code>modulus + (value * base<sup>exponent</sup>)</code>。
     *
     * @param value    带编码的浮点数。
     * @param exponent 编码数的指数。
     * @return 输入浮点数的整数表示。
     */
    private BigInteger innerEncode(BigDecimal value, int exponent) {
        // Compute BASE^(-exponent)
        BigDecimal bigDecBaseExponent = (new BigDecimal(base)).pow(-exponent, MathContext.DECIMAL128);
        // Compute the integer representation, ie, value * (BASE^-exponent)
        BigInteger bigIntRep = value
            .multiply(bigDecBaseExponent)
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger();
        if (BigIntegerUtils.greater(bigIntRep, maxSignificand) ||
            (value.signum() < 0 && BigIntegerUtils.less(bigIntRep, minSignificand))) {
            throw new CryptoEncodeException("Input value cannot be encoded.");
        }
        if (bigIntRep.signum() < 0) {
            bigIntRep = bigIntRep.add(modulus);
        }

        return bigIntRep;
    }

    /**
     * Given an exponent derived from precision and another exponent denoting the maximum desirable exponent,
     * returns the smaller of the two.
     *
     * @param precExponent denotes the exponent derived from precision.
     * @param maxExponent  denotes the max exponent given.
     * @return the smaller exponent.
     */
    private int getExponent(int precExponent, int maxExponent) {
        return Math.min(precExponent, maxExponent);
    }

    /**
     * Returns an exponent derived from precision. The exponent is calculated as
     * <code>floor(log<sub>base</sub>precision)</code>.
     *
     * @param precision input precision used to generate an exponent.
     * @return exponent for this {@code precision}.
     */
    private int getPrecExponent(double precision) {
        return (int) Math.floor(Math.log(precision) / Math.log(base));
    }

    /**
     * 检查编码方案的一致性。
     *
     * @param encoded 编码数。
     */
    public void checkInputs(PhePlaintext encoded) {
        Preconditions.checkArgument(
            equals(encoded.getPlaintextEncoder()), "PhePlaintext has different PhePlaintextEncoder"
        );
    }

    /**
     * 返回{@code PhePlaintext}的符号。
     *
     * @param number 输入的{@code PhePlaintext}。
     * @return -1、0或1分别表示给定{@code PhePlaintext}为负数、0、正数。
     */
    public int signum(PhePlaintext number) {
        checkInputs(number);
        if (number.getValue().equals(BigInteger.ZERO)) {
            return 0;
        }
        if (!isSigned()) {
            return 1;
        }
        // if this context is signed, then a negative significant is strictly greater than modulus/2.
        BigInteger halfModulus = modulus.shiftRight(1);
        return number.getValue().compareTo(halfModulus) > 0 ? -1 : 1;
    }

    /**
     * 返回模数。
     *
     * @return 模数。
     */
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * 返回底数（base），即编码结果以value * base^(exponent)的形式表示。
     *
     * @return 底数。
     */
    public int getBase() {
        return base;
    }

    /**
     * 返回编码精度。
     *
     * @return 编码精度。
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * 返回{@code modulus}下，{@code PhePlaintext}中{@code value}的最大值。
     * 即：编码得到的{@code PhePlaintextEncoder}中，{@code value}必须小于等于{@code maxEncoded}。
     * - 对于有符号{@code PhePlaintextEncoder}，最大值为明文空间首位为0时所能表示的最大BigInteger。
     * - 对于无符号{@code PhePlaintextEncoder}，最大值为明文空间的最大值。
     * <p>
     * {@code modulus}下，{@code PhePlaintext}中{@code value}的最大值。
     */
    public BigInteger getMaxEncoded() {
        return maxEncoded;
    }

    /**
     * 返回{@code modulus}下，{@code PhePlaintext}中{@code value}的最小值。
     * 即：编码得到的{@code PhePlaintext}中，{@code value}必须大于等于{@code minEncoded}。
     * - 对于有符号{@code PhePlaintextEncoder}，最小值为{@code modulus} - {@code maxEncoded}。
     * - 对于无符号{@code PhePlaintextEncoder}，最小值为0。
     *
     * @return {@code modulus}下，{@code PhePlaintext}中{@code value}的最小值。
     */
    public BigInteger getMinEncoded() {
        return minEncoded;
    }

    /**
     * 返回{@code modulus}下，{@code PhePlaintextEncoder}支持编码的最大明文值。
     * - 对于有符号{@code PhePlaintextEncoder}，最大值为最大编码值。
     * - 对于无符号{@code PhePlaintextEncoder}，最大值为最大编码值。
     *
     * @return {@code modulus}下，{@code PhePlaintext}支持编码的最大明文值。
     */
    public BigInteger getMaxSignificand() {
        return maxSignificand;
    }

    /**
     * 返回{@code modulus}下，{@code PhePlaintextEncoder}支持编码的最小明文值。
     * - 对于有符号{@code PhePlaintextEncoder}，最小值为负最大编码值。
     * - 对于无符号{@code PhePlaintextEncoder}，最小值为0。
     *
     * @return {@code modulus}下，{@code PhePlaintextEncoder}支持编码的最小明文值。
     */
    public BigInteger getMinSignificand() {
        return minSignificand;
    }

    /**
     * 检查{@code PhePlaintextEncoder}的{@code value}是否有效。
     * - 无符号{@code PhePlaintextEncoder}：{@code value} <= {@code maxEncoded}。
     * - 有符号{@code PhePlaintextEncoder}：{@code value} <= {@code maxEncoded}（正数），或 >= {@code minEncoded}（负数）。
     *
     * @param encoded 待检查的编码值。
     * @return 如果有效，则返回true；否则返回false。
     */
    public boolean isValid(PhePlaintext encoded) {
        checkInputs(encoded);
        if (encoded.getValue().compareTo(maxEncoded) <= 0) {
            return true;
        } else if (signed && encoded.getValue().compareTo(minEncoded) >= 0) {
            return true;
        }
        // 编码数据不合法，输出错误信息
        LOGGER.error("是否有符号：{}\ncurEncoded = {}\nminEncoded = {}\nmaxEncoded = {}",
            signed, encoded.getValue(), minEncoded, maxEncoded
        );
        return false;
    }

    /**
     * 验证给定{@code value}是否可以被有效编码为{@code PhePlaintext}。
     *
     * @param value 待编码的{@code value}。
     * @return 如果可以被编码，则返回true；否则返回false。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(BigInteger value) {
        // 要求minSignificand <= value <= maxSignificand
        return BigIntegerUtils.greaterOrEqual(value, getMinSignificand())
            && BigIntegerUtils.lessOrEqual(value, getMaxSignificand());
    }

    /**
     * 解码得到准确的{@code BigInteger}。
     *
     * @param encoded 待解码的{@code PhePlaintext}。
     * @return 解码结果。
     */
    public BigInteger decodeBigInteger(PhePlaintext encoded) {
        checkInputs(encoded);
        BigInteger significand = getSignificand(encoded);
        return significand.multiply(BigInteger.valueOf(base).pow(encoded.getExponent()));
    }

    /**
     * 把编码结果按照m = m_0 || m_1 ||...|| m_t拆分为多个短的原始值。
     *
     * @param encoded      待解码的{@code PhePlaintext}。
     * @param num          解码数量。
     * @param maxBitLength 明文最大比特长度。
     * @return 解码结果。
     */
    public BigInteger[] decodeBigIntegers(PhePlaintext encoded, int num, int maxBitLength) {
        int maxPacketSize = getMaxPacketSize(maxBitLength);
        if (num > maxPacketSize) {
            throw new CryptoDecodeException(String.format("解包明文数量 = %s，超过了最大打包数量 = %s", num, maxPacketSize));
        }
        // 先恢复成BigInteger，再逐个解包
        BigInteger decoded = decodeBigInteger(encoded);
        BigInteger packMod = BigInteger.ZERO.setBit(maxBitLength);
        BigInteger[] bigIntegers = new BigInteger[num];
        // 可以在一个循环内处理完所有过程，处理完最后一个索引值后，shiftRight仍然可以正常执行
        for (int index = num - 1; index >= 0; index--) {
            bigIntegers[index] = decoded.mod(packMod);
            decoded = decoded.shiftRight(maxBitLength);
        }
        return bigIntegers;
    }

    /**
     * 解码得到准确的{@code double}。
     *
     * @param encoded 待解码的{@code PhePlaintext}。
     * @return 解码结果。
     * @throws CryptoDecodeException 如果{@code encoded}无效（为{@link Double#POSITIVE_INFINITY}、
     *                               {@link Double#NEGATIVE_INFINITY}或{@link Double#NaN}。
     */
    public double decodeDouble(PhePlaintext encoded) throws CryptoDecodeException {
        checkInputs(encoded);
        BigInteger significand = getSignificand(encoded);
        BigDecimal exp = BigDecimal.valueOf(base).pow(Math.abs(encoded.getExponent()));
        BigDecimal bigDecoded;
        if (encoded.getExponent() < 0) {
            bigDecoded = new BigDecimal(significand).divide(exp, MathContext.DECIMAL128);
        } else {
            bigDecoded = new BigDecimal(significand).multiply(exp, MathContext.DECIMAL128);
        }
        double decoded = bigDecoded.doubleValue();
        if (Double.isInfinite(decoded) || Double.isNaN(decoded)) {
            throw new CryptoDecodeException("Decoded value cannot be represented as double.");
        }
        return decoded;
    }

    /**
     * 解码得到准确的{@code long}。
     *
     * @param encoded 待解码的{@code PhePlaintext}。
     * @return 解码结果。
     * @throws CryptoDecodeException 如果{@code encoded}无效（大于{@link Long#MAX_VALUE}或小于
     *                               {@link Long#MIN_VALUE}）。
     */
    public long decodeLong(PhePlaintext encoded) throws CryptoDecodeException {
        checkInputs(encoded);
        BigInteger decoded = decodeBigInteger(encoded);
        if (BigIntegerUtils.less(decoded, BigIntegerUtils.LONG_MIN_VALUE) ||
            BigIntegerUtils.greater(decoded, BigIntegerUtils.LONG_MAX_VALUE)) {
            throw new CryptoDecodeException("Decoded value cannot be represented as long.");
        }
        return decoded.longValue();
    }

    /**
     * 使用{@code BIG_DECIMAL_ENCODING_PRECISION}定义的准确性解码得到近似的{@code BigDecimal}。
     *
     * @param encoded 待解码的{@code PhePlaintext}。
     * @return 解码结果。
     * @throws CryptoDecodeException 如果{@code encoded}无法被解码。
     */
    public BigDecimal decodeBigDecimal(PhePlaintext encoded) throws CryptoDecodeException {
        return decodeBigDecimal(encoded, BIG_DECIMAL_ENCODING_PRECISION);
    }

    /**
     * 解码得到近似的{@code BigDecimal}。解码得到的近似结果与真实值的误差小于10^-{@code precision}。
     *
     * @param encoded   待解码的{@code PhePlaintext}。
     * @param precision 解码相对误差的上界。
     * @return 解码结果。
     * @throws CryptoDecodeException 如果{@code encoded}无法被解码。
     */
    public BigDecimal decodeBigDecimal(PhePlaintext encoded, int precision) throws CryptoDecodeException {
        checkInputs(encoded);
        BigInteger significant = getSignificand(encoded);
        if (base == 10) {
            return new BigDecimal(significant, -encoded.getExponent());
        }
        MathContext mc = new MathContext(precision + 1, RoundingMode.HALF_EVEN);
        BigDecimal exp = BigDecimal.valueOf(base).pow(encoded.getExponent(), mc);
        return exp.multiply(new BigDecimal(significant), mc);
    }

    /**
     * 返回解码得到的{@code PhePlaintext}。
     * - 如果解码得到的值小于等于{@code maxEncoded}，则返回此值。
     * - 如果{@code PhePlaintextEncoder}是有符号编码，且解码得到的值小于等于{@code minEncoded}，返回解码得到的值减{@code modulus}。
     * - 否则，{@code significand}越界，抛出DecodeException。
     *
     * @param encoded 输入的{@code PhePlaintext}。
     * @return {@code PhePlaintext}的有效数字。
     * @throws CryptoDecodeException 如果解码得到的值大于模数{@code modulus}。
     */
    private BigInteger getSignificand(PhePlaintext encoded) throws CryptoDecodeException {
        checkInputs(encoded);
        final BigInteger value = encoded.getValue();
        if (value.compareTo(modulus) > 0) {
            throw new CryptoDecodeException("The significand of the encoded number is corrupted");
        }
        // Non-negative
        if (value.compareTo(maxEncoded) <= 0) {
            return value;
        }
        // Negative - note that negative encoded numbers are greater than
        // non-negative encoded numbers and hence minEncoded > maxEncoded
        if (signed && value.compareTo(minEncoded) >= 0) {
            return value.subtract(modulus);
        }
        throw new CryptoDecodeException("Detected overflow");
    }

    /**
     * 当使用相同的{@code base}但使用不同的{@code exponent}重新编码得到{@code PhePlaintext}时，所需要的重放缩系数。
     * 重放缩系数等于<code>base</code><sup>expDiff</sup>。
     *
     * @param expDiff 新重放缩系数所关联的指数。
     * @return 重放缩系数。
     */
    public BigInteger getRescalingFactor(int expDiff) {
        return (BigInteger.valueOf(base)).pow(expDiff);
    }

    /**
     * 返回编码方案是否为全精度编码。
     *
     * @return 如果编码方案为全精度，返回true；否则返回false。
     */
    public boolean isFullPrecision() {
        return getPrecision() == getModulus().bitLength();
    }

    /**
     * 将{@code PhePlaintext}的指数降低为{@code newExp}。
     *
     * @param operand 输入。
     * @param newExp  新的{@code exponent}，必须小于当前的{@code exponent}。
     * @return {@code exponent}等于{@code newExp}，且表示相同值的{@code PhePlaintext}。
     * @throws IllegalArgumentException 如果{@code newExp}大于{@code PhePlaintext}当前的{@code exponent}。
     */
    public PhePlaintext decreaseExponentTo(PhePlaintext operand, int newExp) {
        checkInputs(operand);
        Preconditions.checkArgument(isValid(operand));
        int exponent = operand.getExponent();
        if (newExp > exponent) {
            throw new IllegalArgumentException("New exponent: " + newExp +
                "should be more negative than old exponent: " + exponent + ".");
        }

        int expDiff = exponent - newExp;
        BigInteger bigFactor = getRescalingFactor(expDiff);
        BigInteger newEnc = operand.getValue().multiply(bigFactor).mod(modulus);
        return PhePlaintext.fromParams(this, newEnc, newExp);
    }

    /**
     * {@code PhePlaintext} + {@code PhePlaintext}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 相加结果。
     */
    public PhePlaintext add(PhePlaintext operand, PhePlaintext other) {
        checkInputs(operand);
        checkInputs(other);
        // 当执行减法操作时，被加数有可能无法通过验证，但仍然可以正常计算，最后一步验证有效性即可
        Preconditions.checkArgument(isValid(operand));
        BigInteger value1 = operand.getValue();
        BigInteger value2 = other.getValue();
        int exponent1 = operand.getExponent();
        int exponent2 = other.getExponent();
        if (exponent1 > exponent2) {
            value1 = value1.multiply(getRescalingFactor(exponent1 - exponent2));
            exponent1 = exponent2;
        } else if (exponent1 < exponent2) {
            value2 = value2.multiply(getRescalingFactor(exponent2 - exponent1));
        }
        final BigInteger result = value1.add(value2).mod(modulus);

        return PhePlaintext.fromParams(this, result, exponent1);
    }

    /**
     * {@code PhePlaintext} + {@code BigInteger}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    public PhePlaintext add(PhePlaintext operand, BigInteger other) {
        return add(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} + {@code double}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    public PhePlaintext add(PhePlaintext operand, double other) {
        return add(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} + {@code long}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    public PhePlaintext add(PhePlaintext operand, long other) {
        return add(operand, encode(other));
    }

    /**
     * 返回{@code PhePlaintext}在模数{@code modulus}的加法逆元。
     * 注意：这里的加法逆元可能不是合法的编码数（即：PhePlaintextEncoder.isValid()会返回{@code false}）。此运算只适用于模域下的运算。
     *
     * @param operand 输入。
     * @return 输入的加法逆元。
     */
    public PhePlaintext additiveInverse(PhePlaintext operand) {
        checkInputs(operand);
        // 0的加法逆元为0
        if (operand.getValue().signum() == 0) {
            return operand;
        }
        final BigInteger value1 = operand.getValue();
        final BigInteger result = modulus.subtract(value1).mod(modulus);
        return PhePlaintext.fromParams(this, result, operand.getExponent());
    }

    /**
     * {@code PhePlaintext} - {@code PhePlaintext}。
     *
     * @param operand 被减数。
     * @param other   减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext operand, PhePlaintext other) {
        return add(operand, additiveInverse(other));
    }

    /**
     * {@code PhePlaintext} - {@code BigInteger}。
     *
     * @param operand 被减数。
     * @param other   减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext operand, BigInteger other) {
        return subtract(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} - {@code double}。
     *
     * @param operand 被减数。
     * @param other   减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext operand, double other) {
        return subtract(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} - {@code long}。
     *
     * @param operand 被减数。
     * @param other   减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext operand, long other) {
        // NOTE it would be nice to do add(context.encode(-other)) however this
        //      would fail if other == Long.MIN_VALUE since it has no
        //      corresponding positive value.
        return subtract(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} * {@code PhePlaintext}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, PhePlaintext other) {
        checkInputs(operand);
        checkInputs(other);
        final BigInteger value1 = operand.getValue();
        final BigInteger value2 = other.getValue();
        final BigInteger result = value1.multiply(value2).mod(modulus);
        final int exponent = operand.getExponent() + other.getExponent();
        return PhePlaintext.fromParams(this, result, exponent);
    }

    /**
     * {@code PhePlaintext} * {@code BigInteger}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, BigInteger other) {
        return multiply(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} * {@code double}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, double other) {
        return multiply(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} * {@code long}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, long other) {
        return multiply(operand, encode(other));
    }

    /**
     * {@code PhePlaintext} / {@code double}。
     *
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    public PhePlaintext divide(PhePlaintext operand, double other) {
        return multiply(operand, encode(1.0 / other));
    }

    /**
     * {@code PhePlaintext} / {@code long}。
     *
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    public PhePlaintext divide(PhePlaintext operand, long other) {
        return multiply(operand, encode(1.0 / other));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != PhePlaintextEncoder.class) {
            return false;
        }
        PhePlaintextEncoder encoding = (PhePlaintextEncoder) o;
        return new EqualsBuilder()
            .append(this.signed, encoding.signed)
            .append(this.precision, encoding.precision)
            .append(this.base, encoding.base)
            .append(this.modulus, encoding.modulus)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(signed)
            .append(precision)
            .append(base)
            .append(modulus)
            .hashCode();
    }
}
