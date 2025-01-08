package edu.alibaba.mpc4j.common.tool.f3hash;

import edu.alibaba.mpc4j.common.tool.f3hash.F3HashFactory.F3HashType;

/**
 * F3 hash, whose output is 512 F3 elements
 *
 * @author Feng Han
 * @date 2024/10/20
 */
public interface F3Hash {
    /**
     * 单位输出长度
     */
    int OUTPUT_F3_NUM = 512;
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
    default int getOutputByteLength(){
        return OUTPUT_F3_NUM;
    }

    /**
     * 返回哈希函数类型。
     *
     * @return 哈希函数类型。
     */
    F3HashType getHashType();
}
