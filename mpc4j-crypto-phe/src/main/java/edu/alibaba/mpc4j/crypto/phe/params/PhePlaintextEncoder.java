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
import edu.alibaba.mpc4j.crypto.phe.PheDecodeException;
import edu.alibaba.mpc4j.crypto.phe.PheEncodeException;
import edu.alibaba.mpc4j.crypto.phe.PheMathUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.util.Precision;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * PHE plaintext encoder that encodes original plaintexts in different formats into plaintext space [0, modulus).
 * <p>
 * The implementation is based on
 * <a href="https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/StandardEncodingScheme.java">
 * StandardEncodingScheme.java</a>.
 * <p>
 * The encoding scheme is based on
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2.3">
 * Java Language Specification, section 4.2.3</a>.
 * <p>
 * The original plaintext is encoded as <code>value * base<sup>exponent</sup></code>. Different from scientific notation,
 * here we have 0 <= value < modulus.
 *
 * @author Dongyao Wu, mpnd, Weiran Liu
 * @date 2017/09/21
 */
public class PhePlaintextEncoder implements PheParams {
    /**
     * default precision for encoding <code>BigDecimal</code>
     */
    public static final int BIG_DECIMAL_ENCODING_PRECISION = 34;
    /**
     * mantissa bits, i.e., the number of bits used to represent the significand of a double-precision floating-point.
     * There are 52 bits in the significand, plus the implicit leading 1 bit (1 <= fraction < 2), the total is 53 bits.
     */
    private static final int DOUBLE_MANTISSA_BITS = 53;
    /**
     * modulus
     */
    private final BigInteger modulus;
    /**
     * signed encoding or not
     */
    private final boolean signed;
    /**
     * base
     */
    private final int base;
    /**
     * base as {@code BigInteger}
     */
    private final BigInteger bigIntegerBase;
    /**
     * log<sub>2</sub>(base)
     */
    private final double log2Base;
    /**
     * precision, that is, the difference between the original plaintext and the encoded plaintext is less than
     * <code>10<sup>-precision</sup></code>.
     */
    private final int precision;
    /**
     * maximal encoded that can be encrypted in the given modulus.
     * <ul>
     *     <li>If signed, {@code maxEncoded} is the first half of the plaintext space. For example, if
     *     <code>modulus = 11</code>, then {@code maxEncoded  = 11 / 2 = 5}.</li>
     *     <li>If unsigned, {@code maxEncoded} is the maximal <code>BigInteger</code> in the plaintext space. For
     *     example, if <code>modulus = 7</code>, then {@code maxEncoded = modulus - 1 = 6}.
     * </ul>
     */
    private final BigInteger maxEncoded;
    /**
     * minimal encoded that can be encrypted in the given modulus.
     * <ul>
     *     <li>If signed, {@code minEncoded} is the second half of the plaintext space. For example, if
     *     <code>modulus = 11</code>, then {@code minEncoded = modulus - maxEncoded = 6}.</li>
     *     <li>If unsigned, {@code minEncoded} is 0.</li>
     * </ul>
     */
    private final BigInteger minEncoded;
    /**
     * maximal significand supported in the given modulus. We always have {@code maxSignificand = maxEncoded}.
     */
    private final BigInteger maxSignificand;
    /**
     * minimal significand supported in the given modulus.
     * <ul>
     *     <li>If signed, {@code minSignificand = -maxSignificand}.</li>
     *     <li>If unsigned, {@code minSignificand = 0}.</li>
     * </ul>
     */
    private final BigInteger minSignificand;

    /**
     * Creates a PHE plaintext encoder.
     *
     * @param modulus   modulus.
     * @param signed    signed encoding or not.
     * @param precision precision.
     * @param base      base.
     * @return a PHE plaintext encoder.
     */
    public static PhePlaintextEncoder fromParams(BigInteger modulus, boolean signed, int precision, int base) {
        return PhePlaintextEncoder.create(modulus, signed, precision, base);
    }

