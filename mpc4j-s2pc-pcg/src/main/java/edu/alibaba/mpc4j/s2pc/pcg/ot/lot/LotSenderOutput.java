package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

/**
 * 2^l选1-OT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public interface LotSenderOutput {
    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    LotSenderOutput split(int length);

    /**
     * 将当前输出结果数量减少至给定的数量。
     *
     * @param length 给定的数量。
     */
    void reduce(int length);

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    void merge(LotSenderOutput that);

    /**
     * 返回Rb。
     *
     * @param index  索引值。
     * @param choice 选择值。
     * @return Rb。
     */
    byte[] getRb(int index, byte[] choice);

    /**
     * 返回输入比特长度。
     *
     * @return 输入比特长度。
     */
    int getInputBitLength();

    /**
     * 返回输入字节长度。
     *
     * @return 输入字节长度。
     */
    int getInputByteLength();

    /**
     * 返回输出比特长度。
     *
     * @return 输出比特长度。
     */
    int getOutputBitLength();

    /**
     * 返回输出字节长度。
     *
     * @return 输出字节长度。
     */
    int getOutputByteLength();

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    int getNum();
}
