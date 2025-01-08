package edu.alibaba.mpc4j.common.tool.network.decomposer;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Abstract permutation decomposer
 *
 * @author Feng Han
 * @date 2024/8/12
 */
public abstract class AbstractPermutationDecomposer implements PermutationDecomposer {
    /**
     * number of inputs N for the permutation π: [N] → [N], where N = 2^n
     */
    protected final int n;
    /**
     * log(N)
     */
    protected final int logN;
    /**
     * level = 2 * log(N) - 1
     */
    protected final int level;
    /**
     * number of inputs for each sub-permutation π_(i,j): [T] → [T], where i ∈ [0, d), j ∈ [0, N / T], and T = 2^t
     */
    protected final int t;
    /**
     * log(T)
     */
    protected final int logT;
    /**
     * d = 2 * ceil(log(N) / log(T)) - 1
     */
    protected final int d;
    /**
     * whether set permutation
     */
    protected boolean setPermutation;
    /**
     * sub-permutations. We have d layers, each layer contains N / T groups, each group contains a [T] → [T] permutation.
     */
    protected int[][][] subPermutations;

    public AbstractPermutationDecomposer(int n, int t) {
        // set and verify N
        // N > 1, otherwise we do not need to permute
        MathPreconditions.checkGreater("N", n, 1);
        // N = 2^n
        Preconditions.checkArgument(IntMath.isPowerOfTwo(n), "N must be a power of 2: %s", n);
        this.n = n;
        // computes n = log(N) when N = 2^n
        logN = Integer.numberOfTrailingZeros(n);
        // level = 2 * log(N) - 1
        level = 2 * logN - 1;
        // set and verify T, T ∈ [2, N], T = 1 is valid but not necessary
        MathPreconditions.checkInRangeClosed("t", t, 2, n);
        // T = 2^t
        Preconditions.checkArgument(IntMath.isPowerOfTwo(t), "T must be a power of 2: %s", t);
        this.t = t;
        // computes t = log(T) when T = 2^t
        logT = Integer.numberOfTrailingZeros(t);
        // d = 2 * ceil(log(N) / log(T)) - 1
        d = 2 * (int) Math.ceil((double) logN / logT) - 1;
        setPermutation = false;
    }

    /**
     * Gets N.
     *
     * @return N.
     */
    public int getN() {
        return n;
    }

    /**
     * Gets d = 2 * ceil(log(N) / log(T)) - 1, the number of split layers.
     *
     * @return d.
     */
    public int getD() {
        return d;
    }

    /**
     * Gets sub-permutations.
     *
     * @return sub-permutations.
     */
    public int[][][] getSubPermutations() {
        Preconditions.checkArgument(setPermutation, "Please set permutation");
        return subPermutations;
    }
}
