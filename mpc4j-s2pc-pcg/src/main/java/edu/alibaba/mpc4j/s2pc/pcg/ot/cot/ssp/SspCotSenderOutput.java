package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

/**
 * Single single-point COT sender output.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class SspCotSenderOutput implements PcgPartyOutput {
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * R0 array
     */
    private byte[][] r0Array;

    /**
     * Creates a sender output.
     *
     * @param delta   Δ.
     * @param r0Array R0 array.
     * @return a sender output.
     */
    public static SspCotSenderOutput create(byte[] delta, byte[][] r0Array) {
        SspCotSenderOutput senderOutput = new SspCotSenderOutput();
        MathPreconditions.checkEqual("delta.length", "λ in bytes", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        senderOutput.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("R0Array.length", r0Array.length);
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 ->
                MathPreconditions.checkEqual("R0.length", "λ in bytes", r0.length, CommonConstants.BLOCK_BYTE_LENGTH)
            )
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private SspCotSenderOutput() {
        // empty
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets the assigned R0.
     *
     * @param index index.
     * @return the assigned R0.
     */
    public byte[] getR0(int index) {
        return r0Array[index];
    }

    /**
     * Gets the assigned R1.
     *
     * @param index index.
     * @return the assigned R1.
     */
    public byte[] getR1(int index) {
        return BytesUtils.xor(delta, getR0(index));
    }

    @Override
    public int getNum() {
        return r0Array.length;
    }
}