    /**
     * Creates a PHE plaintext encoder from serialized {@code List<byte[]>}.
     *
     * @param byteArrayList serialized {@code List<byte[]>}.
     * @return a PHE plaintext encoder.
     */
    public static PhePlaintextEncoder deserialize(List<byte[]> byteArrayList) {
        // modulus
        BigInteger modulus = PheMathUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        // signed
        byte[] signedBytes = byteArrayList.remove(0);
        Preconditions.checkArgument(signedBytes.length == 1);
        byte signedByte = signedBytes[0];
        Preconditions.checkArgument(signedByte == (byte) 0 || signedByte == (byte) 1);
        boolean signed = (signedByte == 1);
        // precision
        int precision = PheMathUtils.byteArrayToInt(byteArrayList.remove(0));
        // base
        int base = PheMathUtils.byteArrayToInt(byteArrayList.remove(0));

        return PhePlaintextEncoder.create(modulus, signed, precision, base);
    }

    private static PhePlaintextEncoder create(BigInteger modulus, boolean signed, int precision, int base) {
        Preconditions.checkArgument(PheMathUtils.greater(modulus, BigInteger.ONE), "module must be greater than 1.");
        Preconditions.checkArgument(base >= 2, "Base must be at least equals to 2.");
        Preconditions.checkArgument(
            modulus.bitLength() >= precision && precision >= 1,
            "Precision must be greater than zero and less than or equal to the number of bits in the modulus"
        );
        Preconditions.checkArgument(
            !signed || precision >= 2, "Precision must be greater than 1 when signed is true"
        );
        return new PhePlaintextEncoder(modulus, signed, precision, base);
    }

    private PhePlaintextEncoder(BigInteger modulus, boolean signed, int precision, int base) {
        this.modulus = modulus;
        this.signed = signed;
        // set base
        this.base = base;
        bigIntegerBase = BigInteger.valueOf(base);
        log2Base = PheMathUtils.log2(base);
        // set precision
        this.precision = precision;
        // we have required modulus.bitLength() >= precision. If modulus.bitLength() == precision, then the plaintext
        // space is modulus, otherwise the plaintext space is 2^precision.
        BigInteger plaintextSpace = modulus.bitLength() == precision ? modulus : BigInteger.ONE.shiftLeft(precision);
        if (signed) {
            // maxEncoded is the max BigInteger in the plaintext space where the first bit is 0
            maxEncoded = plaintextSpace.add(BigInteger.ONE).shiftRight(1).subtract(BigInteger.ONE);
            // minEncoded is modulus - maxEncoded
            minEncoded = modulus.subtract(maxEncoded);
            // maxSignificand = maxEncoded
            maxSignificand = maxEncoded;
            // minSignificand = -maxEncoded
            minSignificand = maxEncoded.negate();
        } else {
            // maxEncoded is plaintextSpace - 1
            maxEncoded = plaintextSpace.subtract(BigInteger.ONE);
            // minEncoded is 0
            minEncoded = BigInteger.ZERO;
            // maxSignificand = maxEncoded
            maxSignificand = maxEncoded;
            // minSignificand = 0
            minSignificand = BigInteger.ZERO;
        }
    }

    @Override
    public List<byte[]> serialize() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(PheMathUtils.bigIntegerToByteArray(modulus));
        byte signedByte = signed ? (byte) 1 : (byte) 0;
        byteArrayList.add(new byte[]{signedByte});
        byteArrayList.add(PheMathUtils.intToByteArray(precision));
        byteArrayList.add(PheMathUtils.intToByteArray(base));

