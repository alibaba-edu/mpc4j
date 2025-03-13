package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

/**
 * Provides an interface that performs the fast discrete weighted transform (DWT) and its inverse that are used to
 * accelerate polynomial multiplications, batch multiple messages into a single plaintext polynomial. This class
 * template is specialized with integer modular arithmetic for DWT over integer quotient rings, and is used in
 * polynomial multiplications and BatchEncoder. It is also specialized with double-precision complex arithmetic for
 * DWT over the complex field, which is used in CKKSEncoder.
 * <p>
 * The discrete weighted transform (DWT) is a variantion on the discrete Fourier transform (DFT) over
 * arbitrary rings involving weighing the input before transforming it by multiplying element-wise by a weight
 * vector, then weighing the output by another vector. The DWT can be used to perform negacyclic convolution on
 * vectors just like how the DFT can be used to perform cyclic convolution. The DFT of size n requires a primitive
 * n-th root of unity, while the DWT for negacyclic convolution requires a primitive 2n-th root of unity, \psi.
 * In the forward DWT, the input is multiplied element-wise with an incrementing power of \psi, the forward DFT
 * transform uses the 2n-th primitve root of unity \psi^2, and the output is not weighed. In the backward DWT, the
 * input is not weighed, the backward DFT transform uses the 2n-th primitve root of unity \psi^{-2}, and the output
 * is multiplied element-wise with an incrementing power of \psi^{-1}.
 * <p>
 * A fast Fourier transform is an algorithm that computes the DFT or its inverse. The Cooley-Tukey FFT reduces
 * the complexity of the DFT from O(n^2) to O(n\log{n}). The DFT can be interpretted as evaluating an (n-1)-degree
 * polynomial at incrementing powers of a primitive n-th root of unity, which can be accelerated by FFT algorithms.
 * The DWT evaluates incrementing odd powers of a primitive 2n-th root of unity, and can also be accelerated by
 * FFT-like algorithms implemented in this class.
 * <p>
 * Algorithms implemented in this class are based on algorithms 1 and 2 in the paper by Patrick Longa and
 * Michael Naehrig (<a href="https://eprint.iacr.org/2016/504.pdf">https://eprint.iacr.org/2016/504.pdf</a>)
 * with three modifications.
 * <ul>
 *     <li>First, we generalize in this class the algorithms to DWT over arbitrary rings.</li>
 *     <li>Second, the powers of \psi^{-1} used by the IDWT are stored in a scrambled order (in contrast to bit-reversed
 *     order in paper) to create coalesced memory accesses. </li>
 *     <li>Third, the multiplication with 1/n in the IDWT is merged to the last iteration, saving n/2 multiplications.
 *     </li>
 *     <li>Last, we unroll the loops to create coalesced memory accesses to input and output vectors. </li>
 * </ul>
 * <p>
 * In earlier versions of SEAL, the mutiplication with 1/n is done by merging a multiplication of 1/2 in all interactions,
 * which is slower than the current method on CPUs but more efficient on some hardware architectures.
 * <p>
 * The order in which the powers of \psi^{-1} used by the IDWT are stored is unnatural but efficient:
 * the i-th slot stores the (reverse_bits(i - 1, log_n) + 1)-th power of \psi^{-1}.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/dwthandler.h#L75">dwthandler.h</a>,
 *
 * @author Weiran Liu
 * @date 2025/2/14
 */
public class DwtHandler {
    /**
     * private constructor.
     */
    private DwtHandler() {
        // empty
    }

