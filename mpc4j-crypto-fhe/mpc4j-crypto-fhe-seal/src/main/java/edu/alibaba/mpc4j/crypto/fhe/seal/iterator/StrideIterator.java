package edu.alibaba.mpc4j.crypto.fhe.seal.iterator;

/**
 * Stride iterator allows to visit polynomials with the given stride. For example, if one wants to get the i-th
 * coefficient with the given offset and stride, it returns coeff[offset + 0, ..., offset + stride).
 *
 * @author Anony_Trent
 * @date 2024/4/4
 */
public class StrideIterator implements SealIterator {
    /**
     * coefficient. Coefficients of the polynomial under the given stride are coeff[offset + 0, ..., offset + stride).
     */
    private final long[] coeff;
    /**
     * the starting position of the coefficients.
     */
    private final int pos;
    /**
     * stride length
     */
    private final int stride;
    /**
     * offset
     */
    private int offset;

    /**
     * Creates a Stride iterator.
     *
     * @param coeff  the coefficients.
     * @param stride stride length
     */
    public static StrideIterator wrap(long[] coeff, int stride) {
        return wrap(coeff, 0, stride);
    }

    /**
     * Creates a Stride iterator.
     *
     * @param coeff  the coefficients.
     * @param pos    the start point of the coefficients.
     * @param stride stride length
     */
    public static StrideIterator wrap(long[] coeff, int pos, int stride) {
        // we do not need to check pos + stride < coeff.length, since the stride iterator would not be used in this case.
        return new StrideIterator(coeff, pos, stride);
    }

    /**
     * private constructor.
     */
    private StrideIterator(long[] coeff, int pos, int stride) {
        this.coeff = coeff;
        this.pos = pos;
        this.stride = stride;
        offset = 0;
    }

    @Override
    public long[] coeff() {
        return coeff;
    }

    @Override
    public int pos() {
        return pos;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int stepSize() {
        return stride;
    }

    /**
     * Gets the value of this StrideIterator.
     *
     * @return the value of current StrideIterator.
     */
    public long getCoeff() {
        return coeff[pos + offset];
    }

    /**
     * Sets the value of current StrideIterator.
     *
     * @param value the given value.
     */
    public void setCoeff(long value) {
        coeff[pos + offset] = value;
    }
}
