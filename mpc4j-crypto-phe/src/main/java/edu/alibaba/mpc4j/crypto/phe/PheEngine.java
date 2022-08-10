package edu.alibaba.mpc4j.crypto.phe;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.phe.params.*;

import java.math.BigInteger;

/**
 * 半同态加密引擎。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PheEngine {

    /**
     * 密钥生成。
     *
     * @param keyGenParams 密钥生成参数。
     * @return 私钥。
     */
    PhePrivateKey keyGen(PheKeyGenParams keyGenParams);

    /**
     * 返回质数比特长度，以表示安全级别。
     *
     * @return 质数比特长度。
     */
    int primeBitLength(PhePublicKey pk);

    /**
     * 公钥行加密。
     *
     * @param pk 公钥。
     * @param m  明文。
     * @return 密文。
     */
    BigInteger rawEncrypt(PhePublicKey pk, BigInteger m);

    /**
     * 私钥行加密。
     *
     * @param sk 私钥。
     * @param m  明文。
     * @return 密文。
     */
    BigInteger rawEncrypt(PhePrivateKey sk, BigInteger m);

    /**
     * 重随机化行密文。
     *
     * @param pk 公钥。
     * @param ct 密文。
     * @return 重随机化后的密文。
     */
    BigInteger rawObfuscate(PhePublicKey pk, BigInteger ct);

    /**
     * 密文{@code value}行相加。
     *
     * @param pk     公钥。
     * @param value1 第1个密文的{@code value}。
     * @param value2 第2个密文的{@code value}。
     * @return 加法结果。
     */
    BigInteger rawAdd(PhePublicKey pk, BigInteger value1, BigInteger value2);

    /**
     * 密文{@code value}与明文放缩系数{@code scalar}相乘。
     *
     * @param pk         公钥。
     * @param ciphertext 密文的{@code value}。
     * @param factor     相乘系数。
     * @return 乘法结果。
     */
    BigInteger rawMultiply(PhePublicKey pk, BigInteger ciphertext, BigInteger factor);

    /**
     * 解密{@code BigInteger}。
     *
     * @param sk 私钥。
     * @param ct 密文。
     * @return 解密结果。
     */
    BigInteger rawDecrypt(PhePrivateKey sk, BigInteger ct);

    /**
     * 用公钥加密{@code ModulusEncodedNumber}。
     *
     * @param pk      公钥。
     * @param encoded 明文。
     * @return 密文。
     */
    PheCiphertext encrypt(PhePublicKey pk, PhePlaintext encoded);

    /**
     * 用私钥加密{@code ModulusEncodedNumber}。
     *
     * @param sk      私钥。
     * @param encoded 明文。
     * @return 密文。
     */
    PheCiphertext encrypt(PhePrivateKey sk, PhePlaintext encoded);

    /**
     * 用公钥加密{@code BigInteger}。
     *
     * @param pk    公钥。
     * @param value 待加密的明文。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePublicKey pk, BigInteger value) {
        return encrypt(pk, pk.getPlaintextEncoder().encode(value));
    }

    /**
     * 用私钥加密{@code BigInteger}。
     *
     * @param sk    私钥。
     * @param value 待加密的{@code BigInteger}。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePrivateKey sk, BigInteger value) {
        return encrypt(sk, sk.getPublicKey().getPlaintextEncoder().encode(value));
    }

    /**
     * 用公钥加密{@code double}。
     *
     * @param pk    公钥。
     * @param value 待加密的明文。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePublicKey pk, double value) {
        return encrypt(pk, pk.getPlaintextEncoder().encode(value));
    }

    /**
     * 用私钥加密{@code double}。
     *
     * @param sk    私钥。
     * @param value 待加密的明文。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePrivateKey sk, double value) {
        return encrypt(sk, sk.getPublicKey().getPlaintextEncoder().encode(value));
    }

    /**
     * 用公钥加密{@code long}。
     *
     * @param pk    公钥。
     * @param value 待加密的明文。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePublicKey pk, long value) {
        return encrypt(pk, pk.getPlaintextEncoder().encode(value));
    }

    /**
     * 用私钥加密{@code long}。
     *
     * @param sk    私钥。
     * @param value 待加密的明文。
     * @return 加密结果。
     */
    default PheCiphertext encrypt(PhePrivateKey sk, long value) {
        return encrypt(sk, sk.getPublicKey().getPlaintextEncoder().encode(value));
    }

    /**
     * 重随机化{@code PheEncryptedNumber}。
     *
     * @param pk 公钥。
     * @param ct 密文。
     * @return 重随机化密文。
     */
    PheCiphertext obfuscate(PhePublicKey pk, PheCiphertext ct);

    /**
     * {@code PheEncryptedNumber} + {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    PheCiphertext add(PhePublicKey pk, PheCiphertext operand, PheCiphertext other);

    /**
     * {@code PheEncryptedNumber} + {@code ModulusEncodedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    PheCiphertext add(PhePublicKey pk, PheCiphertext operand, PhePlaintext other);

    /**
     * {@code ModulusEncodedNumber} + {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, PhePlaintext operand, PheCiphertext other) {
        return add(pk, other, operand);
    }

    /**
     * {@code PheEncryptedNumber} + {@code BigInteger}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, PheCiphertext operand, BigInteger other) {
        return add(pk, operand, pk.encode(other));
    }

    /**
     * {@code BigInteger} + {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, BigInteger operand, PheCiphertext other) {
        return add(pk, pk.encode(operand), other);
    }

    /**
     * {@code PheEncryptedNumber} + {@code double}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, PheCiphertext operand, double other) {
        return add(pk, operand, pk.encode(other));
    }

    /**
     * {@code double} + {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, double operand, PheCiphertext other) {
        return add(pk, pk.encode(operand), other);
    }

    /**
     * {@code PheEncryptedNumber} + {@code long}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, PheCiphertext operand, long other) {
        return add(pk, operand, pk.encode(other));
    }

    /**
     * {@code PheEncryptedNumber} + {@code long}。
     *
     * @param pk      公钥。
     * @param operand 被加数。
     * @param other   加数。
     * @return 加法结果。
     */
    default PheCiphertext add(PhePublicKey pk, long operand, PheCiphertext other) {
        return add(pk, pk.encode(operand), other);
    }

    /**
     * 返回{@code PheEncryptedNumber}的加法逆元。
     *
     * @param pk      公钥。
     * @param operand 输入。
     * @return 输入加法逆元。
     */
    default PheCiphertext additiveInverse(PhePublicKey pk, PheCiphertext operand) {
        return PheCiphertext.fromParams(
            pk, BigIntegerUtils.modInverse(operand.getCiphertext(), pk.getCiphertextModulus()), operand.getExponent()
        );
    }

    /**
     * {@code PheEncryptedNumber} - {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PheCiphertext operand, PheCiphertext other) {
        return add(pk, operand, additiveInverse(pk, other));
    }

    /**
     * {@code PheEncryptedNumber} - {@code ModulusEncodedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PheCiphertext operand, PhePlaintext other) {
        return add(pk, operand, encrypt(pk, pk.getPlaintextEncoder().additiveInverse(other)));
    }

    /**
     * {@code ModulusEncodedNumber} - {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PhePlaintext operand, PheCiphertext other) {
        return subtract(pk, encrypt(pk, operand), other);
    }

    /**
     * {@code PheEncryptedNumber} - {@code BigInteger}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PheCiphertext operand, BigInteger other) {
        return subtract(pk, operand, pk.encode(other));
    }

    /**
     * {@code BigInteger} - {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, BigInteger operand, PheCiphertext other) {
        return subtract(pk, pk.encode(operand), other);
    }

    /**
     * {@code PheEncryptedNumber} - {@code double}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PheCiphertext operand, double other) {
        return subtract(pk, operand, pk.encode(other));
    }

    /**
     * {@code double} - {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, double operand, PheCiphertext other) {
        return subtract(pk, pk.encode(operand), other);
    }

    /**
     * {@code PheEncryptedNumber} - {@code long}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, PheCiphertext operand, long other) {
        return subtract(pk, operand, pk.encode(other));
    }

    /**
     * {@code long} - {@code PheEncryptedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被减数。
     * @param other   减数。
     * @return 减法结果。
     */
    default PheCiphertext subtract(PhePublicKey pk, long operand, PheCiphertext other) {
        return subtract(pk, pk.encode(operand), other);
    }

    /**
     * {@code PheEncryptedNumber} * {@code ModulusEncodedNumber}。
     *
     * @param pk      公钥。
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 乘法结果。
     */
    PheCiphertext multiply(PhePublicKey pk, PheCiphertext operand, PhePlaintext other);

    /**
     * {@code PheEncryptedNumber} * {@code BigInteger}。
     *
     * @param pk      公钥。
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 乘法结果。
     */
    default PheCiphertext multiply(PhePublicKey pk, PheCiphertext operand, BigInteger other) {
        return multiply(pk, operand, pk.encode(other));
    }

    /**
     * {@code PheEncryptedNumber} * {@code double}。
     *
     * @param pk      公钥。
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 乘法结果。
     */
    default PheCiphertext multiply(PhePublicKey pk, PheCiphertext operand, double other) {
        return multiply(pk, operand, pk.encode(other));
    }

    /**
     * {@code PheEncryptedNumber} * {@code long}。
     *
     * @param pk      公钥。
     * @param operand 被乘数。
     * @param other   乘数。
     * @return 乘法结果。
     */
    default PheCiphertext multiply(PhePublicKey pk, PheCiphertext operand, long other) {
        return multiply(pk, operand, pk.encode(other));
    }

    /**
     * {@code PheEncryptedNumber} / {@code double}。
     *
     * @param pk      公钥。
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    default PheCiphertext divide(PhePublicKey pk, PheCiphertext operand, double other) {
        return multiply(pk, operand, pk.getPlaintextEncoder().encode(1.0 / other));
    }

    /**
     * {@code PheEncryptedNumber} / {@code long}。
     *
     * @param pk      公钥。
     * @param operand 被除数。
     * @param other   除数。
     * @return 除法结果。
     */
    default PheCiphertext divide(PhePublicKey pk, PheCiphertext operand, long other) {
        return multiply(pk, operand, pk.getPlaintextEncoder().encode(1.0 / other));
    }

    /**
     * 解密{@code PheEncryptedNumber}。
     *
     * @param sk 私钥。
     * @param ct 密文。
     * @return 解密结果。
     */
    PhePlaintext decrypt(PhePrivateKey sk, PheCiphertext ct);

    /**
     * 如果{@code newExp}小于{@code PheEncryptedNumber}当前的{@code exponent}，把当前的{@code exponent}降低至{@code newExp}。
     *
     * @param pk     公钥。
     * @param ct     密文。
     * @param newExp 新的{@code exponent}，必须小于当前的{@code exponent}。
     * @return 表示相同值的{@code PheEncryptedNumber}，但{@code exponent}等于{@code newExp}。
     */
    PheCiphertext decreaseExponentTo(PhePublicKey pk, PheCiphertext ct, int newExp);

    /**
     * 返回半同态加密类型。
     *
     * @return 半同态加密类型。
     */
    PheFactory.PheType getPheType();
}