    /**
     * Performs in place a fast multiplication with the DWT matrix.
     * Accesses to powers of root is coalesced.
     * Accesses to values is not coalesced without loop unrolling.
     *
     * @param values values inputs in normal order, outputs in bit-reversed order.
     * @param log_n  log 2 of the DWT size.
     * @param roots  powers of a root in bit-reversed order.
     * @param scalar an optional scalar that is multiplied to all output values.
     */
    public static void transform_to_rev(double[][] values, int log_n, final double[][] roots, final double[] scalar) {
        // constant transform size
        int n = 1 << log_n;
        // registers to hold temporary values
        double[] r;
        double[] u = Arithmetic.createZero();
        double[] v = Arithmetic.createZero();
        // pointers for faster indexing
        double[] x;
        double[] y;
        // variables for indexing
        int gap = (n >> 1);
        int m = 1;

        int rootsPointer = 0;
        for (; m < (n >> 1); m <<= 1) {
            int offset = 0;
            if (gap < 4) {
                for (int i = 0; i < m; i++) {
                    // r = *++roots;
                    rootsPointer++;
                    r = roots[rootsPointer];
                    // x = values + offset;
                    int xPointer = offset;
                    // y = x + gap;
                    int yPointer = offset + gap;
                    for (int j = 0; j < gap; j++) {
                        x = values[xPointer];
                        y = values[yPointer];
                        // u = arithmetic_.guard(*x);
                        Arithmetic.set(u, x);
                        // v = arithmetic_.mul_root(*y, r);
                        Arithmetic.mul(v, y, r);
                        // *x++ = arithmetic_.add(u, v);
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.sub(u, v);
                        Arithmetic.sub(y, u, v);
                        yPointer++;
                    }
                    offset += (gap << 1);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    // r = *++roots;
                    rootsPointer++;
                    r = roots[rootsPointer];
                    // x = values + offset;
                    int xPointer = offset;
                    // y = x + gap;
                    int yPointer = offset + gap;
                    for (int j = 0; j < gap; j += 4) {
                        x = values[xPointer];
                        y = values[yPointer];
                        // u = arithmetic_.guard(*x);
                        Arithmetic.set(u, x);
                        // v = arithmetic_.mul_root(*y, r);
                        Arithmetic.mul(v, y, r);
                        // *x++ = arithmetic_.add(u, v);
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.sub(u, v);
                        Arithmetic.sub(y, u, v);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = arithmetic_.guard(*x);
                        Arithmetic.set(u, x);
                        // v = arithmetic_.mul_root(*y, r);
                        Arithmetic.mul(v, y, r);
                        // *x++ = arithmetic_.add(u, v);
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.sub(u, v);
                        Arithmetic.sub(y, u, v);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = arithmetic_.guard(*x);
                        Arithmetic.set(u, x);
                        // v = arithmetic_.mul_root(*y, r);
                        Arithmetic.mul(v, y, r);
                        // *x++ = arithmetic_.add(u, v);
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.sub(u, v);
                        Arithmetic.sub(y, u, v);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = arithmetic_.guard(*x);
                        Arithmetic.set(u, x);
                        // v = arithmetic_.mul_root(*y, r);
                        Arithmetic.mul(v, y, r);
                        // *x++ = arithmetic_.add(u, v);
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.sub(u, v);
                        Arithmetic.sub(y, u, v);
                        yPointer++;
                    }
                    offset += (gap << 1);
                }
            }
            gap >>= 1;
        }

        int valuePointer = 0;
        if (scalar != null) {
            double[] scaled_r = Arithmetic.createZero();
            for (int i = 0; i < m; i++) {
                // r = *++roots;
                rootsPointer++;
                r = roots[rootsPointer];
                // scaled_r = arithmetic_.mul_root_scalar(r, *scalar);
                Arithmetic.mul(scaled_r, r, scalar);
                // u = arithmetic_.mul_scalar(arithmetic_.guard(values[0]), *scalar);
                Arithmetic.mul(u, values[valuePointer], scalar);
                // v = arithmetic_.mul_root(values[1], scaled_r);
                Arithmetic.mul(v, values[valuePointer + 1], scaled_r);
                // values[0] = arithmetic_.add(u, v);
                Arithmetic.add(values[valuePointer], u, v);
                // values[1] = arithmetic_.sub(u, v);
                Arithmetic.sub(values[valuePointer + 1], u, v);
                // values += 2;
                valuePointer += 2;
            }
        } else {
            for (int i = 0; i < m; i++) {
                // r = *++roots;
                rootsPointer++;
                r = roots[rootsPointer];
                // u = arithmetic_.guard(values[0]);
                Arithmetic.set(u, values[valuePointer]);
                // v = arithmetic_.mul_root(values[1], r);
                Arithmetic.mul(v, values[valuePointer + 1], r);
                // values[0] = arithmetic_.add(u, v);
                Arithmetic.add(values[valuePointer], u, v);
                // values[1] = arithmetic_.sub(u, v);
                Arithmetic.sub(values[valuePointer + 1], u, v);
                // values += 2;
                valuePointer += 2;
            }
        }
    }

