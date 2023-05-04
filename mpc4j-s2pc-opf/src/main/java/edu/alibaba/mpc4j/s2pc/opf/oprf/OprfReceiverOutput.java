package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * OPRF协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class OprfReceiverOutput {
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * 输入
     */
    private final byte[][] inputs;
    /**
     * 伪随机函数输出
     */
    private final byte[][] prfs;

    public OprfReceiverOutput(int prfByteLength, byte[][] inputs, byte[][] prfs) {
        assert prfByteLength >= CommonConstants.BLOCK_BYTE_LENGTH;
        this.prfByteLength = prfByteLength;
        assert inputs.length > 0;
        this.inputs = Arrays.stream(inputs)
            .peek(input -> {
                assert input != null;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        assert prfs.length == inputs.length;
        this.prfs = Arrays.stream(prfs)
            .peek(prf -> {
                assert prf.length == prfByteLength;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    /**
     * 返回伪随机函数输入。
     *
     * @param index 索引值。
     * @return 伪随机函数输入。
     */
    public byte[] getInput(int index) {
        return inputs[index];
    }

    /**
     * 返回伪随机函数输出。
     *
     * @param index 索引值。
     * @return 伪随机函数输出。
     */
    public byte[] getPrf(int index) {
        return prfs[index];
    }

    /**
     * 返回伪随机函数输出比特长度。
     *
     * @return 伪随机函数输出比特长度。
     */
    public int getPrfByteLength() {
        return prfByteLength;
    }

    /**
     * 返回索引值总数量。
     *
     * @return 索引值总数量。
     */
    public int getBatchSize() {
        return inputs.length;
    }
}
