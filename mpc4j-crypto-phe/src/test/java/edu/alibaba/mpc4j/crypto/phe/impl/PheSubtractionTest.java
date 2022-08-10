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
package edu.alibaba.mpc4j.crypto.phe.impl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.crypto.phe.PheEngine;
import edu.alibaba.mpc4j.crypto.phe.PheEngineTestConfiguration;
import edu.alibaba.mpc4j.crypto.phe.PheTestUtils;
import edu.alibaba.mpc4j.crypto.phe.params.PheCiphertext;
import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintext;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 半同态加密减法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/SubtractionTest.java
 * </p>
 *
 * @author Wilko, Weiran Liu
 * @date 2017/02/16
 */
@RunWith(Parameterized.class)
public class PheSubtractionTest {
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

    public PheSubtractionTest(String name, PheEngineTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        pheEngine = configuration.getPheEngine();
        sk = configuration.getPrivateKey();
        pk = sk.getPublicKey();
    }

    @Test
    public void testDoubleSubtraction() {
        // 明文被减数、明文减数、明文减法结果、减法解码结果、误差接受度
        double a, b, plainResult, decodedResult, tolerance;
        // 密文被减数、密文减数、密文减法结果
        PheCiphertext ciphertextA, ciphertextB, encryptedResult;
        // 编码被减数、编码减数、编码解密结果
        PhePlaintext encodedA, encodedB, decryptedResult;
        int maxExponentDiff = (int) (0.5 * pk.getMaxEncoded().bitLength()
            / DoubleUtils.log2(pk.getBase()) - (Math.ceil(Math.log(1L << 53) / Math.log(pk.getBase()))));
        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 每轮生成两个随机的浮点数
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.randomFiniteDouble();
            if (!pk.isSigned()) {
                // 无符号编码，把数据都变为正数，且保证a >= b
                a = Math.abs(a);
                b = Math.abs(b);
                if (a < b) {
                    double tmp = a;
                    a = b;
                    b = tmp;
                }
            }
            encodedA = pk.encode(a);
            encodedB = pk.encode(b);
            // 如果有溢出，需要重新调整b
            if (Math.abs(encodedA.getExponent() - encodedB.getExponent()) > maxExponentDiff) {
                int newExp = encodedA.getExponent()
                    - (int) Math.round((PheTestUtils.SECURE_RANDOM.nextDouble()) * maxExponentDiff);
                encodedB = PhePlaintext.fromParams(pk.getPlaintextEncoder(), encodedB.getValue(), newExp);
            }
            b = encodedB.decodeDouble();
            if (!pk.isSigned()) {
                // now that we changed b, we have to check again if a < b
                if (a < b) {
                    double tmp = a;
                    a = b;
                    b = tmp;
                    encodedA = pk.encode(a);
                }
            }
            encodedB = pk.encode(b);
            // 明文运算
            plainResult = a - b;
            // 密文运算
            ciphertextA = pheEngine.encrypt(pk, a);
            ciphertextB = pheEngine.encrypt(pk, b);
            // 允许误差量
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            // 密文与密文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
            // 密文与明文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
            // 明文与密文运算
            encryptedResult = pheEngine.subtract(pk, encodedA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }

    @Test
    public void testLongSubtraction() {
        // 明文被减数、明文减数、明文减法结果、密文减法解码结果
        long a, b, plainResult, decodedResult;
        // 密文被减数、密文减数、密文减法结果
        PheCiphertext ciphertextA, ciphertextB, encryptedResult;
        // 编码被减数、编码减数、编码解密结果
        PhePlaintext encodedA, encodedB, decryptedResult;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 随机生成两个长整数
            a = PheTestUtils.SECURE_RANDOM.nextLong() >> 1;
            b = PheTestUtils.SECURE_RANDOM.nextLong() >> 1;

            if (!pk.isSigned()) {
                // 无符号编码，把数据都变为正数，且保证a >= b
                a = Math.abs(a);
                b = Math.abs(b);
                if (a < b) {
                    long tmp = a;
                    a = b;
                    b = tmp;
                }
            }
            // 明文运算
            plainResult = a - b;
            // 密文运算
            ciphertextA = pheEngine.encrypt(pk, a);
            ciphertextB = pheEngine.encrypt(pk, b);
            encodedA = pk.encode(a);
            encodedB = pk.encode(b);
            // 密文与密文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
            // 密文与明文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
            // 明文与密文运算
            encryptedResult = pheEngine.subtract(pk, encodedA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }

    @Test
    public void testBigIntegerSubtraction() {
        // 明文被减数、明文减数、明文减法结果、密文减法解码结果
        BigInteger a, b, plainResult, decodedResult;
        // 密文被减数、密文减数、密文减法结果
        PheCiphertext ciphertextA, ciphertextB, encryptedResult;
        // 编码被减数、编码减数、编码解密结果
        PhePlaintext encodedA, encodedB, decryptedResult;
        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 明文a和b都选择精度减3的随机数，让出符号位、求和后影响符号位、以及求和后超过模数的情况
            do {
                a = new BigInteger(pk.getPrecision() - 3, PheTestUtils.SECURE_RANDOM);
            } while (!pk.getPlaintextEncoder().isValid(a));
            do {
                b = new BigInteger(pk.getPrecision() - 3, PheTestUtils.SECURE_RANDOM);
            } while (!pk.getPlaintextEncoder().isValid(b));
            // The random generator above only generates positive BigIntegers, the following code negates some inputs.
            if (pk.isSigned()) {
                if (i % 4 == 1) {
                    b = b.negate();
                } else if (i % 4 == 2) {
                    a = a.negate();
                } else if (i % 4 == 3) {
                    a = a.negate();
                    b = b.negate();
                }
            } else {
                // 无符号编码，保证a >= b
                if (a.compareTo(b) < 0) {
                    BigInteger tmp = a;
                    a = b;
                    b = tmp;
                }
            }
            // 明文运算，保证运算结果的有效性
            plainResult = a.subtract(b);
            while (!pk.getPlaintextEncoder().isValid(plainResult)) {
                a = a.shiftRight(1);
                b = b.shiftRight(1);
                plainResult = a.subtract(b);
            }
            ciphertextA = pheEngine.encrypt(pk, a);
            ciphertextB = pheEngine.encrypt(pk, b);
            encodedA = pk.encode(a);
            encodedB = pk.encode(b);
            // 密文与密文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
            // 密文与明文运算
            encryptedResult = pheEngine.subtract(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
            // 明文与密文运算
            encryptedResult = pheEngine.subtract(pk, encodedA, ciphertextB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }
}
