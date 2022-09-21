package edu.alibaba.mpc4j.common.tool.crypto.kyber.params;

/**
 * Kyber的公私钥对。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/25
 */
public class KyberKeyPair {
    /**
     * pk = A · s + e，序列化为向量
     */
    private final byte[] publicKeyVector;
    /**
     * 矩阵A生成种子
     */
    private final byte[] matrixSeed;
    /**
     * sk = s，包括K个多项式
     */
    private final short[][] secretKeyVector;

    /**
     * 创建Kyber公私钥对。
     *
     * @param publicKeyVec    序列化公钥向量。
     * @param secretKeyVector 私钥向量。
     * @param matrixSeed      矩阵生成种子。
     */
    public KyberKeyPair(byte[] publicKeyVec, short[][] secretKeyVector, byte[] matrixSeed) {
        this.publicKeyVector = publicKeyVec;
        this.secretKeyVector = secretKeyVector;
        this.matrixSeed = matrixSeed;
    }

    /**
     * 返回序列化公钥pk = A · s + e。
     *
     * @return 公钥。
     */
    public byte[] getPublicKey() {
        return publicKeyVector;
    }

    /**
     * 返回矩阵A生成种子。
     *
     * @return 矩阵A生成种子。
     */
    public byte[] getMatrixSeed() {
        return matrixSeed;
    }


    /**
     * 返回私钥向量。
     *
     * @return 私钥向量。
     */
    public short[][] getSecretKey() {
        return secretKeyVector;
    }
}
