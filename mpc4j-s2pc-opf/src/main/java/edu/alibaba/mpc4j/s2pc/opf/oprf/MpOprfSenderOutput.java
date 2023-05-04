package edu.alibaba.mpc4j.s2pc.opf.oprf;

/**
 * MPOPRF发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public interface MpOprfSenderOutput extends OprfSenderOutput {
    /**
     * 返回伪随机函数输出。
     *
     * @param input 伪随机函数输入。
     * @return 伪随机函数输出。
     */
    byte[] getPrf(byte[] input);

    /**
     * 返回伪随机函数输出。
     *
     * @param index  索引值。
     * @param input 伪随机函数输入。
     * @return 伪随机函数输出。
     */
    @Override
    default byte[] getPrf(int index, byte[] input) {
        return getPrf(input);
    }
}
