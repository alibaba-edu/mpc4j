/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Move operations into PheEngine implementations.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.alibaba.mpc4j.crypto.phe.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * 半同态加密密文。源码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/EncryptedNumber.java
 * </p>
 *
 * @author Brian Thorne, Dongyao Wu, mpnd, Max Ott, Weiran Liu
 * @date 2017/09/21
 */
public class PheCiphertext implements PheParams {
    /**
     * 半同态加密公钥
     */
    private PhePublicKey phePublicKey;
    /**
     * 明文{@code value}的密文
     */
    private BigInteger ciphertext;
    /**
     * 加密数的指数
     */
    private int exponent;

    public static PheCiphertext fromParams(PhePublicKey phePublicKey, BigInteger ciphertext, int exponent) {
        return PheCiphertext.create(phePublicKey, ciphertext, exponent);
    }

    public static PheCiphertext fromByteArrayList(PhePublicKey phePublicKey, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 2);
        BigInteger ciphertext = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        int exponent = IntUtils.byteArrayToInt(byteArrayList.remove(0));

        return PheCiphertext.create(phePublicKey, ciphertext, exponent);
    }

    private static PheCiphertext create(PhePublicKey phePublicKey, BigInteger ciphertext, int exponent) {
        Preconditions.checkNotNull(phePublicKey, "phePublicKey must not be null");
        Preconditions.checkNotNull(ciphertext, "ciphertext must not be null");
        Preconditions.checkArgument(ciphertext.signum() >= 0, "ciphertext must be non-negative");
        Preconditions.checkArgument(
            ciphertext.compareTo(phePublicKey.getCiphertextModulus()) < 0,
            "ciphertext must be less than ciphertext modulus"
        );
        PheCiphertext encryptedNumber = new PheCiphertext();
        encryptedNumber.phePublicKey = phePublicKey;
        encryptedNumber.ciphertext = ciphertext;
        encryptedNumber.exponent = exponent;

        return encryptedNumber;
    }

    private PheCiphertext() {
        // empty
    }

    /**
     * 返回{@code PheEncryptedNumber}的{@code PhePublicKey}。
     *
     * @return {@code PheEncryptedNumber}的{@code PhePublicKey}。
     */
    public PhePublicKey getPhePublicKey() {
        return phePublicKey;
    }

    /**
     * 返回{@code PheEncryptedNumber}的{@code ciphertext}。
     *
     * @return {@code PheEncryptedNumber}的{@code ciphertext}。
     */
    public BigInteger getCiphertext() {
        return ciphertext;
    }

    /**
     * 返回{@code PheEncryptedNumber}的{@code exponent}。
     *
     * @return {@code PheEncryptedNumber}的{@code exponent}。
     */
    public int getExponent() {
        return exponent;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(phePublicKey)
            .append(ciphertext)
            .append(exponent)
            .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != PheCiphertext.class) {
            return false;
        }
        PheCiphertext that = (PheCiphertext) o;
        return new EqualsBuilder()
            .append(this.phePublicKey, that.phePublicKey)
            .append(this.ciphertext, that.ciphertext)
            .append(this.exponent, that.exponent)
            .isEquals();
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(ciphertext));
        byteArrayList.add(IntUtils.intToByteArray(exponent));

        return byteArrayList;
    }
}
