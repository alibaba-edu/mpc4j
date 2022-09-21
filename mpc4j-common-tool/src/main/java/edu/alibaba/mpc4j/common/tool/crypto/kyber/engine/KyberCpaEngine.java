package edu.alibaba.mpc4j.common.tool.crypto.kyber.engine;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngine;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.KyberEngineFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberKeyPair;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.utils.Poly;

import java.security.SecureRandom;


/**
 * CPA-secure Kyber encryption engine.
 *
 * @author Sheng Hu, Weiran Liu.
 * @date 2022/09/01
 */
public class KyberCpaEngine implements KyberEngine {
    /**
     * parameter K, can be 2, 3 or 4.
     */
    private final int paramsK;
    /**
     * byte length of public key t
     */
    private final int publicKeyByteLength;
    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;

    public KyberCpaEngine(int paramsK) {
        this(paramsK, new SecureRandom());
    }

    public KyberCpaEngine(int paramsK, SecureRandom secureRandom) {
        assert KyberParams.validParamsK(paramsK) : KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK;
        this.paramsK = paramsK;
        switch (paramsK) {
            case 2:
                publicKeyByteLength = KyberParams.POLY_VECTOR_BYTES_512;
                break;
            case 3:
                publicKeyByteLength = KyberParams.POLY_VECTOR_BYTES_768;
                break;
            case 4:
                publicKeyByteLength = KyberParams.POLY_VECTOR_BYTES_1024;
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        this.secureRandom = secureRandom;
    }

    @Override
    public KyberEngineFactory.KyberType getKyberType() {
        return KyberEngineFactory.KyberType.KYBER_CPA;
    }

    @Override
    public int publicKeyByteLength() {
        return publicKeyByteLength;
    }

    @Override
    public KyberKeyPair generateKeyPair() {
        // matrix seed
        byte[] matrixSeed = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(matrixSeed);
        // secret key s
        short[][] sVector = Poly.createEmptyPolyVector(paramsK);
        // public key t
        short[][] tVector = Poly.createEmptyPolyVector(paramsK);
        // noise e
        short[][] e = Poly.createEmptyPolyVector(paramsK);
        byte[] noiseSeed = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(noiseSeed);
        // 生成了公钥中的A
        short[][][] matrixA = KyberEngineHelper.generateMatrix(matrixSeed, false, paramsK);
        byte nonce = (byte) 0;
        // generate secret key vector
        for (int i = 0; i < paramsK; i++) {
            sVector[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
            nonce = (byte) (nonce + (byte) 1);
        }
        // generate noise vector
        for (int i = 0; i < paramsK; i++) {
            e[i] = Poly.getNoisePoly(noiseSeed, nonce, paramsK);
            nonce = (byte) (nonce + (byte) 1);
        }
        Poly.inPolyVectorNtt(sVector);
        Poly.inPolyVectorBarrettReduce(sVector);
        Poly.inPolyVectorNtt(e);
        // A · s + e
        for (int i = 0; i < paramsK; i++) {
            tVector[i] = Poly.polyVectorPointWiseAccMontgomery(matrixA[i], sVector);
            Poly.inPolyToMontgomery(tVector[i]);
        }
        Poly.inPolyVectorAdd(tVector, e);
        Poly.inPolyVectorBarrettReduce(tVector);
        return new KyberKeyPair(Poly.polyVectorToByteArray(tVector), sVector, matrixSeed);
    }

    @Override
    public byte[] randomPublicKey() {
        byte[] randomPublicKey = new byte[publicKeyByteLength];
        secureRandom.nextBytes(randomPublicKey);
        short[][] tVector = Poly.polyVectorFromBytes(randomPublicKey);
        Poly.inPolyVectorBarrettReduce(tVector);
        return Poly.polyVectorToByteArray(tVector);
    }

    @Override
    public byte[] encapsulate(byte[] key, byte[] publicKey, byte[] matrixSeed) {
        assert key.length == KyberParams.SYM_BYTES
            : "Invalid key length, must be " + KyberParams.SYM_BYTES + ": " + key.length;
        secureRandom.nextBytes(key);
        return KyberEngineHelper.encrypt(key, Poly.polyVectorFromBytes(publicKey), matrixSeed, paramsK, secureRandom);
    }

    @Override
    public byte[] decapsulate(byte[] ciphertext, short[][] secretKey, byte[] publicKey, byte[] matrixSeed) {
        return KyberEngineHelper.decrypt(ciphertext, secretKey, paramsK);
    }
}
