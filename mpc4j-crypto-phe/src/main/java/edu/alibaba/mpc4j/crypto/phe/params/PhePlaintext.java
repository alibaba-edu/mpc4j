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
import edu.alibaba.mpc4j.crypto.phe.PheMathUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * PHE plaintext.
 * <p>
 * The implementation is based on
 * <a href="https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/EncryptedNumber.java">
 * EncryptedNumber.java</a>.
 *
 * @author Brian Thorne, Wilko Henecka, Dongyao Wu, mpnd, Max Ott, Weiran Liu
 * @date 2017/09/21
 */
public class PhePlaintext implements PheParams {
    /**
     * plaintext encoder.
     */
    private final PhePlaintextEncoder plaintextEncoder;
    /**
     * value, for which the plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     */
    private final BigInteger value;
    /**
     * exponent, for which the plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     */
    private final int exponent;

    /**
     * Creates a {@code PhePlaintext} from given parameters, in which the original plaintext is encoded as
     * <code>value * base<sup>exponent</sup></code>.
     *
     * @param plaintextEncoder plaintext encoder.
     * @param value            value.
     * @param exponent         exponent.
     * @return {@code PhePlaintext} from given parameters.
     */
    public static PhePlaintext fromParams(PhePlaintextEncoder plaintextEncoder, BigInteger value, int exponent) {
        return PhePlaintext.create(plaintextEncoder, value, exponent);
    }

    /**
     * Creates a {@code PhePlaintext} from serialized {@code List<byte[]>}.
     *
     * @param plaintextEncoder plaintext encoder.
     * @param byteArrayList    serialized {@code List<byte[]>}.
     * @return {@code PhePlaintext} from serialized {@code List<byte[]>}.
     */
    public static PhePlaintext deserialize(PhePlaintextEncoder plaintextEncoder, List<byte[]> byteArrayList) {
        BigInteger value = PheMathUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        int exponent = PheMathUtils.byteArrayToInt(byteArrayList.remove(0));

        return PhePlaintext.create(plaintextEncoder, value, exponent);
    }

    private static PhePlaintext create(PhePlaintextEncoder plaintextEncoder, BigInteger value, int exponent) {
        Preconditions.checkNotNull(plaintextEncoder, "plaintextEncoder must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkArgument(PheMathUtils.nonNegative(value), "value must be non-negative");
        Preconditions.checkArgument(PheMathUtils.less(value, plaintextEncoder.getModulus()), "value must be less than modulus");

        return new PhePlaintext(plaintextEncoder, value, exponent);
    }

    private PhePlaintext(PhePlaintextEncoder plaintextEncoder, BigInteger value, int exponent) {
        this.plaintextEncoder = plaintextEncoder;
        this.value = value;
        this.exponent = exponent;
    }

    /**
     * Gets the plaintext encoder.
     *
     * @return the plaintext encoder.
     */
    public PhePlaintextEncoder getPlaintextEncoder() {
        return plaintextEncoder;
    }

    /**
     * Gets value, for which the plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     *
     * @return value.
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Gets exponent, for which the plaintext is encoded as <code>value * base<sup>exponent</sup></code>.
     *
     * @return exponent.
     */
    public int getExponent() {
        return exponent;
    }

    /**
     * Decodes to the original plaintext as a single {@code BigInteger}.
     *
     * @return original plaintext as a single {@code BigInteger}.
     */
    public BigInteger decodeBigInteger() {
        return plaintextEncoder.decodeBigInteger(this);
    }

    /**
     * Decodes to the original plaintext and splits it into slots
     * <code>m<sub>0</sub> || m<sub>1</sub> ||...|| m<sub>num - 1</sub></code>.
     *
     * @param num          slot num.
     * @param maxBitLength max bit length of each <code>m_i</code>.
     * @return <code>m<sub>0</sub> || m<sub>1</sub> ||...|| m<sub>num - 1</sub></code>.
     */
    public BigInteger[] decodeSlots(int num, int maxBitLength) {
        return plaintextEncoder.decodeSlots(this, num, maxBitLength);
    }

    /**
     * Decodes to the original plaintext as a single {@code double}.
     *
     * @return original plaintext as a single {@code double}.
     */
    public double decodeDouble() {
        return plaintextEncoder.decodeDouble(this);
    }

    /**
     * Decodes to the original plaintext as a single {@code long}.
     *
     * @return original plaintext as a single {@code long}.
     */
    public long decodeLong() {
        return plaintextEncoder.decodeLong(this);
    }

    /**
     * Decodes to the original plaintext as a single {@code BigDecimal}, where the precision is set to default
     * {@code #PhePlaintextEncoder.BIG_DECIMAL_ENCODING_PRECISION = 34}) so that the difference between the original
     * plaintext and the decoded plaintext is less than <code>10<sup>-34</sup></code>.
     *
     * @return original plaintext as a single {@code BigDecimal}.
     */
    public BigDecimal decodeBigDecimal() {
        return plaintextEncoder.decodeBigDecimal(this);
    }

    /**
     * Decodes to the original plaintext as a single {@code BigDecimal} with the given <code>precision</code> so that
     * the difference between the original plaintext and the decoded plaintext is less than
     * <code>10<sup>-precision</sup></code>.
     *
     * @param precision precision.
     * @return original plaintext as a single {@code BigDecimal}.
     */
    public BigDecimal decodeBigDecimal(int precision) {
        return plaintextEncoder.decodeBigDecimal(this, precision);
    }

    /**
     * Adds current {@code PhePlaintext} with {@code PhePlaintext other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext add(PhePlaintext other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * Adds current {@code PhePlaintext} with {@code BigInteger other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext add(BigInteger other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * Adds current {@code PhePlaintext} with {@code double other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext add(double other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * Adds current {@code PhePlaintext} with {@code long other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext add(long other) {
        return plaintextEncoder.add(this, other);
    }

    /**
     * Subtracts {@code PhePlaintext other} from current {@code PhePlaintext} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext subtract(PhePlaintext other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * Subtracts {@code BigInteger other} from current {@code PhePlaintext} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext subtract(BigInteger other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * Subtracts {@code double other} from current {@code PhePlaintext} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext subtract(double other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * Subtracts {@code long other} from current {@code PhePlaintext} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext subtract(long other) {
        return plaintextEncoder.subtract(this, other);
    }

    /**
     * Multiplies current {@code PhePlaintext} with {@code PhePlaintext other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext multiply(PhePlaintext other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * Multiplies current {@code PhePlaintext} with {@code BigInteger other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext multiply(BigInteger other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * Multiplies current {@code PhePlaintext} with {@code double other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext multiply(double other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * Multiplies current {@code PhePlaintext} with {@code long other} and returns the result.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext multiply(long other) {
        return plaintextEncoder.multiply(this, other);
    }

    /**
     * Divides current {@code PhePlaintext} by {@code PhePlaintext other} and returns the result. Here we compute
     * <code>1.0 / other</code> and multiply it with current {@code PhePlaintext}.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext divide(double other) {
        return plaintextEncoder.divide(this, other);
    }

    /**
     * Divides current {@code PhePlaintext} by {@code long other} and returns the result. Here we compute
     * <code>1.0 / other</code> and multiply it with current {@code PhePlaintext}.
     *
     * @param other other.
     * @return result.
     */
    public PhePlaintext divide(long other) {
        return plaintextEncoder.divide(this, other);
    }

    @Override
    public List<byte[]> serialize() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(PheMathUtils.bigIntegerToByteArray(value));
        byteArrayList.add(PheMathUtils.intToByteArray(exponent));

        return byteArrayList;
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
}
