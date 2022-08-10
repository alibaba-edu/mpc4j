package edu.alibaba.mpc4j.common.tool.crypto.hash;

/**
 * 哈希函数接口。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public interface Hash {
    /**
     * 哈希给定的消息，输出的字节长度为初始化时设置的字节长度。
     *
     * @param message 消息。
     * @return 哈希值。
     */
    byte[] digestToBytes(byte[] message);

    /**
     * 返回输出字节长度。
     *
     * @return 输出字节长度。
     */
    int getOutputByteLength();

    /**
     * 返回哈希函数类型。
     *
     * @return 哈希函数类型。
     */
    HashFactory.HashType getHashType();
}
