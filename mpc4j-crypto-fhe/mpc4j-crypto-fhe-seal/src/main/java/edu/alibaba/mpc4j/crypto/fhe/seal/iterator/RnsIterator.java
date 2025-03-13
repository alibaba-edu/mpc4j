package edu.alibaba.mpc4j.crypto.fhe.seal.iterator;

import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyCore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.LongBuffer;

/**
 * RnsIterator represents k degree-(N-1) polynomials in RNS representation. A degree-(N-1) polynomial has N coefficients.
 * Suppose RNS base is q = [q1, q2, ..., qk]. Each coefficient can be spilt into k parts. Therefore, we use 1D array
 * the length at least k * N to represent k degree-(N-1) polynomials in RNS representation with the following form:
 * <p>[ c_11 mod q1, c_12 mod q1, ..., c_1n mod q1]
 * <p>...
 * <p>[ c_k1 mod qk, c_k2 mod qk, ..., c_kn mod qk]
 * <p>But most of the time, we use this matrix via column, i.e., operate on [c_1i mod q1, c_2i mod q2, ..., cki mod qk]^T.
 * <p>
 * The implementation is from <code>RNSIter</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L951">iterator.h</a>.
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class RnsIterator implements SealIterator {
    /**
     * Allocates an 1D-array RNS representations with all coefficients initialized as 0.
     *
     * @param n modulus polynomial degree.
     * @param k number of RNS bases.
     * @return an 1D-array RNS representations with all coefficients initialized as 0.
     */
    public static long[] allocateArray(int n, int k) {
        assert n > 0;
        assert k > 0;
        return PolyCore.allocateZeroPolyArray(k, n, 1);
    }

    /**
     * Creates an 1D-array RNS representation from an 2D-array.
     *
     * @param data an 2D-array RNS representation.
     * @return an 1D-array RNS representation.
     */
    public static long[] from1dArray(long[][] data) {
        int k = data.length;
        assert k > 0;
        int n = data[0].length;
        assert n > 0;

        LongBuffer longBuffer = LongBuffer.allocate(n * k);
        for (long[] coeff : data) {
            assert coeff.length == n;
            longBuffer.put(coeff);
        }
        return longBuffer.array();
    }

    /**
     * Converts an 1D-array RNS representation into an 2D-array RNS representation.
     *
     * @param coeff an 1D-arry RNS representation.
     * @param n     modulus polynomial degree.
     * @param k     number of RNS bases.
     * @return an 2D-array RNS representation.
     */
    public static long[][] to2dArray(long[] coeff, int n, int k) {
        assert n > 0;
        assert k > 0;
        assert n * k == coeff.length;

        LongBuffer longBuffer = LongBuffer.wrap(coeff);
        long[][] data = new long[k][n];
        for (int j = 0; j < k; j++) {
            longBuffer.get(data[j]);
        }
        return data;
    }

    /**
     * Allocates an 1D-array RNS representations with all coefficients initialized as 0.
     *
     * @param n modulus polynomial degree.
     * @param k number of RNS bases.
     * @return an 1D-array RNS representations with all coefficients initialized as 0.
     */
    public static RnsIterator allocate(int n, int k) {
        assert n > 0;
        assert k > 0;

        return new RnsIterator(new long[n * k], 0, n, k);
    }

    /**
     * Warps coefficients by an RNS iterator.
     *
     * @param coeff coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     K, i.e., the number of RNS bases.
     * @return RNS iterator wrapping coefficients.
     */
    public static RnsIterator wrap(long[] coeff, int n, int k) {
        return wrap(coeff, 0, n, k);
    }

    /**
     * Warps coefficients by a coefficient iterator.
     *
     * @param coeff coefficients.
     * @param pos   the start position of the coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     K, i.e., the number of RNS bases.
     * @return coefficient iterator wrapping coefficients.
     */
    public static RnsIterator wrap(long[] coeff, int pos, int n, int k) {
        return new RnsIterator(coeff, pos, n, k);
    }

    /**
     * The coefficient for the j-th degree-(N-1) polynomial is coeff[offset + j * N + 0 .. offset + j * N + N).
     */
    private final long[] coeff;
    /**
     * the starting position of the coefficients.
     */
    private final int pos;
    /**
     * k, i.e., the number of RNS bases.
     */
    private final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    private final int n;
    /**
     * coefficient iterators
     */
    public final CoeffIterator[] coeffIter;
    /**
     * offset
     */
    private int offset;

    /**
     * Creates an RNS iterator.
     *
     * @param coeff the coefficient.
     * @param pos   the starting position of the coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    private RnsIterator(long[] coeff, int pos, int n, int k) {
        assert coeff.length >= pos + n * k;
        this.coeff = coeff;
        this.pos = pos;
        this.n = n;
        this.k = k;
        offset = 0;
        coeffIter = new CoeffIterator[k];
        for (int j = 0; j < k; j++) {
            int jPos = pos + j * stepSize();
            coeffIter[j] = CoeffIterator.wrap(coeff, jPos, n);
        }
    }

    /**
     * Creates a sub-RNS iterator.
     *
     * @param fromIndex the start index, included
     * @param toIndex   the end index, included, the max value is: k - 1
     * @return a sub-RNS iterator.
     */
    public RnsIterator subRnsIterator(int fromIndex, int toIndex) {
        assert fromIndex >= 0 && fromIndex < toIndex;
        assert toIndex < k;
        return RnsIterator.wrap(coeff, pos + fromIndex * stepSize(), n, toIndex - fromIndex + 1);
    }

    /**
     * Creates a sub-RNS iterator in [fromIndex, k)
     *
     * @param fromIndex the start index, included
     * @return a sub-RNS iterator.
     */
    public RnsIterator subRnsIterator(int fromIndex) {
        assert fromIndex >= 0 && fromIndex < k;
        return RnsIterator.wrap(coeff, pos + fromIndex * stepSize(), n, k - fromIndex);
    }

    /**
     * Converts the RNS iterator to 2D array.
     *
     * @return the 2D array with size k * n.
     */
    public long[][] to2dArray() {
        long[][] data = new long[k][n];
        for (int j = 0; j < k; j++) {
            System.arraycopy(coeff, pos + j * stepSize(), data[j], 0, n);
        }
        return data;
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
        return n;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
        for (int j = 0; j < k; j++) {
            int jPos = pos + offset + j * stepSize();
            coeffIter[j] = CoeffIterator.wrap(coeff, jPos, n);
        }
    }

    /**
     * Gets k, i.e., number of RNS bases.
     *
     * @return k, i.e., number of RNS bases.
     */
    public int k() {
        return k;
    }

    /**
     * Gets N, i.e., modulus polynomial degree.
     *
     * @return N, i.e., modulus polynomial degree.
     */
    public int n() {
        return n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RnsIterator that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < k * stepSize(); i++) {
            equalsBuilder.append(this.coeff[this.pos + i], that.coeff[that.pos + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(n);
        hashCodeBuilder.append(k);
        // add coefficients
        for (int i = 0; i < k * stepSize(); i++) {
            hashCodeBuilder.append(coeff[pos + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
