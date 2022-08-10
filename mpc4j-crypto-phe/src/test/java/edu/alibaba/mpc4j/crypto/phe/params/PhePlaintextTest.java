/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
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
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.crypto.phe.CryptoEncodeException;
import edu.alibaba.mpc4j.crypto.phe.PheParamsTestConfiguration;
import edu.alibaba.mpc4j.crypto.phe.PheTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 半同态编码数测试。源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/PaillierEncodedNumberTest.java
 * </p>
 *
 * @author Wilko Henecka, Brian Thorne, mpnd, Weiran Liu
 * @date 2017/09/21
 */
@RunWith(Enclosed.class)
public class PhePlaintextTest {

    @RunWith(Parameterized.class)
    public static class PhePlaintextParamTest {
        /**
         * 编码方案
         */
        private final PhePlaintextEncoder plaintextEncoder;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> configurations() {
            Collection<Object[]> configurationParams = new ArrayList<>();
            configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_512);
            configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_1024);

            return configurationParams;
        }

        public PhePlaintextParamTest(String name, PheParamsTestConfiguration configuration) {
            Preconditions.checkArgument(StringUtils.isNotBlank(name));
            plaintextEncoder = configuration.getPlaintextEncoder();
        }

        @Test
        public void testPackable() {
            List<byte[]> byteArrayList = plaintextEncoder.toByteArrayList();
            PhePlaintextEncoder that = PhePlaintextEncoder.fromByteArrayList(byteArrayList);
            Assert.assertEquals(plaintextEncoder, that);
        }

        @Test
        public void testLongSmall() {
            for (long i = -1024; i <= 1024; ++i) {
                testLong(i);
            }
        }

        @Test
        public void testLongLarge() {
            testLong(Long.MAX_VALUE);
            testLong(Long.MIN_VALUE);
        }

        @Test
        public void testLongRandom() {
            for (int i = 0; i < 100000; ++i) {
                testLong(PheTestUtils.SECURE_RANDOM.nextLong());
            }
        }

        private void testLong(long value) {
            BigInteger valueBig = BigInteger.valueOf(value);
            double valueDouble = (double)value;
            try {
                PhePlaintext encoded = plaintextEncoder.encode(value);
                if (value < 0 && !plaintextEncoder.isSigned()) {
                    Assert.fail("ERROR: Successfully encoded negative integer with unsigned encoding");
                }
                BigInteger expected = valueBig;

                if (!expected.equals(BigInteger.ZERO)) {
                    while (expected.mod(BigInteger.valueOf(plaintextEncoder.getBase())).compareTo(
                        BigInteger.ZERO) == 0) {
                        expected = expected.divide(BigInteger.valueOf(plaintextEncoder.getBase()));
                    }
                }
                if (value < 0) {
                    expected = plaintextEncoder.getModulus().add(expected);
                }
                Assert.assertEquals(expected, encoded.getValue());
                Assert.assertEquals(value, encoded.decodeLong());
                Assert.assertEquals(valueBig, encoded.decodeBigInteger());
                Assert.assertEquals(valueDouble, encoded.decodeDouble(), PheTestUtils.EPSILON);
            } catch (CryptoEncodeException e) {
                if (value >= 0 || plaintextEncoder.isSigned()) {
                    throw e;
                }
            }
        }

        @Test
        public void testZeroDouble() {
            PhePlaintext zero = plaintextEncoder.encode(0.0);
            Assert.assertEquals(0, zero.getExponent());
        }

        @Test
        public void testDoubleConstants() {
            testDouble(Double.MAX_VALUE);
            testDouble(Math.nextAfter(Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
            testDouble(1.0);
            testDouble(Math.nextAfter(Double.MIN_NORMAL, Double.POSITIVE_INFINITY));
            testDouble(Double.MIN_NORMAL);
            testDouble(Math.nextAfter(Double.MIN_NORMAL, Double.NEGATIVE_INFINITY));
            testDouble(Double.MIN_VALUE);
            testDouble(0.0);
            testDouble(-0.0);
            testDouble(-Double.MIN_VALUE);
            testDouble(-Math.nextAfter(Double.MIN_NORMAL, Double.NEGATIVE_INFINITY));
            testDouble(-Double.MIN_NORMAL);
            testDouble(-Math.nextAfter(Double.MIN_NORMAL, Double.POSITIVE_INFINITY));
            testDouble(-1.0);
            testDouble(-Math.nextAfter(Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
            testDouble(-Double.MAX_VALUE);
        }

        @Test
        public void testDoubleRandom() {
            for (int i = 0; i < 100000; ++i) {
                testDouble(PheTestUtils.randomFiniteDouble());
            }
        }

        @Test
        public void testDoubleNonFinite() {
            // 测试编码负无穷大、正无穷大、未定义浮点数，应无法编码成功
            double[] nonFinite = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN};
            for (double value : nonFinite) {
                try {
                    plaintextEncoder.encode(value);
                    Assert.fail("ERROR: Successfully encoded non-finite double");
                } catch (CryptoEncodeException ignored) {

                }
            }
            // 测试编码随机未定义浮点数（即表示方法不满足浮点数定义标准）
            for (int i = 0; i < 1000; ++i) {
                try {
                    plaintextEncoder.encode(PheTestUtils.randomNaNDouble());
                    Assert.fail("ERROR: Successfully encoded non-finite double");
                } catch (CryptoEncodeException ignored) {

                }
            }
        }

        private void testDouble(double value) {
            try {
                PhePlaintext encoded = plaintextEncoder.encode(value);
                if (value < 0 && !plaintextEncoder.isSigned()) {
                    Assert.fail("ERROR: Successfully encoded negative double with unsigned encoding");
                }
                double tolerance = PheTestUtils.EPSILON;
                double decodedResult = encoded.decodeDouble();
                double absValue = Math.abs(value);
                if (Precision.equals(absValue, 0, DoubleUtils.PRECISION) || absValue > 1.0) {
                    tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(value));
                }
                Assert.assertEquals(value, decodedResult, tolerance);
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testBigDecimalConstants() {
            testBigDecimal(BigDecimal.ZERO);
            testBigDecimal(BigDecimal.ONE);
            testBigDecimal(BigDecimal.ONE.negate());
            testBigDecimal(new BigDecimal(plaintextEncoder.getMaxSignificand()));
            testBigDecimal(new BigDecimal(plaintextEncoder.getMinSignificand()));
        }

        @Test
        public void testBigDecimalRandom() {
            int numBits = plaintextEncoder.getPrecision() / 2;
            for (int i = 0; i < 100000; ++i) {
                testBigDecimal(
                    new BigDecimal(new BigInteger(numBits, PheTestUtils.SECURE_RANDOM),
                        PheTestUtils.SECURE_RANDOM.nextInt(60) - 30)
                );
            }
        }

        private void testBigDecimal(BigDecimal value) {
            try {
                PhePlaintext encoded = plaintextEncoder.encode(value);
                if (value.compareTo(BigDecimal.ZERO) < 0 && !plaintextEncoder.isSigned()) {
                    Assert.fail("ERROR: Successfully encoded negative BigDecimal with unsigned encoding");
                }
                BigDecimal decodedResult = encoded.decodeBigDecimal();
                BigDecimal EPSILON = new BigDecimal(BigInteger.ONE, PhePlaintextEncoder.BIG_DECIMAL_ENCODING_PRECISION);
                BigDecimal relError = value.compareTo(BigDecimal.ZERO) == 0 ? decodedResult
                    : value.subtract(decodedResult)
                        .divide(value, new MathContext(PhePlaintextEncoder.BIG_DECIMAL_ENCODING_PRECISION + 1))
                        .abs();
                Assert.assertTrue(relError.compareTo(EPSILON) <= 0);
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testBigIntegers() {
            testBigIntegers(1);
            testBigIntegers(Byte.SIZE);
            testBigIntegers(Integer.SIZE);
            testBigIntegers(Long.SIZE);
        }

        private void testBigIntegers(int maxBitLength) {
            try {
                int maxPacketSize = plaintextEncoder.getMaxPacketSize(maxBitLength);
                if (maxPacketSize <= 0) {
                    Assert.fail("ERROR: Successfully encode BigIntegers beyonds maxPacketSize");
                }
                // 编码一个随机元素
                BigInteger[] encodeOne = new BigInteger[] {
                    new BigInteger(maxBitLength, PheTestUtils.SECURE_RANDOM),
                };
                PhePlaintext encodedOne = plaintextEncoder.encode(encodeOne, maxBitLength);
                BigInteger[] decodeOne = encodedOne.decodeBigIntegers(encodeOne.length, maxBitLength);
                Assert.assertArrayEquals(encodeOne, decodeOne);
                // 编码最大数量个随机元素
                BigInteger[] encodeMax = IntStream.range(0, maxPacketSize)
                    .mapToObj(index -> new BigInteger(maxBitLength, PheTestUtils.SECURE_RANDOM))
                    .toArray(BigInteger[]::new);
                PhePlaintext encodedMax = plaintextEncoder.encode(encodeMax, maxBitLength);
                BigInteger[] decodeMax = encodedMax.decodeBigIntegers(encodeMax.length, maxBitLength);
                Assert.assertArrayEquals(encodeMax, decodeMax);
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testRange() {
            BigInteger modulus = plaintextEncoder.getModulus();
            int precision = plaintextEncoder.getPrecision();
            if (!plaintextEncoder.isSigned() && plaintextEncoder.isFullPrecision()) {
                // 无符号满精度，BigInteger最大和最小可编码值
                BigInteger max = modulus.subtract(BigInteger.ONE);
                Assert.assertEquals(max, plaintextEncoder.getMaxSignificand());
                Assert.assertEquals(BigInteger.ZERO, plaintextEncoder.getMinSignificand());
                // long最大和最小可编码值
                long maxLong = max.compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                long actualMaxLong = plaintextEncoder.getMaxSignificand()
                    .compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                Assert.assertEquals(maxLong, actualMaxLong);
                Assert.assertEquals(BigInteger.ZERO.longValue(),
                    plaintextEncoder.getMinSignificand().longValue());
                // double最大和最小可编码值
                Assert.assertEquals(
                    max.doubleValue(), plaintextEncoder.getMaxSignificand().doubleValue(),
                    PheTestUtils.EPSILON * plaintextEncoder.getMaxSignificand().doubleValue()
                );
                Assert.assertEquals(
                    BigInteger.ZERO.doubleValue(), plaintextEncoder.getMinSignificand().doubleValue(), 0.0
                );
            } else if (!plaintextEncoder.isSigned() && !plaintextEncoder.isFullPrecision()) {
                // 无符号非满精度，BigInteger最大和最小可编码值
                BigInteger max = BigInteger.ONE.shiftLeft(precision).subtract(BigInteger.ONE);
                Assert.assertEquals(max, plaintextEncoder.getMaxSignificand());
                Assert.assertEquals(BigInteger.ZERO, plaintextEncoder.getMinSignificand());
                // long最大和最小可编码值
                long maxLong = max.compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                long actualMaxLong = plaintextEncoder.getMaxSignificand()
                    .compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                Assert.assertEquals(maxLong, actualMaxLong);
                Assert.assertEquals(BigInteger.ZERO.longValue(),
                    plaintextEncoder.getMinSignificand().longValue());
                // double最大和最小可编码值
                Assert.assertEquals(
                    max.doubleValue(), plaintextEncoder.getMaxSignificand().doubleValue(),
                    PheTestUtils.EPSILON * plaintextEncoder.getMaxSignificand().doubleValue()
                );
                Assert.assertEquals(
                    BigInteger.ZERO.doubleValue(), plaintextEncoder.getMinSignificand().doubleValue(), 0.0
                );
            } else if (plaintextEncoder.isSigned() && plaintextEncoder.isFullPrecision()) {
                // 有符号满精度，BigInteger最大和最小可编码值
                BigInteger max = plaintextEncoder.getModulus().shiftRight(1);
                BigInteger min = max.negate();
                Assert.assertEquals(max, plaintextEncoder.getMaxSignificand());
                Assert.assertEquals(min, plaintextEncoder.getMinSignificand());
                // long最大和最小可编码值
                long maxLong = max.compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                long actualMaxLong = plaintextEncoder.getMaxSignificand()
                    .compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                Assert.assertEquals(maxLong, actualMaxLong);
                long minLong = min.compareTo(BigIntegerUtils.LONG_MIN_VALUE) <= 0 ? Long.MIN_VALUE : min.longValue();
                long actualMinLong = plaintextEncoder.getMinSignificand()
                    .compareTo(BigIntegerUtils.LONG_MIN_VALUE) <= 0 ? Long.MIN_VALUE : min.longValue();
                Assert.assertEquals(minLong, actualMinLong);
                // double最大和最小可编码值
                Assert.assertEquals(
                    max.doubleValue(), plaintextEncoder.getMaxSignificand().doubleValue(),
                    PheTestUtils.EPSILON * plaintextEncoder.getMaxSignificand().doubleValue()
                );
                Assert.assertEquals(
                    min.doubleValue(), plaintextEncoder.getMinSignificand().doubleValue(),
                    PheTestUtils.EPSILON * Math.abs(plaintextEncoder.getMinSignificand().doubleValue())
                );
            } else if (plaintextEncoder.isSigned() && !plaintextEncoder.isFullPrecision()) {
                // 有符号非满精度，BigInteger最大和最小可编码值
                BigInteger max = BigInteger.ONE.shiftLeft(precision - 1).subtract(BigInteger.ONE);
                BigInteger min = max.negate();
                Assert.assertEquals(max, plaintextEncoder.getMaxSignificand());
                Assert.assertEquals(min, plaintextEncoder.getMinSignificand());
                // long最大和最小可编码值
                long maxLong = max.compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                long actualMaxLong = plaintextEncoder.getMaxSignificand()
                    .compareTo(BigIntegerUtils.LONG_MAX_VALUE) >= 0 ? Long.MAX_VALUE : max.longValue();
                Assert.assertEquals(maxLong, actualMaxLong);
                long minLong = min.compareTo(BigIntegerUtils.LONG_MIN_VALUE) <= 0 ? Long.MIN_VALUE : min.longValue();
                long actualMinLong = plaintextEncoder.getMinSignificand()
                    .compareTo(BigIntegerUtils.LONG_MIN_VALUE) <= 0 ? Long.MIN_VALUE : min.longValue();
                Assert.assertEquals(minLong, actualMinLong);
                // double最大和最小可编码值
                Assert.assertEquals(
                    max.doubleValue(), plaintextEncoder.getMaxSignificand().doubleValue(),
                    PheTestUtils.EPSILON * plaintextEncoder.getMaxSignificand().doubleValue()
                );
                Assert.assertEquals(
                    min.doubleValue(), plaintextEncoder.getMinSignificand().doubleValue(),
                    PheTestUtils.EPSILON * Math.abs(plaintextEncoder.getMinSignificand().doubleValue())
                );
            } else {
                Assert.fail("Invalid defConfig!");
            }
        }

        @Test
        public void testSignum() {
            BigInteger[] testNumbers = new BigInteger[] {
                BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.negate(),
                plaintextEncoder.getMaxSignificand(), plaintextEncoder.getMinSignificand()
            };
            for (BigInteger n : testNumbers) {
                try {
                    PhePlaintext en = plaintextEncoder.encode(n);
                    if (plaintextEncoder.isValid(en)) {
                        Assert.assertEquals(plaintextEncoder.signum(en), n.signum());
                    }
                } catch (Exception e) {
                    if (!e.getClass().equals(CryptoEncodeException.class)) {
                        Assert.fail("unexpected Exception");
                    }
                }
            }
        }

        @Test
        public void testMaxEncodableNumber() {
            PhePlaintext maxNumber = plaintextEncoder.encode(plaintextEncoder.getMaxSignificand());
            PheTestUtils.testEncodable(plaintextEncoder, maxNumber);
        }

        @Test
        public void testMinEncodableNumber() {
            PhePlaintext minNumber = plaintextEncoder.encode(plaintextEncoder.getMinSignificand());
            PheTestUtils.testEncodable(plaintextEncoder, minNumber);
        }

        @Test
        public void testInvalidLargeMaxNumber() {
            // so base won't divide significant
            BigInteger humongous = plaintextEncoder.getMaxSignificand().nextProbablePrime();
            PheTestUtils.testUnencodable(plaintextEncoder, humongous);
        }

        @Test
        public void testInvalidLargeMinNumber() {
            BigInteger negHumongous = plaintextEncoder.getMinSignificand().subtract(BigInteger.ONE);
            while (!negHumongous.isProbablePrime(20)) {
                negHumongous = negHumongous.subtract(BigInteger.ONE);
            }
            PheTestUtils.testUnencodable(plaintextEncoder, negHumongous);
        }

        @Test
        public void testDecodeInvalidPositiveNumbers() {
            // NOTE: decodeException only applies to partial precision
            if (plaintextEncoder.isSigned() && !plaintextEncoder.isFullPrecision()) {
                PhePlaintext encodedNumber = PhePlaintext.fromParams(
                    plaintextEncoder, plaintextEncoder.getMaxEncoded().add(BigInteger.ONE), 0
                );
                PheTestUtils.testUndecodable(plaintextEncoder, encodedNumber);
            }
        }

        @Test
        public void testDecodeInvalidNegativeNumbers() {
            // NOTE: decodeException only applies to partial precision
            if (plaintextEncoder.isSigned() && !plaintextEncoder.isFullPrecision()) {
                PhePlaintext encodedNumber = PhePlaintext.fromParams(
                    plaintextEncoder, plaintextEncoder.getMinEncoded().subtract(BigInteger.ONE), 0
                );
                PheTestUtils.testUndecodable(plaintextEncoder, encodedNumber);
            }
        }

        @Test
        public void testAddLongToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.add(encodedNumber1, 2);
            Assert.assertEquals(3.7, plaintextEncoder.decodeDouble(encodedNumber2), PheTestUtils.EPSILON);
        }

        @Test
        public void testAddDoubleToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.add(encodedNumber1, 2.0);
            Assert.assertEquals(3.7, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testAddBigIntegerToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.add(encodedNumber1, new BigInteger("2"));
            Assert.assertEquals(3.7, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testSubtractLongToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(17);
            PhePlaintext encodedNumber2 = plaintextEncoder.subtract(encodedNumber1, 2);
            Assert.assertEquals(15, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testSubtractDoubleToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(17);
            PhePlaintext encodedNumber2 = plaintextEncoder.subtract(encodedNumber1, 2.0);
            Assert.assertEquals(15, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testSubtractBigIntegerToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(17);
            PhePlaintext encodedNumber2 = plaintextEncoder.subtract(encodedNumber1, new BigInteger("2"));
            Assert.assertEquals(15, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testMultiplyLongToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.multiply(encodedNumber1, 2);
            Assert.assertEquals(3.4, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testMultiplyDoubleToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.multiply(encodedNumber1, 2.0);
            Assert.assertEquals(3.4, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testMultiplyBigIntegerToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.multiply(encodedNumber1, new BigInteger("2"));
            Assert.assertEquals(3.4, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testDivideLongToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.divide(encodedNumber1, 2);
            Assert.assertEquals(0.85, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testDivideDoubleToEncodedNumber() {
            PhePlaintext encodedNumber1 = plaintextEncoder.encode(1.7);
            PhePlaintext encodedNumber2 = plaintextEncoder.divide(encodedNumber1, 2.0);
            Assert.assertEquals(0.85, encodedNumber2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testPositiveEncodedDecreaseExponentTo() {
            PhePlaintext number1 = plaintextEncoder.encode(3.14);
            int originalExp = number1.getExponent();
            int newExp = originalExp - 20;
            PhePlaintext number2 = plaintextEncoder.decreaseExponentTo(number1, newExp);

            if (originalExp < number2.getExponent()) {
                Assert.fail("Fail to decrease the encoded number's exponent");
            }
            Assert.assertEquals(newExp, number2.getExponent());
            Assert.assertEquals(3.14, number2.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testNegativeEncodedDecreaseExponentTo() {
            if (plaintextEncoder.isSigned()) {
                // 只有有符号编码才支持负数降幂
                PhePlaintext number1 = plaintextEncoder.encode(-3.14);
                int originalExp = number1.getExponent();
                int newExp = originalExp - 20;
                PhePlaintext number2 = plaintextEncoder.decreaseExponentTo(number1, newExp);

                if (originalExp < number2.getExponent()) {
                    Assert.fail("Fail to decrease the encoded number's exponent");
                }
                Assert.assertEquals(newExp, number2.getExponent());
                Assert.assertEquals(-3.14, number2.decodeDouble(), PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testManualPrecisionPositiveDouble() {
            double originalNumber = 3.171234e-7;
            double precision = 1e-8;

            PhePlaintext number = plaintextEncoder.encode(originalNumber, precision);
            double decodedNumber = number.decodeDouble();
            if (decodedNumber < originalNumber - precision || decodedNumber > originalNumber + precision) {
                Assert.fail("decodedNumber: " + decodedNumber + " is not in the correct range.");
            }

            PhePlaintext number2 = plaintextEncoder.encode(
                decodedNumber + 0.500001 * precision, precision
            );
            double decodedNumber2 = number2.decodeDouble();
            // 对比精度要小于precision * 0.5，这里取precision / 10
            if (Precision.equals(decodedNumber, decodedNumber2, precision / 10)) {
                Assert.fail(
                    "decodedNumber: " + decodedNumber + " should not be the same as decodedNumber2: " + decodedNumber2
                );
            }

            if (decodedNumber2 < originalNumber - precision / 2
                || decodedNumber2 > originalNumber + precision * 1.5001) {
                Assert.fail("decodedNumber2: " + decodedNumber2 + "is not in the correct range.");
            }

            double value = decodedNumber + precision / 16;
            PhePlaintext number3 = plaintextEncoder.encode(value, precision);
            Assert.assertEquals(decodedNumber, number3.decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testManualPrecisionNegativeDouble() {
            if (plaintextEncoder.isSigned()) {
                double originalNumber = -3.171234e-7;
                double precision = 1e-8;

                PhePlaintext number = plaintextEncoder.encode(originalNumber, precision);
                double decodedNumber = number.decodeDouble();
                if (decodedNumber < originalNumber - precision || decodedNumber > originalNumber + precision) {
                    Assert.fail("decodedNumber: " + decodedNumber + " is not in the correct range.");
                }

                PhePlaintext number2 = plaintextEncoder.encode(
                    decodedNumber + 0.500001 * precision, precision
                );
                double decodedNumber2 = number2.decodeDouble();
                // 对比精度要小于precision * 0.5，这里取precision / 10
                if (Precision.equals(decodedNumber, decodedNumber2, precision / 10)) {
                    Assert.fail("decodedNumber: " + decodedNumber + " should not be the same as decodedNumber2: "
                        + decodedNumber2);
                }

                if (decodedNumber2 < originalNumber - precision / 2
                    || decodedNumber2 > originalNumber + precision * 1.5001) {
                    Assert.fail("decodedNumber2: " + decodedNumber2 + "is not in the correct range.");
                }

                double value = decodedNumber + precision / 16;
                PhePlaintext number3 = plaintextEncoder.encode(value, precision);
                Assert.assertEquals(decodedNumber, number3.decodeDouble(), PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testEncodedDecreaseExponentTo0() {
            // 生成一个高幂的数，降低到低幂后解码，看结果是否相同
            PhePlaintext number1 = plaintextEncoder.encode(1.01, Math.pow(1.0, -8));
            Assert.assertTrue(-30 < number1.getExponent());
            PhePlaintext number2 = plaintextEncoder.decreaseExponentTo(number1, -30);

            if (number1.getExponent() < -30) {
                Assert.fail("-30 < number1.getExponent()");
            }
            Assert.assertEquals(-30, number2.getExponent());
            Assert.assertEquals(1.01, number2.decodeDouble(), Math.pow(1.0, -8));
        }

        @Test
        public void testEncodedDecreaseExponentTo1() {
            if (plaintextEncoder.isSigned()) {
                PhePlaintext number1 = plaintextEncoder.encode(-1.01, Math.pow(1.0, -8));
                Assert.assertTrue(-30 < number1.getExponent());
                PhePlaintext number2 = plaintextEncoder.decreaseExponentTo(number1, -30);

                if (number1.getExponent() < -30) {
                    Assert.fail("-30 < number1.getExponent()");
                }
                Assert.assertEquals(-30, number2.getExponent());
                Assert.assertEquals(-1.01, number2.decodeDouble(), Math.pow(1.0, -8));
            }
        }
    }

    public static class PlaintextTest {
        /**
         * 有符号全精度模数编码方案
         */
        private static final PhePlaintextEncoder DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME =
            PheParamsTestConfiguration.SIGNED_FULL_PRECISION_1024.getPlaintextEncoder();
        /**
         * 无符号全精度模数编码方案
         */
        private static final PhePlaintextEncoder DEFAULT_UNSIGNED_MODULUS_ENCODE_SCHEME =
            PheParamsTestConfiguration.UNSIGNED_FULL_PRECISION_1024.getPlaintextEncoder();
        /**
         * 有符号部分精度模数编码方案
         */
        private static final PhePlaintextEncoder DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME =
            PheParamsTestConfiguration.SIGNED_PARTIAL_PRECISION_1024.getPlaintextEncoder();

        @Test
        public void testConstructor() {
            PhePlaintext encodedNumber;

            try {
                encodedNumber = PhePlaintext.fromParams(null, BigInteger.ONE, 0);
                Assert.fail("Successfully create an encoded number with null ModulusEncodeScheme");
                Assert.assertNull(encodedNumber);
            } catch (NullPointerException ignored) {

            }

            try {
                encodedNumber = PhePlaintext.fromParams(DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME, null, 0);
                Assert.fail("Successfully create an encoded number with null value");
                Assert.assertNull(encodedNumber);
            } catch (NullPointerException ignored) {

            }

            try {
                encodedNumber = PhePlaintext.fromParams(DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME,
                    BigInteger.ONE.negate(), 0);
                Assert.fail("Successfully create an encoded number with negative value");
                Assert.assertNull(encodedNumber);
            } catch (IllegalArgumentException ignored) {

            }

            try {
                encodedNumber = PhePlaintext.fromParams(DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME,
                    DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.getModulus(), 0);
                Assert.fail("Successfully create an encoded number with value equal to modulus");
                Assert.assertNull(encodedNumber);
            } catch (IllegalArgumentException ignored) {

            }

            encodedNumber = PhePlaintext.fromParams(DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME, BigInteger.ONE, 0);
            Assert.assertNotNull(encodedNumber);
            Assert.assertEquals(BigInteger.ONE, encodedNumber.getValue());
            Assert.assertEquals(0, encodedNumber.getExponent());
        }

        @Test
        public void testIsEncodedNumberValid() {
            PhePlaintext encodedNumber1 = PhePlaintext.fromParams(
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME,
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.getMaxEncoded(), 0);
            PhePlaintext encodedNumber2 = PhePlaintext.fromParams(
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME,
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.getMinEncoded(), 0);
            PhePlaintext encodedNumber3 = PhePlaintext.fromParams(
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME,
                DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.getMaxEncoded().add(BigInteger.ONE), 0);

            Assert.assertTrue(DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.isValid(encodedNumber1));
            Assert.assertTrue(DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.isValid(encodedNumber2));
            Assert.assertFalse(DEFAULT_PARTIAL_SIGNED_MODULUS_ENCODE_SCHEME.isValid(encodedNumber3));
        }

        @Test
        public void testEquals() {
            PhePlaintext encodedNumber = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(17);
            // Compare to itself
            Assert.assertEquals(encodedNumber, encodedNumber);
            // Compare to null
            Assert.assertNotEquals(null, encodedNumber);

            PhePlaintext otherEncodedNumber = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(3);
            // Compare to an encoded number with different value
            Assert.assertNotEquals(encodedNumber, otherEncodedNumber);
        }

        @Test
        public void testEncodedDecreaseInvalidExponent() {
            PhePlaintext enc1 = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(3.14);
            Assert.assertTrue(enc1.getExponent() < -10);

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.decreaseExponentTo(enc1, -10);
            } catch (IllegalArgumentException ignored) {

            }
        }

        @Test
        public void testInvalidNumber() {
            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NaN);
                Assert.fail("Successfully encode a NaN");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.POSITIVE_INFINITY);
                Assert.fail("Successfully encode positive infinity");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NEGATIVE_INFINITY);
                Assert.fail("Successfully encode negative infinity");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_UNSIGNED_MODULUS_ENCODE_SCHEME.encode(-1.0);
                Assert.fail("Successfully encode a negative number using an unsigned ModulusEncodeScheme");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NaN, 1);
                Assert.fail("Successfully encode a NaN with a specific exponent");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.POSITIVE_INFINITY, 1);
                Assert.fail("Successfully encode positive infinity with a specific exponent");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NEGATIVE_INFINITY, 1);
                Assert.fail("Successfully encode negative infinity with a specific exponent");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_UNSIGNED_MODULUS_ENCODE_SCHEME.encode(-1.0, 1);
                Assert.fail(
                    "Successfully encode a negative number with a specific exponent "
                        + "using an unsigned ModulusEncodeScheme"
                );
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NaN, 1e-3);
                Assert.fail("Successfully encode a NaN with a specific precision");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.POSITIVE_INFINITY, 1e-3);
                Assert.fail("Successfully encode positive infinity with a specific precision");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Double.NEGATIVE_INFINITY, 1e-3);
                Assert.fail("Successfully encode negative infinity with a specific precision");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(-1.0, -1e-3);
                Assert.fail("Successfully encode a number with invalid precision");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(-1.0, 1e3);
                Assert.fail("Successfully encode a number with invalid precision");
            } catch (CryptoEncodeException ignored) {

            }

            try {
                DEFAULT_UNSIGNED_MODULUS_ENCODE_SCHEME.encode(-1.0, 1e-3);
                Assert.fail("Successfully encode a negative number using an unsigned Paillier context");
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testAutomaticPrecisionAgreesWithEpsilon() {
            double eps = Math.ulp(1.0);

            double floorHappy = Math.ceil(Math.log(PheParamsTestConfiguration.DEFAULT_BASE) / Math.log(2.0)) * 2;

            for (double i = -floorHappy; i <= floorHappy; i++) {
                PhePlaintext enc1 = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(Math.pow(2.0, i));
                PhePlaintext enc2 = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(
                    Math.pow(2.0, i), (eps * Math.pow(2.0, i))
                );
                Assert.assertEquals(String.valueOf(i), enc1.getExponent(), enc2.getExponent());

                double realEps = eps * Math.pow(2.0, (i - 1));
                double val = Math.pow(2.0, i) - realEps;
                Assert.assertTrue(val != Math.pow(2.0, i));

                PhePlaintext enc3 = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(val);
                PhePlaintext enc4 = DEFAULT_SIGNED_MODULUS_ENCODE_SCHEME.encode(val, realEps);
                Assert.assertEquals(String.valueOf(i), enc3.getExponent(), enc4.getExponent());
            }
        }
    }
}
