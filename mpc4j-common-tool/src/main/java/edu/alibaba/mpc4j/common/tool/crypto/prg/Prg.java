package edu.alibaba.mpc4j.common.tool.crypto.prg;

/**
 * 伪随机数生成器接口。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public interface Prg {
    /**
     * 返回输出字节长度。
     *
     * @return 输出字节长度。
     */
    int getOutputByteLength();

    /**
     * 将输入种子扩展指定字节长度的随机数。
     *
     * @param seed 种子。
     * @return 扩展的随机数。
     */
    byte[] extendToBytes(byte[] seed);

    /**
     * 返回伪随机数生成器类型。
     *
     * @return 伪随机数生成器类型。
     */
    PrgFactory.PrgType getPrgType();
}
