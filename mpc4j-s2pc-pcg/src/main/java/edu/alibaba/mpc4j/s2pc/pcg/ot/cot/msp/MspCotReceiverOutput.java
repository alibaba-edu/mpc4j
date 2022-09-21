package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * MSP-COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotReceiverOutput {
    /**
     * α数组
     */
    private int[] alphaArray;
    /**
     * Rb数组
     */
    private byte[][] rbArray;

    /**
     * 创建接收方输出。
     *
     * @param alphaArray α数组。
     * @param rbArray Rb数组。
     * @return 接收方输出。
     */
    public static MspCotReceiverOutput create(int[] alphaArray, byte[][] rbArray) {
        MspCotReceiverOutput receiverOutput = new MspCotReceiverOutput();
        assert alphaArray.length > 0;
        assert rbArray.length > 0;
        receiverOutput.alphaArray = Arrays.stream(alphaArray)
            // 0 <= α_i < n
            .peek(alpha -> {
                assert alpha >= 0 && alpha < rbArray.length;
            })
            // 去重并排序
            .distinct()
            .sorted()
            // IntStream的toArray()具有拷贝功能呢
            .toArray();
        // 去重后的数量应等于去重前的数量
        assert receiverOutput.alphaArray.length == alphaArray.length;
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
    private MspCotReceiverOutput() {
        // empty
    }

    /**
     * 返回α数组。
     *
     * @return α数组。
     */
    public int[] getAlphaArray() {
        return alphaArray;
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
