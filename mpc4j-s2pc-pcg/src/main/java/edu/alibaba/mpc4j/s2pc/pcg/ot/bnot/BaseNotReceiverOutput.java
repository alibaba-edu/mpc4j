package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.Arrays;

/**
 * 基础n选1-OT协议接收方输出。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public class BaseNotReceiverOutput {
    /**
     * 最大选择值
     */
    private final int maxChoice;
    /**
     * 选择值数组
     */
    private final int[] choices;
    /**
     * Rb数组
     */
    private final byte[][] rbArray;

    public BaseNotReceiverOutput(int maxChoice, int[] choices, byte[][] rbArray) {
        assert maxChoice > 1 : "n must be greater than 1: " + maxChoice;
        this.maxChoice = maxChoice;
        assert choices.length > 0 : "num must be greater than 0: " + choices.length;
        assert choices.length == rbArray.length : "# of choices must match # of randomness";
        this.choices = Arrays.stream(choices)
            .peek(choice -> {
                assert choice >= 0 && choice < maxChoice : "choice must be in range [0, " + maxChoice + ")";
            })
            .toArray();
        this.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .toArray(byte[][]::new);
    }

    /**
     * 返回选择值。
     *
     * @param index 索引值。
     * @return 选择值。
     */
    public int getChoice(int index) {
        return choices[index];
    }

    /**
     * 返回选择值数组。
     *
     * @return 选择值数组。
     */
    public int[] getChoices() {
        return choices;
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
     * 返回最大选择值。
     *
     * @return 最大选择值。
     */
    public int getMaxChoice() {
        return maxChoice;
    }

    /**
     * 返回NOT数量。
     *
     * @return NOT数量。
     */
    public int getNum() {
        return rbArray.length;
    }
}
