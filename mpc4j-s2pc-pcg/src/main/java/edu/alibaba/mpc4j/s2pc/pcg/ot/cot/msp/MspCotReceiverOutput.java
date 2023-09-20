package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

/**
 * multi single-point COT receiver output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotReceiverOutput implements PcgPartyOutput {
    /**
     * α array
     */
    private int[] alphaArray;
    /**
     * Rb array
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param alphaArray α array.
     * @param rbArray    Rb array.
     * @return a receiver output.
     */
    public static MspCotReceiverOutput create(int[] alphaArray, byte[][] rbArray) {
        MspCotReceiverOutput receiverOutput = new MspCotReceiverOutput();
        MathPreconditions.checkPositive("rbArray.length", rbArray.length);
        int num = rbArray.length;
        MathPreconditions.checkPositiveInRangeClosed("alphaArray.length", alphaArray.length, num);
        receiverOutput.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, num))
            .distinct()
            .sorted()
            .toArray();
        MathPreconditions.checkEqual(
            "(distinct) alphaArray.length", "alphaArray.length",
            receiverOutput.alphaArray.length, alphaArray.length
        );
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> MathPreconditions.checkEqual(
                "rb.length", "λ in bytes", rb.length, CommonConstants.BLOCK_BYTE_LENGTH
            ))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private MspCotReceiverOutput() {
        // empty
    }

    /**
     * Gets α array.
     *
     * @return α array.
     */
    public int[] getAlphaArray() {
        return alphaArray;
    }

    /**
     * Gets the assigned Rb.
     *
     * @param index index.
     * @return the assigned Rb.
     */
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    /**
     * Gets Rb array.
     *
     * @return Rb array.
     */
    public byte[][] getRbArray() {
        return rbArray;
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }
}
