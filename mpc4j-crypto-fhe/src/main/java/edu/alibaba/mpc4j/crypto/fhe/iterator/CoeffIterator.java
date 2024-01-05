package edu.alibaba.mpc4j.crypto.fhe.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * CoeffIterator represents a degree-(N-1) polynomial. A degree-(N-1) polynomial has N coefficients. Therefore, we use
 * long[] with at least N elements to represent a degree-(N-1) polynomial with the following form:
 * <p>[ c1 mod q, c2 mod q, ..., cn mod q]</p>
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L588
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class CoeffIterator {
    /**
     * The coefficient for the degree-(N-1) polynomial is coeff[offset + 0 .. offset + N).
     */
    private final long[] coeff;
    /**
     * offset
     */
    private final int offset;
    /**
     * N, i.e., modulus polynomial degree.
     */
    private final int n;

    /**
     * Creates a coefficient iterator.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param n      N, i.e., the modulus polynomial degree.
     */
    public CoeffIterator(long[] coeff, int offset, int n) {
        assert coeff.length >= offset + n;
        this.coeff = coeff;
        this.offset = offset;
        this.n = n;
    }

    /**
     * Sets the coefficient.
     *
     * @param i     coefficient index.
     * @param value value.
     */
    public void setCoefficient(int i, long value) {
        assert i >= 0 && i < n;
        coeff[offset + i] = value;
    }

    /**
     * Gets the coefficient.
     *
     * @param i coefficient index.
     * @return value.
     */
    public long getCoefficient(int i) {
        assert i >= 0 && i < n;
        return coeff[offset + i];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoeffIterator)) {
            return false;
        }
        CoeffIterator that = (CoeffIterator) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.n, that.n);
        // add coefficients
        for (int i = 0; i < n; i++) {
            equalsBuilder.append(this.coeff[this.offset + i], that.coeff[that.offset + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(n);
        // add coefficients
        for (int i = 0; i < n; i++) {
            hashCodeBuilder.append(coeff[offset + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
