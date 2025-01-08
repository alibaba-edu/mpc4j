package edu.alibaba.mpc4j.crypto.swhe.seal.iterator;

/**
 * Stride iterator allows to visit degree-(N-1) polynomials with the given stride. For example, if one wants to get the
 * i-th coefficient with the given starting_point and stride, it returns coeff[starting_point + i * stride].
 *
 * @author Anony_Trent
 * @date 2024/4/4
 */
public class StrideIterator {
    /**
     * The coefficient for the j-th degree-(N-1) polynomial is coeff[offset + j * N + 0 ... offset + j * N + N).
     */
    public final long[] coeff;
    /**
     * start point of the coefficients
     */
    public final int ptrIt;
    /**
     * the stride length
     */
    public final int stride;

    /**
     * Creates a Stride iterator.
     *
     * @param coeff  the coefficients.
     * @param ptrIt  the start point of the coefficients.
     * @param stride stride length
     */
    public StrideIterator(long[] coeff, int ptrIt, int stride) {
        this.coeff = coeff;
        this.ptrIt = ptrIt;
        this.stride = stride;
    }

    /**
     * get the i-th value of this StrideIterator.
     *
     * @param i the i-th position of current StrideIterator.
     * @return the i-th value of current StrideIterator.
     */
    public long getCoefficient(int i) {
        assert ptrIt + i * stride < coeff.length;
        return coeff[ptrIt + i * stride];
    }

    /**
     * get the i-th value of current StrideIterator.
     *
     * @param i     the i-th position of current StrideIterator.
     * @param value the given value.
     */
    public void setCoefficient(int i, long value) {
        assert ptrIt + i * stride < coeff.length;
        coeff[ptrIt + i * stride] = value;
    }
}
