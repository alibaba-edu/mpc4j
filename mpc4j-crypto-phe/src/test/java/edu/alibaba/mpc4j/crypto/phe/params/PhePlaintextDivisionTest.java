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

import java.util.ArrayList;
import java.util.Collection;

/**
 * 模数编码除法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/DivisionTest.java
 * </p>
 *
 * @author Brian Thorne, Weiran Liu
 * @date 2017/02/15
 */
@RunWith(Parameterized.class)
public class PhePlaintextDivisionTest {
    /**
     * 编码方案
     */
    private final PhePlaintextEncoder encodeScheme;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_512);
        configurationParams.addAll(PheParamsTestConfiguration.NAME_CONFIGURATION_1024);

        return configurationParams;
    }

    public PhePlaintextDivisionTest(String name, PheParamsTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        encodeScheme = configuration.getPlaintextEncoder();
    }

    @Test
    public void testDivideDouble() {
        // 明文被除数、明文除数、明文除数倒数、明文除法结果、除法解码结果、误差接受度
        double a, b, invertedB, plainResult, decodedResult, tolerance;
        // 编码被除数、编码除数
        PhePlaintext encodedA;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.randomFiniteDouble();
            if (!encodeScheme.isSigned()) {
                // 无符号编码，均转换为正数
                a = Math.abs(a);
                b = Math.abs(b);
            }
            // 求倒数
            invertedB = 1 / b;
            if (Double.isInfinite(invertedB) || Double.isNaN(invertedB)) {
                continue;
            }
            plainResult = a / b;
            if (Double.isInfinite(plainResult) || Double.isNaN(plainResult)) {
                continue;
            }
            // 误差容忍度
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0, DoubleUtils.PRECISION) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            // 编码运算
            encodedA = encodeScheme.encode(a);
            decodedResult = encodedA.divide(b).decodeDouble();
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }

    @Test
    public void testDivideLong() {
        // 明文被除数、明文除数倒数、明文除法结果、密文除法解码结果、误差接受度
        double a, invertedB, plainResult, decodedResult, tolerance;
        // 明文除数
        long b;
        // 编码被除数
        PhePlaintext encodedA;
        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.SECURE_RANDOM.nextLong();
            if (!encodeScheme.isSigned()) {
                // 无符号编码，均转换为正数
                a = Math.abs(a);
                b = Math.abs(b);
            }
            // 明文倒数
            invertedB = 1 / (double)b;
            if (Double.isInfinite(invertedB) || Double.isNaN(invertedB)) {
                continue;
            }
            plainResult = a / (double)b;
            if (Double.isInfinite(plainResult) || Double.isNaN(plainResult)) {
                continue;
            }
            // 错误容忍度
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            // 编码运算
            encodedA = encodeScheme.encode(a);
            decodedResult = encodedA.divide(b).decodeDouble();

            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }
}
