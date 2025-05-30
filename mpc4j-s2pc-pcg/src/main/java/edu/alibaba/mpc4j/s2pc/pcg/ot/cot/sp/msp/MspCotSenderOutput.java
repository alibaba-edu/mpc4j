package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

/**
 * multi single-point COT sender output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotSenderOutput implements PcgPartyOutput {
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
    public static MspCotSenderOutput create(byte[] delta, byte[][] r0Array) {
        MspCotSenderOutput senderOutput = new MspCotSenderOutput();
        Preconditions.checkArgument(BlockUtils.valid(delta));
        senderOutput.delta = BlockUtils.clone(delta);
        MathPreconditions.checkPositive("r0Array.length", r0Array.length);
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> Preconditions.checkArgument(BlockUtils.valid(r0)))
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private MspCotSenderOutput() {
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
     * Gets R0 array.
     *
     * @return R0 array.
     */
    public byte[][] getR0Array() {
        return r0Array;
    }

    /**
     * Gets the assigned R1.
     *
     * @param index the assigned R1.
     * @return R1.
     */
    public byte[] getR1(int index) {
        return BlockUtils.xor(delta, getR0(index));
    }

    @Override
    public int getNum() {
        return r0Array.length;
    }

}
