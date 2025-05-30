package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * Single single-point COT receiver output.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class SspCotReceiverOutput implements PcgPartyOutput {
    /**
     * α
     */
    private int alpha;
    /**
     * Rb array
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param alpha   α.
     * @param rbArray Rb array.
     * @return a receiver output.
     */
    public static SspCotReceiverOutput create(int alpha, byte[][] rbArray) {
        SspCotReceiverOutput receiverOutput = new SspCotReceiverOutput();
        MathPreconditions.checkPositive("RbArray.length", rbArray.length);
        MathPreconditions.checkNonNegativeInRange("α", alpha, rbArray.length);
        receiverOutput.alpha = alpha;
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> Preconditions.checkArgument(BlockUtils.valid(rb)))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private SspCotReceiverOutput() {
        // empty
    }

    /**
     * Gets α. Note that b[α] = 1 and b[i] = 0 for i ≠ α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Gets Rb.
     *
     * @param index index.
     * @return Rb.
     */
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }
}
