package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;

/**
 * Complex roots.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/croots.h">croots.h</a>
 * and
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/croots.h">croots.c</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/14
 */
public class ComplexRoots {
    /**
     * π
     */
    private static final double PI = 3.1415926535897932384626433832795028842;
    /**
     * Contains 0~(n/8-1)-th powers of the n-th primitive root.
     */
    private final double[][] roots;
    /**
     * degree of roots
     */
    private final int degree_of_roots;

    public ComplexRoots(int degree_of_roots) {
        this.degree_of_roots = degree_of_roots;
        // int power = util::get_power_of_two(degree_of_roots_);
        int power = UintCore.getPowerOfTwo(degree_of_roots);
        if (power < 0) {
            throw new IllegalArgumentException("degree_of_roots must be a power of two");
        } else if (power < 3) {
            throw new IllegalArgumentException("degree_of_roots must be at least 8");
        }
        // roots_ = allocate<complex<double>>(degree_of_roots_ / 8 + 1, pool_);
        roots = new double[degree_of_roots / 8 + 1][2];

        // Generate 1/8 of all roots.
        // Alternatively, choose from precomputed high-precision roots in files.
        for (int i = 0; i <= degree_of_roots / 8; i++) {
            // roots_[i] = polar<double>(1.0, 2 * PI_ * static_cast<double>(i) / static_cast<double>(degree_of_roots_));
            roots[i] = Arithmetic.create(
                Math.cos(2 * PI * i / degree_of_roots),
                Math.sin(2 * PI * i / degree_of_roots)
            );
        }
    }

    /**
     * Gets root.
     *
     * @param index index.
     * @return root.
     */
    public double[] get_root(int index) {
        // index &= degree_of_roots_ - 1;
        index &= degree_of_roots - 1;

        // This express the 8-fold symmetry of all n-th roots.
        if (index <= degree_of_roots / 8) {
            double[] root = Arithmetic.createZero();
            Arithmetic.set(root, roots[index]);
            return root;
        } else if (index <= degree_of_roots / 4) {
            double[] root = Arithmetic.createZero();
            Arithmetic.set(root, roots[degree_of_roots / 4 - index]);
            Arithmetic.mirrori(root);
            return root;
        } else if (index <= degree_of_roots / 2) {
            // return -conj(get_root(degree_of_roots_ / 2 - index));
            double[] root = get_root(degree_of_roots / 2 - index);
            Arithmetic.conji(root);
            Arithmetic.negi(root);
            return root;
        } else if (index <= 3 * degree_of_roots / 4) {
            // return -get_root(index - degree_of_roots_ / 2);
            double[] root = get_root(index - degree_of_roots / 2);
            Arithmetic.negi(root);
            return root;
        } else {
            // return conj(get_root(degree_of_roots_ - index));
            double[] root = get_root(degree_of_roots - index);
            Arithmetic.conji(root);
            return root;
        }
    }
}
