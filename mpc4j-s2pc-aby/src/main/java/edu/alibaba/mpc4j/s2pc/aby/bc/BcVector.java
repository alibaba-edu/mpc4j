package edu.alibaba.mpc4j.s2pc.aby.bc;

/**
 * 布尔电路向量（Boolean Circuit Vector）。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface BcVector {
    /**
     * 返回是否为公开向量。
     *
     * @return 是否为公开向量。
     */
    boolean isPublic();

    /**
     * 返回向量比特长度。
     *
     * @return 向量比特长度。
     */
    int bitLength();

    /**
     * 返回向量字节长度。
     *
     * @return 向量字节长度。
     */
    int byteLength();
}
