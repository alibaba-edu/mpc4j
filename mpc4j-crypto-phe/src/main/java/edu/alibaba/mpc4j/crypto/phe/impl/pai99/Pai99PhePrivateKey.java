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
package edu.alibaba.mpc4j.crypto.phe.impl.pai99;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintextEncoder;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import edu.alibaba.mpc4j.crypto.phe.PheFactory;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

/**
 * Pai99半同态加密私钥。部分代码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/PaillierPrivateKey.java
 * </p>
 *
 * @author Brian Thorne, Wilko Henecka, Dongyao Wu, mpnd, Max Ott, Weiran Liu
 * @date 2017/09/19
 */
public class Pai99PhePrivateKey implements PhePrivateKey {
    /**
     * 公钥
     */
    Pai99PhePublicKey publicKey;
    /**
     * The first prime number, {@code p} such that {@code p * q = n}.
     */
    BigInteger p;
    /**
     * The second prime number, {@code q} such that {@code p * q = n}.
     */
    BigInteger q;
    /**
     * The modulus (n = p * q)
     */
    BigInteger modulus;
    /**
     * The value <code>p<sup>2</sup></code>
     */
    BigInteger pSquared;
    /**
     * The value <code>q<sup>2</sup></code>
     */
    BigInteger qSquared;
    /**
     * The modular inverse of <code>p modulo q</code>
     */
    BigInteger pInverse;
    /**
     * Precomputed <code>hp</code> as defined in Paillier's paper page 12: Decryption using Chinese-remaindering.
     */
    BigInteger hp;
    /**
     * Precomputed <code>hq</code> as defined in Paillier's paper page 12: Decryption using Chinese-remaindering.
     */
    BigInteger hq;
    /**
     * Precomputed p^2 - p, used in private key encryption
     */
    BigInteger pSquaredOrder;
    /**
     * Precomputed q^2 - q, used in private key encryption
     */
    BigInteger qSquaredOrder;
    /**
     * Precomputed ((p^2)^(-1) mod q^2), used in private key encryption
     */
    BigInteger pSquaredInverse;

    static Pai99PhePrivateKey fromParams(int modulusBitLength, boolean signed, int precision, int base,
                                         SecureRandom secureRandom) {
        // 因为明文比特长度一定是Byte.SIZE的整数倍，因此modulusBitLength / 2一定可以被整除
        int primeLength = modulusBitLength / 2;
        // Find two primes p and q whose multiple has the same number of bits as primeBitLength
        BigInteger p, q, modulus;
        do {
            p = BigInteger.probablePrime(primeLength, secureRandom);
            do {
                q = BigInteger.probablePrime(primeLength, secureRandom);
            } while (p.equals(q));
            modulus = p.multiply(q);
        } while (modulus.bitLength() != modulusBitLength);
        Pai99PhePrivateKey privateKey = Pai99PhePrivateKey.createLocalParams(p, q);
        PhePlaintextEncoder encodeScheme = PhePlaintextEncoder.fromParams(privateKey.modulus, signed, precision, base);
        privateKey.publicKey = Pai99PhePublicKey.fromParams(encodeScheme);

        return privateKey;
    }

    public static Pai99PhePrivateKey fromByteArrayList(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        Preconditions.checkArgument(
            PheFactory.PheType.PAI99.ordinal() == typeIndex, "类型索引 = %s，要求类型索引 = %s",
            typeIndex, PheFactory.PheType.PAI99.ordinal()
        );
        BigInteger p = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger q = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        Pai99PhePrivateKey privateKey = Pai99PhePrivateKey.createLocalParams(p, q);
        Pai99PhePublicKey publicKey = Pai99PhePublicKey.fromByteArrayList(byteArrayList);
        Preconditions.checkArgument(
            privateKey.modulus.equals(publicKey.getModulus()),
            "private key and public key modulus mismatch"
        );
        privateKey.publicKey = publicKey;

        return privateKey;
    }

    private static Pai99PhePrivateKey createLocalParams(BigInteger p, BigInteger q) {
        Preconditions.checkNotNull(p, "the prime p must not be null");
        Preconditions.checkNotNull(q, "the prime q must not be null");
        Pai99PhePrivateKey pai99PhePrivateKey = new Pai99PhePrivateKey();
        pai99PhePrivateKey.modulus = p.multiply(q);
        pai99PhePrivateKey.p = p;
        pai99PhePrivateKey.pSquared = p.multiply(p);
        pai99PhePrivateKey.q = q;
        pai99PhePrivateKey.qSquared = q.multiply(q);
        pai99PhePrivateKey.pInverse = BigIntegerUtils.modInverse(p, q);
        // the generator is always set to modulus + 1, as this allows a more efficient encryption function.
        BigInteger generator = pai99PhePrivateKey.modulus.add(BigInteger.ONE);
        pai99PhePrivateKey.hp = Pai99PheEngine.hFunction(generator, pai99PhePrivateKey.p, pai99PhePrivateKey.pSquared);
        pai99PhePrivateKey.hq = Pai99PheEngine.hFunction(generator, pai99PhePrivateKey.q, pai99PhePrivateKey.qSquared);
        // precompute parameters for private key encryption
        pai99PhePrivateKey.pSquaredOrder = pai99PhePrivateKey.pSquared.subtract(pai99PhePrivateKey.p);
        pai99PhePrivateKey.qSquaredOrder = pai99PhePrivateKey.qSquared.subtract(pai99PhePrivateKey.q);
        pai99PhePrivateKey.pSquaredInverse = BigIntegerUtils.modInverse(pai99PhePrivateKey.pSquared, pai99PhePrivateKey.qSquared);

        return pai99PhePrivateKey;
    }

    @Override
    public int hashCode() {
        // We don't need to hash any other variables since they are uniquely determined by publicKey
        return new HashCodeBuilder()
            .append(p)
            .append(q)
            .append(publicKey)
            .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != Pai99PhePrivateKey.class) {
            return false;
        }
        Pai99PhePrivateKey that = (Pai99PhePrivateKey) o;
        // We don't need to compare any other variables since they are uniquely determined by publicKey
        return new EqualsBuilder()
            .append(this.p, that.p)
            .append(this.q, that.q)
            .append(this.publicKey, that.publicKey)
            .isEquals();
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(IntUtils.intToByteArray(PheFactory.PheType.PAI99.ordinal()));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(p));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(q));
        byteArrayList.addAll(publicKey.toByteArrayList());

        return byteArrayList;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.PAI99;
    }

    @Override
    public PhePublicKey getPublicKey() {
        return publicKey;
    }
}
