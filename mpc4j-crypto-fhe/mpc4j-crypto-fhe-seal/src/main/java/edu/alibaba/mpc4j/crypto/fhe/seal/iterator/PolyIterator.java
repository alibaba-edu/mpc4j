package edu.alibaba.mpc4j.crypto.fhe.seal.iterator;

import edu.alibaba.mpc4j.crypto.fhe.seal.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyCore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.LongBuffer;

/**
 * PolyIterator represents multiple (m >= 1) degree-(N-1) RNS representations.
 * <p>
 * The implementation is from <code>PolyIter</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/iterator.h#L1304">iterator.h</a>.
 *
 * @author Weiran Liu
 * @date 2023/11/5
 */
public class PolyIterator implements SealIterator {
    /**
     * Creates an 1D-array Poly-RNS representations with all coefficients initialized as 0.
     *
     * @param m number of RNS representations.
     * @param n modulus polynomial degree.
     * @param k number of RNS bases.
     * @return an 1D-array Poly-RNS representations with all coefficients initialized as 0.
     */
    public static long[] allocateArray(int m, int n, int k) {
        assert m > 0;
        assert n > 0;
        assert k > 0;
        return PolyCore.allocateZeroPolyArray(m * k, n, 1);
    }

    /**
     * Converts an 3D-array Poly-RNS representation into an 1D-array Poly-RNS representation.
     *
     * @param data an 3D-array Poly-RNS representation.
     * @return an an 1D-array Poly-RNS representation.
     */
    public static long[] createFrom3dArray(long[][][] data) {
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
     * Converts an 1D-array Poly-RNS representation into an 3D-array Poly-RNS representation.
     *
     * @param coeff an 1D-array Poly-RNS representation.
     * @param m     number of RNS representations.
     * @param n     modulus polynomial degree.
     * @param k     number of RNS bases.
     * @return an 3D-array Poly-RNS representation.
     */
    public static long[][][] to3dArray(long[] coeff, int m, int n, int k) {
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
     * Wraps the ciphertext by a polynomial iterator.
     *
     * @param ciphertext ciphertext.
     * @return Wrapped polynomial iterator.
     */
    public static PolyIterator fromCiphertext(Ciphertext ciphertext) {
        return PolyIterator.wrap(
            ciphertext.data(), ciphertext.size(), ciphertext.polyModulusDegree(), ciphertext.getCoeffModulusSize()
        );
    }

    /**
     * Wraps the ciphertext by a polynomial iterator.
     *
     * @param ciphertext a Ciphertext object.
     * @param polyIndex  polynomial index.
     * @return Wrapped polynomial iterator.
     */
    public static PolyIterator fromCiphertext(Ciphertext ciphertext, int polyIndex) {
        return PolyIterator.wrap(
            ciphertext.data(), ciphertext.getPolyOffset(polyIndex), ciphertext.size(),
            ciphertext.polyModulusDegree(), ciphertext.getCoeffModulusSize()
        );
    }

    /**
     * Creates a poly iterator with all zero value.
     *
     * @param m m, i.e., the number of RNS representations.
     * @param n N, i.e., the modulus polynomial degree.
     * @param k k, i.e., the number of RNS bases.
     * @return a poly iterator with all zero value
     */
    public static PolyIterator allocate(int m, int n, int k) {
        assert m > 0;
        assert n > 0;
        assert k > 0;

        return new PolyIterator(new long[m * n * k], 0, m, n, k);
    }

    /**
     * Creates a poly iterator without the number of RNS representations.
     *
     * @param coeff the coefficient.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    public static PolyIterator dynamicWrap(long[] coeff, int n, int k) {
        return dynamicWrap(coeff, 0, n, k);
    }

    /**
     * Creates a poly iterator without the number of RNS representations.
     *
     * @param coeff the coefficient.
     * @param pos   the starting position of coefficients.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    public static PolyIterator dynamicWrap(long[] coeff, int pos, int n, int k) {
        // In CKKS we do not verify assert (coeff.length - pos) % (n * k) == 0 since CKKS implementation abuses tools.
        // If we add assert (coeff.length - pos) % (n * k) == 0, then the example would fail.
        int m = (coeff.length - pos) / (n * k);
        return wrap(coeff, pos, m, n, k);
    }

    /**
     * Creates a poly iterator without the number of RNS representations.
     *
     * @param coeff the coefficient.
     * @param m     M, i.e., number of RNS representations.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    public static PolyIterator wrap(long[] coeff, int m, int n, int k) {
        return wrap(coeff, 0, m, n, k);
    }

    /**
     * Creates a poly iterator without the number of RNS representations.
     *
     * @param coeff the coefficient.
     * @param pos   the starting position of coefficients.
     * @param m     M, i.e., number of RNS representations.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    public static PolyIterator wrap(long[] coeff, int pos, int m, int n, int k) {
        return new PolyIterator(coeff, pos, m, n, k);
    }

    /**
     * The coefficient for the r-th RNS representation is coeff[offset + r * N * k + 0 .. offset + r * N * k + N * k).
     */
    private final long[] coeff;
    /**
     * the starting position of the coefficients.
     */
    private final int pos;
    /**
     * m, i.e., the number of RNS representations.
     */
    private final int m;
    /**
     * k, i.e., the number of RNS bases.
     */
    private final int k;
    /**
     * N, i.e., modulus polynomial degree.
     */
    private final int n;
    /**
     * offset
     */
    private int offset;
    /**
     * RNS iterators
     */
    public final RnsIterator[] rnsIter;

    /**
     * Creates a poly iterator.
     *
     * @param coeff the coefficient.
     * @param pos   the starting position.
     * @param m     m, i.e., the number of RNS representations.
     * @param n     N, i.e., the modulus polynomial degree.
     * @param k     k, i.e., the number of RNS bases.
     */
    private PolyIterator(long[] coeff, int pos, int m, int n, int k) {
        assert coeff.length >= pos + m * n * k;
        this.coeff = coeff;
        this.pos = pos;
        this.m = m;
        this.n = n;
        this.k = k;
        offset = 0;
        rnsIter = new RnsIterator[m];
        for (int r = 0; r < m; r++) {
            int rPos = pos + r * stepSize();
            rnsIter[r] = RnsIterator.wrap(coeff, rPos, n, k);
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
        return new PolyIterator(coeff, pos + fromIndex * stepSize(), toIndex - fromIndex + 1, n, k);
    }

    @Override
    public long[] coeff() {
        return coeff;
    }

    @Override
    public int pos() {
        return pos;
    }

    /**
     * Gets the pointer for the r-th RNS representation.
     *
     * @param r r.
     * @return the pointer for the r-th RNS representation.
     */
    public int ptr(int r) {
        return pos + r * stepSize();
    }

    @Override
    public int stepSize() {
        return n * k;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
        for (int r = 0; r < m; r++) {
            int rPos = pos + offset + r * stepSize();
            rnsIter[r] = RnsIterator.wrap(coeff, rPos, n, k);
        }
    }

    /**
     * Gets m, i.e., number of RNS representations.
     *
     * @return number of RNS representations.
     */
    public int m() {
        return m;
    }

    /**
     * Gets k, i.e., number of RNS bases.
     *
     * @return number of RNS bases.
     */
    public int k() {
        return k;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PolyIterator that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.m, that.m);
        equalsBuilder.append(this.n, that.n);
        equalsBuilder.append(this.k, that.k);
        // add coefficients
        for (int i = 0; i < m * stepSize(); i++) {
            equalsBuilder.append(this.coeff[this.pos + i], that.coeff[that.pos + i]);
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
        for (int i = 0; i < m * stepSize(); i++) {
            hashCodeBuilder.append(coeff[pos + i]);
        }
        return hashCodeBuilder.hashCode();
    }
}
