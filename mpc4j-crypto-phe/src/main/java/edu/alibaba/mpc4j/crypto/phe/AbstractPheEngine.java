/*
 * Copyright 2015 NICTA.
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

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.phe.params.*;

import java.math.BigInteger;

/**
 * 半同态加密（Partially Homomorphic Encryption，PHE）引擎抽象类。部分代码参考：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/PaillierContext.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public abstract class AbstractPheEngine implements PheEngine {

    /**
     * 检查{@code PhePublicKey}和{@code PheEncryptedNumber}的上下文一致性。
     *
     * @param pk 公钥。
     * @param ct 密文。
     * @throws CryptoContextMismatchException 公钥和半同态密文上下文不一致。
     */
    private void checkInput(PhePublicKey pk, PheCiphertext ct) throws CryptoContextMismatchException {
        if (!pk.equals(ct.getPhePublicKey())) {
            throw new CryptoContextMismatchException(
                "Given PhePublicKey and PhePublicKey in PheCiphertext are different"
            );
        }
    }

    /**
     * 检查{@code PhePublicKey}和{@code ModulusEncodedNumber}的上下文一致性。
     *
     * @param pk            公钥
     * @param encodedNumber 编码数。
     * @throws CryptoContextMismatchException 公钥和编码数上下文不一致。
     */
    private void checkInput(PhePublicKey pk, PhePlaintext encodedNumber) throws CryptoContextMismatchException {
        if (!pk.getPlaintextEncoder().equals(encodedNumber.getPlaintextEncoder())) {
            throw new CryptoContextMismatchException(
                "Given PhePublicKey and PheEncryptedNumber have different ModulusEncodeScheme"
            );
        }
    }

    /**
     * The Chinese Remainder Theorem as needed for decryption / modPow.
     *
     * @param mod1      the solution modulo n1.
     * @param mod2      the solution modulo n2.
     * @param n1        the modulus n1.
     * @param n2        the modulus n2.
     * @param n1Inverse (n1^(-1) mod n2).
     * @return the solution modulo n = n1 * n2.
     */
    protected BigInteger crt(BigInteger mod1, BigInteger mod2, BigInteger n1, BigInteger n2, BigInteger n1Inverse) {
        BigInteger u = mod2.subtract(mod1).multiply(n1Inverse).mod(n2);
        return mod1.add(u.multiply(n1));
    }

    @Override
    public PheCiphertext encrypt(PhePublicKey pk, PhePlaintext encoded) {
        checkInput(pk, encoded);
        final BigInteger value = encoded.getValue();
        final BigInteger ciphertext = rawEncrypt(pk, value);
        return PheCiphertext.fromParams(pk, ciphertext, encoded.getExponent());
    }

    @Override
    public PheCiphertext encrypt(PhePrivateKey sk, PhePlaintext encoded) {
        checkInput(sk.getPublicKey(), encoded);
        final BigInteger value = encoded.getValue();
        final BigInteger ciphertext = rawEncrypt(sk, value);
        return PheCiphertext.fromParams(sk.getPublicKey(), ciphertext, encoded.getExponent());
    }

    @Override
    public PheCiphertext obfuscate(PhePublicKey pk, PheCiphertext ct) {
        checkInput(pk, ct);
        final BigInteger obfuscated = rawObfuscate(pk, ct.getCiphertext());
        return PheCiphertext.fromParams(pk, obfuscated, ct.getExponent());
    }

    @Override
    public PheCiphertext add(PhePublicKey pk, PheCiphertext operand, PheCiphertext other) {
        checkInput(pk, operand);
        checkInput(pk, other);
        PhePlaintextEncoder encodeScheme = pk.getPlaintextEncoder();
        BigInteger value1 = operand.getCiphertext();
        BigInteger value2 = other.getCiphertext();
        int exponent1 = operand.getExponent();
        int exponent2 = other.getExponent();
        if (exponent1 > exponent2) {
            value1 = this.rawMultiply(pk, value1, encodeScheme.getRescalingFactor(exponent1 - exponent2));
            exponent1 = exponent2;
        } else if (exponent1 < exponent2) {
            value2 = this.rawMultiply(pk, value2, encodeScheme.getRescalingFactor(exponent2 - exponent1));
        }
        final BigInteger result = this.rawAdd(pk, value1, value2);
        return PheCiphertext.fromParams(pk, result, exponent1);
    }

    @Override
    public PheCiphertext add(PhePublicKey pk, PheCiphertext operand, PhePlaintext other) {
        checkInput(pk, operand);
        checkInput(pk, other);
        // addition only works if both numbers have the same exponent. Adjusting the exponent of an encrypted number can
        // only be done with an encrypted multiplication (internally, this is done with a modular exponentiation).
        // It is going to be computationally much cheaper to adjust the encoded number before the encryption as we only
        // need to do a modular multiplication.
        PhePlaintextEncoder encodeScheme = pk.getPlaintextEncoder();
        int exponent1 = operand.getExponent();
        int exponent2 = other.getExponent();
        BigInteger value2 = other.getValue();
        if (exponent1 < exponent2) {
            value2 = value2
                .multiply(encodeScheme.getRescalingFactor(exponent2 - exponent1))
                .mod(pk.getModulus()
                );
            PhePlaintext encodedPt = PhePlaintext.fromParams(encodeScheme, value2, exponent1);
            PheCiphertext ct2 = encrypt(pk, encodedPt);
            return add(pk, operand, ct2);
        }
        if (exponent1 > exponent2 && encodeScheme.signum(other) == 1) {
            // test if we can shift value2 to the right without loosing information. Only works for positive values.
            BigInteger rescalingFactor = encodeScheme.getRescalingFactor(exponent1 - exponent2);
            boolean canShift = value2.mod(rescalingFactor).equals(BigInteger.ZERO);
            if (canShift) {
                value2 = value2.divide(rescalingFactor);
                PhePlaintext encodedPt = PhePlaintext.fromParams(encodeScheme, value2, exponent1);
                PheCiphertext ct2 = encrypt(pk, encodedPt);
                return add(pk, operand, ct2);
            }
        }
        PheCiphertext ct2 = encrypt(pk, other);
        return add(pk, operand, ct2);
    }

    @Override
    public PheCiphertext multiply(PhePublicKey pk, PheCiphertext operand, PhePlaintext other) {
        checkInput(pk, operand);
        checkInput(pk, other);
        BigInteger value1 = operand.getCiphertext();
        BigInteger value2 = other.getValue();
        BigInteger negPlain = pk.getModulus().subtract(value2);
        // If the plaintext is large, exponentiate using its negative instead.
        if (negPlain.compareTo(pk.getPlaintextEncoder().getMaxEncoded()) <= 0) {
            value1 = BigIntegerUtils.modInverse(value1, pk.getCiphertextModulus());
            value2 = negPlain;
        }
        final BigInteger result = this.rawMultiply(pk, value1, value2);
        final int exponent = operand.getExponent() + other.getExponent();
        return PheCiphertext.fromParams(pk, result, exponent);
    }

    @Override
    public PhePlaintext decrypt(PhePrivateKey sk, PheCiphertext ct) {
        checkInput(sk.getPublicKey(), ct);
        PhePublicKey pk = sk.getPublicKey();
        checkInput(pk, ct);
        return PhePlaintext.fromParams(
            pk.getPlaintextEncoder(), this.rawDecrypt(sk, ct.getCiphertext()), ct.getExponent()
        );
    }

    @Override
    public PheCiphertext decreaseExponentTo(PhePublicKey pk, PheCiphertext ct, int newExp) {
        checkInput(pk, ct);
        BigInteger ciphertext = ct.getCiphertext();
        int exponent = ct.getExponent();
        Preconditions.checkArgument(
            newExp <= exponent,
            "New exponent: " + newExp + "should be more negative than old exponent: " + exponent + ".");

        int expDiff = exponent - newExp;
        BigInteger bigFactor = pk.getPlaintextEncoder().getRescalingFactor(expDiff);
        BigInteger newEnc = this.rawMultiply(pk, ciphertext, bigFactor);
        return PheCiphertext.fromParams(pk, newEnc, newExp);
    }
}
