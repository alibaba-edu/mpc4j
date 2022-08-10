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
package edu.alibaba.mpc4j.crypto.phe;

import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintextEncoder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 半同态加密编码测试配置信息。源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/TestConfiguration.java
 * </p>
 *
 * @author Brian Thorne, mpmd, Weiran Liu
 * @date 2017/09/21
 */
public class PheParamsTestConfiguration {
    /**
     * 默认底数
     */
    public static final int DEFAULT_BASE = 16;
    /**
     * 512比特模数配置项
     */
    public static final List<Object[]> NAME_CONFIGURATION_512 = generateNameConfigurations(512);
    /**
     * 1024比特模数配置项，此为默认测试项
     */
    public static final List<Object[]> NAME_CONFIGURATION_1024 = generateNameConfigurations(1024);
    public static final PheParamsTestConfiguration SIGNED_FULL_PRECISION_1024 = createSignedFullPrecision(1024);
    public static final PheParamsTestConfiguration UNSIGNED_FULL_PRECISION_1024 = createUnsignedFullPrecision(1024);
    public static final PheParamsTestConfiguration SIGNED_PARTIAL_PRECISION_1024 = createSignedPartialPrecision(1024);

    private static List<Object[]> generateNameConfigurations(int modulusBitLength) {
        PheParamsTestConfiguration[] configurations = new PheParamsTestConfiguration[] {
            createUnsignedFullPrecision(modulusBitLength),
            createUnsignedPartialPrecision(modulusBitLength),
            createSignedFullPrecision(modulusBitLength),
            createSignedPartialPrecision(modulusBitLength),
            createUnsignedFullPrecision(modulusBitLength, 2),
            createUnsignedPartialPrecision(modulusBitLength, 2),
            createSignedFullPrecision(modulusBitLength, 2),
            createSignedPartialPrecision(modulusBitLength, 2),
            createUnsignedFullPrecision(modulusBitLength, 13),
            createUnsignedPartialPrecision(modulusBitLength, 13),
            createSignedFullPrecision(modulusBitLength, 13),
            createSignedPartialPrecision(modulusBitLength, 13),
            createUnsignedFullPrecision(modulusBitLength, 64),
            createUnsignedPartialPrecision(modulusBitLength, 64),
            createSignedFullPrecision(modulusBitLength, 64),
            createSignedPartialPrecision(modulusBitLength, 64),
        };

        return Arrays.stream(configurations)
            .map(configuration ->  new Object[] {PheParamsTestConfiguration.getName(configuration), configuration})
            .collect(Collectors.toList());
    }

    private static String getName(PheParamsTestConfiguration configuration) {
        PhePlaintextEncoder plaintextEncoder = configuration.getPlaintextEncoder();
        return (plaintextEncoder.isSigned() ? "SIGNED_" : "UNSIGNED_")
            + (plaintextEncoder.isFullPrecision() ? "FULL_" : "PARTIAL_")
            + plaintextEncoder.getModulus().bitLength() + "_"
            + "BASE_" + plaintextEncoder.getBase();
    }

    /**
     * 编码方案
     */
    private final PhePlaintextEncoder plaintextEncoder;

    private PheParamsTestConfiguration(PhePlaintextEncoder plaintextEncoder) {
        this.plaintextEncoder = plaintextEncoder;
    }

    /**
     * 创建测试配置。
     *
     * @param modulusBitLength 模数比特长度。
     * @param signed           是否支持有符号数。
     * @param precision        精度。
     * @param base             底数。
     * @return 测试配置。
     */
    private static PheParamsTestConfiguration create(int modulusBitLength, boolean signed, int precision, int base) {
        BigInteger modulus = BigInteger.probablePrime(modulusBitLength, PheTestUtils.SECURE_RANDOM);
        PhePlaintextEncoder plaintextEncoder = PhePlaintextEncoder.fromParams(modulus, signed, precision, base);
        return new PheParamsTestConfiguration(plaintextEncoder);
    }

    public static PheParamsTestConfiguration createUnsignedFullPrecision(int modulusBitLength) {
        return create(modulusBitLength, false, modulusBitLength, DEFAULT_BASE);
    }

    public static PheParamsTestConfiguration createUnsignedFullPrecision(int modulusBitLength, int base) {
        return create(modulusBitLength, false, modulusBitLength, base);
    }

    public static PheParamsTestConfiguration createUnsignedPartialPrecision(int modulusBitLength) {
        return create(modulusBitLength, false, modulusBitLength - 2, DEFAULT_BASE);
    }

    public static PheParamsTestConfiguration createUnsignedPartialPrecision(int modulusBitLength, int base) {
        return create(modulusBitLength, false, modulusBitLength - 2, base);
    }

    public static PheParamsTestConfiguration createSignedFullPrecision(int modulusBitLength) {
        return create(modulusBitLength, true, modulusBitLength, DEFAULT_BASE);
    }

    public static PheParamsTestConfiguration createSignedFullPrecision(int modulusBitLength, int base) {
        return create(modulusBitLength, true, modulusBitLength, base);
    }

    public static PheParamsTestConfiguration createSignedPartialPrecision(int modulusBitLength) {
        return create(modulusBitLength, true, modulusBitLength - 2, DEFAULT_BASE);
    }

    public static PheParamsTestConfiguration createSignedPartialPrecision(int modulusBitLength, int base) {
        return create(modulusBitLength, true, modulusBitLength - 2, base);
    }

    /**
     * 返回编码方案。
     *
     * @return 编码方案。
     */
    public PhePlaintextEncoder getPlaintextEncoder() {
        return this.plaintextEncoder;
    }
}
