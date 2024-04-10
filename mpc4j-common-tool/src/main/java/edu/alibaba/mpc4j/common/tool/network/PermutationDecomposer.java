package edu.alibaba.mpc4j.common.tool.network;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.*;

/**
 * Given a permutation π: [N] → [N], where N = 2^n for some integer, we decompose π to π_1 ◦ ... ◦ π_d, such that each
 * π_i is itself a composition of N / T disjoint permutation, each acting on T elements, for some parameter T = 2^t.
 * Here d = 2 * ceil(log(N) / log(T)) - 1.
 * <p></p>
 * The following paper uses this decomposition to implement efficient secret-shared permutation.
 * <p>
 * Chase, Melissa, Esha Ghosh, and Oxana Poburinnaya. Secret-shared shuffle. ASIACRYPT 2020, pp. 342-372.
 * </p>
 * The implementation is inspired by the personal communication with authors of the following paper:
 * <p>
 * Hou, Xiaoyang, Jian Liu, Jingyu Li, Yuhan Li, Wen-jie Lu, Cheng Hong, and Kui Ren. CipherGPT: Secure Two-party GPT
 * Inference." Cryptology ePrint Archive, 2023/1147.
 * </p>
 * The Permutation decomposer generates two 3D arrays: split_groups[d][N/T][T], and sub_permutations[d][N/T][T], where
 * split_groups represents how to (constantly) permute the data before executing sub-permutations, and sub_permutations
 * represents N / T disjoint permutation.
 *
 * @author Weiran Liu
 * @date 2024/3/27
 */
public class PermutationDecomposer {
    /**
     * the permutation
     */
    private final int[] permutation;
    /**
     * number of inputs N for the permutation π: [N] → [N], where N = 2^n
     */
    private final int n;
    /**
     * log(N)
     */
    private final int logN;
    /**
     * level = 2 * log(N) - 1
     */
    private final int level;
    /**
     * number of inputs for each sub-permutation π_(i,j): [T] → [T], where i ∈ [0, d), j ∈ [0, N / T], and T = 2^t
     */
    private final int t;
    /**
     * log(T)
     */
    private final int logT;
    /**
     * d = 2 * ceil(log(N) / log(T)) - 1
     */
    private final int d;
    /**
     * d split groups, each group contains N / T permutations π_(i,j): [T] → [T].
     */
    private final int[][][] splitGroups;
    /**
     * d-layer permutations, each layer contains N / T sub-permutations π_(i,j): [T] → [T].
     */
    private final int[][][] subPermutations;

