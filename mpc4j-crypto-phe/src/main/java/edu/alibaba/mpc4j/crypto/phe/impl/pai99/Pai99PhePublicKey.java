/*
 * Copyright 2015 NICTA.
 * Modified by Weiran Liu. Introduce other optimization and adjust the code based on Alibaba Java Code Guidelines.
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
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintextEncoder;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import edu.alibaba.mpc4j.crypto.phe.PheFactory;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Pai99半同态加密公钥。部分代码来自：
 * <p>
 * https://github.com/n1analytics/javallier/blob/master/src/main/java/com/n1analytics/paillier/PaillierPublicKey.java
 * </p>
 * 公钥计算时应用了DJN10论文的优化方法：加密时，可以把原始Pai99中r^n运算替换为h_s^r运算。原始论文来自：
 * <p>
 * A generalization of Paillier’s public-key system with applications to electronic voting. International Journal of
 * Information Security, 2010, 9(6): 371-385.
 * </p>
 *
 * @author Brian Thorne, Wilko Henecka, Dongyao Wu, Max Ott, Weiran Liu
 * @date 2017/09/19
 */
public class Pai99PhePublicKey implements PhePublicKey {
    /**
     * 模数编码方案
     */
    private PhePlaintextEncoder plaintextEncoder;
    /**
     * The modulus (n = p * q) of the public key.
     */
    BigInteger modulus;
    /**
     * The modulus squared (n<sup>2</sup>) of the public key.
     */
    BigInteger modulusSquared;

    public static Pai99PhePublicKey fromParams(PhePlaintextEncoder encodeScheme) {
        return Pai99PhePublicKey.create(encodeScheme);
    }

    public static Pai99PhePublicKey fromByteArrayList(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        Preconditions.checkArgument(
            PheFactory.PheType.PAI99.ordinal() == typeIndex, "类型索引 = %s，要求类型索引 = %s",
            typeIndex, PheFactory.PheType.PAI99.ordinal()
        );
        PhePlaintextEncoder encodeScheme = PhePlaintextEncoder.fromByteArrayList(byteArrayList);
        // 根据编码方案和hs构造公钥
        return Pai99PhePublicKey.create(encodeScheme);
    }

    private static Pai99PhePublicKey create(PhePlaintextEncoder plaintextEncoder) {
        Preconditions.checkNotNull(plaintextEncoder, "ModulusEncodeScheme must not be null");
        BigInteger modulus = plaintextEncoder.getModulus();
        Preconditions.checkArgument(modulus.compareTo(BigInteger.ONE) > 0, "modulus must be greater than 1");
        BigInteger modulusSquared = modulus.multiply(modulus);
        Pai99PhePublicKey pai99PhePublicKey = new Pai99PhePublicKey();
        pai99PhePublicKey.plaintextEncoder = plaintextEncoder;
        pai99PhePublicKey.modulus = modulus;
        pai99PhePublicKey.modulusSquared = modulusSquared;

        return pai99PhePublicKey;
    }

    private Pai99PhePublicKey() {
        // empty
    }

    @Override
    public PhePlaintextEncoder getPlaintextEncoder() {
        return plaintextEncoder;
    }

    @Override
    public BigInteger getModulus() {
        return modulus;
    }

    @Override
    public BigInteger getCiphertextModulus() {
        return modulusSquared;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.PAI99;
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(IntUtils.intToByteArray(PheFactory.PheType.PAI99.ordinal()));
        byteArrayList.addAll(plaintextEncoder.toByteArrayList());

        return byteArrayList;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(plaintextEncoder)
            .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != Pai99PhePublicKey.class) {
            return false;
        }
        Pai99PhePublicKey that = (Pai99PhePublicKey) o;
        // We don't need to compare modulusSquared or generator since they are uniquely determined by modulus.
        return new EqualsBuilder()
            .append(this.plaintextEncoder, that.plaintextEncoder)
            .isEquals();
    }
}
