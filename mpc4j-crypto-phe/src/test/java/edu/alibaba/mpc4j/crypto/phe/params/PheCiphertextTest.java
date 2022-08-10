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
import edu.alibaba.mpc4j.crypto.phe.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 半同态加密数测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/PaillierEncryptedNumberTest.java
 * </p>
 *
 * @author Brian Thorne, mpnd, Weiran Liu
 * @date 2017/09/21
 */
@RunWith(Enclosed.class)
public class PheCiphertextTest {

    @RunWith(Parameterized.class)
    public static class CiphertextParamTest {
        /**
         * 半同态加密引擎
         */
        private final PheEngine pheEngine;
        /**
         * 私钥
         */
        private final PhePrivateKey sk;
        /**
         * 公钥
         */
        private final PhePublicKey pk;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> configurations() {
            Collection<Object[]> configurationParams = new ArrayList<>();
            // OU98
            configurationParams.addAll(PheEngineTestConfiguration.OU98_NAME_CONFIGURATIONS_40);
            configurationParams.addAll(PheEngineTestConfiguration.OU98_NAME_CONFIGURATIONS_80);
            // Pai99
            configurationParams.addAll(PheEngineTestConfiguration.PAI99_NAME_CONFIGURATIONS_40);
            configurationParams.addAll(PheEngineTestConfiguration.PAI99_NAME_CONFIGURATIONS_80);

            return configurationParams;
        }

        public CiphertextParamTest(String name, PheEngineTestConfiguration configuration) {
            Preconditions.checkArgument(StringUtils.isNotBlank(name));
            pheEngine = configuration.getPheEngine();
            sk = configuration.getPrivateKey();
            pk = sk.getPublicKey();
        }

        @Test
        public void testAutomaticPrecision0() {
            double eps = Math.ulp(1.0d);
            double onePlusEps = 1.0d + eps;
            Assert.assertTrue(onePlusEps > 1);

            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, onePlusEps);
            double decryption1 = pheEngine.decrypt(sk, ciphertext1).decodeDouble();
            Assert.assertEquals(String.valueOf(onePlusEps), String.valueOf(decryption1));

            PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, eps);
            double decryption2 = pheEngine.decrypt(sk, ciphertext2).decodeDouble();
            Assert.assertEquals(String.valueOf(onePlusEps + eps), String.valueOf(decryption2));

            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, eps / 5.0d);
            double decryption3 = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
            Assert.assertEquals(String.valueOf(onePlusEps), String.valueOf(decryption3));

            PheCiphertext ciphertext4 = pheEngine.add(pk, ciphertext3, eps * 4.0d / 5.0d);
            double decryption4 = pheEngine.decrypt(sk, ciphertext4).decodeDouble();
            Assert.assertNotEquals(onePlusEps, decryption4, 0.0d);
            Assert.assertEquals(String.valueOf(onePlusEps + eps), String.valueOf(decryption4));
        }

        @Test
        public void testMulZero() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 3.0);
            PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 0);
            Assert.assertEquals(0.0, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
        }

        @Test
        public void testLongConstants() {
            testEncryptDecryptLong(Long.MAX_VALUE);
            testEncryptDecryptLong(Long.MIN_VALUE);
        }

        @Test
        public void testLongRandom() {
            for (int i = 0; i < 100; ++i) {
                testEncryptDecryptLong(PheTestUtils.SECURE_RANDOM.nextLong());
            }
        }

        private void testEncryptDecryptLong(long value) {
            try {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, value);
                PheCiphertext ciphertext2 = pheEngine.encrypt(sk, value);
                if (value < 0 && !pk.isSigned()) {
                    Assert.fail("ERROR: Successfully encrypted negative integer with unsigned encoding");
                }
                Assert.assertEquals(value, pheEngine.decrypt(sk, ciphertext1).decodeLong());
                Assert.assertEquals(value, pheEngine.decrypt(sk, ciphertext2).decodeLong());
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testDoubleConstants() {
            testEncryptDecryptDouble(Double.MAX_VALUE);
            testEncryptDecryptDouble(Math.nextAfter(Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
            testEncryptDecryptDouble(1.0);
            testEncryptDecryptDouble(Math.nextAfter(Double.MIN_NORMAL, Double.POSITIVE_INFINITY));
            testEncryptDecryptDouble(Double.MIN_NORMAL);
            testEncryptDecryptDouble(Math.nextAfter(Double.MIN_NORMAL, Double.NEGATIVE_INFINITY));
            testEncryptDecryptDouble(Double.MIN_VALUE);
            testEncryptDecryptDouble(0.0);
            testEncryptDecryptDouble(-0.0);
            testEncryptDecryptDouble(-Double.MIN_VALUE);
            testEncryptDecryptDouble(-Math.nextAfter(Double.MIN_NORMAL, Double.NEGATIVE_INFINITY));
            testEncryptDecryptDouble(-Double.MIN_NORMAL);
            testEncryptDecryptDouble(-Math.nextAfter(Double.MIN_NORMAL, Double.POSITIVE_INFINITY));
            testEncryptDecryptDouble(-1.0);
            testEncryptDecryptDouble(-Math.nextAfter(Double.MAX_VALUE, Double.NEGATIVE_INFINITY));
            testEncryptDecryptDouble(-Double.MAX_VALUE);
        }

        @Test
        public void testDoubleRandom() {
            for (int i = 0; i < 100; ++i) {
                testEncryptDecryptDouble(PheTestUtils.randomFiniteDouble());
            }
        }

        private void testEncryptDecryptDouble(double value) {
            try {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, value);
                PheCiphertext ciphertext2 = pheEngine.encrypt(sk, value);
                if (value < 0 && !pk.isSigned()) {
                    Assert.fail("ERROR: Successfully encrypted negative integer with unsigned encoding");
                }
                double tolerance = PheTestUtils.EPSILON;
                double result1 = pheEngine.decrypt(sk, ciphertext1).decodeDouble();
                double result2 = pheEngine.decrypt(sk, ciphertext2).decodeDouble();
                double absValue = Math.abs(value);
                if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                    tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(value));
                }
                Assert.assertEquals(value, result1, tolerance);
                Assert.assertEquals(value, result2, tolerance);
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testBigIntegerConstants() {
            if (pk.isSigned()) {
                testEncryptDecryptBigInteger(pk.getMaxEncoded().negate());
            } else {
                testEncryptDecryptBigInteger(pk.getMinEncoded());
            }
            testEncryptDecryptBigInteger(BigIntegerUtils.LONG_MIN_VALUE);
            testEncryptDecryptBigInteger(BigIntegerUtils.LONG_MIN_VALUE.add(BigInteger.ONE));
            testEncryptDecryptBigInteger(BigInteger.TEN.negate());
            testEncryptDecryptBigInteger(BigInteger.ONE.negate());
            testEncryptDecryptBigInteger(BigInteger.ZERO);
            testEncryptDecryptBigInteger(BigInteger.ONE);
            testEncryptDecryptBigInteger(BigInteger.TEN);
            testEncryptDecryptBigInteger(BigIntegerUtils.LONG_MAX_VALUE.subtract(BigInteger.ONE));
            testEncryptDecryptBigInteger(BigIntegerUtils.LONG_MAX_VALUE);
            testEncryptDecryptBigInteger(pk.getMaxEncoded());
        }

        @Test
        public void testBigIntegerRandom() {
            int[] bitLengths = {16, 32, 64, 128, 256};

            for (int bitLength : bitLengths) {
                for (int j = 0; j < 20; ++j) {
                    testEncryptDecryptBigInteger(PheTestUtils.randomBigInteger(bitLength));
                }
            }
        }

        private void testEncryptDecryptBigInteger(BigInteger value) {
            try {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, value);
                PheCiphertext ciphertext2 = pheEngine.encrypt(sk, value);
                if (value.compareTo(BigInteger.ZERO) < 0 && !pk.isSigned()) {
                    Assert.fail("ERROR: Successfully encrypted negative integer with unsigned encoding");
                }
                Assert.assertEquals(value, pheEngine.decrypt(sk, ciphertext1).decodeBigInteger());
                Assert.assertEquals(value, pheEngine.decrypt(sk, ciphertext2).decodeBigInteger());
            } catch (CryptoEncodeException ignored) {

            }
        }

        @Test
        public void testSubWithDifferentPrecisionFloat0() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(0.1, 1e-3));
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, pk.encode(0.2, 1e-20));
            Assert.assertNotEquals(ciphertext1.getExponent(), ciphertext2.getExponent());

            if (pk.isSigned()) {
                PheCiphertext ciphertext3 = pheEngine.subtract(pk, ciphertext1, ciphertext2);
                Assert.assertEquals(ciphertext2.getExponent(), ciphertext3.getExponent());

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(-0.1, decryption, 1e-3);
            }
        }

        @Test
        public void testEncryptedNegativeLongWithEncryptedLong() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -15);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                long additionResult = pheEngine.decrypt(sk, ciphertext3).decodeLong();
                Assert.assertEquals(-14, additionResult);
            }
        }

        @Test
        public void testAddEncryptedLongs() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 15);
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1);
            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

            long additionResult = pheEngine.decrypt(sk, ciphertext3).decodeLong();
            Assert.assertEquals(16, additionResult);
        }

        @Test
        public void testAddWithEncryptedNegativeLongWithEncryptedNegativeLong() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -15);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, -1);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                long additionResult = pheEngine.decrypt(sk, ciphertext3).decodeLong();
                Assert.assertEquals(-16, additionResult);
            }
        }

        @Test
        public void testSubtractEncryptedLongWithEncryptedLong() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 15);
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1);
            PheCiphertext ciphertext3 = pheEngine.subtract(pk, ciphertext1, ciphertext2);

            long decryption = pheEngine.decrypt(sk, ciphertext3).decodeLong();
            Assert.assertEquals(14, decryption);
        }

        @Test
        public void testAddEncryptedNegativeDoubleWithEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -15.0);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1.0);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(-14.0, decryption, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddEncryptedDoubleWithEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -15.0);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1.0);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext2, ciphertext1);

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(-14.0, decryption, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddEncryptedDoubles() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 15.0);
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, 1.0);
            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

            double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
            Assert.assertEquals(16.0, decryption, PheTestUtils.EPSILON);
        }

        @Test
        public void testAddEncryptedNegativeDoubleWithEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -15.0);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, -1.0);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(-16.0, decryption, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddEncryptedDoubleWithEncryptedNegativeDouble2() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.3842);
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, -0.4);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(0.9842, decryption, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddEncryptedDoublesDiffPrec() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(0.1, 1e-3));
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, pk.encode(0.2, 1e-20));
            Assert.assertNotEquals(ciphertext1.getExponent(), ciphertext2.getExponent());

            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);
            Assert.assertEquals(ciphertext2.getExponent(), ciphertext3.getExponent());

            double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
            Assert.assertEquals(0.3, decryption, PheTestUtils.EPSILON);
        }

        @Test
        public void testSubtractEncryptedDoubleFromEncryptedDoubleDiffPrec() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(0.1, 1e-3));
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, pk.encode(0.2, 1e-20));
                Assert.assertNotEquals(ciphertext1.getExponent(), ciphertext2.getExponent());

                PheCiphertext ciphertext3 = pheEngine.subtract(pk, ciphertext1, ciphertext2);
                Assert.assertEquals(ciphertext2.getExponent(), ciphertext3.getExponent());

                double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
                Assert.assertEquals(-0.1, decryption, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddLongToEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, 4);
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testAddDoubleToEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, 4.0);
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testAddBigIntegerToEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, new BigInteger("4"));
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testAddDoubleWithEncryptedDouble() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.98);
            PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, 4.3);
            Assert.assertEquals(6.28, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testAddNegativeDoubleWithEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 240.9);
                PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, -40.8);
                Assert.assertEquals(200.1, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddLongWithEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 3.9);
                PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, -40);
                Assert.assertEquals(-36.1, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testSubtractLongFromEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, -4);
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testSubtractDoubleFromEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, -4.0);
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testSubtractBigIntegerFromEncryptedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, new BigInteger("-4"));
                Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testSubtractEncryptedDoubleFromEncodedLong() {
            // Right-operation: 4 - encrypt(1.98)
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.98);
            PheCiphertext ciphertext2 = pheEngine.subtract(pk, pk.encode(4), ciphertext1);
            Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
        }

        @Test
        public void testSubtractEncryptedDoubleFromEncodedDouble() {
            // Right-operation: 4 - encrypt(1.98)
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.98);
            PheCiphertext ciphertext2 = pheEngine.subtract(pk, pk.encode(4.0), ciphertext1);
            Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
        }

        @Test
        public void testSubtractEncryptedDoubleFromEncodedBigInteger() {
            // Right-operation: 4 - encrypt(1.98)
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.98);
            PheCiphertext ciphertext2 = pheEngine.subtract(pk, pk.encode(new BigInteger("4")), ciphertext1);
            Assert.assertEquals(2.02, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
        }

        @Test
        public void testSubtractNegativeDoubleWithEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1.98);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, -4.3);
                Assert.assertEquals(6.28, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testSubDoubleFromEncodedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, pk.encode(4.3), ciphertext1);
                Assert.assertEquals(6.28, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testSubtractDoubleFromEncryptedDouble() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 240.9);
            PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, 40.8);
            Assert.assertEquals(200.1, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testSubtractLongFromEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 3.9);
                PheCiphertext ciphertext2 = pheEngine.subtract(pk, ciphertext1, 40);
                Assert.assertEquals(-36.1, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testMultiplyLongByEncryptedNumber() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 4);
                Assert.assertEquals(-7.92, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testMultiplyDoubleByEncryptedDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 4.0);
                Assert.assertEquals(-7.92, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testMultiplyBigIntegerByEncryptedNumber() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, new BigInteger("4"));
                Assert.assertEquals(-7.92, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testMultiplyEncryptedNegativeDoubleWithOne() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.3);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 1);
                double decryption = pheEngine.decrypt(sk, ciphertext2).decodeDouble();

                Assert.assertEquals(ciphertext1.getExponent(), ciphertext2.getExponent());
                Assert.assertEquals(-1.3, decryption, 0.0);
            }
        }

        @Test
        public void testMultiplyEncryptedDoubleWithTwo() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 2.3);
            PhePlaintext two = pk.encode(2);
            PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, two);
            double decryption = pheEngine.decrypt(sk, ciphertext2).decodeDouble();

            Assert.assertEquals(ciphertext1.getExponent() + two.getExponent(), ciphertext2.getExponent());
            Assert.assertEquals(4.6, decryption, 0.0);
        }

        @Test
        public void testMultiplicationResultExponent() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -0.1);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 31.4);
                Assert.assertEquals(-3.14, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
                Assert.assertNotEquals(ciphertext2.getExponent(), ciphertext1.getExponent());

                int expOf314 = pk.encode(-31.4).getExponent();
                Assert.assertEquals(ciphertext2.getExponent(), ciphertext1.getExponent() + expOf314);
            }
        }

        @Test
        public void testMultiplyEncodedDoubleWithEncryptedNumber() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(1.2345678e-12, 1e-14));
            PhePlaintext encoded1 = pk.encode(1.38734864, 1e-2);
            PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, encoded1);
            Assert.assertEquals(1.71e-12, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testMultiplyEncryptedNegativeDoubleWithNegativeOne() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.3);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, -1);
                double decryption = pheEngine.decrypt(sk, ciphertext2).decodeDouble();

                Assert.assertEquals(ciphertext1.getExponent(), ciphertext2.getExponent());
                Assert.assertEquals(1.3, decryption, 0.0);
            }
        }

        @Test
        public void testMultiplyEncryptedDoubleWithNegativeTwo() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 2.3);
                PhePlaintext minusTwo = pk.encode(-2);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, minusTwo);
                double decryption = pheEngine.decrypt(sk, ciphertext2).decodeDouble();

                Assert.assertEquals(ciphertext1.getExponent() + minusTwo.getExponent(), ciphertext2.getExponent());
                Assert.assertEquals(-4.6, decryption, 0.0);
            }
        }

        @Test
        public void testMultiplicationResultExponent2() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -0.1);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, -31.4);

                Assert.assertEquals(3.14, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
                Assert.assertNotEquals(ciphertext2.getExponent(), ciphertext1.getExponent());

                int expOf314 = pk.encode(-31.4).getExponent();
                Assert.assertEquals(ciphertext2.getExponent(), ciphertext1.getExponent() + expOf314);
            }
        }

        @Test
        public void testMultiplyEncodedNegativeDoubleWithEncryptedDouble() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(1.2345678e-12, 1e-14));
            PhePlaintext encoded1 = pk.encode(1.38734864, 1e-2);
            PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, encoded1);
            Assert.assertEquals(-1.71e-12, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testDivideLongByEncryptedNumber() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.divide(pk, ciphertext1, 4);
                Assert.assertEquals(-0.495, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testMultiplyRight() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 0.1);
            PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, 31.4);
            PheCiphertext ciphertext3 = pheEngine.multiply(pk, ciphertext1, pk.encode(31.4));

            Assert.assertEquals(
                pheEngine.decrypt(sk, ciphertext3).decodeDouble(),
                pheEngine.decrypt(sk, ciphertext2).decodeDouble(),
                0.0
            );
            Assert.assertEquals(3.14, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
        }

        @Test
        public void testDivideEncryptedNegativeDoubleByDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -1.98);
                PheCiphertext ciphertext2 = pheEngine.divide(pk, ciphertext1, 4.0);
                Assert.assertEquals(-0.495, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
            }
        }

        @Test
        public void testDivideEncryptedDoubleWithLong() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 6.28);
            PheCiphertext ciphertext2 = pheEngine.divide(pk, ciphertext1, 2);
            Assert.assertEquals(3.14, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);

            PheCiphertext ciphertext3 = pheEngine.divide(pk, ciphertext1, 3.14);
            Assert.assertEquals(2.0, pheEngine.decrypt(sk, ciphertext3).decodeDouble(), 0.0);
        }

        @Test
        public void testAdditiveInverse() {
            if (pk.isSigned()) {
                double number = 1.98;
                PheCiphertext ciphertext = pheEngine.encrypt(pk, number);
                PheCiphertext negativeCiphertext = pheEngine.additiveInverse(pk, ciphertext);
                double decryptedNegativeNumber = pheEngine.decrypt(sk, negativeCiphertext).decodeDouble();
                Assert.assertEquals(-number, decryptedNegativeNumber, PheTestUtils.EPSILON);

                double number2 = -number;
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, number2);
                PheCiphertext negativeCiphertext2 = pheEngine.additiveInverse(pk, ciphertext2);
                double decryptedNegativeNumber2 = pheEngine.decrypt(sk, negativeCiphertext2).decodeDouble();
                Assert.assertEquals(number, decryptedNegativeNumber2, PheTestUtils.EPSILON);
            }
        }

        @Test
        public void testAddEncryptedDoubleWithEncodedDouble() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(0.1, 1e-3));
            PhePlaintext encoded1 = pk.encode(0.2, 1e-20);
            Assert.assertNotEquals(ciphertext1.getExponent(), encoded1.getExponent());

            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, encoded1);
            Assert.assertEquals(encoded1.getExponent(), ciphertext3.getExponent());

            double decryption = pheEngine.decrypt(sk, ciphertext3).decodeDouble();
            Assert.assertEquals(0.3, decryption, 1e-3);
        }

        @Test
        public void testMultiplyEncryptedNegativeDoubleWithEncodedNegativeDouble() {
            if (pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, -0.1);
                PhePlaintext encoded1 = pk.encode(-31.4);
                PheCiphertext ciphertext2 = pheEngine.multiply(pk, ciphertext1, encoded1);

                Assert.assertEquals(3.14, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), 0.0);
                Assert.assertNotEquals(ciphertext2.getExponent(), ciphertext1.getExponent());

                int expOf314 = pk.encode(-31.4).getExponent();
                Assert.assertEquals(ciphertext2.getExponent(), ciphertext1.getExponent() + expOf314);
            }
        }

        @Test
        public void testEncryptIntPositiveOverflowAdd() {
            if (!pk.isFullPrecision()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.getMaxSignificand());
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, BigInteger.ONE);
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                try {
                    BigInteger result = pheEngine.decrypt(sk, ciphertext3).decodeBigInteger();
                    Assert.fail("Successfully decode an overflow encoded number generated by addition operation");
                    Assert.assertNull(result);
                } catch (CryptoDecodeException ignored) {

                }
            }
        }

        @Test
        public void testEncryptIntNegativeOverflowAdd() {
            if (!pk.isFullPrecision() && pk.isSigned()) {
                PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.getMinSignificand());
                PheCiphertext ciphertext2 = pheEngine.encrypt(pk, BigInteger.ONE.negate());
                PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);

                try {
                    BigInteger result = pheEngine.decrypt(sk, ciphertext3).decodeBigInteger();
                    Assert.fail("Successfully decode an overflow encoded number generated by addition operation");
                    Assert.assertNull(result);
                } catch (CryptoDecodeException ignored) {

                }
            }
        }

        @Test
        public void testAddWithEncryptedIntAndEncodedNumberDiffExp0() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 1);
            PhePlaintext encoded1 = pk.encode(0.05);
            Assert.assertTrue(ciphertext1.getExponent() > encoded1.getExponent());

            PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, encoded1);
            Assert.assertEquals(1.05, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testAddWithEncryptedIntAndEncodedNumberDiffExp1() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, 0.05);
            PhePlaintext encoded1 = pk.encode(1);
            Assert.assertTrue(ciphertext1.getExponent() < encoded1.getExponent());

            PheCiphertext ciphertext2 = pheEngine.add(pk, ciphertext1, encoded1);
            Assert.assertEquals(1.05, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testAddWithDifferentPrecisionFloat4() {
            PhePlaintext number1 = pk.encode(0.1, 1e-3);
            PhePlaintext number2 = pk.encode(0.2, 1e-20);

            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, number1);
            PheCiphertext ciphertext2 = pheEngine.encrypt(pk, number2);
            Assert.assertNotEquals(ciphertext1.getExponent(), ciphertext2.getExponent());

            int oldExponent = ciphertext1.getExponent();
            PheCiphertext ciphertext3 = pheEngine.add(pk, ciphertext1, ciphertext2);
            Assert.assertEquals(ciphertext2.getExponent(), ciphertext3.getExponent());
            Assert.assertEquals(oldExponent, ciphertext1.getExponent());

            Assert.assertEquals(0.3, pheEngine.decrypt(sk, ciphertext3).decodeDouble(), PheTestUtils.EPSILON);
        }

        @Test
        public void testDecreaseExponentTo() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(pk, pk.encode(1.01, Math.pow(1.0, -8)));
            Assert.assertTrue(-30 < ciphertext1.getExponent());
            PheCiphertext ciphertext2 = pheEngine.decreaseExponentTo(pk, ciphertext1, -30);

            Assert.assertTrue(-30 < ciphertext1.getExponent());
            Assert.assertEquals(-30, ciphertext2.getExponent());
            Assert.assertEquals(1.01, pheEngine.decrypt(sk, ciphertext2).decodeDouble(), Math.pow(1.0, -8));
        }
    }

    @RunWith(Parameterized.class)
    public static class CiphertextTest {
        /**
         * 明文数组
         */
        private static final BigInteger[] PLAINTEXTS = new BigInteger[] {
            new BigInteger("123456789"), new BigInteger("314159265359"),
            new BigInteger("271828182846"), new BigInteger("-987654321"),
            new BigInteger("-161803398874"), new BigInteger("1414213562373095")
        };
        /**
         * 半同态加密引擎
         */
        private final PheEngine pheEngine;
        /**
         * 半同态加密私钥
         */
        private final PhePrivateKey privateKey;
        /**
         * 半同态加密公钥
         */
        private final PhePublicKey publicKey;
        /**
         * 另一个半同态加密私钥
         */
        private final PhePrivateKey otherPrivateKey;
        /**
         * 另一个半同态加密公钥
         */
        private final PhePublicKey otherPublicKey;
        /**
         * 半精度半同态加密公钥
         */
        private final PhePublicKey partialPublicKey;
        /**
         * 密文数组
         */
        private final PheCiphertext[] ciphertexts;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> configurations() {
            Collection<Object[]> configurationParams = new ArrayList<>();
            // 添加配置项
            configurationParams.add(new Object[] {PheFactory.PheType.PAI99.name(), PheFactory.PheType.PAI99});
            return configurationParams;
        }

        public CiphertextTest(String name, PheFactory.PheType pheType) {
            Preconditions.checkArgument(StringUtils.isNotBlank(name));
            pheEngine = PheFactory.createInstance(pheType, PheTestUtils.SECURE_RANDOM);
            // 生成有符号全精度公私钥
            PheKeyGenParams signedFullKeyGenParams = new PheKeyGenParams(
                PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
                PheFactory.getModulusBitLength(pheType, PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
            );
            privateKey = pheEngine.keyGen(signedFullKeyGenParams);
            publicKey = privateKey.getPublicKey();
            // 生成另一组有符号全精度公私钥
            PheKeyGenParams otherSignedFullKeyGenParams = new PheKeyGenParams(
                PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
                PheFactory.getModulusBitLength(pheType, PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
            );
            otherPrivateKey = pheEngine.keyGen(otherSignedFullKeyGenParams);
            otherPublicKey = otherPrivateKey.getPublicKey();
            // 生成有符号半精度公私钥
            PheKeyGenParams signedPartialKeyGenParams = new PheKeyGenParams(
                PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
                PheFactory.getModulusBitLength(pheType, PheTestUtils.DEFAULT_PHE_SEC_LEVEL) - 2
            );
            PhePrivateKey partialPrivateKey = pheEngine.keyGen(signedPartialKeyGenParams);
            partialPublicKey = partialPrivateKey.getPublicKey();
            // 加密明文
            ciphertexts = Arrays.stream(PLAINTEXTS)
                .map(plaintext -> pheEngine.encrypt(publicKey, plaintext))
                .toArray(PheCiphertext[]::new);
        }

        @Test
        public void testConstructor() {
            PheCiphertext ct;

            try {
                ct = PheCiphertext.fromParams(null, BigInteger.ONE, 0);
                Assert.fail("Successfully created an encrypted number with null public key");
                Assert.assertNull(ct);
            } catch (NullPointerException ignored) {

            }

            try {
                ct = PheCiphertext.fromParams(publicKey, null, 0);
                Assert.fail("Successfully created an encrypted number with null ciphertext");
                Assert.assertNull(ct);
            } catch (NullPointerException ignored) {

            }

            try {
                ct = PheCiphertext.fromParams(publicKey, BigInteger.ONE.negate(), 0);
                Assert.fail("Successfully created an encrypted number with negative ciphertext");
                Assert.assertNull(ct);
            } catch (IllegalArgumentException ignored) {

            }

            try {
                ct = PheCiphertext.fromParams(publicKey, publicKey.getCiphertextModulus().add(BigInteger.ONE), 0);
                Assert.fail("Successfully created an encrypted number with ciphertext greater than ciphertext modulus");
                Assert.assertNull(ct);
            } catch (IllegalArgumentException ignored) {

            }
        }

        @Test
        public void testConstantsPackable() {
            testPackable(publicKey.getMaxEncoded().negate());
            testPackable(BigIntegerUtils.LONG_MIN_VALUE);
            testPackable(BigIntegerUtils.LONG_MIN_VALUE.add(BigInteger.ONE));
            testPackable(BigInteger.TEN.negate());
            testPackable(BigInteger.ONE.negate());
            testPackable(BigInteger.ZERO);
            testPackable(BigInteger.ONE);
            testPackable(BigInteger.TEN);
            testPackable(BigIntegerUtils.LONG_MAX_VALUE.subtract(BigInteger.ONE));
            testPackable(BigIntegerUtils.LONG_MAX_VALUE);
            testPackable(partialPublicKey.getMaxEncoded());
        }

        @Test
        public void testRandomPackable() {
            int[] bitLengths = {16, 32, 64, 128, 256};
            for (int bitLength : bitLengths) {
                for (int j = 0; j < 20; ++j) {
                    testPackable(PheTestUtils.randomBigInteger(bitLength));
                }
            }
        }

        private void testPackable(BigInteger value) {
            PheCiphertext ciphertext1 = pheEngine.encrypt(publicKey, value);
            List<byte[]> byteArrayList = ciphertext1.toByteArrayList();
            PheCiphertext ciphertext2 = PheCiphertext.fromByteArrayList(publicKey, byteArrayList);
            Assert.assertEquals(ciphertext1, ciphertext2);
        }

        @Test
        public void testCantEncryptDecryptIntWithDifferentKey() {
            long data = 1564;
            PheCiphertext ciphertext = pheEngine.encrypt(publicKey, data);
            try {
                pheEngine.decrypt(otherPrivateKey, ciphertext).decodeLong();
                Assert.fail("successfully decrypt a ciphertext with wrong plaintext");
            } catch (CryptoContextMismatchException ignored) {

            }
        }

        @Test
        public void testCantAddWithDifferentKey() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(publicKey, -15);
            PheCiphertext ciphertext2 = pheEngine.encrypt(otherPublicKey, 1);

            try {
                pheEngine.add(publicKey, ciphertext1, ciphertext2);
                Assert.fail("successfully add two ciphertexts with different public keys");
            } catch (CryptoContextMismatchException ignored) {

            }
        }

        @Test
        public void testCantAddEncodedWithDifferentKey() {
            PheCiphertext ciphertext1 = pheEngine.encrypt(publicKey, -15);
            PhePlaintext ciphertext2 = PhePlaintext.fromParams(
                otherPublicKey.getPlaintextEncoder(), BigInteger.ONE, ciphertext1.getExponent()
            );

            try {
                pheEngine.add(publicKey, ciphertext1, ciphertext2);
                Assert.fail("successfully add ciphertext and plaintext with different encode scheme");
            } catch (CryptoContextMismatchException ignored) {

            }
        }

        @Test
        public void testMultipleAddWithEncryptDecryptInt0() {
            PheCiphertext ciphertext = pheEngine.add(publicKey, ciphertexts[0], ciphertexts[1]);
            ciphertext = pheEngine.add(publicKey, ciphertext, ciphertexts[2]);
            BigInteger decryption = pheEngine.decrypt(privateKey, ciphertext).decodeBigInteger();

            BigInteger expectedResult = PLAINTEXTS[0].add(PLAINTEXTS[1]).add(PLAINTEXTS[2]);
            Assert.assertEquals(expectedResult, decryption);
        }

        @Test
        public void testMultipleAddWithEncryptDecryptInt1() {
            PheCiphertext ciphertext = pheEngine.add(publicKey, ciphertexts[3], ciphertexts[4]);
            ciphertext = pheEngine.add(publicKey, ciphertext, ciphertexts[5]);
            BigInteger decryption = pheEngine.decrypt(privateKey, ciphertext).decodeBigInteger();

            BigInteger expectedResult = PLAINTEXTS[3].add(PLAINTEXTS[4]).add(PLAINTEXTS[5]);
            Assert.assertEquals(expectedResult, decryption);
        }

        @Test
        public void testMultipleAddWithEncryptDecryptInt2() {
            PheCiphertext ciphertext1 = pheEngine.add(publicKey, ciphertexts[0], ciphertexts[1]);
            ciphertext1 = pheEngine.add(publicKey, ciphertext1, ciphertexts[2]);
            PheCiphertext ciphertext2 = pheEngine.add(publicKey, ciphertexts[3], ciphertexts[4]);
            PheCiphertext ciphertext3 = pheEngine.add(publicKey, ciphertext1, ciphertext2);
            BigInteger decryption = pheEngine.decrypt(privateKey, ciphertext3).decodeBigInteger();

            BigInteger expectedResult1 = PLAINTEXTS[0].add(PLAINTEXTS[1]).add(PLAINTEXTS[2]);
            BigInteger expectedResult2 = PLAINTEXTS[3].add(PLAINTEXTS[4]);
            BigInteger expectedResult3 = expectedResult1.add(expectedResult2);

            Assert.assertEquals(expectedResult3, decryption);
        }

        @Test
        public void testMultipleAddWithEncryptDecryptInt3() {
            PheCiphertext ciphertext1 = pheEngine.add(publicKey, ciphertexts[0], ciphertexts[1]);
            ciphertext1 = pheEngine.add(publicKey, ciphertext1, ciphertexts[2]);
            PheCiphertext ciphertext2 = pheEngine.add(publicKey, ciphertexts[3], ciphertexts[4]);
            ciphertext2 = pheEngine.add(publicKey, ciphertext2, ciphertexts[5]);
            PheCiphertext ciphertext3 = pheEngine.add(publicKey, ciphertext1, ciphertext2);
            BigInteger decryption = pheEngine.decrypt(privateKey, ciphertext3).decodeBigInteger();

            BigInteger expectedResult1 = PLAINTEXTS[0].add(PLAINTEXTS[1]).add(PLAINTEXTS[2]);
            BigInteger expectedResult2 = PLAINTEXTS[3].add(PLAINTEXTS[4]).add(PLAINTEXTS[5]);
            BigInteger expectedResult3 = expectedResult1.add(expectedResult2);

            Assert.assertEquals(expectedResult3, decryption);
        }

        @Test
        public void testMultipleAddWithEncryptDecryptIntLimits() {
            BigInteger sum3Pos2Neg1 = PLAINTEXTS[0].add(PLAINTEXTS[1]).add(PLAINTEXTS[2]);
            BigInteger sum3Pos2Neg2 = PLAINTEXTS[3].add(PLAINTEXTS[4]);
            BigInteger sum3Pos2Neg3 = sum3Pos2Neg1.add(sum3Pos2Neg2);

            BigInteger sum3Pos3Neg1 = PLAINTEXTS[0].add(PLAINTEXTS[1]).add(PLAINTEXTS[2]);
            BigInteger sum3Pos3Neg2 = PLAINTEXTS[3].add(PLAINTEXTS[4]).add(PLAINTEXTS[5]);
            BigInteger sum3Pos3Neg3 = sum3Pos3Neg1.add(sum3Pos3Neg2);

            PheCiphertext ciphertextSum3Pos2Neg1 = pheEngine.add(publicKey, ciphertexts[0], ciphertexts[1]);
            ciphertextSum3Pos2Neg1 = pheEngine.add(publicKey, ciphertextSum3Pos2Neg1, ciphertexts[2]);
            PheCiphertext ciphertextSum3Pos2Neg2 = pheEngine.add(publicKey, ciphertexts[3], ciphertexts[4]);
            PheCiphertext ciphertextSum3Pos2Neg3 = pheEngine.add(
                publicKey, ciphertextSum3Pos2Neg1, ciphertextSum3Pos2Neg2
            );

            PheCiphertext ciphertextSum3Pos3Neg1 = pheEngine.add(publicKey, ciphertexts[0], ciphertexts[1]);
            ciphertextSum3Pos3Neg1 = pheEngine.add(publicKey, ciphertextSum3Pos3Neg1, ciphertexts[2]);
            PheCiphertext ciphertextSum3Pos3Neg2 = pheEngine.add(publicKey, ciphertexts[3], ciphertexts[4]);
            ciphertextSum3Pos3Neg2 = pheEngine.add(publicKey, ciphertextSum3Pos3Neg2, ciphertexts[5]);
            PheCiphertext ciphertextSum3Pos3Neg3 = pheEngine.add(
                publicKey, ciphertextSum3Pos3Neg1, ciphertextSum3Pos3Neg2
            );

            // Add many positive and negative numbers to reach maxInt.
            PheCiphertext ciphertext1 = pheEngine.encrypt(
                publicKey, publicKey.getMaxSignificand().subtract(sum3Pos2Neg3)
            );
            PheCiphertext ciphertext2 = pheEngine.add(publicKey, ciphertextSum3Pos2Neg3, ciphertext1);
            BigInteger decryption = pheEngine.decrypt(privateKey, ciphertext2).decodeBigInteger();
            Assert.assertEquals(publicKey.getMaxSignificand(), decryption);

            // Add many positive and negative numbers to reach -maxInt.
            PheCiphertext ciphertext3 = pheEngine.encrypt(
                publicKey, publicKey.getMinSignificand().add(sum3Pos3Neg3)
            );
            PheCiphertext ciphertext4 = pheEngine.subtract(publicKey, ciphertext3, ciphertextSum3Pos3Neg3);
            BigInteger decryption2 = pheEngine.decrypt(privateKey, ciphertext4).decodeBigInteger();
            Assert.assertEquals(publicKey.getMinSignificand(), decryption2);
        }

        @Test
        public void testRawCiphertextObfuscation() {
            PheCiphertext encryptedNumber = pheEngine.encrypt(publicKey, 3.14);
            BigInteger ciphertext = encryptedNumber.getCiphertext();
            BigInteger obfuscateCiphertext = pheEngine.rawObfuscate(publicKey, ciphertext);
            Assert.assertNotEquals(obfuscateCiphertext, ciphertext);

            BigInteger rawDecryption1 = pheEngine.rawDecrypt(privateKey, ciphertext);
            BigInteger rawDecryption2 = pheEngine.rawDecrypt(privateKey, obfuscateCiphertext);
            Assert.assertEquals(rawDecryption1, rawDecryption2);
        }

        @Test
        public void testCiphertextObfuscation() {
            PheCiphertext encryptedNumber = pheEngine.encrypt(publicKey, 3.14);
            PheCiphertext obfuscatedEncryptedNumber = pheEngine.obfuscate(publicKey, encryptedNumber);
            Assert.assertNotEquals(encryptedNumber, obfuscatedEncryptedNumber);

            double decryption1 = pheEngine.decrypt(privateKey, encryptedNumber).decodeDouble();
            double decryption2 = pheEngine.decrypt(privateKey, obfuscatedEncryptedNumber).decodeDouble();
            Assert.assertEquals(decryption1, decryption2, 0.0);
        }

        @Test
        public void testAddObfuscated() {
            PheCiphertext encryptedNumber1 = pheEngine.encrypt(publicKey, 94.5);
            PheCiphertext encryptedNumber2 = pheEngine.encrypt(publicKey, 107.3);
            PheCiphertext encryptedNumber3 = pheEngine.add(publicKey, encryptedNumber1, encryptedNumber2);
            PheCiphertext encryptedNumber4 = pheEngine.obfuscate(publicKey, encryptedNumber3);
            Assert.assertNotEquals(encryptedNumber3, encryptedNumber4);

            double decryption1 = pheEngine.decrypt(privateKey, encryptedNumber3).decodeDouble();
            double decryption2 = pheEngine.decrypt(privateKey, encryptedNumber4).decodeDouble();
            Assert.assertEquals(decryption1, decryption2, 0.0);
        }

        @Test
        public void testEquals() {
            PheCiphertext encrypted = pheEngine.encrypt(publicKey, 17);
            PheCiphertext partialEncrypted = pheEngine.encrypt(partialPublicKey, 17);
            // Compare to itself
            Assert.assertEquals(encrypted, encrypted);
            // Compare to null
            Assert.assertNotEquals(null, encrypted);

            PheCiphertext encrypted2 = pheEngine.encrypt(publicKey, 3.14);
            // Compare to an encrypted number with different value
            Assert.assertNotEquals(encrypted, encrypted2);
            // Compare to an encrypted number with different context
            Assert.assertNotEquals(encrypted, partialEncrypted);
        }

        @Test
        public void testDecreaseInvalidExponent() {
            PheCiphertext ciphertext = pheEngine.encrypt(publicKey, publicKey.encode(1.01, 1e-8));
            Assert.assertTrue(ciphertext.getExponent() < 20);

            try {
                pheEngine.decreaseExponentTo(publicKey, ciphertext, 20);
                Assert.fail("successfully decrease the exponent of a ciphertext to have a negative number");
            } catch (IllegalArgumentException ignored) {

            }
        }
    }
}
