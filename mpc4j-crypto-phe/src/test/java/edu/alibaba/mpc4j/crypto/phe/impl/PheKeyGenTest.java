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
import edu.alibaba.mpc4j.crypto.phe.*;
import edu.alibaba.mpc4j.crypto.phe.params.PheKeyGenParams;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

/**
 * 半同态加密密钥生成测试。部分源码来自：
 * <a href="https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/PaillierPublicKeyTest.java">
 * PaillierPublicKeyTest.java</a>
 * 以及
 * <p>
 * <a href="https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/PaillierPrivateKeyTest.java">
 * PaillierPrivateKeyTest.java</a>.
 *
 * @author Brian Thorne, mpnd, Weiran Liu
 * @date 2017/09/21
 */
@RunWith(Parameterized.class)
public class PheKeyGenTest {
    /**
     * 半同态加密类型
     */
    private final PheEngine pheEngine;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{PheType.OU98.name(), PheType.OU98,});
        configurations.add(new Object[]{PheType.PAI99.name(), PheType.PAI99,});

        return configurations;
    }

    public PheKeyGenTest(String name, PheType pheType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        pheEngine = PheFactory.createInstance(pheType, PheTestUtils.SECURE_RANDOM);
    }

    @Test
    public void testCreateSignedFullPublicKey() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
        );
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();
        Assert.assertTrue(publicKey.isSigned());
        Assert.assertTrue(publicKey.isFullPrecision());
    }

    @Test
    public void testCreateUnsignedFullPublicKey() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, false,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
        );
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();
        Assert.assertFalse(publicKey.isSigned());
        Assert.assertTrue(publicKey.isFullPrecision());
    }

    @Test
    public void testCreateSignedPartialContext() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL) - 2
        );
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();
        Assert.assertTrue(publicKey.isSigned());
        Assert.assertFalse(publicKey.isFullPrecision());
    }

    @Test
    public void testCreateUnsignedPartialContext() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, false,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL) - 2
        );
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();
        Assert.assertFalse(publicKey.isSigned());
        Assert.assertFalse(publicKey.isFullPrecision());
    }

    @Test
    public void testKeyUniqueness40() {
        testKeyUniqueness(PheSecLevel.LAMBDA_40);
    }

    @Test
    public void testKeyUniqueness80() {
        testKeyUniqueness(PheSecLevel.LAMBDA_80);
    }

    @Test
    public void testKeyUniqueness112() {
        testKeyUniqueness(PheSecLevel.LAMBDA_112);
    }

    private void testKeyUniqueness(PheSecLevel pheSecLevel) {
        Set<PhePrivateKey> privateKeySet = new HashSet<>(PheTestUtils.MAX_ITERATIONS);
        while (privateKeySet.size() < PheTestUtils.MAX_ITERATIONS) {
            PheKeyGenParams keyGenParams = new PheKeyGenParams(
                pheSecLevel, true, PheFactory.getModulusBitLength(pheEngine.getPheType(), pheSecLevel)
            );
            PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
            if (privateKeySet.contains(privateKey)) {
                Assert.fail("Non unique keypair.");
            }
            privateKeySet.add(privateKey);
        }
    }

    @Test
    public void testEquals() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
        );
        // generate two pairs of keys
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();
        PhePrivateKey otherPrivateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey otherPublicKey = otherPrivateKey.getPublicKey();
        // Compare to null
        Assert.assertNotEquals(null, publicKey);
        Assert.assertNotEquals(null, privateKey);
        // Compare to another public key with different modulus
        Assert.assertNotEquals(otherPublicKey, publicKey);
        Assert.assertNotEquals(otherPrivateKey, privateKey);
    }

    @Test
    public void testSerialize() {
        PheKeyGenParams keyGenParams = new PheKeyGenParams(PheTestUtils.DEFAULT_PHE_SEC_LEVEL, true,
            PheFactory.getModulusBitLength(pheEngine.getPheType(), PheTestUtils.DEFAULT_PHE_SEC_LEVEL)
        );
        PhePrivateKey privateKey = pheEngine.keyGen(keyGenParams);
        PhePublicKey publicKey = privateKey.getPublicKey();

        List<byte[]> privateKeyByteArray = privateKey.serialize();
        PhePrivateKey recoverPrivateKey = PheFactory.phasePhePrivateKey(privateKeyByteArray);
        Assert.assertEquals(privateKey, recoverPrivateKey);

        List<byte[]> publicKeyByteArray = publicKey.serialize();
        PhePublicKey recoverPublicKey = PheFactory.phasePhePublicKey(publicKeyByteArray);
        Assert.assertEquals(publicKey, recoverPublicKey);
    }
}
