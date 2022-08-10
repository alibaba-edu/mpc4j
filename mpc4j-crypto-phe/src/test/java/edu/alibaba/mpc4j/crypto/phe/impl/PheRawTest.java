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
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 半同态加密行运算测试。部分代码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/test/java/com/n1analytics/paillier/RawPaillierTest.java
 * </p>
 *
 * @author Mentari Djatmiko, Weiran Liu
 * @date 2016/02/05
 */
@RunWith(Parameterized.class)
public class PheRawTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // OU98
        configurationParams.add(PheEngineTestConfiguration.OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40);
        configurationParams.add(PheEngineTestConfiguration.OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80);
        // Pai99
        configurationParams.add(PheEngineTestConfiguration.PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40);
        configurationParams.add(PheEngineTestConfiguration.PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80);

        return configurationParams;
    }

    public PheRawTest(String name, PheEngineTestConfiguration configuration) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        pheEngine = configuration.getPheEngine();
        sk = configuration.getPrivateKey();
        pk = sk.getPublicKey();
    }

    /**
     * 半同态加密引擎
     */
    private final PheEngine pheEngine;
    /**
     * 公钥
     */
    private final PhePublicKey pk;
    /**
     * 私钥
     */
    private final PhePrivateKey sk;

    @Test
    public void testEncryptionDecryption() {
        // 公钥加密
        BigInteger plaintext = new BigInteger("42");
        BigInteger ciphertext = pheEngine.rawEncrypt(pk, plaintext);
        Assert.assertNotEquals(plaintext, ciphertext);
        Assert.assertEquals(plaintext, pheEngine.rawDecrypt(sk, ciphertext));
        // 私钥加密
        ciphertext = pheEngine.rawEncrypt(sk, plaintext);
        Assert.assertNotEquals(plaintext, ciphertext);
        Assert.assertEquals(plaintext, pheEngine.rawDecrypt(sk, ciphertext));
    }

    @Test
    public void testRowAdd() {
        BigInteger a = new BigInteger("123");
        BigInteger b = new BigInteger("7654");
        BigInteger encryptedA = pheEngine.rawEncrypt(pk, a);
        BigInteger encryptedB = pheEngine.rawEncrypt(pk, b);
        BigInteger ciphertext = pheEngine.rawAdd(pk, encryptedA, encryptedB);
        Assert.assertEquals(a.add(b), pheEngine.rawDecrypt(sk, ciphertext));
        // test overflow
        a = pk.getModulus();
        b = BigInteger.ONE;
        encryptedA = pheEngine.rawEncrypt(pk, a);
        encryptedB = pheEngine.rawEncrypt(pk, b);
        ciphertext = pheEngine.rawAdd(pk, encryptedA, encryptedB);
        Assert.assertEquals(BigInteger.ONE, pheEngine.rawDecrypt(sk, ciphertext));
    }

    @Test
    public void testRawMultiply() {
        BigInteger a = new BigInteger("95831");
        BigInteger k = BigInteger.ONE;
        BigInteger encryptedA = pheEngine.rawEncrypt(pk, a);

        BigInteger ciphertext = pheEngine.rawMultiply(pk, encryptedA, k);
        Assert.assertEquals(a, pheEngine.rawDecrypt(sk, ciphertext));

        k = new BigInteger("842");
        ciphertext = pheEngine.rawMultiply(pk, encryptedA, k);
        Assert.assertEquals(a.multiply(k), pheEngine.rawDecrypt(sk, ciphertext));

        a = pk.getModulus().subtract(BigInteger.ONE);
        encryptedA = pheEngine.rawEncrypt(pk, a);
        k = new BigInteger("42");
        ciphertext = pheEngine.rawMultiply(pk, encryptedA, k);
        Assert.assertEquals(a.multiply(k).mod(pk.getModulus()), pheEngine.rawDecrypt(sk, ciphertext));
    }

    @Test
    public void testObfuscate() {
        BigInteger a = new BigInteger("123456789");
        BigInteger ciphertext = pheEngine.rawEncrypt(pk, a);
        BigInteger obfuscatedCiphertext = pheEngine.rawObfuscate(pk, ciphertext);
        Assert.assertNotEquals(ciphertext, obfuscatedCiphertext);
        Assert.assertEquals(a, pheEngine.rawDecrypt(sk, obfuscatedCiphertext));
    }
}