    /**
     * Creates a permutation decomposer.
     *
     * @param permutation the permutation.
     * @param t           the size of the sub-permutation.
     */
    public PermutationDecomposer(int[] permutation, int t) {
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(permutation));
        // set and verify N
        n = permutation.length;
        // N > 1, otherwise we do not need to permute
        MathPreconditions.checkGreater("N", n, 1);
        // N = 2^n
        Preconditions.checkArgument(IntMath.isPowerOfTwo(n), "N must be a power of 2: %s", n);
        // computes n = log(N) when N = 2^n
        logN = Integer.numberOfTrailingZeros(n);
        // level = 2 * log(N) - 1
        level = 2 * logN - 1;
        // copy the permutation
        this.permutation = Arrays.copyOf(permutation, n);
        // set and verify T, T ∈ [2, N], T = 1 is valid but not necessary
        MathPreconditions.checkInRangeClosed("t", t, 2, n);
        // T = 2^t
        Preconditions.checkArgument(IntMath.isPowerOfTwo(n), "N must be a power of 2: %s", n);
        this.t = t;
        // computes t = log(T) when T = 2^t
        logT = Integer.numberOfTrailingZeros(t);
        // d = 2 * ceil(log(N) / log(T)) - 1
        d = 2 * (int) Math.ceil((double) logN / logT) - 1;
        int[][] splitLayerPermutation = computeSplitLayerPermutation();
        splitGroups = computeSplitGroups();
        subPermutations = computeSubPermutations(splitGroups, splitLayerPermutation);
    }

    private int[][] computeSplitLayerPermutation() {
        /*
         * Compute permutation results for each layer.
         * There are 2 * log(N) permutation results for 2 * log(N) - 1 layers, each layer contains n elements.
         * For example, when N = 2^5, level = 2 * log(N) - 1 = 9, there are 10 permutations results with the form:
         * ( 0) (1) (2) (3) (4) (5) (6) (7) (8) ( 9 )
         *  00   □   □   □   □   □   □   □   □  π(00)
         *  01   □   □   □   □   □   □   □   □  π(01)
         *  02   □   □   □   □   □   □   □   □  π(02)
         *  ...
         *  30   □   □   □   □   □   □   □   □  π(30)
         *  31   □   □   □   □   □   □   □   □  π(31)
         *  |<-         2 * log(N) = 10         ->|
         */
        int[][] eachLayerPermutation = new int[level + 1][n];
        for (int i = 0; i < n; i++) {
            // set the initial permutation result: (0, 1, ..., N - 1)
            eachLayerPermutation[0][i] = i;
            // set the final permutation result after 2 * log(N) - 1 levels: (π(0), π(1), ..., π(N - 1))
            eachLayerPermutation[level][i] = permutation[i];
        }
        for (int li = 1; li < logN; li++) {
            // compute permutations[li] and permutations[ri], li is left index, ri = 2 * log(N) - 1 - li is right index
            int ri = level - li;
            int step = (1 << (logN - li));
            int[][] subPermutation = new int[n][2];
            for (int j = 0; j < n; j++) {
                subPermutation[eachLayerPermutation[li - 1][j]][0] = eachLayerPermutation[li - 1][j ^ step];
                subPermutation[eachLayerPermutation[2 * logN - li][j]][1] = eachLayerPermutation[2 * logN - li][j ^ step];
            }
            int[] path = getBipartiteGraphColor(subPermutation);
            // copy permutations[li - 1] to permutations[li]
            System.arraycopy(eachLayerPermutation[li - 1], 0, eachLayerPermutation[li], 0, n);
            for (int j = 0; j < n; j++) {
                if ((j & step) == 0) {
                    if (path[eachLayerPermutation[li][j]] == 1 && path[eachLayerPermutation[li][j ^ step]] == 0) {
                        int temp = eachLayerPermutation[li][j];
                        eachLayerPermutation[li][j] = eachLayerPermutation[li][j ^ step];
                        eachLayerPermutation[li][j ^ step] = temp;
                    }
                }
            }
            // copy permutation[ri + 1] to permutation[ri]
            System.arraycopy(eachLayerPermutation[ri + 1], 0, eachLayerPermutation[ri], 0, n);
            for (int j = 0; j < n; j++) {
                if ((j & step) == 0) {
                    if (path[eachLayerPermutation[ri][j]] == 1 && path[eachLayerPermutation[ri][j ^ step]] == 0) {
                        int temp = eachLayerPermutation[ri][j];
                        eachLayerPermutation[ri][j] = eachLayerPermutation[ri][j ^ step];
                        eachLayerPermutation[ri][j ^ step] = temp;
                    }
                }
            }
        }
        /*
         * Create and set permutations based on the split parameter T.
         * There are 2 * ceil(log(N) / log(T)) split permutation results, each layer contains n elements. For example,
         * when N = 2^5, T = 2^4, we have 2 * ceil(5 / 4) - 1 = 3 sub-permutations and 4 sub-permutation results.
         * ( 0) (1) (2) (3) (4) (5) (6) (7) (8) ( 9 )
         *  00   □   □   □   ■   ■   □   □   □  π(00)
         *  01   □   □   □   ■   ■   □   □   □  π(01)
         *  02   □   □   □   ■   ■   □   □   □  π(02)
         *  ...
         *  30   □   □   □   ■   ■   □   □   □  π(30)
         *  31   □   □   □   ■   ■   □   □   □  π(31)
         *  |<- log(T) = 4 ->|   |<- log(T) = 4 ->|
         *  |<-         2 * log(N) = 10         ->|
         */
        int[][] splitLayerPermutation = new int[d + 1][n];
        // copy assigned layer permutation (except the last one)
        for (int i = 0; i < (int) Math.ceil((double) logN / logT); i++) {
            System.arraycopy(eachLayerPermutation[i * logT], 0, splitLayerPermutation[i], 0, n);
            System.arraycopy(eachLayerPermutation[level - i * logT], 0, splitLayerPermutation[d - i], 0, n);
        }
        return splitLayerPermutation;
    }

    private int[] getBipartiteGraphColor(int[][] subPermutation) {
        assert subPermutation.length == n;
        // initially, all elements in path are -1
        int[] path = new int[n];
        Arrays.fill(path, -1);
        Stack<int[]> stack = new Stack<>();
        // enumerate all elements in path
        for (int i = 0; i < n; i++) {
            // push a candidate color
            if (path[i] < 0) {
                stack.push(new int[]{i, 0});
            }
            while (!stack.empty()) {
                int[] pair = stack.pop();
                int curFirst = pair[0];
                int curSecond = pair[1];
                path[curFirst] = curSecond;
                for (int p : subPermutation[curFirst]) {
                    if (path[p] < 0) {
                        stack.push(new int[]{p, curSecond ^ 1});
                    }
                }
            }
        }
        return path;
    }

    private int[][][] computeSplitGroups() {
        int[][][] splitGroup = new int[d][n / t][t];
        // create the left and the right split
        for (int i = 1; i < (int) Math.ceil((double) logN / logT); i++) {
            int step = 1 << (logN - i * logT);
            int groupCounter = 0;
            int[] vis = new int[n];
            for (int j = 0; j < n; j++) {
                if (vis[j] != 0) {
                    continue;
                }
                for (int k = 0; k < t; k++) {
                    splitGroup[i - 1][groupCounter][k] = j + k * step;
                    splitGroup[d - i][groupCounter][k] = j + k * step;
                    vis[j + k * step] = 1;
                }
                groupCounter++;
            }
        }
        // create the middle split
        int middleLayerIndex = (int) Math.ceil((double) logN / logT) - 1;
        for (int i = 0; i < n / t; i++) {
            for (int j = 0; j < t; j++) {
                splitGroup[middleLayerIndex][i][j] = i * t + j;
            }
        }
        return splitGroup;
    }

    private int[][][] computeSubPermutations(final int[][][] splitGroup, final int[][] splitLayerPermutation) {
        int[][][] subPermutations = new int[d][n / t][t];
        for (int i = 0; i < d; i++) {
            int[] mp = new int[n];
            for (int j = 0; j < n / t; j++) {
                for (int k = 0; k < t; k++) {
                    mp[splitLayerPermutation[i][splitGroup[i][j][k]]] = k;
                }
            }
            for (int j = 0; j < n / t; j++) {
                for (int k = 0; k < t; k++) {
                    subPermutations[i][j][k] = mp[splitLayerPermutation[i + 1][splitGroup[i][j][k]]];
                }
            }
        }
        return subPermutations;
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
     * Gets the number of sub-permutations in each layer, i.e, N / T.
     *
     * @return the number of sub-permutations in each layer.
     */
    public int getSubNum() {
        return n / t;
    }

    /**
     * Gets the original permutation.
     *
     * @return the original permutation.
     */
    public int[] getPermutation() {
        return permutation;
    }

    /**
     * Gets split groups. We have d layers, each contains N / T groups, each group contains T sub-permutations.
     *
     * @return split groups.
     */
    public int[][][] getSplitGroups() {
        return splitGroups;
    }

    /**
     * Gets sub-permutations. We have d layers, each contains N / T groups, each group contains T sub-permutations.
     *
     * @return sub-permutations.
     */
    public int[][][] getSubPermutations() {
        return subPermutations;
    }

    /**
     * Gets T, the sub-permutation size.
     *
     * @return T.
     */
    public int getT() {
        return t;
    }
}
