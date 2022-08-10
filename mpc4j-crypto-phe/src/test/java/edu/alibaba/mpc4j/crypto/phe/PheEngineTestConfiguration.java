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

import edu.alibaba.mpc4j.crypto.phe.params.PheKeyGenParams;
import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintextEncoder;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 半同态加密引擎测试类配置。
 *
 * @author Weiran Liu
 * @date 2021/10/30
 */
public class PheEngineTestConfiguration {
    /**
     * Pai99，40比特安全性
     */
    static final PheEngineTestConfiguration[] PAI99_CONFIGURATIONS_40
        = generateConfigurations(PheFactory.PheType.PAI99, PheSecLevel.LAMBDA_40);
    public static final List<Object[]> PAI99_NAME_CONFIGURATIONS_40
        = Arrays.stream(PAI99_CONFIGURATIONS_40)
        .map(configuration -> new Object[]{PheEngineTestConfiguration.getName(configuration), configuration})
        .collect(Collectors.toList());
    public static final Object[] PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40 = new Object[]{
        PheEngineTestConfiguration.getName(PAI99_CONFIGURATIONS_40[2]), PAI99_CONFIGURATIONS_40[2]
    };
    /**
     * Pai99，80比特安全性
     */
    static final PheEngineTestConfiguration[] PAI99_CONFIGURATIONS_80
        = generateConfigurations(PheFactory.PheType.PAI99, PheSecLevel.LAMBDA_80);
    public static final List<Object[]> PAI99_NAME_CONFIGURATIONS_80
        = Arrays.stream(PAI99_CONFIGURATIONS_80)
        .map(configuration -> new Object[]{PheEngineTestConfiguration.getName(configuration), configuration})
        .collect(Collectors.toList());
    public static final Object[] PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80 = new Object[]{
        PheEngineTestConfiguration.getName(PAI99_CONFIGURATIONS_80[2]), PAI99_CONFIGURATIONS_80[2]
    };

    /**
     * OU98，40比特安全性
     */
    static final PheEngineTestConfiguration[] OU98_CONFIGURATIONS_40
        = generateConfigurations(PheFactory.PheType.OU98, PheSecLevel.LAMBDA_40);
    public static final List<Object[]> OU98_NAME_CONFIGURATIONS_40
        = Arrays.stream(OU98_CONFIGURATIONS_40)
        .map(configuration -> new Object[]{PheEngineTestConfiguration.getName(configuration), configuration})
        .collect(Collectors.toList());
    public static final Object[] OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40 = new Object[]{
        PheEngineTestConfiguration.getName(OU98_CONFIGURATIONS_40[2]), OU98_CONFIGURATIONS_40[2]
    };
    /**
     * OU98，80比特安全性
     */
    static final PheEngineTestConfiguration[] OU98_CONFIGURATIONS_80
        = generateConfigurations(PheFactory.PheType.OU98, PheSecLevel.LAMBDA_80);
    public static final List<Object[]> OU98_NAME_CONFIGURATIONS_80
        = Arrays.stream(OU98_CONFIGURATIONS_80)
        .map(configuration -> new Object[]{PheEngineTestConfiguration.getName(configuration), configuration})
        .collect(Collectors.toList());
    public static final Object[] OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80 = new Object[]{
        PheEngineTestConfiguration.getName(OU98_CONFIGURATIONS_80[2]), OU98_CONFIGURATIONS_80[2]
    };

