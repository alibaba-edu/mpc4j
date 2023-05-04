package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtReceiverOutput;

/**
 * 基础OT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/10
 */
public class BaseOtReceiverOutput implements OtReceiverOutput {
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
        int num = choices.length;
        assert rbArray.length == num : "# of Rb must be equal to " + num + ": " + rbArray.length;
        this.choices = choices;
        this.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "rb byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + rb.length;
            })
            .toArray(byte[][]::new);
    }

    @Override
    public boolean getChoice(int index) {
        return choices[index];
    }

    @Override
    public boolean[] getChoices() {
        return choices;
    }

    @Override
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    @Override
    public byte[][] getRbArray() {
        return rbArray;
    }

    @Override
    public int getNum() {
        return choices.length;
    }
}
