package edu.alibaba.mpc4j.crypto.swhe.seal.iterator;

import edu.alibaba.mpc4j.crypto.swhe.seal.Ciphertext;
import edu.alibaba.mpc4j.crypto.swhe.seal.rq.PolyCore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.LongBuffer;

/**
 * PolyIterator represents multiple (m >= 1) degree-(N-1) RNS representations.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L1304">
 * PolyIter in iterator.h
 * </a>.
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class PolyIterator {
    /**
     * Converts an 3D-array Poly-RNS representation into an 1D-array Poly-RNS representation.
     *
     * @param data an 3D-array Poly-RNS representation.
     * @return an an 1D-array Poly-RNS representation.
     */
    public static long[] createPolyFrom3dArray(long[][][] data) {
        int m = data.length;
        assert m > 0;
        int k = data[0].length;
        assert k > 0;
        int n = data[0][0].length;
        assert n > 0;

        LongBuffer longBuffer = LongBuffer.allocate(m * n * k);
        for (long[][] rns : data) {
            assert rns.length == k;
            for (long[] coeff : rns) {
                assert coeff.length == n;
                longBuffer.put(coeff);
            }
        }
        return longBuffer.array();
    }

    /**
     * Creates an 1D-array Poly-RNS representations with all coefficients initialized as 0.
     *
     * @param m number of RNS representations.
     * @param n modulus polynomial degree.
     * @param k number of RNS bases.
     * @return an 1D-array Poly-RNS representations with all coefficients initialized as 0.
     */
    public static long[] allocateZeroPoly(int m, int n, int k) {
        assert m > 0;
        assert n > 0;
        assert k > 0;
        return PolyCore.allocateZeroPolyArray(m * k, n, 1);
    }

    /**
     * Converts an 1D-array Poly-RNS representation into an 3D-array Poly-RNS representation.
     *
     * @param coeff an 1D-array Poly-RNS representation.
     * @param m     number of RNS representations.
     * @param n     modulus polynomial degree.
     * @param k     number of RNS bases.
     * @return an 3D-array Poly-RNS representation.
     */
    public static long[][][] polyTo3dArray(long[] coeff, int m, int n, int k) {
        assert m > 0;
        assert n > 0;
        assert k > 0;
        assert m * n * k == coeff.length;

        LongBuffer longBuffer = LongBuffer.wrap(coeff);
        long[][][] data = new long[m][k][n];
        for (int r = 0; r < m; r++) {
            for (int j = 0; j < k; j++) {
                longBuffer.get(data[r][j]);
            }
        }
        return data;
    }

    /**
     * The coefficient for the r-th RNS representation is coeff[offset + r * N * k + 0 .. offset + r * N * k + N * k).
     */
    public final long[] coeff;
    /**
     * offset
     */
    public final int offset;
    /**
     * m, i.e., the number of RNS representations.
     */
    public final int m;
    /**
     * k, i.e., the number of RNS bases.
     */
    public final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    public final int n;
    /**
     * RNS iterators
     */
    public final RnsIterator[] rnsIterators;

    /**
     * @param ciphertext a Ciphertext object
     * @return a PolyIterator
     */
    public static PolyIterator fromCiphertext(Ciphertext ciphertext) {
        return new PolyIterator(ciphertext.data(), 0, ciphertext.size(),
            ciphertext.polyModulusDegree(), ciphertext.getCoeffModulusSize());
    }

    /**
     * @param ciphertext a Ciphertext object
     * @return a PolyIterator
     */
    public static PolyIterator fromCiphertext(Ciphertext ciphertext, int polyIndex) {
        return new PolyIterator(ciphertext.data(), ciphertext.getPolyOffset(polyIndex), ciphertext.size(),
            ciphertext.polyModulusDegree(), ciphertext.getCoeffModulusSize());
    }


    /**
     * Creates a poly iterator without the number of RNS representations.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public PolyIterator(long[] coeff, int offset, int n, int k) {
        assert (coeff.length - offset) % (n * k) == 0;
        this.m = (coeff.length - offset) / (n * k);

        assert coeff.length >= offset + m * n * k;
        this.coeff = coeff;
        this.offset = offset;
        this.n = n;
        this.k = k;
        rnsIterators = new RnsIterator[m];
        for (int r = 0; r < m; r++) {
            int rOffset = offset + r * n * k;
            rnsIterators[r] = new RnsIterator(coeff, rOffset, n, k);
        }
    }

    /**
     * Creates a poly iterator.
     *
     * @param coeff  the coefficient.
     * @param offset the offset.
     * @param m      m, i.e., the number of RNS representations.
     * @param n      N, i.e., the modulus polynomial degree.
     * @param k      k, i.e., the number of RNS bases.
     */
    public PolyIterator(long[] coeff, int offset, int m, int n, int k) {
        assert coeff.length >= offset + m * n * k;
        this.coeff = coeff;
        this.offset = offset;
        this.m = m;
        this.n = n;
        this.k = k;
        rnsIterators = new RnsIterator[m];
        for (int r = 0; r < m; r++) {
            int rOffset = offset + r * n * k;
            rnsIterators[r] = new RnsIterator(coeff, rOffset, n, k);
        }
    }

    /**
     * Creates a sub-Poly iterator.
     *
     * @param fromIndex the start index, included
     * @param toIndex   the end index, included, the max value is: m - 1
     * @return a sub-RNS iterator.
     */
    public PolyIterator subPolyIterator(int fromIndex, int toIndex) {
        assert fromIndex >= 0 && fromIndex <= toIndex;
        assert toIndex < m;
        return new PolyIterator(coeff, offset + fromIndex * n * k, toIndex - fromIndex + 1, n, k);
    }

    /**
     * Creates a sub-Poly iterator in [fromIndex, m)
     *
     * @param fromIndex the start index, included
     * @return a sub-RNS iterator.
     */
    public PolyIterator subPolyIterator(int fromIndex) {
        assert fromIndex >= 0 && fromIndex < m;
        return new PolyIterator(coeff, offset + fromIndex * n * k, m - fromIndex, n, k);
    }

    /**
     * Creates a poly iterator with all zero value
     *
     * @param m m, i.e., the number of RNS representations.
     * @param n N, i.e., the modulus polynomial degree.
     * @param k k, i.e., the number of RNS bases.
     * @return a poly iterator with all zero value
     */
    public static PolyIterator createZeroPoly(int m, int n, int k) {
        assert m > 0;
        assert n > 0;
        assert k > 0;

        return new PolyIterator(new long[m * n * k], 0, m, n, k);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolyIterator)) {
            return false;
        }
        PolyIterator that = (PolyIterator) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.m, that.m);
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < m * n * k; i++) {
            equalsBuilder.append(this.coeff[this.offset + i], that.coeff[that.offset + i]);
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(m);
        hashCodeBuilder.append(n);
        hashCodeBuilder.append(k);
        // add coefficients
        for (int i = 0; i < m * n * k; i++) {
            hashCodeBuilder.append(coeff[offset + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
