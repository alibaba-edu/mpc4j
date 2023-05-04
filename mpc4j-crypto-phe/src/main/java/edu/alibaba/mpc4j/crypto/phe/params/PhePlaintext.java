/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Move operations into PheEngine implementations.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.alibaba.mpc4j.crypto.phe.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * 半同态加密明文。
 *
 * @author Brian Thorne, Wilko Henecka, Dongyao Wu, mpnd, Max Ott, Weiran Liu
 * @date 2017/09/21
 */
public class PhePlaintext implements PheParams {
    /**
     * 模数编码方案
     */
    private PhePlaintextEncoder plaintextEncoder;
    /**
     * 编码数的值，必须为一个非负整数，且取值小于模数
     */
    private BigInteger value;
    /**
     * 编码数的幂
     */
    private int exponent;

    /**
     * 构造{@code PhePlaintext}，其中{@code value}必须为一个非负整数，且小于模数。
     *
     * @param plaintextEncoder 编码方案。
     * @param value            {@code PhePlaintext}的值，必须为一个非负整数，且小于模数。
     * @param exponent         {@code PhePlaintext}的幂。
     */
    public static PhePlaintext fromParams(PhePlaintextEncoder plaintextEncoder, BigInteger value, int exponent) {
        return PhePlaintext.create(plaintextEncoder, value, exponent);
    }

    public static PhePlaintext fromByteArrayList(PhePlaintextEncoder plaintextEncoder, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 2);
        BigInteger value = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        int exponent = IntUtils.byteArrayToInt(byteArrayList.remove(0));

        return PhePlaintext.create(plaintextEncoder, value, exponent);
    }

    private static PhePlaintext create(PhePlaintextEncoder plaintextEncoder, BigInteger value, int exponent) {
        Preconditions.checkNotNull(plaintextEncoder, "PhePlaintextEncoder must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkArgument(value.signum() >= 0, "value must be non-negative");
        Preconditions.checkArgument(
            value.compareTo(plaintextEncoder.getModulus()) < 0, "value must be less than modulus"
        );
        PhePlaintext phePlaintext = new PhePlaintext();
        phePlaintext.plaintextEncoder = plaintextEncoder;
        phePlaintext.value = value;
        phePlaintext.exponent = exponent;

        return phePlaintext;
    }

    private PhePlaintext() {
        // empty
    }

    /**
     * 返回模数编码方案。
     *
     * @return 模数编码方案。
     */
    public PhePlaintextEncoder getPlaintextEncoder() {
        return plaintextEncoder;
    }

    /**
     * 返回{@code PhePlaintext}的{@code value}。
     *
     * @return {@code PhePlaintext}的{@code value}。
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * 返回{@code PhePlaintext}的{@code exponent}。
     *
     * @return {@code PhePlaintext}的{@code exponent}。
     */
    public int getExponent() {
        return exponent;
    }

    /**
     * 解码得到准确的{@code BigInteger}。
     *
     * @return 解码结果。
     */
    public BigInteger decodeBigInteger() {
        return plaintextEncoder.decodeBigInteger(this);
    }

    /**
     * 把编码结果按照m = m_0 || m_1 ||...|| m_t拆分为多个短的原始值。
     *
     * @param num          解码数量。
     * @param maxBitLength 明文最大比特长度。
     * @return 解码结果。
     */
    public BigInteger[] decodeBigIntegers(int num, int maxBitLength) {
        return plaintextEncoder.decodeBigIntegers(this, num, maxBitLength);
    }

    /**
     * 解码得到准确的{@code double}。
     *
     * @return 解码结果。
     */
    public double decodeDouble() {
        return plaintextEncoder.decodeDouble(this);
    }

    /**
     * 解码得到准确的{@code long}。
     *
     * @return 解码结果。
     */
    public long decodeLong() {
        return plaintextEncoder.decodeLong(this);
    }

    /**
     * 使用{@code BIG_DECIMAL_ENCODING_PRECISION}定义的准确性解码得到近似的{@code BigDecimal}。
     *
     * @return 解码结果。
     */
    public BigDecimal decodeBigDecimal() {
        return plaintextEncoder.decodeBigDecimal(this);
    }

    /**
     * 解码得到近似的{@code BigDecimal}。解码得到的近似结果与真实值的误差小于10^-{@code precision}。
     *
     * @param precision 解码相对误差的上界。
     * @return 解码结果。
     */
    public BigDecimal decodeBigDecimal(int precision) {
        return plaintextEncoder.decodeBigDecimal(this, precision);
    }

    /**
     * {@code PhePlaintext} + {@code PhePlaintext}。
     *
     * @param other 加数。
     * @return 相加结果。
     */
    public PhePlaintext add(PhePlaintext other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * {@code PhePlaintext} + {@code BigInteger}。
     *
     * @param other 加数。
     * @return 相加结果。
     */
    public PhePlaintext add(BigInteger other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * {@code PhePlaintext} + {@code double}。
     *
     * @param other 加数。
     * @return 相加结果。
     */
    public PhePlaintext add(double other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * {@code PhePlaintext} + {@code long}。
     *
     * @param other 加数。
     * @return 相加结果。
     */
    public PhePlaintext add(long other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * {@code PhePlaintext} - {@code PhePlaintext}。
     *
     * @param other 减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * {@code PhePlaintext} - {@code BigInteger}。
     *
     * @param other 减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(BigInteger other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * {@code PhePlaintext} - {@code double}。
     *
     * @param other 减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(double other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * {@code PhePlaintext} - {@code long}。
     *
     * @param other 减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(long other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * {@code PhePlaintext} * {@code PhePlaintext}。
     *
     * @param other 乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * {@code PhePlaintext} * {@code BigInteger}。
     *
     * @param other 乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(BigInteger other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * {@code PhePlaintext} * {@code double}。
     *
     * @param other 乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(double other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * {@code PhePlaintext} * {@code long}。
     *
     * @param other 乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(long other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * {@code PhePlaintext} / {@code double}。
     *
     * @param other 除数。
     * @return 相除结果。
     */
    public PhePlaintext divide(double other) {
        return plaintextEncoder.divide(this, other);
    }

    /**
     * {@code PhePlaintext} / {@code other}。
     *
     * @param other 除数。
     * @return 相除结果。
     */
    public PhePlaintext divide(long other) {
        return plaintextEncoder.divide(this, other);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(plaintextEncoder)
            .append(value)
            .append(exponent)
            .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != PhePlaintext.class) {
            return false;
        }
        PhePlaintext that = (PhePlaintext) o;
        return new EqualsBuilder()
            .append(this.plaintextEncoder, that.plaintextEncoder)
            .append(this.value, that.value)
            .append(this.exponent, that.exponent)
            .isEquals();
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(value));
        byteArrayList.add(IntUtils.intToByteArray(exponent));

        return byteArrayList;
    }
}