    /**
     * 根据半同态类型和模数比特长度构造测试配置项。
     *
     * @param pheType     半同态类型。
     * @param pheSecLevel 半同态加密安全级别。
     * @return 测试配置项。
     */
    private static PheEngineTestConfiguration[] generateConfigurations(PheFactory.PheType pheType, PheSecLevel pheSecLevel) {
        return new PheEngineTestConfiguration[]{
            createUnsignedFullPrecision(pheType, pheSecLevel),
            createUnsignedPartialPrecision(pheType, pheSecLevel),
            createSignedFullPrecision(pheType, pheSecLevel),
            createSignedPartialPrecision(pheType, pheSecLevel),
            createUnsignedFullPrecision(pheType, pheSecLevel, 2),
            createUnsignedPartialPrecision(pheType, pheSecLevel, 2),
            createSignedFullPrecision(pheType, pheSecLevel, 2),
            createSignedPartialPrecision(pheType, pheSecLevel, 2),
            createUnsignedFullPrecision(pheType, pheSecLevel, 13),
            createUnsignedPartialPrecision(pheType, pheSecLevel, 13),
            createSignedFullPrecision(pheType, pheSecLevel, 13),
            createSignedPartialPrecision(pheType, pheSecLevel, 13),
            createUnsignedFullPrecision(pheType, pheSecLevel, 64),
            createUnsignedPartialPrecision(pheType, pheSecLevel, 64),
            createSignedFullPrecision(pheType, pheSecLevel, 64),
            createSignedPartialPrecision(pheType, pheSecLevel, 64),
        };
    }

    private static String getName(PheEngineTestConfiguration configuration) {
        PhePlaintextEncoder encodeScheme = configuration.getPrivateKey().getPublicKey().getPlaintextEncoder();
        return configuration.getPrivateKey().getPublicKey().getPheType().name() + "_"
            + (encodeScheme.isSigned() ? "SIGNED_" : "UNSIGNED_")
            + (encodeScheme.isFullPrecision() ? "FULL_" : "PARTIAL_")
            + encodeScheme.getModulus().bitLength() + "_"
            + "BASE_" + encodeScheme.getBase();
    }

    /**
     * 半同态加密引擎
     */
    private final PheEngine pheEngine;
    /**
     * 私钥
     */
    private final PhePrivateKey privateKey;

    public PheEngineTestConfiguration(PheFactory.PheType pheType, PheKeyGenParams keyGenParams) {
        pheEngine = PheFactory.createInstance(pheType, PheTestUtils.SECURE_RANDOM);
        privateKey = pheEngine.keyGen(keyGenParams);
    }

    private static PheEngineTestConfiguration create(PheFactory.PheType pheType,
                                                     PheSecLevel pheSecLevel, boolean signed, int precision, int base) {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(pheSecLevel, signed, precision, base);
        return new PheEngineTestConfiguration(pheType, keyGenParams);
    }

    private static PheEngineTestConfiguration create(PheFactory.PheType pheType,
                                                     PheSecLevel pheSecLevel, boolean signed, int precision) {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(pheSecLevel, signed, precision);
        return new PheEngineTestConfiguration(pheType, keyGenParams);
    }

    public static PheEngineTestConfiguration createUnsignedFullPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, false, modulusBitLength);
    }

    public static PheEngineTestConfiguration createUnsignedFullPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel,
                                                                         int base) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, false, modulusBitLength, base);
    }

    public static PheEngineTestConfiguration createUnsignedPartialPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, false, modulusBitLength - 2);
    }

    public static PheEngineTestConfiguration createUnsignedPartialPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel,
                                                                            int base) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, false, modulusBitLength - 2, base);
    }

    public static PheEngineTestConfiguration createSignedFullPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, true, modulusBitLength);
    }

    public static PheEngineTestConfiguration createSignedFullPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel,
                                                                       int base) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, true, modulusBitLength, base);
    }

    public static PheEngineTestConfiguration createSignedPartialPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, true, modulusBitLength - 2);
    }

    public static PheEngineTestConfiguration createSignedPartialPrecision(PheFactory.PheType pheType, PheSecLevel pheSecLevel,
                                                                          int base) {
        int modulusBitLength = PheFactory.getModulusBitLength(pheType, pheSecLevel);
        return create(pheType, pheSecLevel, true, modulusBitLength - 2, base);
    }

    public PhePrivateKey getPrivateKey() {
        return privateKey;
    }

    public PheEngine getPheEngine() {
        return pheEngine;
    }
}
