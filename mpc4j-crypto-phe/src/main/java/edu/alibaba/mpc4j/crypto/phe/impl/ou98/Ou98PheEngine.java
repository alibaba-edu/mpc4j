package edu.alibaba.mpc4j.crypto.phe.impl.ou98;

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
 * OU98半同态加密引擎。论文来源：
 * <p>
 * Coron J S, Naccache D, Paillier P. Accelerating Okamoto-Uchiyama public-key cryptosystem[J]. Electronics Letters,
 * 1999, 35(4): 291-292.
 * </p>
 * 注意：虽然论文中提出了加速方案，但要求私钥生成的质数p满足p = 2 * t + 1，但这个生成过程太慢。因此本实现仍然使用最原始的OU方案。
 *
 * @author Weiran Liu
 * @date 2021/11/09
 */
public class Ou98PheEngine extends AbstractPheEngine {
    /**
     * Computes the L function as defined in OU's paper. That is: L(x,p) = (x - 1) / p.
     *
     * @param x the input x.
     * @param p the input p.
     * @return L(x, p) = (x - 1) / p.
     */
    static BigInteger lFunction(BigInteger x, BigInteger p) {
        return x.subtract(BigInteger.ONE).divide(p);
    }

    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;

    public Ou98PheEngine(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public PheFactory.PheType getPheType() {
        return PheFactory.PheType.OU98;
    }

    @Override
    public PhePrivateKey keyGen(PheKeyGenParams keyGenParams) {
        int modulusBitLength = PheFactory.getModulusBitLength(PheFactory.PheType.OU98, keyGenParams.getPheSecLevel());
        boolean signed = keyGenParams.isSigned();
        int precision = keyGenParams.getPrecision();
        int base = keyGenParams.getBase();

        return Ou98PhePrivateKey.fromParams(modulusBitLength, signed, precision, base, secureRandom);
    }

    @Override
    public int primeBitLength(PhePublicKey pk) {
        return pk.getPlaintextEncoder().getModulus().bitLength();
    }

    @Override
    public BigInteger rawEncrypt(PhePublicKey pk, BigInteger m) {
        Preconditions.checkArgument(pk instanceof Ou98PhePublicKey);
        Ou98PhePublicKey publicKey = (Ou98PhePublicKey) pk;
        BigInteger n = publicKey.n;
        BigInteger g = publicKey.g;
        BigInteger h = publicKey.h;
        // pick r < n uniformly at random and encrypt the (k − 1)-bit message m by c = g^m * h^r mod n
        BigInteger r = BigIntegerUtils.randomPositive(n, secureRandom);
        return BigIntegerUtils.modPow(g, m, n).multiply(BigIntegerUtils.modPow(h, r, n)).mod(n);
    }

    @Override
    public BigInteger rawEncrypt(PhePrivateKey sk, BigInteger m) {
        Preconditions.checkArgument(sk instanceof Ou98PhePrivateKey);
        Ou98PhePrivateKey privateKey = (Ou98PhePrivateKey) sk;
        Ou98PhePublicKey publicKey = (Ou98PhePublicKey) privateKey.getPublicKey();
        BigInteger n = publicKey.n;
        BigInteger g = publicKey.g;
        BigInteger h = publicKey.h;
        // pick r < n uniformly at random and encrypt the (k − 1)-bit message m by c = g^m * h^r mod n
        BigInteger r = BigIntegerUtils.randomPositive(n, secureRandom);
        // ct = (g^m mod n) * h^r mod n，分别把g^m mod n和h^r mod n拆到CRT里面
        BigInteger m1 = m.mod(privateKey.pSquaredOrder);
        BigInteger m2 = m.mod(privateKey.q);
        BigInteger gm1 = BigIntegerUtils.modPow(g, m1, privateKey.pSquared);
        BigInteger gm2 = BigIntegerUtils.modPow(g, m2, privateKey.q);
        BigInteger gm = crt(gm1, gm2, privateKey.pSquared, privateKey.q, privateKey.pSquaredInverse);

        BigInteger r1 = r.mod(privateKey.pSquaredOrder);
        BigInteger r2 = r.mod(privateKey.q);
        BigInteger hr1 = BigIntegerUtils.modPow(h, r1, privateKey.pSquared);
        BigInteger hr2 = BigIntegerUtils.modPow(h, r2, privateKey.q);
        BigInteger hr = crt(hr1, hr2, privateKey.pSquared, privateKey.q, privateKey.pSquaredInverse);

        return gm.multiply(hr).mod(n);
    }

    @Override
    public BigInteger rawObfuscate(PhePublicKey pk, BigInteger ct) {
        Preconditions.checkArgument(pk instanceof Ou98PhePublicKey);
        Ou98PhePublicKey publicKey = (Ou98PhePublicKey) pk;
        BigInteger n = publicKey.n;
        BigInteger h = publicKey.h;
        // pick r < n uniformly at random and encrypt the (k − 1)-bit message m by c = G^m * H^r mod n
        BigInteger r = BigIntegerUtils.randomPositive(n, secureRandom);
        return BigIntegerUtils.modPow(h, r, n).multiply(ct).mod(n);
    }

    @Override
    public BigInteger rawAdd(PhePublicKey pk, BigInteger value1, BigInteger value2) {
        Preconditions.checkArgument(pk instanceof Ou98PhePublicKey);
        return value1.multiply(value2).mod(pk.getCiphertextModulus());
    }

    @Override
    public BigInteger rawMultiply(PhePublicKey pk, BigInteger ciphertext, BigInteger factor) {
        Preconditions.checkArgument(pk instanceof Ou98PhePublicKey);
        return BigIntegerUtils.modPow(ciphertext, factor, pk.getCiphertextModulus());
    }

    @Override
    public BigInteger rawDecrypt(PhePrivateKey sk, BigInteger ct) {
        Preconditions.checkArgument(sk instanceof Ou98PhePrivateKey);
        Ou98PhePrivateKey privateKey = (Ou98PhePrivateKey) sk;
        BigInteger pSquared = privateKey.pSquared;
        BigInteger p = privateKey.p;
        // c′ = c^(p - 1) mod p^2, m = log(c′) log(g_p)^(−1) mod p
        BigInteger cPrime = BigIntegerUtils.modPow(ct, p.subtract(BigInteger.ONE), pSquared);

        return lFunction(cPrime, p).multiply(privateKey.gpInverse).mod(p);
    }
}
