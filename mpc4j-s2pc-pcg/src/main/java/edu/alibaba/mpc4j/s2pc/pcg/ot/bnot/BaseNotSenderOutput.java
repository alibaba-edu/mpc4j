package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.Arrays;

/**
 * 基础n选1-OT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/9/17
 */
public class BaseNotSenderOutput {
    /**
     * 最大选择值
     */
    private final int maxChoice;
    /**
     * 数量
     */
    private final int num;
    /**
     * Rn数组
     */
    private final byte[][][] rMatrix;

    public BaseNotSenderOutput(int maxChoice, byte[][][] rMatrix) {
        assert maxChoice > 1 : "n must be greater than 1: " + maxChoice;
        this.maxChoice = maxChoice;
        assert rMatrix.length > 0 : "num must be greater than 0: " + rMatrix.length;
        this.num = rMatrix.length;
        this.rMatrix = Arrays.stream(rMatrix)
            .peek(rnArray -> {
                assert rnArray.length == maxChoice : "# of Rs must be equal to " + maxChoice + ": " + rnArray.length;
                for (int i = 0; i < maxChoice; i++) {
                    assert rnArray[i].length == CommonConstants.BLOCK_BYTE_LENGTH;
                }
            })
            .toArray(byte[][][]::new);
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
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回Ri。
     *
     * @param index  索引值。
     * @param choice 选择值。
     * @return Rb。
     */
    public byte[] getRi(int index, int choice) {
        return rMatrix[index][choice];
    }
}
