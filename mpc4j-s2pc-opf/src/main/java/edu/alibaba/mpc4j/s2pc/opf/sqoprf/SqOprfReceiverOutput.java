package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * single-query OPRF receiver output.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfReceiverOutput {
    /**
     * the inputs.
     */
    private final byte[][] inputs;
    /**
     * the prfs.
     */
    private final byte[][] prfs;

    public SqOprfReceiverOutput(byte[][] inputs, byte[][] prfs) {
        MathPreconditions.checkPositive("inputs.length", inputs.length);
        MathPreconditions.checkEqual("inputs.length", "prfs.length", inputs.length, prfs.length);
        this.inputs = Arrays.stream(inputs)
            .peek(input -> {
                assert input != null;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        this.prfs = Arrays.stream(prfs)
            .peek(prf -> {
                assert prf.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    /**
     * Gets the input.
     *
     * @param index the index.
     * @return the input.
     */
    public byte[] getInput(int index) {
        return inputs[index];
    }

    /**
     * Gets the output.
     *
     * @param index the index.
     * @return the PRF output.
     */
    public byte[] getPrf(int index) {
        return prfs[index];
    }

    /**
     * Gets the batch size.
     *
     * @return the batch size.
     */
    public int getBatchSize() {
        return inputs.length;
    }

    /**
     * Gets PRF byte length.
     *
     * @return PRF byte length.
     */
    public int getPrfByteLength() {
        return CommonConstants.BLOCK_BYTE_LENGTH;
    }
}
