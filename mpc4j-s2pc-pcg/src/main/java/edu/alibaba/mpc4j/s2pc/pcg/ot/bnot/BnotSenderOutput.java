package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

/**
 * 基础n选1-OT协议接收方输出。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public interface BnotSenderOutput {
    /**
     * 返回Rb。
     *
     * @param index  索引值。
     * @param choice 选择值。
     * @return Rb。
     */
    byte[] getRb(int index, int choice);

    /**
     * 返回最大选择值。
     *
     * @return 最大选择值。
     */
    int getN();

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    int getNum();
}
