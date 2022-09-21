package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * 基础OT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/10
 */
public class BaseOtSenderOutput {
    /**
     * R0数组
     */
    private final byte[][] r0Array;
    /**
     * R1数组
     */
    private final byte[][] r1Array;
    /**
     * 数量
     */
    private final int num;

    /**
     * 构建2选1-ROT发送方输出。
     *
     * @param r0Array R0数组。
     * @param r1Array R1数组。
     */
    public BaseOtSenderOutput(byte[][] r0Array, byte[][] r1Array) {
        assert r0Array.length > 0 : "num must be greater than 0: " + r0Array.length;
        num = r0Array.length;
        assert r1Array.length == num: "# of R1 must be equal to " + num + ": " + r1Array.length;
        this.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> {
                assert r0.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        this.r1Array = Arrays.stream(r1Array)
            .peek(r1 -> {
                assert r1.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    /**
     * 返回R0。
     *
     * @param index 索引值。
     * @return R0。
     */
    public byte[] getR0(int index) {
        assert index >= 0 && index < getNum();
        return r0Array[index];
    }

    /**
     * 返回R1。
     *
     * @param index 索引值。
     * @return R1。
     */
    public byte[] getR1(int index) {
        assert index >= 0 && index < getNum();
        return r1Array[index];
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
