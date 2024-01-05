package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.zq.MultiplyUintModOperand;

/**
 * Provides an interface that performs the fast discrete weighted transform (DWT) and its inverse that are used to
 * accelerate polynomial multiplications, batch multiple messages into a single plaintext polynomial. This class
 * template is specialized with integer modular arithmetic for DWT over integer quotient rings, and is used in
 * polynomial multiplications and BatchEncoder. It is also specialized with double-precision complex arithmetic for
 * DWT over the complex field, which is used in CKKSEncoder.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/dwthandler.h
 * <p></p>
 * For details of fast NTT and INTT, see the following paper:
 * <p>
 * Longa, Patrick, and Michael Naehrig. Speeding up the number theoretic transform for faster ideal lattice-based
 * cryptography. CANS 2016, pp. 124-139.
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
class NttHandler {
    /**
     * lazy modulo arithmetic operation for q
     */
    private final ModArithLazy arithmetic;

    /**
     * Creates an NTT handler.
     *
     * @param arithmetic lazy modulo arithmetic operation for q.
     */
    public NttHandler(ModArithLazy arithmetic) {
        this.arithmetic = arithmetic;
    }

    /**
     * NTT based on the Cooley-Tukey (CT) butterfly. See Algorithm 1 of the paper for more details.
     *
     * @param values     A vector a = (a[0 + startIndex], ..., a[n + startIndex − 1]) ∈ Z_n^q in standard ordering.
     * @param startIndex the start index.
     * @param logN       log(n) such that n = 2^k so that n is a power of 2.
     * @param roots      a precomputed table Ψ[0, ..., n) ∈ Z_n^q storing powers of ψ in bit-reversed order.
     * @param scalar     an optional scalar that is multiplied to all output values.
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public void transformToRev(long[] values, int startIndex, int logN, MultiplyUintModOperand[] roots, MultiplyUintModOperand scalar) {
        // constant transform size
        int n = 1 << logN;
        // registers to hold temporary values
        MultiplyUintModOperand r;
        long u;
        long v;
        // variables for indexing, gap is t in algorithm 1. t = n, but in the first loop t = t / 2.
        int gap = n >>> 1;
        int m = 1;
        // rootsIndex is m + i
        int rootsIndex = 0;
        // for (m = 1; m < n; m = 2m) do
        for (; m < (n >>> 1); m <<= 1) {
            int offSet = 0;
            if (gap < 4) {
                // for (i = 0; i < m; i++) do
                for (int i = 0; i < m; i++) {
                    // S = Ψ[m + i]
                    r = roots[++rootsIndex];
                    // for (j = j_1; j ≤ j_2; j++) do, here j_1 = offSet
                    for (int j = 0; j < gap; j++) {
                        // U = a[j]
                        u = arithmetic.guard(values[startIndex + offSet + j]);
                        // V = a[j + t] · S
                        v = arithmetic.mulRoot(values[startIndex + offSet + gap + j], r);
                        // a[j] = U + V mod q
                        values[startIndex + offSet + j] = arithmetic.add(u, v);
                        // a[j + t] = U − V mod q
                        values[startIndex + offSet + gap + j] = arithmetic.sub(u, v);
                    }
                    // offset is j_1 = 2 · i · t
                    offSet += (gap << 1);
                }
            } else {
                // the following is similar, except that we manually expand loops for speeding up the performance
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
                    for (int j = 0; j < gap; j += 4) {
                        u = arithmetic.guard(values[startIndex + offSet + j]);
                        v = arithmetic.mulRoot(values[startIndex + offSet + gap + j], r);
                        values[startIndex + offSet + j] = arithmetic.add(u, v);
                        values[startIndex + offSet + gap + j] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[startIndex + offSet + j + 1]);
                        v = arithmetic.mulRoot(values[startIndex + offSet + gap + j + 1], r);
                        values[startIndex + offSet + j + 1] = arithmetic.add(u, v);
                        values[startIndex + offSet + gap + j + 1] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[startIndex + offSet + j + 2]);
                        v = arithmetic.mulRoot(values[startIndex + offSet + gap + j + 2], r);
                        values[startIndex + offSet + j + 2] = arithmetic.add(u, v);
                        values[startIndex + offSet + gap + j + 2] = arithmetic.sub(u, v);

                        u = arithmetic.guard(values[startIndex + offSet + j + 3]);
                        v = arithmetic.mulRoot(values[startIndex + offSet + gap + j + 3], r);
                        values[startIndex + offSet + j + 3] = arithmetic.add(u, v);
                        values[startIndex + offSet + gap + j + 3] = arithmetic.sub(u, v);
                    }
                    offSet += (gap << 1);
                }
            }
            // t = t / 2
            gap >>>= 1;
        }

        // handle scalar
        int valuesIndex = 0;
        if (scalar != null) {
            MultiplyUintModOperand scaledR;
            for (int i = 0; i < m; i++) {
                r = roots[++rootsIndex];
                scaledR = arithmetic.mulRootScalar(r, scalar);
                u = arithmetic.mulScalar(arithmetic.guard(values[startIndex + 0 + valuesIndex]), scalar);
                v = arithmetic.mulRoot(values[startIndex + 1 + valuesIndex], scaledR);
                values[startIndex + 0 + valuesIndex] = arithmetic.add(u, v);
                values[startIndex + 1 + valuesIndex] = arithmetic.sub(u, v);
                valuesIndex += 2;
            }
        } else {
            for (int i = 0; i < m; i++) {
                r = roots[++rootsIndex];
                u = arithmetic.guard(values[startIndex + 0 + valuesIndex]);
                v = arithmetic.mulRoot(values[startIndex + 1 + valuesIndex], r);
                values[startIndex + 0 + valuesIndex] = arithmetic.add(u, v);
                values[startIndex + 1 + valuesIndex] = arithmetic.sub(u, v);
                valuesIndex += 2;
            }
        }
    }

    /**
     * Function INTT based on the Gentleman-Sande (GS) butterfly.
     *
     * @param values A vector a = (a[0], a[1], ..., a[n − 1]) ∈ Z_n^q in bit-reversed ordering.
     * @param logN        log(n) such that n = 2^k so n is a power of 2.
     * @param roots       a precomputed table Ψ^{-1}[0, ..., n) ∈ Z_n^q storing powers of ψ in bit-reversed order of [0, -n).
     * @param scalar      optional scalar that is multiplied to all output values.
     */
    public void transformFromRev(long[] values, int startIndex, int logN, MultiplyUintModOperand[] roots, MultiplyUintModOperand scalar) {
        // constant transform size
        int n = 1 << logN;
        // registers to hold temporary values
        MultiplyUintModOperand r;
        long u;
        long v;
        // variables for indexing, gap is t in algorithm 2.
        int gap = 1;
        // m = n, but in the first loop, h = m / 2, so here h is m in Algorithm 2
        int m = n >>> 1;
        // rootsIndex = (h + i) % n. The precomputed table Ψ^{-1}[0, ..., n) is well-organized to reduce computation.
        int rootsIndex = 0;
        // for (m = n; m > 1; m = m/2) do
        for (; m > 1; m >>= 1) {
            int offset = 0;
            if (gap < 4) {
                // for (i = 0; i < h; i++) do
                for (int i = 0; i < m; i++) {
                    // S = Ψ^{-1}[h + i]
                    r = roots[++rootsIndex];
                    // for (j = j_1; j ≤ j_2; j++) do, here j_1 = offSet
                    for (int j = 0; j < gap; j++) {
                        // U = a[j]
                        u = values[startIndex + offset + j];
                        // V = a[j + t]
                        v = values[startIndex + offset + gap + j];
                        // a[j] = U + V mod q
                        values[startIndex + offset + j] = arithmetic.guard(arithmetic.add(u, v));
                        // a[j + t] = (U − V) · S mod q
                        values[startIndex + offset + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                    }
                    // j_1 = j_1 + 2t
                    offset += (gap << 1);
                }
            } else {
                // the following is similar, except that we manually expand loops for speeding up the performance
                for (int i = 0; i < m; i++) {
                    r = roots[++rootsIndex];
                    for (int j = 0; j < gap; j += 4) {
                        u = values[startIndex + offset + j];
                        v = values[startIndex + offset + gap + j];
                        values[startIndex + offset + j] = arithmetic.guard(arithmetic.add(u, v));
                        values[startIndex + offset + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[startIndex + offset + j + 1];
                        v = values[startIndex + offset + gap + j + 1];
                        values[startIndex + offset + j + 1] = arithmetic.guard(arithmetic.add(u, v));
                        values[startIndex + offset + gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[startIndex + offset + j + 2];
                        v = values[startIndex + offset + gap + j + 2];
                        values[startIndex + offset + j + 2] = arithmetic.guard(arithmetic.add(u, v));
                        values[startIndex + offset + gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                        u = values[startIndex + offset + j + 3];
                        v = values[startIndex + offset + gap + j + 3];
                        values[startIndex + offset + j + 3] = arithmetic.guard(arithmetic.add(u, v));
                        values[startIndex + offset + gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                    }
                    offset += (gap << 1);
                }
            }
            gap <<= 1;
        }

        // handle scalar
        if (scalar != null) {
            r = roots[++rootsIndex];
            MultiplyUintModOperand scaledR = arithmetic.mulRootScalar(r, scalar);
            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    u = arithmetic.guard(values[startIndex + j]);
                    v = values[startIndex + gap + j];
                    values[startIndex + j] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[startIndex + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);
                }
            } else {
                for (int j = 0; j < gap; j += 4) {
                    u = arithmetic.guard(values[startIndex + j]);
                    v = values[startIndex + gap + j];
                    values[startIndex + j] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[startIndex + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[startIndex + j + 1]);
                    v = values[startIndex + gap + j + 1];
                    values[startIndex + j + 1] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[startIndex + gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[startIndex + j + 2]);
                    v = values[startIndex + gap + j + 2];
                    values[startIndex + j + 2] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[startIndex + gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);

                    u = arithmetic.guard(values[startIndex + j + 3]);
                    v = values[startIndex + gap + j + 3];
                    values[startIndex + j + 3] = arithmetic.mulScalar(arithmetic.guard(arithmetic.add(u, v)), scalar);
                    values[startIndex + gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), scaledR);
                }
            }
        } else {

            r = roots[++rootsIndex];
            if (gap < 4) {
                for (int j = 0; j < gap; j++) {
                    u = values[startIndex + j];
                    v = values[startIndex + gap + j];
                    values[startIndex + j] = arithmetic.guard(arithmetic.add(u, v));
                    values[startIndex + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                }
            } else {
                for (int j = 0; j < gap; j += 4) {

                    u = values[startIndex + j];
                    v = values[startIndex + gap + j];
                    values[startIndex + j] = arithmetic.guard(arithmetic.add(u, v));
                    values[startIndex + gap + j] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[startIndex + j + 1];
                    v = values[startIndex + gap + j + 1];
                    values[startIndex + j + 1] = arithmetic.guard(arithmetic.add(u, v));
                    values[startIndex + gap + j + 1] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[startIndex + j + 2];
                    v = values[startIndex + gap + j + 2];
                    values[startIndex + j + 2] = arithmetic.guard(arithmetic.add(u, v));
                    values[startIndex + gap + j + 2] = arithmetic.mulRoot(arithmetic.sub(u, v), r);

                    u = values[startIndex + j + 3];
                    v = values[startIndex + gap + j + 3];
                    values[startIndex + j + 3] = arithmetic.guard(arithmetic.add(u, v));
                    values[startIndex + gap + j + 3] = arithmetic.mulRoot(arithmetic.sub(u, v), r);
                }
            }
        }
    }
}
