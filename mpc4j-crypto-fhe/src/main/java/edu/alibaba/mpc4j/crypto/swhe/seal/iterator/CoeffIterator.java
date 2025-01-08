package edu.alibaba.mpc4j.crypto.swhe.seal.iterator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * (Constant) CoeffIterator represents a degree-(N-1) polynomial. A degree-(N-1) polynomial has N coefficients.
 * Therefore, we use long[] with at least N elements to represent a degree-(N-1) polynomial with the following form:
 * <p>[ c1 mod q, c2 mod q, ..., cn mod q]
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L588">
 * PtrIter in iterator.h
 * </a>. Note that
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L312">
 * iterator.h
 * </a> defines <code>using CoeffIter = PtrIter<std::uint64_t *>;</code>.
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class CoeffIterator {
    /**
     * The coefficient for the degree-(N-1) polynomial is coeff[offset + 0 ... offset + N).
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
     * Creates a coefficient iterator with all zero value.
     *
     * @param n N, i.e., the modulus polynomial degree.
     * @return a coefficient iterator.
     */
    public static CoeffIterator createZeroCoeff(int n) {
        return new CoeffIterator(new long[n], 0, n);
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


    public long[] getCoeff() {
        return coeff;
    }

    public int getOffset() {
        return offset;
    }

    public int getN() {
        return n;
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
