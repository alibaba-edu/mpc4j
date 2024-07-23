package edu.alibaba.mpc4j.crypto.algs.utils.range;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.math.BigInteger;

/**
 * Range that contains a BigInteger range from start (inclusive) to end (inclusive).
 * <p></p>
 * The implementation is modified from https://github.com/ssavvides/jope/blob/master/src/jope/ValueRange.java.
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class BigIntegerRange {
    /**
     * start
     */
    private BigInteger start;
    /**
     * end
     */
    private BigInteger end;

    /**
     * Creates a Range from start (inclusive) to end (inclusive).
     *
     * @param start start value.
     * @param end   end value.
     */
    public BigIntegerRange(long start, long end) {
        this(BigInteger.valueOf(start), BigInteger.valueOf(end));
    }

    /**
     * Creates a Range from start (inclusive) to end (inclusive).
     *
     * @param start start value.
     * @param end   end value.
     */
    public BigIntegerRange(BigInteger start, BigInteger end) {
        this.start = start;
        this.end = end;
        validate();
    }

    /**
     * Copies from the other Range.
     *
     * @param other the other Range.
     */
    public BigIntegerRange(BigIntegerRange other) {
        this(other.start, other.end);
    }

    /**
     * Sets the start value.
     *
     * @param start the start value.
     */
    public void setStart(long start) {
        setStart(BigInteger.valueOf(start));
    }

    /**
     * Sets the start value.
     *
     * @param start the start value.
     */
    public void setStart(BigInteger start) {
        this.start = start;
        validate();
    }

    /**
     * Gets the start value.
     *
     * @return the start value.
     */
    public BigInteger getStart() {
        return start;
    }

    /**
     * Sets the end value.
     *
     * @param end the end value.
     */
    public void setEnd(long end) {
        setEnd(BigInteger.valueOf(end));
    }

    /**
     * Sets the end value.
     *
     * @param end the end value.
     */
    public void setEnd(BigInteger end) {
        this.end = end;
        validate();
    }

    /**
     * Gets the end value.
     *
     * @return the end value.
     */
    public BigInteger getEnd() {
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
    public BigInteger size() {
        return end.subtract(start).add(BigInteger.ONE);
    }

    /**
     * Returns if the number is in the ValueRange.
     *
     * @param number the number.
     * @return true if the number is in [start, end]; false otherwise.
     */
    public boolean contains(long number) {
        return contains(BigInteger.valueOf(number));
    }

    /**
     * Returns if the number is in the ValueRange.
     *
     * @param number the number.
     * @return true if the number is in [start, end]; false otherwise.
     */
    public boolean contains(BigInteger number) {
        return number.compareTo(start) >= 0 && number.compareTo(end) <= 0;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
