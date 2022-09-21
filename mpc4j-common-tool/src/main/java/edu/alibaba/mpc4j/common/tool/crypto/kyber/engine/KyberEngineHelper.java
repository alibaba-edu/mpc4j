package edu.alibaba.mpc4j.common.tool.crypto.kyber.engine;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberUniformRandom;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.UnpackedCiphertext;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.utils.Poly;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Indistinguishability under chosen plaintext attack (IND-CPA) helper class. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/Indcpa.java
 * </p>
 *
 * @author Steven K Fisher, Sheng Hu, Weiran Liu
 */
public class KyberEngineHelper {
    /**
     * Pseudo-random function to derive a deterministic array of random bytes from the supplied secret key object and
     * other parameters.
     *
     * @param prfByteLength PRF byte length.
     * @param key           key.
     * @param nonce         nonce.
     * @return pseudo-random result.
     */
    public static byte[] generatePrfByteArray(int prfByteLength, byte[] key, byte nonce) {
        Hash shake256Hash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_256, prfByteLength);
        byte[] newKey = new byte[key.length + 1];
        System.arraycopy(key, 0, newKey, 0, key.length);
        newKey[key.length] = nonce;
        return shake256Hash.digestToBytes(newKey);
    }

    /**
     * Runs rejection sampling on uniform random bytes to generate uniform random integers modulo `Q`.
     *
     * @param uniformRandom    where uniform random bytes to put in.
     * @param buf              input uniform random buffer.
     * @param bufferByteLength byte length that will be used in the sampling.
     * @param l                output uniform random length.
     */
    public static void generateUniform(KyberUniformRandom uniformRandom, byte[] buf, int bufferByteLength, int l) {
        short[] uniformPoly = new short[KyberParams.POLY_BYTES];
        int d1;
        int d2;
        // Always start at 0
        int uniformI = 0;
        int j = 0;
        while ((uniformI < l) && ((j + KyberParams.MATH_THREE) <= bufferByteLength)) {
            d1 = (((buf[j] & 0xFF) | ((buf[j + 1] & 0xFF) << 8)) & 0xFFF);
            d2 = (((((buf[j + 1] & 0xFF)) >> 4) | ((buf[j + 2] & 0xFF) << 4)) & 0xFFF);
            j = j + 3;
            if (d1 < KyberParams.PARAMS_Q) {
                uniformPoly[uniformI] = (short) d1;
                uniformI++;
            }
            if (uniformI < l && d2 < KyberParams.PARAMS_Q) {
                uniformPoly[uniformI] = (short) d2;
                uniformI++;
            }
        }
        uniformRandom.setUniformIntervalBound(uniformI);
        uniformRandom.setUniformRandom(uniformPoly);
    }

    /**
     * Generate a Z_q^{K * K} matrix from the given matrix seed.
     *
     * @param matrixSeed matrix seed.
     * @param transposed transpose or not.
     * @param paramsK    parameter K, can be 2, 3 or 4.
     * @return generated matrix.
     */
    public static short[][][] generateMatrix(byte[] matrixSeed, boolean transposed, int paramsK) {
        Hash shake128Hash = HashFactory.createInstance(HashFactory.HashType.BC_SHAKE_128, 672);
        short[][][] matrix = new short[paramsK][paramsK][KyberParams.POLY_BYTES];
        // 先创建均匀随机数变量，后续此变量可以复用，减少内存开销
        KyberUniformRandom uniformRandom = new KyberUniformRandom();
        for (int i = 0; i < paramsK; i++) {
            matrix[i] = Poly.createEmptyPolyVector(paramsK);
            for (int j = 0; j < paramsK; j++) {
                byte[] ij = new byte[2];
                if (transposed) {
                    ij[0] = (byte) i;
                    ij[1] = (byte) j;
                } else {
                    ij[0] = (byte) j;
                    ij[1] = (byte) i;
                }
                byte[] hashSeed = new byte[34];
                System.arraycopy(matrixSeed, 0, hashSeed, 0, KyberParams.SYM_BYTES);
                System.arraycopy(ij, 0, hashSeed, KyberParams.SYM_BYTES, 2);
                byte[] uniformRandomBuffer = shake128Hash.digestToBytes(hashSeed);
                byte[] frontBuffer = Arrays.copyOfRange(uniformRandomBuffer, 0, 504);
                generateUniform(uniformRandom, frontBuffer, frontBuffer.length, KyberParams.PARAMS_N);
                int ui = uniformRandom.getUniformIntervalBound();
                matrix[i][j] = uniformRandom.getUniformRandom();
                while (ui < KyberParams.PARAMS_N) {
                    // 如果有效的采样数量不足256，则用剩余的部分额外补充随机稀疏
                    byte[] endBuffer = Arrays.copyOfRange(uniformRandomBuffer, 504, 672);
                    generateUniform(uniformRandom, endBuffer, endBuffer.length, KyberParams.PARAMS_N - ui);
                    int ctrn = uniformRandom.getUniformIntervalBound();
                    short[] missing = uniformRandom.getUniformRandom();
                    // 只补充后面的部分，不会修改前面的部分。
                    if (KyberParams.PARAMS_N - ui >= 0) {
                        System.arraycopy(missing, 0, matrix[i][j], ui, KyberParams.PARAMS_N - ui);
                    }
                    ui = ui + ctrn;
                }
            }
        }
        return matrix;
    }

    /**
     * Pack the ciphertext into a byte array.
     *
     * @param u       u.
     * @param v       v.
     * @param paramsK parameter K, can be 2, 3 or 4.
     * @return packed ciphertext.
     */
    private static byte[] packCiphertext(short[][] u, short[] v, int paramsK) {
        byte[] bCompress = Poly.compressPolyVector(u, paramsK);
        byte[] vCompress = Poly.compressPoly(v, paramsK);
        byte[] returnArray = new byte[bCompress.length + vCompress.length];
        System.arraycopy(bCompress, 0, returnArray, 0, bCompress.length);
        System.arraycopy(vCompress, 0, returnArray, bCompress.length, vCompress.length);
        return returnArray;
    }

    /**
     * Unpack the ciphertext from a byte array into u and v.
     * 解包密文
     *
     * @param packedCiphertext packed ciphertext.
     * @param paramsK          parameter K, can be 2, 3 or 4.
     * @return u and v.
     */
    private static UnpackedCiphertext unpackCiphertext(byte[] packedCiphertext, int paramsK) {
        UnpackedCiphertext unpackedCipherText = new UnpackedCiphertext();
        byte[] compressPolyU;
        byte[] compressPolyV;
        switch (paramsK) {
            case 2:
                compressPolyU = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_512];
                break;
            case 3:
                compressPolyU = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_768];
                break;
            case 4:
                compressPolyU = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_1024];
                break;
            default:
                throw new IllegalArgumentException(KyberParams.INVALID_PARAMS_K_ERROR_MESSAGE + paramsK);
        }
        System.arraycopy(packedCiphertext, 0, compressPolyU, 0, compressPolyU.length);
        compressPolyV = new byte[packedCiphertext.length - compressPolyU.length];
        System.arraycopy(packedCiphertext, compressPolyU.length, compressPolyV, 0, compressPolyV.length);
        unpackedCipherText.setVectorU(Poly.decompressPolyVector(compressPolyU, paramsK));
        unpackedCipherText.setV(Poly.decompressPoly(compressPolyV, paramsK));

        return unpackedCipherText;
    }

    /**
     * Encrypt message.
     *
     * @param m          message.
     * @param publicKey  public key.
     * @param matrixSeed matrix generation seed.
     * @param paramsK    parameter K, can be 2, 3 or 4.
     * @return ciphertext.
     */
    public static byte[] encrypt(byte[] m, short[][] publicKey, byte[] matrixSeed, int paramsK, SecureRandom secureRandom) {
        byte[] coins = new byte[KyberParams.SYM_BYTES];
        secureRandom.nextBytes(coins);
        return encrypt(m, publicKey, matrixSeed, paramsK, coins);
    }

    /**
     * Encrypt message with the given randomness.
     *
     * @param m          message.
     * @param publicKey  public key t.
     * @param matrixSeed matrix generation seed.
     * @param paramsK    parameter K, can be 2, 3 or 4.
     * @param coins      given randomness.
     * @return ciphertext.
     */
    public static byte[] encrypt(byte[] m, short[][] publicKey, byte[] matrixSeed, int paramsK, byte[] coins) {
        short[][] rVector = Poly.createEmptyPolyVector(paramsK);
        short[][] e1Vector = Poly.createEmptyPolyVector(paramsK);
        short[][] uVector = Poly.createEmptyPolyVector(paramsK);
        // 「q / 2」 · m
        short[] polyM = Poly.polyFromMessage(m);
        // A^T
        short[][][] transposeMatrixA = KyberEngineHelper.generateMatrix(matrixSeed, true, paramsK);
        // generate r, e_1
        for (int i = 0; i < paramsK; i++) {
            rVector[i] = Poly.getNoisePoly(coins, (byte) (i), paramsK);
            e1Vector[i] = Poly.getNoisePoly(coins, (byte) (i + paramsK), 3);
        }
        // generate e_2
        short[] epp = Poly.getNoisePoly(coins, (byte) (paramsK * 2), 3);
        // A^T · r
        Poly.inPolyVectorNtt(rVector);
        Poly.inPolyVectorBarrettReduce(rVector);
        for (int i = 0; i < paramsK; i++) {
            uVector[i] = Poly.polyVectorPointWiseAccMontgomery(transposeMatrixA[i], rVector);
        }
        // t^T · r
        short[] v = Poly.polyVectorPointWiseAccMontgomery(publicKey, rVector);
        Poly.inPolyVectorInvNttMontgomery(uVector);
        Poly.inPolyInvNttMontgomery(v);
        // u = A^T · r + e_1
        Poly.inPolyVectorAdd(uVector, e1Vector);
        // t^T · r + e_2
        Poly.inPolyAdd(v, epp);
        // t^T · r + e_2 + 「q / 2」 · m
        Poly.inPolyAdd(v, polyM);
        // pack ciphertext
        Poly.inPolyVectorBarrettReduce(uVector);
        Poly.inPolyBarrettReduce(v);
        return KyberEngineHelper.packCiphertext(uVector, v, paramsK);
    }

    /**
     * Decrypt message.
     *
     * @param c         ciphertext.
     * @param secretKey secret key s.
     * @param paramsK   parameter K, can be 2, 3 or 4.
     * @return plaintext.
     */
    public static byte[] decrypt(byte[] c, short[][] secretKey, int paramsK) {
        // 需要复制一份密钥，否则解密一次时私钥会发生变化
        short[][] copySecretKey = new short[secretKey.length][];
        for (int i = 0; i < secretKey.length; i++) {
            copySecretKey[i] = new short[secretKey[i].length];
            System.arraycopy(secretKey[i], 0, copySecretKey[i], 0, secretKey[i].length);
        }
        UnpackedCiphertext unpackedCipherText = KyberEngineHelper.unpackCiphertext(c, paramsK);
        short[][] u = unpackedCipherText.getVectorU();
        short[] v = unpackedCipherText.getV();
        // s^T · u
        Poly.inPolyVectorNtt(u);
        short[] mp = Poly.polyVectorPointWiseAccMontgomery(copySecretKey, u);
        Poly.inPolyInvNttMontgomery(mp);
        // m = v - s^T · u
        short[] polyM = Poly.polySub(v, mp);
        Poly.inPolyBarrettReduce(polyM);
        // 将结果返回成消息
        return Poly.polyToMessage(polyM);
    }
}
