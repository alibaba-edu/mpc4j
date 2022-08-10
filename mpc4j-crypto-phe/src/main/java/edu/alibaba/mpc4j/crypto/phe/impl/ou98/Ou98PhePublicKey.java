package edu.alibaba.mpc4j.crypto.phe.impl.ou98;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
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
 * OU98半同态加密公钥。
 *
 * @author Weiran Liu
 * @date 2021/11/09
 */
public class Ou98PhePublicKey implements PhePublicKey {
    /**
     * 模数编码方案，模数 = p
     */
    private PhePlaintextEncoder encodeScheme;
    /**
     * modulus = n = p^2 * q
     */
    BigInteger n;
    /**
     * G = g^u mod n
     */
    BigInteger g;
    /**
     * H = g'^(nu) mod n
     */
    BigInteger h;

    public static Ou98PhePublicKey fromParams(PhePlaintextEncoder encodeScheme, BigInteger n, BigInteger g, BigInteger h) {
        return Ou98PhePublicKey.create(encodeScheme, n, g, h);
    }

    public static Ou98PhePublicKey fromByteArrayList(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        Preconditions.checkArgument(
            PheFactory.PheType.OU98.ordinal() == typeIndex, "类型索引 = %s，要求类型索引 = %s",
            typeIndex, PheFactory.PheType.OU98.ordinal()
        );
        BigInteger n = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger gCapital = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger hCapital = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        PhePlaintextEncoder encodeScheme = PhePlaintextEncoder.fromByteArrayList(byteArrayList);

        return Ou98PhePublicKey.create(encodeScheme, n, gCapital, hCapital);
    }

    private static Ou98PhePublicKey create(PhePlaintextEncoder encodeScheme, BigInteger n, BigInteger g, BigInteger h) {
        Preconditions.checkNotNull(encodeScheme, "ModulusEncodeScheme must not be null");
        BigInteger p = encodeScheme.getModulus();
        Preconditions.checkArgument(n.compareTo(p) > 0, "n = p^2 * q must be greater than p");
        Preconditions.checkArgument(
            n.divideAndRemainder(p)[1].compareTo(BigInteger.ZERO) == 0, "n = p^2 * q must divide p"
        );
        Preconditions.checkArgument(
            g.compareTo(BigInteger.ONE) > 0 && g.compareTo(n) < 0, "g must be in range (1, n)"
        );
        Preconditions.checkArgument(
            h.compareTo(BigInteger.ONE) > 0 && h.compareTo(n) < 0, "H must be in range (1, n)"
        );
        Ou98PhePublicKey ou98PhePublicKey = new Ou98PhePublicKey();
        ou98PhePublicKey.encodeScheme = encodeScheme;
        ou98PhePublicKey.n = n;
        ou98PhePublicKey.g = g;
        ou98PhePublicKey.h = h;

        return ou98PhePublicKey;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.OU98;
    }

    @Override
    public PhePlaintextEncoder getPlaintextEncoder() {
        return encodeScheme;
    }

    @Override
    public BigInteger getModulus() {
        return encodeScheme.getModulus();
    }

    @Override
    public BigInteger getCiphertextModulus() {
        return n;
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(IntUtils.intToByteArray(PheFactory.PheType.OU98.ordinal()));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(n));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(g));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(h));
        byteArrayList.addAll(encodeScheme.toByteArrayList());

        return byteArrayList;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(n)
            .append(g)
            .append(h)
            .append(encodeScheme)
            .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != Ou98PhePublicKey.class) {
            return false;
        }
        Ou98PhePublicKey that = (Ou98PhePublicKey)o;
        return new EqualsBuilder()
            .append(this.n, that.n)
            .append(this.g, that.g)
            .append(this.h, that.h)
            .append(this.encodeScheme, that.encodeScheme)
            .isEquals();
    }
}
