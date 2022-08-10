/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Add some comments and adjust the code based on Alibaba Java Code Guidelines.
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
package edu.alibaba.mpc4j.crypto.phe.impl.pai99;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.phe.AbstractPheEngine;
import edu.alibaba.mpc4j.crypto.phe.PheFactory;
import edu.alibaba.mpc4j.crypto.phe.params.PheKeyGenParams;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Pai99半同态加密引擎。
 *
 * @author Weiran Liu
 * @date 2021/10/30
 */
public class Pai99PheEngine extends AbstractPheEngine {
    /**
     * Computes the L function as defined in Paillier's paper. That is: L(x,p) = (x - 1) / p.
     *
     * @param x the input x.
     * @param p the input p.
     * @return L(x, p) = (x - 1) / p.
     */
    static BigInteger lFunction(BigInteger x, BigInteger p) {
        return x.subtract(BigInteger.ONE).divide(p);
    }

    /**
     * Computes the h-function as defined in Paillier's paper page 12, 'Decryption using Chinese-remaindering'.
     *
     * @param generator the generator g = modulus + 1.
     * @param x         the input x.
     * @param xSquared  the input xSquared.
     * @return h(x, x ^ 2) = ((g^(x - 1) mod x^2 - 1) / x)^-1 mod x.
     */
    static BigInteger hFunction(BigInteger generator, BigInteger x, BigInteger xSquared) {
        return lFunction(BigIntegerUtils.modPow(generator, x.subtract(BigInteger.ONE), xSquared), x).modInverse(x);
    }

    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;

    public Pai99PheEngine(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.PAI99;
    }

    @Override
    public PhePrivateKey keyGen(PheKeyGenParams keyGenParams) {
        int modulusBitLength = PheFactory.getModulusBitLength(PheFactory.PheType.PAI99, keyGenParams.getPheSecLevel());
        boolean signed = keyGenParams.isSigned();
        int precision = keyGenParams.getPrecision();
        int base = keyGenParams.getBase();

        return Pai99PhePrivateKey.fromParams(modulusBitLength, signed, precision, base, secureRandom);
    }

    @Override
    public int primeBitLength(PhePublicKey pk) {
        return pk.getPlaintextEncoder().getModulus().bitLength() / 2;
    }

    @Override
    public BigInteger rawEncrypt(PhePublicKey pk, BigInteger m) {
        Preconditions.checkArgument(pk instanceof Pai99PhePublicKey);
        Pai99PhePublicKey pai99PhePublicKey = (Pai99PhePublicKey) pk;
        BigInteger modulus = pai99PhePublicKey.modulus;
        BigInteger modulusSquared = pai99PhePublicKey.modulusSquared;
        // ct = g^pt * r^n mod n^2 = (modulus + 1)^pt * r^n mod n^2 (modulus * pt + 1) * r^n mod n^2
        BigInteger r = BigIntegerUtils.randomPositive(modulus.shiftRight(1), secureRandom);
        return modulus.multiply(m).add(BigInteger.ONE).mod(modulusSquared)
            .multiply(BigIntegerUtils.modPow(r, modulus, modulusSquared)).mod(modulusSquared);
    }

    @Override
    public BigInteger rawEncrypt(PhePrivateKey sk, BigInteger m) {
        Preconditions.checkArgument(sk instanceof Pai99PhePrivateKey);
        Pai99PhePrivateKey pai99PhePrivateKey = (Pai99PhePrivateKey) sk;
        Pai99PhePublicKey pai99PhePublicKey = (Pai99PhePublicKey) sk.getPublicKey();
        // 获得参数
        BigInteger modulus = pai99PhePrivateKey.modulus;
        BigInteger modulusSquared = pai99PhePublicKey.modulusSquared;
        BigInteger pSquared = pai99PhePrivateKey.pSquared;
        BigInteger qSquared = pai99PhePrivateKey.qSquared;
        BigInteger pSquaredOrder = pai99PhePrivateKey.pSquaredOrder;
        BigInteger qSquaredOrder = pai99PhePrivateKey.qSquaredOrder;
        BigInteger pSquaredInverse = pai99PhePrivateKey.pSquaredInverse;
        // 在Z_n上随机选取r
        BigInteger r = BigIntegerUtils.randomPositive(modulus.shiftRight(1), secureRandom);
        // ct = (modulus * pt + 1) * r^n mod n^2，把r^n mod n^2拆到CRT里面
        BigInteger r1 = r.mod(pSquaredOrder);
        BigInteger mod1 = BigIntegerUtils.modPow(r1, modulus, pSquared);
        BigInteger r2 = r.mod(qSquaredOrder);
        BigInteger mod2 = BigIntegerUtils.modPow(r2, modulus, qSquared);
        BigInteger mod = crt(mod1, mod2, pSquared, qSquared, pSquaredInverse);

        return modulus.multiply(m).add(BigInteger.ONE).mod(modulusSquared).multiply(mod).mod(modulusSquared);
    }

    @Override
    public BigInteger rawObfuscate(PhePublicKey pk, BigInteger ct) {
        Preconditions.checkArgument(pk instanceof Pai99PhePublicKey);
        BigInteger modulus = pk.getModulus();
        BigInteger modulusSquared = pk.getCiphertextModulus();
        // 重随机化也使用DJN10优化方案，ct' = ct * r'^n mod n^2，其中r' ∈ Z_n
        BigInteger r = BigIntegerUtils.randomPositive(modulus.shiftRight(1), secureRandom);
        return BigIntegerUtils.modPow(r, modulus, modulusSquared).multiply(ct).mod(modulusSquared);
    }

    @Override
    public BigInteger rawAdd(PhePublicKey pk, BigInteger value1, BigInteger value2) {
        Preconditions.checkArgument(pk instanceof Pai99PhePublicKey);
        return value1.multiply(value2).mod(pk.getCiphertextModulus());
    }

    @Override
    public BigInteger rawMultiply(PhePublicKey pk, BigInteger ciphertext, BigInteger factor) {
        Preconditions.checkArgument(pk instanceof Pai99PhePublicKey);
        return BigIntegerUtils.modPow(ciphertext, factor, pk.getCiphertextModulus());
    }

    @Override
    public BigInteger rawDecrypt(PhePrivateKey sk, BigInteger ct) {
        Preconditions.checkArgument(sk instanceof Pai99PhePrivateKey);
        Pai99PhePrivateKey privateKey = (Pai99PhePrivateKey) sk;
        // mod1 = L_p(c^(p - 1) mod p^2) h_p mod p
        BigInteger p = privateKey.p;
        BigInteger pSquared = privateKey.pSquared;
        BigInteger hp = privateKey.hp;
        BigInteger decryptedToP = lFunction(BigIntegerUtils.modPow(ct, p.subtract(BigInteger.ONE), pSquared), p)
            .multiply(hp).mod(p);
        // mod2 = L_q(c^(q - 1) mod q^2) h_q mod q
        BigInteger q = privateKey.q;
        BigInteger qSquared = privateKey.qSquared;
        BigInteger hq = privateKey.hq;
        BigInteger decryptedToQ = lFunction(BigIntegerUtils.modPow(ct, q.subtract(BigInteger.ONE), qSquared), q)
            .multiply(hq).mod(q);
        // m = CRT(m_p, m_q) mod pq = (((mp - mq) * p^(-1) mod q) * p) + m_q
        BigInteger pInverse = privateKey.pInverse;
        return crt(decryptedToP, decryptedToQ, p, q, pInverse);
    }
}
