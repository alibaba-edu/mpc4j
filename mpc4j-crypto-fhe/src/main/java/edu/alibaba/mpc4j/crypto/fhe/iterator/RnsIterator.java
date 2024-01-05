package edu.alibaba.mpc4j.crypto.fhe.iterator;

import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.LongBuffer;

/**
 * RnsIterator represents k degree-(N-1) polynomials in RNS representation. A degree-(N-1) polynomial has N coefficients.
 * Suppose RNS base is q = [q1, q2, ..., qk]. Each coefficient can be spilt into k parts. Therefore, we use 1D array
 * the length at least k * N to represent k degree-(N-1) polynomials in RNS representation with the following form:
 * <p>[ c_11 mod q1, c_12 mod q1, ..., c_1n mod q1]</p>
 * <p>...</p>
 * <p>[ c_k1 mod qk, c_k2 mod qk, ..., c_kn mod qk]</p>
 * But most of the time, we use this matrix via column, i.e., operate on [c_1i mod q1, c_2i mod q2, ..., cki mod qk]^T.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L951
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class RnsIterator {
    /**
     * Converts an 2D-array RNS representation into an 1D-array RNS representation.
     *
     * @param data an 2D-array RNS representation.
     * @return an 1D-array RNS representation.
     */
    public static long[] createRnsFrom2dArray(long[][] data) {
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
     * Creates an 1D-array RNS representations with all coefficients initialized as 0.
     *
     * @param n     modulus polynomial degree.
     * @param k     number of RNS bases.
     * @return an 1D-array RNS representations with all coefficients initialized as 0.
     */
    public static long[] allocateZeroRns(int n, int k) {
        assert n > 0;
        assert k > 0;
        return PolyCore.allocateZeroPolyArray(k, n, 1);
    }

    /**
     * Converts an 1D-array RNS representation into an 2D-array RNS representation.
     *
     * @param coeff an 1D-arry RNS representation.
     * @param n     modulus polynomial degree.
     * @param k     number of RNS bases.
     * @return an 2D-array RNS representation.
     */
    public static long[][] rnsTo2dArray(long[] coeff, int n, int k) {
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
     * The coefficient for the j-th degree-(N-1) polynomial is coeff[offset + j * N + 0 .. offset + j * N + N).
     */
    public final long[] coeff;
    /**
     * offset
     */
    public final int offset;
    /**
     * k, i.e., the number of RNS bases.
     */
    public final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    public final int n;
    /**
     * coefficient iterators
     */
    public final CoeffIterator[] coeffIterators;

    /**
     * Creates an RNS iterator.
     *
     * @param coeff  the coefficient.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public RnsIterator(long[] coeff, int n, int k) {
        this(coeff, 0, n, k);
    }

    /**
     * Creates an RNS iterator.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public RnsIterator(long[] coeff, int offset, int n, int k) {
        assert coeff.length >= offset + n * k;
        this.coeff = coeff;
        this.offset = offset;
        this.n = n;
        this.k = k;
        coeffIterators = new CoeffIterator[k];
        for (int j = 0; j < k; j++) {
            int jOffset = offset + j * n;
            coeffIterators[j] = new CoeffIterator(coeff, jOffset, n);
        }
    }

    /**
     * Creates a sub-RNS iterator.
     *
     * @param fromIndex the start index.
     * @param toIndex the end index.
     * @return a sub-RNS iterator.
     */
    public RnsIterator subRnsIterator(int fromIndex, int toIndex) {
        assert fromIndex >= 0 && fromIndex < toIndex;
        assert toIndex < k;
        return new RnsIterator(coeff, offset + fromIndex * n, n, toIndex - fromIndex);
    }

    /**
     * Converts the RNS iterator to 2D array.
     *
     * @return the 2D array with size k * n.
     */
    public long[][] to2dArray() {
        long[][] data = new long[k][n];
        for (int j = 0; j < k; j++) {
            System.arraycopy(coeff, offset + j * n, data[j], 0, n);
        }
        return data;
    }

    /**
     * Converts the RNS iterator to a transpose 2D array.
     *
     * @return the 2D array with size n * k.
     */
    public long[][] toTranspose2dArray() {
        long[][] data = new long[n][k];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < k; i++) {
                CoeffIterator coeffIterator = coeffIterators[i];
                data[j][i] = coeffIterators[i].getCoefficient(j);
            }
        }
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RnsIterator)) {
            return false;
        }
        RnsIterator that = (RnsIterator) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < n * k; i++) {
            equalsBuilder.append(this.coeff[this.offset + i], that.coeff[that.offset + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(n);
        hashCodeBuilder.append(k);
        // add coefficients
        for (int i = 0; i < n * k; i++) {
            hashCodeBuilder.append(coeff[offset + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
