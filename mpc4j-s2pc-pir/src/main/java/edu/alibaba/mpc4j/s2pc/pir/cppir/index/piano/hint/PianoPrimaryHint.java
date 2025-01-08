package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * primary hint for PIANO.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface PianoPrimaryHint extends PianoHint {
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
        int offset = Math.abs(x % chunkSize);
        int expandOffset = expandOffset(chunkId);

        return offset == expandOffset;
    }

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
