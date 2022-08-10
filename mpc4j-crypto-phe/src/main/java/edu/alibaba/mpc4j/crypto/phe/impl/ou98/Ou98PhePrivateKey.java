package edu.alibaba.mpc4j.crypto.phe.impl.ou98;

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
 * OU98半同态加密私钥。
 *
 * @author Weiran Liu
 * @date 2021/11/09
 */
public class Ou98PhePrivateKey implements PhePrivateKey {
    /**
     * 公钥
     */
    Ou98PhePublicKey publicKey;
    /**
     * The first prime number, {@code p} such that {@code p^2 * q = n}.
     */
    BigInteger p;
    /**
     * p^2
     */
    BigInteger pSquared;
    /**
     * Precomputed p^2 - p, used in private key encryption
     */
    BigInteger pSquaredOrder;
    /**
     * The second prime number, {@code q} such that {@code p^2 * q = n}.
     */
    BigInteger q;
    /**
     * Precomputed ((p^2)^(-1) mod q), used in private key encryption
     */
    BigInteger pSquaredInverse;
    /**
     * log(g_p)^(-1) mod p, where g_p = g^(p−1) mod p^2 is of order p in Z_(p^2)^*
     */
    BigInteger gpInverse;

    static Ou98PhePrivateKey fromParams(int modulusBitLength, boolean signed, int precision, int base,
        SecureRandom secureRandom) {
        // Generate two k-bit primes p and q (typically 3k = 1023)
        BigInteger p;
        do {
            p = BigInteger.probablePrime(modulusBitLength, secureRandom);
        } while (p.bitLength() != modulusBitLength);
        BigInteger q;
        do {
            q = BigInteger.probablePrime(modulusBitLength, secureRandom);
        } while (q.equals(p));
        // set n = p^2 * q
        BigInteger pSquared = p.multiply(p);
        BigInteger n = pSquared.multiply(q);
        // Randomly select a number g < n such that g_p = g^{p - 1} mod p^2 is of order p in Z_{p^2}^*
        BigInteger g, gp;
        do {
            g = BigIntegerUtils.randomPositive(n, secureRandom);
            gp = BigIntegerUtils.modPow(g, p.subtract(BigInteger.ONE), pSquared);
        } while (
            // g > 1，且g_p^p mod p^2 = 1
            g.compareTo(BigInteger.ONE) <= 0 || BigIntegerUtils.modPow(gp, p, pSquared).compareTo(BigInteger.ONE) != 0
        );
        // Similarly, choose g′ < n at random and publish H = g'^(nu) mod n
        BigInteger gPrime = BigIntegerUtils.randomPositive(n, secureRandom);
        BigInteger h = BigIntegerUtils.modPow(gPrime, n, n);

        PhePlaintextEncoder encodeScheme = PhePlaintextEncoder.fromParams(p, signed, precision, base);
        Ou98PhePublicKey publicKey = Ou98PhePublicKey.fromParams(encodeScheme, n, g, h);
        Ou98PhePrivateKey privateKey = new Ou98PhePrivateKey();
        privateKey.p = p;
        privateKey.pSquared = pSquared;
        privateKey.pSquaredOrder = pSquared.subtract(p);
        privateKey.q = q;
        privateKey.gpInverse = BigIntegerUtils.modInverse(Ou98PheEngine.lFunction(gp, p), p);
        privateKey.pSquaredInverse = BigIntegerUtils.modInverse(privateKey.pSquared, privateKey.q);
        privateKey.publicKey = publicKey;

        return privateKey;
    }

    public static Ou98PhePrivateKey fromByteArrayList(List<byte[]> byteArrayList) {
        int typeIndex = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        Preconditions.checkArgument(
            PheFactory.PheType.OU98.ordinal() == typeIndex, "类型索引 = %s，要求类型索引 = %s",
            typeIndex, PheFactory.PheType.OU98.ordinal()
        );
        BigInteger p = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger q = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger gpInverse = BigIntegerUtils.byteArrayToBigInteger(byteArrayList.remove(0));
        BigInteger pSquared = p.multiply(p);
        Ou98PhePrivateKey privateKey = new Ou98PhePrivateKey();
        privateKey.p = p;
        privateKey.pSquared = pSquared;
        privateKey.pSquaredOrder = pSquared.subtract(p);
        privateKey.q = q;
        privateKey.gpInverse = gpInverse;
        privateKey.pSquaredInverse = BigIntegerUtils.modInverse(pSquared, q);
        privateKey.publicKey = Ou98PhePublicKey.fromByteArrayList(byteArrayList);
        BigInteger n = pSquared.multiply(q);
        Preconditions.checkArgument(
            n.compareTo(privateKey.publicKey.n) == 0, "private key and public key modulus mismatch"
        );

        return privateKey;
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        byteArrayList.add(IntUtils.intToByteArray(PheFactory.PheType.OU98.ordinal()));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(p));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(q));
        byteArrayList.add(BigIntegerUtils.bigIntegerToByteArray(gpInverse));
        byteArrayList.addAll(publicKey.toByteArrayList());

        return byteArrayList;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.OU98;
    }

    @Override
    public PhePublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(p)
            .append(q)
            .append(gpInverse)
            .append(publicKey)
            .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != Ou98PhePrivateKey.class) {
            return false;
        }
        Ou98PhePrivateKey that = (Ou98PhePrivateKey)o;
        return new EqualsBuilder()
            .append(this.p, that.p)
            .append(this.q, that.q)
            .append(this.gpInverse, that.gpInverse)
            .append(this.publicKey, that.publicKey)
            .isEquals();
    }
}
