package edu.alibaba.mpc4j.crypto.algs.range;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * ValueRange that contains a long range from start (inclusive) to end (inclusive).
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class LongValueRange {
    /**
     * start
     */
    private long start;
    /**
     * end
     */
    private long end;

    /**
     * Creates a ValueRange from start (inclusive) to end (inclusive).
     *
     * @param start start value.
     * @param end end value.
     */
    public LongValueRange(long start, long end) {
        // start <= end
        MathPreconditions.checkLessOrEqual("start", start, end);
        this.start = start;
        this.end = end;
    }

    /**
     * Copies from a ValueRange.
     *
     * @param other the other ValueRange.
     */
    public LongValueRange(LongValueRange other) {
        this(other.start, other.end);
    }

    /**
     * Sets the start value.
     *
     * @param start the start value.
     */
    public void setStart(long start) {
        this.start = start;
        validate();
    }

    /**
     * Gets the start value.
     *
     * @return the start value.
     */
    public long getStart() {
        return start;
    }

    /**
     * Sets the end value.
     *
     * @param end the end value.
     */
    public void setEnd(long end) {
        this.end = end;
        validate();
    }

    /**
     * Gets the end value.
     *
     * @return the end value.
     */
    public long getEnd() {
        return end;
    }

    private void validate() {
        MathPreconditions.checkLessOrEqual("start", start, end);
    }

    /**
     * Gets the range length, that is, the number of elements in [start, end]/
     *
     * @return the range length.
     */
    public long size() {
        return end - start + 1;
    }

    /**
     * Returns if the number is in the ValueRange.
     *
     * @param number the number.
     * @return true if the number is in [start, end]; false otherwise.
     */
    public boolean contains(long number) {
        return number >= start && number <= end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