        return byteArrayList;
    }

    /**
     * Encodes a {@code BigInteger} to {@code PhePlaintext}.
     *
     * @param value given {@code BigInteger}.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeBigInteger(BigInteger value) throws PheEncodeException {
        if (value == null) {
            throw new PheEncodeException("cannot encode 'null'");
        }
        if (PheMathUtils.less(value, BigInteger.ZERO) && !isSigned()) {
            throw new PheEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        // compute exponent by dividing value with base until value mod base == 0
        int exponent = 0;
        if (!value.equals(BigInteger.ZERO)) {
            while (value.mod(bigIntegerBase).equals(BigInteger.ZERO)) {
                value = value.divide(bigIntegerBase);
                exponent++;
            }
        }
        if (!isValid(value)) {
            throw new PheEncodeException("Input value cannot be encoded.");
        }
        if (value.signum() < 0) {
            value = value.add(modulus);
        }
        return PhePlaintext.fromParams(this, value, exponent);
    }

    /**
     * Gets the maximum number of slots if we encode multiple short plaintexts
     * <code>m<sub>0</sub>, m<sub>1</sub>, ..., m<sub>num - 1</sub></code> to
     * <code>m<sub>0</sub> || m<sub>1</sub> ||...|| m<sub>num - 1</sub></code>.
     *
     * @param maxBitLength max bit length of each <code>m_i</code>.
     * @return maximum number of slots.
     */
    public int getMaxSlots(int maxBitLength) {
        return (getMaxSignificand().bitLength() - 1) / maxBitLength;
    }

    /**
     * Encodes multiple short plaintexts <code>m<sub>0</sub>, m<sub>1</sub>, ..., m<sub>num - 1</sub></code>.
     *
     * @param values       <code>m<sub>0</sub>, m<sub>1</sub>, ..., m<sub>num - 1</sub></code>.
     * @param maxBitLength max bit length of each <code>m_i</code>.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeSlots(BigInteger[] values, int maxBitLength) {
        Preconditions.checkArgument(maxBitLength > 0);
        Arrays.stream(values).forEach(value -> {
            Preconditions.checkArgument(PheMathUtils.nonNegative(value));
            Preconditions.checkArgument(value.bitLength() <= maxBitLength);
        });
        int maxSlots = getMaxSlots(maxBitLength);
        if (values.length > maxSlots) {
            throw new PheEncodeException(String.format(
                "values.length = %s, should be less than or equal to maxSlots = %s", values.length, maxSlots
            ));
        }
        BigInteger encoded = BigInteger.ZERO.add(values[0]);
        for (int index = 1; index < values.length; index++) {
            encoded = encoded.shiftLeft(maxBitLength).add(values[index]);
        }
        return encodeBigInteger(encoded);
    }

    /**
     * Encodes a {@code double} to {@code PhePlaintext}.
     *
     * @param value given {@code double}.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeDouble(double value) throws PheEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new PheEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new PheEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        int exponent = Precision.equals(value, 0, Double.MIN_VALUE) ? 0 : getDoublePrecExponent(value);
        return PhePlaintext.fromParams(this, innerEncode(new BigDecimal(value), exponent), exponent);
    }

    /**
     * Encodes {@code double} to {@code PhePlaintext} with a maximum exponent.
     *
     * @param value       given {@code double}.
     * @param maxExponent the maximum exponent.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeDouble(double value, int maxExponent) throws PheEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new PheEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new PheEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        int exponent = getExponent(getDoublePrecExponent(value), maxExponent);
        return PhePlaintext.fromParams(
            this,
            innerEncode(new BigDecimal(value), getExponent(getDoublePrecExponent(value), maxExponent)),
            exponent
        );
    }

    private int getDoublePrecExponent(double value) {
        int binFltExponent = Math.getExponent(value) + 1;
        int binLsbExponent = binFltExponent - DOUBLE_MANTISSA_BITS;
        return (int) Math.floor((double) binLsbExponent / log2Base);
    }

    /**
     * Encodes {@code double} to {@code PhePlaintext} with a given precision represented as a double, i.e., the precision
     * is exactly the maximal difference between the original plaintext and the encoded plaintext.
     *
     * @param value     given {@code double}.
     * @param precision precision as a double.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeDouble(double value, double precision) throws PheEncodeException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new PheEncodeException("Input value cannot be encoded.");
        }
        if (value < 0 && !isSigned()) {
            throw new PheEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        if (precision > 1 || precision <= 0) {
            throw new PheEncodeException("Precision must be 10^-i where i > 0.");
        }
        int exponent = getPrecExponent(precision);
        return PhePlaintext.fromParams(this, innerEncode(new BigDecimal(value), exponent), exponent);
    }

    /**
     * Encodes {@code long} to {@code PhePlaintext}.
     *
     * @param value given {@code long}.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeLong(long value) throws PheEncodeException {
        return encodeBigInteger(BigInteger.valueOf(value));
    }

    /**
     * Encodes {@code BigDecimal} to {@code PhePlaintext} with a given precision represented as an integer, i.e., the
     * maximum difference between the original plaintext and the encoded plaintext is less than
     * <code>10<sup>-precision</sup></code>.
     *
     * @param value     given {@code BigDecimal}.
     * @param precision precision as an integer.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeBigDecimal(BigDecimal value, int precision) throws PheEncodeException {
        if (value == null) {
            throw new PheEncodeException("cannot encode 'null'");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 && !isSigned()) {
            throw new PheEncodeException("Input value cannot be encoded using this PheEncoding.");
        }
        if (base == 10) {
            BigInteger significant;
            int exp = -value.scale();
            if (value.scale() > 0) {
                significant = value.scaleByPowerOfTen(value.scale()).toBigInteger();
            } else {
                significant = value.unscaledValue();
            }
            if (PheMathUtils.greater(significant, maxSignificand) || PheMathUtils.less(significant, minSignificand)) {
                throw new PheEncodeException("Input value cannot be encoded.");
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
                    PheMathUtils.log(value.multiply(epsilon, mc), base)
                );
                BigDecimal newValue = newExponent < 0 ?
                    value.multiply(BigDecimal.valueOf(base).pow(-newExponent, mc), mc)
                    : value.divide(BigDecimal.valueOf(base).pow(newExponent, mc), mc);
                BigInteger significant = newValue.setScale(0, RoundingMode.HALF_EVEN).unscaledValue();
                if (PheMathUtils.greater(significant, maxSignificand)
                    || PheMathUtils.less(significant, minSignificand)) {
                    throw new PheEncodeException("Input value cannot be encoded.");
                }
                if (significant.signum() < 0) {
                    significant = modulus.add(significant);
                }
                return PhePlaintext.fromParams(this, significant, newExponent);
            } else {
                // so we can turn it into a BigInteger without precision loss
                return encodeBigInteger(value.toBigInteger());
            }
        }
    }

    /**
     * Encodes {@code BigDecimal} to {@code PhePlaintext} with a default precision {@value #BIG_DECIMAL_ENCODING_PRECISION}.
     *
     * @param value given {@code BigDecimal}.
     * @return encoded {@code PhePlaintext}.
     */
    public PhePlaintext encodeBigDecimal(BigDecimal value) throws PheEncodeException {
        return encodeBigDecimal(value, BIG_DECIMAL_ENCODING_PRECISION);
    }

    /**
     * Returns if it is a signed encoding.
     *
     * @return true if it is a signed encoding, false otherwise.
     */
    public boolean isSigned() {
        return signed;
    }

    private BigInteger innerEncode(BigDecimal value, int exponent) {
        // Compute BASE^(-exponent)
        BigDecimal bigDecBaseExponent = (new BigDecimal(base)).pow(-exponent, MathContext.DECIMAL128);
        // Compute the integer representation, i.e., value * (BASE^-exponent)
        BigInteger bigIntRep = value
            .multiply(bigDecBaseExponent)
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger();
        if (PheMathUtils.greater(bigIntRep, maxSignificand) ||
            (value.signum() < 0 && PheMathUtils.less(bigIntRep, minSignificand))) {
            throw new PheEncodeException("Input value cannot be encoded.");
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
     * Checks if the given {@code PhePlaintext} uses the same {@code PhePlaintextEncoder} as this one.
     *
     * @param encoded the given {@code PhePlaintext}.
     */
    private void checkInputs(PhePlaintext encoded) {
        Preconditions.checkArgument(
            equals(encoded.getPlaintextEncoder()), "PhePlaintext has different PhePlaintextEncoder"
        );
    }

    /**
     * Gets the sign of the given {@code PhePlaintext}.
     *
     * @param number given {@code PhePlaintext}.
     * @return -1 if the number is negative, 0 if the number is zero, and 1 if the number is positive.
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
     * Gets the modulus.
     *
     * @return the modulus.
     */
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * Gets the base.
     *
     * @return the base.
     */
    public int getBase() {
        return base;
    }

    /**
     * Gets the precision.
     *
     * @return precision.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Gets max encoded.
     *
     * @return max encoded.
     */
    public BigInteger getMaxEncoded() {
        return maxEncoded;
    }

    /**
     * Gets min encoded.
     *
     * @return min encoded.
     */
    public BigInteger getMinEncoded() {
        return minEncoded;
    }

    /**
     * Gets max significand.
     *
     * @return max significand.
     */
    public BigInteger getMaxSignificand() {
        return maxSignificand;
    }

    /**
     * Gets min significand.
     *
     * @return min significand.
     */
    public BigInteger getMinSignificand() {
        return minSignificand;
    }

    /**
     * Returns if the given {@code PhePlaintext} is valid under this {@code PhePlaintextEncoder}.
     * <ul>
     *   <li>If signed, {@code value} <= {@code maxEncoded} (positive), or {@code value} >= {@code minEncoded} (negative).</li>
     *   <li>Unsigned {@code PhePlaintextEncoder}: {@code value} <= {@code maxEncoded}.</li>
     * </ul>
     *
     * @param encoded given {@code PhePlaintext}.
     * @return true if the given {@code PhePlaintext} is valid, false otherwise.
     */
    public boolean isValid(PhePlaintext encoded) {
        checkInputs(encoded);
        if (PheMathUtils.lessOrEqual(encoded.getValue(), maxEncoded)) {
            return true;
        } else {
            return signed && PheMathUtils.greaterOrEqual(encoded.getValue(), minEncoded);
        }
    }

    /**
     * Returns if the given {@code value} is valid under this {@code PhePlaintextEncoder}.
     *
     * @param value given {@code value}.
     * @return true if the given {@code value} is valid, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(BigInteger value) {
        // minSignificand <= value <= maxSignificand
        return PheMathUtils.greaterOrEqual(value, getMinSignificand()) && PheMathUtils.lessOrEqual(value, getMaxSignificand());
    }

    /**
     * Decodes to the original plaintext as a single {@code BigInteger}.
     *
     * @param encoded given {@code PhePlaintext}.
     * @return original plaintext as a single {@code BigInteger}.
     */
    public BigInteger decodeBigInteger(PhePlaintext encoded) {
        checkInputs(encoded);
        BigInteger significand = getSignificand(encoded);
        return significand.multiply(BigInteger.valueOf(base).pow(encoded.getExponent()));
    }

    /**
     * Decodes to the original plaintext and splits it into slots
     * <code>m<sub>0</sub> || m<sub>1</sub> ||...|| m<sub>num - 1</sub></code>.
     *
     * @param num          slot num.
     * @param maxBitLength max bit length of each <code>m_i</code>.
     * @return <code>m<sub>0</sub> || m<sub>1</sub> ||...|| m<sub>num - 1</sub></code>.
     */
    public BigInteger[] decodeSlots(PhePlaintext encoded, int num, int maxBitLength) {
        int maxSlots = getMaxSlots(maxBitLength);
        if (num > maxSlots) {
            throw new PheDecodeException(String.format(
                "num = %s, should be less than or equal to maxSlots = %s", num, maxSlots
            ));
        }
        // first, decode to BigInteger
        BigInteger decoded = decodeBigInteger(encoded);
        BigInteger slotMod = BigInteger.ZERO.setBit(maxBitLength);
        BigInteger[] bigIntegers = new BigInteger[num];
        // 可以在一个循环内处理完所有过程，处理完最后一个索引值后，shiftRight仍然可以正常执行
        for (int index = num - 1; index >= 0; index--) {
            bigIntegers[index] = decoded.mod(slotMod);
            decoded = decoded.shiftRight(maxBitLength);
        }
        return bigIntegers;
    }

    /**
     * Decodes to the original plaintext as a single {@code double}.
     *
     * @param encoded given {@code PhePlaintext}.
     * @return original plaintext as a single {@code double}.
     */
    public double decodeDouble(PhePlaintext encoded) throws PheDecodeException {
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
            throw new PheDecodeException("Decoded value cannot be represented as double.");
        }
        return decoded;
    }

    /**
     * Decodes to the original plaintext as a single {@code long}.
     *
     * @param encoded given {@code PhePlaintext}.
     * @return original plaintext as a single {@code long}.
     */
    public long decodeLong(PhePlaintext encoded) throws PheDecodeException {
        checkInputs(encoded);
        BigInteger decoded = decodeBigInteger(encoded);
        if (PheMathUtils.less(decoded, PheMathUtils.LONG_MIN_BIGINTEGER_VALUE) ||
            PheMathUtils.greater(decoded, PheMathUtils.LONG_MAX_BIGINTEGER_VALUE)) {
            throw new PheDecodeException("Decoded value cannot be represented as long.");
        }
        return decoded.longValue();
    }

    /**
     * Decodes to the original plaintext as a single {@code BigDecimal}, where the precision is set to default
     * {@value #BIG_DECIMAL_ENCODING_PRECISION}) so that the difference between the original
     * plaintext and the decoded plaintext is less than <code>10<sup>{@value #BIG_DECIMAL_ENCODING_PRECISION}</sup></code>.
     *
     * @return original plaintext as a single {@code BigDecimal}.
     */
    public BigDecimal decodeBigDecimal(PhePlaintext encoded) throws PheDecodeException {
        return decodeBigDecimal(encoded, BIG_DECIMAL_ENCODING_PRECISION);
    }

    /**
     * Decodes to the original plaintext as a single {@code BigDecimal} with the given <code>precision</code> so that
     * the difference between the original plaintext and the decoded plaintext is less than
     * <code>10<sup>-precision</sup></code>.
     *
     * @param precision precision.
     * @return original plaintext as a single {@code BigDecimal}.
     */
    public BigDecimal decodeBigDecimal(PhePlaintext encoded, int precision) throws PheDecodeException {
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
     * Gets the significand of the given {@code PhePlaintext}.
     *
     * @param encoded given {@code PhePlaintext}.
     * @return the significand of the given {@code PhePlaintext}.
     */
    private BigInteger getSignificand(PhePlaintext encoded) throws PheDecodeException {
        checkInputs(encoded);
        final BigInteger value = encoded.getValue();
        if (value.compareTo(modulus) > 0) {
            throw new PheDecodeException("The significand of the encoded number is corrupted");
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
        throw new PheDecodeException("Detected overflow");
    }

    /**
     * Gets rescaling factor when re-encoding {@code PhePlaintext} by rescaling with <code>base</code><sup>expDiff</sup>.
     *
     * @param expDiff exponent difference.
     * @return rescaling factor.
     */
    public BigInteger getRescalingFactor(int expDiff) {
        return (BigInteger.valueOf(base)).pow(expDiff);
    }

    /**
     * Returns if the encoding scheme is full precision.
     *
     * @return true if the encoding scheme is full precision, false otherwise.
     */
    public boolean isFullPrecision() {
        return getPrecision() == getModulus().bitLength();
    }

    /**
     * Re-encoding the given {@code PhePlaintext} by decreasing its exponent to {@code newExp}.
     *
     * @param operand given {@code PhePlaintext}.
     * @param newExp  new {@code exponent}, must be less than current {@code exponent}.
     * @return re-encoded {@code PhePlaintext}.
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
     * {@code PhePlaintext} + {@code PhePlaintext}.
     *
     * @param operand operand.
     * @param other   other.
     * @return result.
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
        return add(operand, encodeBigInteger(other));
    }

    /**
     * {@code PhePlaintext} + {@code double}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    public PhePlaintext add(PhePlaintext operand, double other) {
        return add(operand, encodeDouble(other));
    }

    /**
     * {@code PhePlaintext} + {@code long}。
     *
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    public PhePlaintext add(PhePlaintext operand, long other) {
        return add(operand, encodeLong(other));
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
        return subtract(operand, encodeBigInteger(other));
    }

    /**
     * {@code PhePlaintext} - {@code double}。
     *
     * @param operand 被减数。
     * @param other   减数。
     * @return 相减结果。
     */
    public PhePlaintext subtract(PhePlaintext operand, double other) {
        return subtract(operand, encodeDouble(other));
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
        return subtract(operand, encodeLong(other));
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
        return multiply(operand, encodeBigInteger(other));
    }

    /**
     * {@code PhePlaintext} * {@code double}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, double other) {
        return multiply(operand, encodeDouble(other));
    }

    /**
     * {@code PhePlaintext} * {@code long}。
     *
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 相乘结果。
     */
    public PhePlaintext multiply(PhePlaintext operand, long other) {
        return multiply(operand, encodeLong(other));
    }

    /**
     * {@code PhePlaintext} / {@code double}。
     *
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    public PhePlaintext divide(PhePlaintext operand, double other) {
        return multiply(operand, encodeDouble(1.0 / other));
    }

    /**
     * {@code PhePlaintext} / {@code long}。
     *
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    public PhePlaintext divide(PhePlaintext operand, long other) {
        return multiply(operand, encodeDouble(1.0 / other));
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
