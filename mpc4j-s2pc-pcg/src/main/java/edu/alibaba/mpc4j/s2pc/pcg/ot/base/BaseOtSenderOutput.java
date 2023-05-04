package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtSenderOutput;

/**
 * 基础OT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/10
 */
public class BaseOtSenderOutput implements OtSenderOutput {
    /**
     * R0数组
     */
    private final byte[][] r0Array;
    /**
     * R1数组
     */
    private final byte[][] r1Array;

    /**
     * 构建2选1-ROT发送方输出。
     *
     * @param r0Array R0数组。
     * @param r1Array R1数组。
     */
    public BaseOtSenderOutput(byte[][] r0Array, byte[][] r1Array) {
        assert r0Array.length > 0 : "num must be greater than 0: " + r0Array.length;
        int num = r0Array.length;
        assert r1Array.length == num : "# of R1 must be equal to " + num + ": " + r1Array.length;
        this.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> {
                assert r0.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "r0 byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + r0.length;
            })
            .toArray(byte[][]::new);
        this.r1Array = Arrays.stream(r1Array)
            .peek(r1 -> {
                assert r1.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "r1 byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + r1.length;
            })
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getR0(int index) {
        return r0Array[index];
    }

    @Override
    public byte[][] getR0Array() {
        return r0Array;
    }

    @Override
    public byte[] getR1(int index) {
        return r1Array[index];
    }

    @Override
    public byte[][] getR1Array() {
        return r1Array;
    }

    @Override
    public int getNum() {
        return r0Array.length;
    }
}
