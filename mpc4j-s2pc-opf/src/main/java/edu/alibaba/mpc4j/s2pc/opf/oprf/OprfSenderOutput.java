package edu.alibaba.mpc4j.s2pc.opf.oprf;

/**
 * OPRF协议发送方输出接口。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public interface OprfSenderOutput {
    /**
     * 返回伪随机函数输出。
     *
     * @param index  索引值。
     * @param input 伪随机函数输入。
     * @return 伪随机函数输出。
     */
    byte[] getPrf(int index, byte[] input);

    /**
     * 返回伪随机函数输出字节长度。
     *
     * @return 码字字节长度。
     */
    int getPrfByteLength() ;

    /**
     * 返回索引值总数量。
     *
     * @return 索引值总数量。
     */
    int getBatchSize();
}
