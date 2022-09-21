package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * 基础OT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/10
 */
public class BaseOtReceiverOutput {
    /**
     * 数量
     */
    private final int num;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * Rb数组
     */
    private final byte[][] rbArray;

    public BaseOtReceiverOutput(boolean[] choices, byte[][] rbArray) {
        assert choices.length > 0 : "num must be greater than 0: " + choices.length;
        num = choices.length;
        assert rbArray.length == num : "# of Rb must be equal to " + num + ": " + rbArray.length;
        this.choices = BinaryUtils.clone(choices);
        this.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    /**
     * 返回选择比特。
     *
     * @param index 索引值。
     * @return 选择比特。
     */
    public boolean getChoice(int index) {
        return choices[index];
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
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }
}
