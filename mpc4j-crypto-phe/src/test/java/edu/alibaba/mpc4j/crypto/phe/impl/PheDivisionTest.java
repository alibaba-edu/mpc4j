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
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 半同态加密除法测试。部分源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/DivisionTest.java
 * </p>
 *
 * @author Brian Thorne, Weiran Liu
 * @date 2017/02/15
 */
@RunWith(Parameterized.class)
public class PheDivisionTest {
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

    public PheDivisionTest(String name, PheEngineTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        pheEngine = configuration.getPheEngine();
        sk = configuration.getPrivateKey();
        pk = sk.getPublicKey();
    }

    @Test
    public void testDivideDouble() {
        // 明文被除数、明文除数、明文除数倒数、明文除法结果、密文除法解码结果、误差接受度
        double a, b, invertedB, plainResult, decodedResult, tolerance;
        // 密文被除数、密文除法结果
        PheCiphertext ciphertextA, encryptedResult;

        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.randomFiniteDouble();
            if (!pk.isSigned()) {
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
            // 密文运算
            ciphertextA = pheEngine.encrypt(pk, a);
            encryptedResult = pheEngine.divide(pk, ciphertextA, b);
            decodedResult = pheEngine.decrypt(sk, encryptedResult).decodeDouble();
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
    public void testDivideLong() {
        // 明文被除数、明文除数倒数、明文除法结果、密文除法解码结果、误差接受度
        double a, invertedB, plainResult, decodedResult, tolerance;
        // 明文除数
        long b;
        // 密文被除数、密文除法结果
        PheCiphertext ciphertextA, encryptedResult;
        for (int i = 0; i < PheTestUtils.MAX_ITERATIONS; i++) {
            a = PheTestUtils.randomFiniteDouble();
            b = PheTestUtils.SECURE_RANDOM.nextLong();
            if (!pk.isSigned()) {
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
            // 密文运算
            ciphertextA = pheEngine.encrypt(pk, a);
            encryptedResult = pheEngine.divide(pk, ciphertextA, b);
            decodedResult = pheEngine.decrypt(sk, encryptedResult).decodeDouble();
            double absValue = Math.abs(plainResult);
            if (Precision.equals(absValue, 0.0, Double.MIN_VALUE) || absValue > 1.0) {
                tolerance = PheTestUtils.EPSILON * Math.pow(2.0, Math.getExponent(plainResult));
            } else {
                tolerance = PheTestUtils.EPSILON;
            }
            Assert.assertEquals(plainResult, decodedResult, tolerance);
        }
    }
}
