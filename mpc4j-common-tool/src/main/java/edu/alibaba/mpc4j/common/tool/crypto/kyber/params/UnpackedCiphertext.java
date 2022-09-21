package edu.alibaba.mpc4j.common.tool.crypto.kyber.params;

/**
 * Helper class for unpacked ciphertext. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/kyber/UnpackedCipherText.java
 * </p>
 *
 * @author Steven K Fisher, Sheng Hu, Weiran Liu
 */
public class UnpackedCiphertext {
    /**
     * 向量u = A^T · r + e_1
     */
    private short[][] vectorU;
    /**
     * v = t^T · r + e_2 + 2「q/2」
     */
    private short[] v;

    /**
     * 构造Kyber非序列化密文。
     */
    public UnpackedCiphertext() {

    }

    /**
     * 返回向量u。
     *
     * @return 向量u。
     */
    public short[][] getVectorU() {
        return vectorU;
    }

    /**
     * 设置向量u。
     *
     * @param vectorU 向量u。
     */
    public void setVectorU(short[][] vectorU) {
        this.vectorU = vectorU;
    }

    /**
     * 返回向量v。
     *
     * @return 向量v。
     */
    public short[] getV() {
        return v;
    }

    /**
     * 设置向量v。
     *
     * @param v 向量v。
     */
    public void setV(short[] v) {
        this.v = v;
    }
}
