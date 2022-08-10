package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * 基础n选1-OT协议接收方输出。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public class BnotReceiverOutput {
    /**
     * 最大选择值
     */
    private final int n;
    /**
     * 选择值数组
     */
    private final int[] choices;
    /**
     * Ri数组
     */
    private final byte[][] rbArray;

    public BnotReceiverOutput(int n, int[] choices, byte[][] rbArray) {
        assert n > 1 : "n must be greater than 1: " + n;
        this.n = n;
        assert choices.length > 0 : "# of choices must be greater than 0";
        assert choices.length == rbArray.length : "# of choices must match # of randomness";
        Arrays.stream(choices).forEach(choice -> {
            assert choice >= 0 && choice < n : "choice must be in range [0, " + n + ")";
        });
        this.choices = Arrays.copyOf(choices, choices.length);
        this.rbArray = Arrays.stream(rbArray)
                .map(BytesUtils::clone)
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
     * 返回Rb数组。
     *
     * @return Rb数组。
     */
    public byte[][] getRbArray() {
        return rbArray;
    }

    /**
     * 返回最大选择值。
     *
     * @return 最大选择值。
     */
    public int getN() {
        return n;
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