    /**
     * Performs in place a fast multiplication with the DWT matrix.
     * Accesses to powers of root is coalesced.
     * Accesses to values is not coalesced without loop unrolling.
     *
     * @param values inputs in bit-reversed order, outputs in normal order.
     * @param roots  powers of a root in scrambled order.
     * @param scalar an optional scalar that is multiplied to all output values.
     */
    public static void transform_from_rev(double[][] values, int log_n, double[][] roots, double[] scalar) {
        // constant transform size
        int n = 1 << log_n;
        // registers to hold temporary values
        double[] r;
        double[] u = Arithmetic.createZero();
        double[] v = Arithmetic.createZero();
        // pointers for faster indexing
        double[] x;
        double[] y;
        // variables for indexing
        int gap = 1;
        int m = n >> 1;

        int rootsPointer = 0;
        for (; m > 1; m >>= 1) {
            int offset = 0;
            if (gap < 4) {
                for (int i = 0; i < m; i++) {
                    // r = *++roots;
                    rootsPointer++;
                    r = roots[rootsPointer];
                    // x = values + offset;
                    int xPointer = offset;
                    // y = x + gap;
                    int yPointer = offset + gap;
                    for (int j = 0; j < gap; j++) {
                        x = values[xPointer];
                        y = values[yPointer];
                        // u = *x;
                        Arithmetic.set(u, x);
                        // v = *y;
                        Arithmetic.set(v, y);
                        // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                        Arithmetic.sub(y, u, v);
                        Arithmetic.muli(y, r);
                        yPointer++;
                    }
                    offset += (gap << 1);
                }
            } else {
                for (int i = 0; i < m; i++) {
                    // r = *++roots;
                    rootsPointer++;
                    r = roots[rootsPointer];
                    // x = values + offset;
                    int xPointer = offset;
                    // y = x + gap;
                    int yPointer = offset + gap;
                    for (int j = 0; j < gap; j += 4) {
                        x = values[xPointer];
                        y = values[yPointer];
                        // u = *x;
                        Arithmetic.set(u, x);
                        // v = *y;
                        Arithmetic.set(v, y);
                        // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                        Arithmetic.sub(y, u, v);
                        Arithmetic.muli(y, r);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = *x;
                        Arithmetic.set(u, x);
                        // v = *y;
                        Arithmetic.set(v, y);
                        // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                        Arithmetic.sub(y, u, v);
                        Arithmetic.muli(y, r);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = *x;
                        Arithmetic.set(u, x);
                        // v = *y;
                        Arithmetic.set(v, y);
                        // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                        Arithmetic.sub(y, u, v);
                        Arithmetic.muli(y, r);
                        yPointer++;

                        x = values[xPointer];
                        y = values[yPointer];
                        // u = *x;
                        Arithmetic.set(u, x);
                        // v = *y;
                        Arithmetic.set(v, y);
                        // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                        Arithmetic.add(x, u, v);
                        xPointer++;
                        // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                        Arithmetic.sub(y, u, v);
                        Arithmetic.muli(y, r);
                        yPointer++;
                    }
                    offset += (gap << 1);
                }
            }
            gap <<= 1;
        }

        if (scalar != null) {
            // r = *++roots;
            rootsPointer++;
            r = roots[rootsPointer];
            // RootType scaled_r = arithmetic_.mul_root_scalar(r, * scalar);
            double[] scaled_r = Arithmetic.createZero();
            Arithmetic.mul(scaled_r, r, scalar);
            // x = values;
            int xPointer = 0;
            // y = x + gap;
            int yPointer = gap;
            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    x = values[xPointer];
                    y = values[yPointer];
                    // u = arithmetic_.guard( * x);
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.mul_scalar(arithmetic_.guard(arithmetic_.add(u, v)), * scalar);
                    Arithmetic.add(x, u, v);
                    Arithmetic.muli(x, scalar);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), scaled_r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, scaled_r);
                    yPointer++;
                }
            } else {
                for (int j = 0; j < gap; j += 4) {
                    x = values[xPointer];
                    y = values[yPointer];
                    // u = arithmetic_.guard( * x);
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.mul_scalar(arithmetic_.guard(arithmetic_.add(u, v)), * scalar);
                    Arithmetic.add(x, u, v);
                    Arithmetic.muli(x, scalar);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), scaled_r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, scaled_r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = arithmetic_.guard( * x);
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.mul_scalar(arithmetic_.guard(arithmetic_.add(u, v)), * scalar);
                    Arithmetic.add(x, u, v);
                    Arithmetic.muli(x, scalar);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), scaled_r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, scaled_r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = arithmetic_.guard( * x);
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.mul_scalar(arithmetic_.guard(arithmetic_.add(u, v)), * scalar);
                    Arithmetic.add(x, u, v);
                    Arithmetic.muli(x, scalar);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), scaled_r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, scaled_r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = arithmetic_.guard( * x);
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.mul_scalar(arithmetic_.guard(arithmetic_.add(u, v)), * scalar);
                    Arithmetic.add(x, u, v);
                    Arithmetic.muli(x, scalar);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), scaled_r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, scaled_r);
                    yPointer++;
                }
            }
        } else {
            // r = *++roots;
            rootsPointer++;
            r = roots[rootsPointer];
            // x = values;
            int xPointer = 0;
            // y = x + gap;
            int yPointer = gap;
            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    x = values[xPointer];
                    y = values[yPointer];
                    // u = *x;
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                    Arithmetic.add(x, u, v);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, r);
                    yPointer++;
                }
            } else {
                for (int j = 0; j < gap; j += 4) {
                    x = values[xPointer];
                    y = values[yPointer];
                    // u = *x;
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                    Arithmetic.add(x, u, v);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = *x;
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                    Arithmetic.add(x, u, v);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = *x;
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                    Arithmetic.add(x, u, v);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, r);
                    yPointer++;

                    x = values[xPointer];
                    y = values[yPointer];
                    // u = *x;
                    Arithmetic.set(u, x);
                    // v = *y;
                    Arithmetic.set(v, y);
                    // *x++ = arithmetic_.guard(arithmetic_.add(u, v));
                    Arithmetic.add(x, u, v);
                    xPointer++;
                    // *y++ = arithmetic_.mul_root(arithmetic_.sub(u, v), r);
                    Arithmetic.sub(y, u, v);
                    Arithmetic.muli(y, r);
                    yPointer++;
                }
            }
        }
    }
}
