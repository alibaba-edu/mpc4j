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
 * 半同态加密乘法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/MultiplicationTest.java
 * </p>
 *
 * @author Brian Thorne, Weiran Liu
 * @date 2017/02/15
 */
@RunWith(Parameterized.class)
public class PheMultiplicationTest {
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

    public PheMultiplicationTest(String name, PheEngineTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        pheEngine = configuration.getPheEngine();
        sk = configuration.getPrivateKey();
        pk = sk.getPublicKey();
    }

    @Test
    public void testDoubleMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果、误差接受度
        double a, b, plainResult, decodedResult, tolerance;
        // 密文被乘数、密文乘法结果
        PheCiphertext ciphertextA, encryptedResult;
        // 编码乘数、编码解密结果
        PhePlaintext encodedB, decryptedResult;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 有可能两个浮点数相乘后为无穷大或未定义，至少要保证明文运算结果是有效的
            do {
                a = PheTestUtils.randomFiniteDouble();
                b = PheTestUtils.randomFiniteDouble();

                if (!pk.isSigned()) {
                    // 无符号编码，把数据都变为正数
                    a = Math.abs(a);
                    b = Math.abs(b);
                }
                // 明文运算结果
                plainResult = a * b;
            } while (Double.isInfinite(plainResult) || Double.isNaN(plainResult));
            // 密文
            ciphertextA = pheEngine.encrypt(pk, a);
            // 编码
            encodedB = pk.encode(b);
            // 密文与明文运算
            encryptedResult = pheEngine.multiply(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeDouble();

            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }

    @Test
    public void testLongMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果
        long a, b, plainResult, decodedResult;
        // 密文被乘数、密文乘法结果
        PheCiphertext ciphertextA, encryptedResult;
        // 编码乘数、编码解密结果
        PhePlaintext encodedB, decryptedResult;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 取整数，且右移一位，防止溢出
            a = PheTestUtils.SECURE_RANDOM.nextInt() >> 1;
            b = PheTestUtils.SECURE_RANDOM.nextInt() >> 1;
            if (!pk.isSigned()) {
                a = Math.abs(a);
                b = Math.abs(b);
            }
            // 明文运算结果
            plainResult = a * b;
            // 密文
            ciphertextA = pheEngine.encrypt(pk, a);
            // 编码
            encodedB = pk.encode(b);
            // 密文与明文运算
            encryptedResult = pheEngine.multiply(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }

    @Test
    public void testBigIntegerMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果
        BigInteger a, b, plainResult, decodedResult;
        // 密文被乘数、密文乘法结果
        PheCiphertext ciphertextA, encryptedResult;
        // 编码乘数、编码解密结果
        PhePlaintext encodedB, decryptedResult;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            do {
                // 精度只取一半减1，防止溢出
                a = new BigInteger((pk.getPrecision() - 1) / 2, PheTestUtils.SECURE_RANDOM);
            } while (!pk.getPlaintextEncoder().isValid(a));
            do {
                // 精度只取一半减1，防止溢出
                b = new BigInteger((pk.getPrecision() - 1) / 2, PheTestUtils.SECURE_RANDOM);
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
            }
            // 明文运算结果，保证明文结果的有效性
            plainResult = a.multiply(b);
            while (!pk.getPlaintextEncoder().isValid(plainResult)) {
                b = b.shiftRight(1);
                plainResult = a.multiply(b);
            }
            // 密文
            ciphertextA = pheEngine.encrypt(pk, a);
            // 编码
            encodedB = pk.encode(b);
            // 密文与明文运算
            encryptedResult = pheEngine.multiply(pk, ciphertextA, encodedB);
            decryptedResult = pheEngine.decrypt(sk, encryptedResult);
            decodedResult = decryptedResult.decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }
}
