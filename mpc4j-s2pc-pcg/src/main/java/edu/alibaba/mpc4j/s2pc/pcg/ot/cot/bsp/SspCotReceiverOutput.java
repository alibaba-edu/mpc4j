package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * SSP-COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class SspCotReceiverOutput {
    /**
     * α
     */
    private int alpha;
    /**
     * Rb数组
     */
    private byte[][] rbArray;

    /**
     * 创建接收方输出。
     *
     * @param alpha   α。
     * @param rbArray Rb数组。
     * @return 接收方输出。
     */
    public static SspCotReceiverOutput create(int alpha, byte[][] rbArray) {
        SspCotReceiverOutput receiverOutput = new SspCotReceiverOutput();
        assert alpha >= 0 && alpha < rbArray.length : "α must be in range [0, " + rbArray.length + "): " + alpha;
        receiverOutput.alpha = alpha;
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private SspCotReceiverOutput() {
        // empty
    }

    /**
     * 返回单点索引值。
     *
     * @return 单点索引值。
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * 返回Rb。
     *
     * @param index 索引值。
     * @return Rb。
     */
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    /**
     * 返回Rb数组。
     *
     * @return Rb数组。
     */
    public byte[][] getRbArray() {
        return rbArray;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return rbArray.length;
    }
}
