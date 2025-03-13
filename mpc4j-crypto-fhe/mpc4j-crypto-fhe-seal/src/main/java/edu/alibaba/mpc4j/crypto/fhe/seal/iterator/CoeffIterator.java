package edu.alibaba.mpc4j.crypto.fhe.seal.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * (Constant) CoeffIterator represents a degree-(N-1) polynomial. A degree-(N-1) polynomial has N coefficients.
 * Therefore, we use long[] with at least N elements to represent a degree-(N-1) polynomial with the following form:
 * <p>
 * [ c1 mod q, c2 mod q, ..., cn mod q]
 * <p>
 * The implementation is from <code>PtrIter</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L588">iterator.h</a>.
 * Note that
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L312">iterator.h</a>
 * defines <code>using CoeffIter = PtrIter<std::uint64_t *>;</code>.
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class CoeffIterator implements SealIterator {
    /**
     * Allocates coefficients with all zero values and wraps by a coefficient iterator.
     *
     * @param n N, i.e., the modulus polynomial degree.
     * @return a coefficient iterator.
     */
    public static CoeffIterator allocate(int n) {
        return new CoeffIterator(new long[n], 0, n);
    }

    /**
     * Warps coefficients by a coefficient iterator.
     *
     * @param coeff coefficients.
     * @return coefficient iterator wrapping coefficients.
     */
    public static CoeffIterator wrap(long[] coeff) {
        return wrap(coeff, 0, coeff.length);
    }

    /**
     * Warps coefficients by a coefficient iterator.
     *
     * @param coeff coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @return coefficient iterator wrapping coefficients.
     */
    public static CoeffIterator wrap(long[] coeff, int n) {
        return wrap(coeff, 0, n);
    }

    /**
     * Warps coefficients by a coefficient iterator.
     *
     * @param coeff coefficients.
     * @param pos   the start position of the coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @return coefficient iterator wrapping coefficients.
     */
    public static CoeffIterator wrap(long[] coeff, int pos, int n) {
        return new CoeffIterator(coeff, pos, n);
    }

    /**
     * The coefficient for the degree-(N-1) polynomial is coeff[offset + 0 ... offset + N).
     */
    private final long[] coeff;
    /**
     * the starting position of the coefficients.
     */
    private final int pos;
    /**
     * N, i.e., modulus polynomial degree.
     */
    private final int n;
    /**
     * offset
     */
    private int offset;

    private CoeffIterator(long[] coeff, int pos, int n) {
        assert coeff.length >= pos + n;
        this.coeff = coeff;
        this.pos = pos;
        this.n = n;
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
    public int stepSize() {
        return 1;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Returns the number of coefficients.
     *
     * @return the number of coefficients.
     */
    public int n() {
        return n;
    }

    /**
     * Sets the coefficient.
     *
     * @param i     coefficient index.
     * @param value value.
     */
    public void setCoeff(int i, long value) {
        assert i >= 0 && i < n;
        coeff[pos + offset + i] = value;
    }

    /**
     * Gets the coefficient.
     *
     * @param i coefficient index.
     * @return value.
     */
    public long getCoeff(int i) {
        assert i >= 0 && i < n;
        return coeff[pos + offset + i];
    }

    /**
     * Sets the coefficient.
     *
     * @param value value.
     */
    public void setCoeff(long value) {
        coeff[pos + offset] = value;
    }

    /**
     * Gets the coefficient.
     *
     * @return value.
     */
    public long getCoeff() {
        return coeff[pos + offset];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoeffIterator that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.n, that.n);
        // add coefficients
        for (int i = 0; i < n * stepSize(); i++) {
            equalsBuilder.append(this.coeff[this.pos + i], that.coeff[that.pos + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(n);
        // add coefficients
        for (int i = 0; i < n * stepSize(); i++) {
            hashCodeBuilder.append(coeff[pos + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
