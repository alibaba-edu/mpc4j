package edu.alibaba.mpc4j.common.tool.coder;

/**
 * 编码算法接口类。
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public interface Coder {
    /**
     * 返回数据字（dataword）比特长度。
     *
     * @return 数据字比特长度。
     */
    int getDatawordBitLength();

    /**
     * 返回数据字（dataword）字节长度。
     *
     * @return 数据字字节长度。
     */
    int getDatawordByteLength();

    /**
     * 返回码字（codeword）比特长度。
     *
     * @return 码字比特长度。
     */
    int getCodewordBitLength();

    /**
     * 返回码字（codeword）字节长度。
     *
     * @return 码字字节长度。
     */
    int getCodewordByteLength();

    /**
     * 返回最小汉明距离。
     *
     * @return 最小汉明距离。
     */
    int getMinimalHammingDistance();

    /**
     * 编码。
     *
     * @param input 数据字。
     * @return 码字。
     */
    byte[] encode(byte[] input);
}
