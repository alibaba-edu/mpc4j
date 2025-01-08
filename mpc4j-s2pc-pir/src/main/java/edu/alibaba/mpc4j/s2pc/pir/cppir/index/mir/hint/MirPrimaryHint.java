package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * primary hint for MIR.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public interface MirPrimaryHint extends MirHint {
    /**
     * Gets if the given x is in the indexes.
     *
     * @param x the given x.
     * @return true if the given x is in the indexes.
     */
    default boolean contains(int x) {
        int chunkNum = getChunkNum();
        int chunkSize = getChunkSize();
        MathPreconditions.checkNonNegativeInRange("x", x, chunkNum * chunkSize);
        // compute chunk ID and offset
        int chunkId = x / chunkSize;
        // the first case happens with more than 50%, so we first decide the first case.
        if (containsChunkId(chunkId)) {
            int offset = Math.abs(x % chunkSize);
            int expandOffset = expandOffset(chunkId);
            return offset == expandOffset;
        } else {
            return false;
        }
    }

    /**
     * Gets the parity.
     *
     * @return the parity.
     */
    byte[] getParity();

    /**
     * Inplace XOR the current parity with the other parity.
     *
     * @param otherParity other parity.
     */
    void xori(byte[] otherParity);

    /**
     * Gets the programmed index if the hint needs to be further amended; otherwise (the hint is a direct primary hint
     * or the hint has been amended), return -1.
     *
     * @return the programmed index if the hint needs to be further amended, or -1 if it does not need to be amended.
     */
    int getAmendIndex();

    /**
     * Amends the parity.
     *
     * @param parity parity.
     */
    void amendParity(byte[] parity);
}
