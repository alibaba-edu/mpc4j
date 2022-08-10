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
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
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
 * 模数编码加法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/AdditionTest.java
 * </p>
 *
 * @author Brian Thorne, Weiran Liu
 * @date 2017/02/15
 */
@RunWith(Parameterized.class)
public class PhePlaintextAdditionTest {
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

    public PhePlaintextAdditionTest(String name, PheParamsTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        encodeScheme = configuration.getPlaintextEncoder();
    }

    @Test
    public void testDoubleAddition() {
        // 明文被加数、明文加数、明文加法结果、加法解码结果、误差接受度
        double a, b, plainResult, decodedResult, tolerance;
        // 编码被加数、编码加数
        PhePlaintext encodedA, encodedB;
        int maxExponentDiff = (int) (0.5 * encodeScheme.getModulus().bitLength() / DoubleUtils.log2(encodeScheme.getBase()));

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.randomFiniteDouble();
            // 如果是无符号编码，则生成结果都转换为正数
            if (!encodeScheme.isSigned() && (a < 0 || b < 0)) {
                a = Math.abs(a);
                b = Math.abs(b);
            }
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            // 如果出现溢出，则纠正溢出值
            if (Math.abs(encodedA.getExponent() - encodedB.getExponent()) > maxExponentDiff) {
                int newExp = encodedA.getExponent()
                    - (int) Math.round((PheTestUtils.SECURE_RANDOM.nextDouble()) * maxExponentDiff);
                encodedB = PhePlaintext.fromParams(encodeScheme, encodedB.getValue(), newExp);
            }
            b = encodedB.decodeDouble();
            encodedB = encodeScheme.encode(b);
            // 明文运算
            plainResult = a + b;
            // 误差容忍度
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            // 编码与编码运算
            decodedResult = encodedA.add(encodedB).decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
            decodedResult = encodedB.add(encodedA).decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }

    @Test
    public void testLongAddition() {
        // 明文被加数、明文加数、明文加法结果、加法解码结果
        long a, b, plainResult, decodedResult;
        // 编码被加数、编码加数
        PhePlaintext encodedA, encodedB;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 生成2个随机长整数，为避免溢出，右移1位
            a = PheTestUtils.SECURE_RANDOM.nextLong() >> 1;
            b = PheTestUtils.SECURE_RANDOM.nextLong() >> 1;
            // 如果是无符号编码，则生成结果都转换为正数
            if (!encodeScheme.isSigned() && (a < 0 || b < 0)) {
                a = Math.abs(a);
                b = Math.abs(b);
            }
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            // 明文运算
            plainResult = a + b;
            // 编码与编码运算
            decodedResult = encodedA.add(encodedB).decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
            decodedResult = encodedB.add(encodedA).decodeLong();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }

    @Test
    public void testBigIntegerAddition() {
        // 明文被加数、明文加数、明文加法结果、加法解码结果
        BigInteger a, b, plainResult, decodedResult;
        // 编码被加数、编码加数
        PhePlaintext encodedA, encodedB;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            // 明文a和b都选择精度减3的随机数，让出符号位、求和后影响符号位、以及求和后超过模数的情况
            do {
                a = new BigInteger(encodeScheme.getPrecision() - 3, PheTestUtils.SECURE_RANDOM);
            } while (!encodeScheme.isValid(a));
            do {
                b = new BigInteger(encodeScheme.getPrecision() - 3, PheTestUtils.SECURE_RANDOM);
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
            // 明文运算，保证运算结果的有效性
            plainResult = a.add(b);
            while (!encodeScheme.isValid(plainResult)) {
                a = a.shiftRight(1);
                b = b.shiftRight(1);
                plainResult = a.add(b);
            }
            encodedA = encodeScheme.encode(a);
            encodedB = encodeScheme.encode(b);
            // 编码与编码运算
            decodedResult = encodedA.add(encodedB).decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
            decodedResult = encodedB.add(encodedA).decodeBigInteger();
            Assert.assertEquals(plainResult, decodedResult);
        }
    }
}
