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
import edu.alibaba.mpc4j.crypto.phe.PheParamsTestConfiguration;
import edu.alibaba.mpc4j.crypto.phe.PheTestUtils;
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
 * 模数编码乘法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/MultiplicationTest.java
 * </p>
 *
 * @author Brian Thorne, Weiran Liu
 * @date 2017/02/15
 */
@RunWith(Parameterized.class)
public class PhePlaintextMultiplicationTest {
    /**
     * 编码方案
     */
    private final PhePlaintextEncoder encodeScheme;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_512);
        configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_1024);

        return configurationParams;
    }

    public PhePlaintextMultiplicationTest(String name, PheParamsTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        encodeScheme = configuration.getPlaintextEncoder();
    }

    @Test
    public void testDoubleMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果、误差接受度
        double a, b, plainResult, decodedResult, tolerance;
        // 编码被乘数、编码乘数
        PhePlaintext encodedA, encodedB;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 有可能两个浮点数相乘后为无穷大或未定义，至少要保证明文运算结果是有效的
            do {
                a = PheTestUtils.randomFiniteDouble();
                b = PheTestUtils.randomFiniteDouble();

                if (!encodeScheme.isSigned()) {
                    // 无符号编码，把数据都变为正数
                    a = Math.abs(a);
                    b = Math.abs(b);
                }
                // 明文运算结果
                plainResult = a * b;
            } while (Double.isInfinite(plainResult) || Double.isNaN(plainResult));
            // 误差容忍度
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            // 编码与编码运算
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            decodedResult = encodedA.multiply(encodedB).decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
            decodedResult = encodedB.multiply(encodedA).decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }

    @Test
    public void testLongMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果
        long a, b, plainResult, decodedResult;
        // 编码被乘数、编码乘数、编码解密结果
        PhePlaintext encodedA, encodedB;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 取整数，且右移一位，防止溢出
            a = PheTestUtils.SECURE_RANDOM.nextInt() >> 1;
            b = PheTestUtils.SECURE_RANDOM.nextInt() >> 1;
            if (!encodeScheme.isSigned()) {
                a = Math.abs(a);
                b = Math.abs(b);
            }
            // 明文运算结果
            plainResult = a * b;
            // 编码运算结果
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            decodedResult = encodedA.multiply(encodedB).decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
            decodedResult = encodedB.multiply(encodedA).decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }

    @Test
    public void testBigIntegerMultiplication() {
        // 明文被乘数、明文乘数、明文乘法结果、乘法解码结果
        BigInteger a, b, plainResult, decodedResult;
        // 编码被乘数、编码乘数
        PhePlaintext encodedA, encodedB;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            do {
                // 精度只取一半减1，防止溢出
                a = new BigInteger((encodeScheme.getPrecision() - 1) / 2, PheTestUtils.SECURE_RANDOM);
            } while (!encodeScheme.isValid(a));
            do {
                // 精度只取一半减1，防止溢出
                b = new BigInteger((encodeScheme.getPrecision() - 1) / 2, PheTestUtils.SECURE_RANDOM);
            } while (!encodeScheme.isValid(b));
            // The random generator above only generates positive BigIntegers, the following code negates some inputs.
            if (encodeScheme.isSigned()) {
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
            while (!encodeScheme.isValid(plainResult)) {
                b = b.shiftRight(1);
                plainResult = a.multiply(b);
            }
            // 编码运算结果
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            decodedResult = encodedA.multiply(encodedB).decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
            decodedResult = encodedB.multiply(encodedA).decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }
}
