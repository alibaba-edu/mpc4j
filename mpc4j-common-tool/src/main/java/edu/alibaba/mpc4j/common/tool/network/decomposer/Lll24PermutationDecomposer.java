package edu.alibaba.mpc4j.common.tool.network.decomposer;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;

import java.util.Arrays;
import java.util.Stack;

/**
 * The difference between opt and the normal PermutationDecomposer is that the opt one can reduce the T in the first layer,
 * such that the total cost for the CST would be reduced.
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
 * @author Feng Han
 * @date 2024/7/26
 */
public class Lll24PermutationDecomposer extends AbstractPermutationDecomposer{
    /**
     * group num, g = N / T
     */
    private final int g;
    /**
     * number of inputs for each sub-permutation in the first layer
     */
    private final int tInFirstLayer;
    /**
     * group num in the first layer, gInFirstLayer = (1 << (ceilDiv * logT - logN)) * g;
     */
    private final int gInFirstLayer;
    /**
     * split groups. We have d layers, each layer contains N / T groups, each group contains T elements.
     */
    private final int[][][] splitGroups;

    /**
     * Creates a permutation decomposer.
     *
     * @param n number of inputs.
     * @param t the size of the sub-permutation.
     */
    public Lll24PermutationDecomposer(int n, int t) {
        super(n, t);
        int ceilDiv = (int) Math.ceil((double) logN / logT);
        g = n / t;
        tInFirstLayer = t / (1 << (ceilDiv * logT - logN));
        gInFirstLayer = (1 << (ceilDiv * logT - logN)) * g;
        splitGroups = new int[d][][];
        computeSplitGroups();
    }

    private void computeSplitGroups() {
        int ceilDiv = (int) Math.ceil((double) logN / logT);
        for (int i = 1; i < d - 1; i++) {
            splitGroups[i] = new int[g][t];
        }
        splitGroups[0] = new int[gInFirstLayer][tInFirstLayer];
        splitGroups[d - 1] = new int[gInFirstLayer][tInFirstLayer];
        // create the middle split
        int middleLayerIndex = ceilDiv - 1;
        for (int i = 0; i < n / t; i++) {
            for (int j = 0; j < t; j++) {
                splitGroups[middleLayerIndex][i][j] = i * t + j;
            }
        }
        // create the left and the right split
        for (int i = 1; i < ceilDiv; i++) {
            int step = 1 << (i * logT);
            int groupCounter = 0;
            int[] vis = new int[n];
            int currentStep = i == ceilDiv - 1 ? tInFirstLayer : t;
            for (int j = 0; j < n; j++) {
                if (vis[j] != 0) {
                    continue;
                }
                for (int k = 0; k < currentStep; k++) {
                    splitGroups[middleLayerIndex - i][groupCounter][k] = j + k * step;
                    splitGroups[middleLayerIndex + i][groupCounter][k] = j + k * step;
                    vis[j + k * step] = 1;
                }
                groupCounter++;
            }
            assert groupCounter == (i == ceilDiv - 1 ? gInFirstLayer : g);
        }
    }

    /**
     * Splits vector into groups.
     *
     * @param vector vector.
     * @param i      layer index.
     * @return groups.
     */
    @Override
    public byte[][][] splitVector(byte[][] vector, int i) {
        MathPreconditions.checkEqual("n", "vector.length", n, vector.length);
        MathPreconditions.checkNonNegativeInRange("i", i, d);
        if (i == 0 || i == d - 1) {
            byte[][][] groups = new byte[gInFirstLayer][tInFirstLayer][];
            for (int j = 0; j < gInFirstLayer; j++) {
                for (int k = 0; k < tInFirstLayer; k++) {
                    groups[j][k] = vector[splitGroups[i][j][k]];
                }
            }
            return groups;
        } else {
            byte[][][] groups = new byte[g][t][];
            for (int j = 0; j < g; j++) {
                MathPreconditions.checkEqual("t", "group[" + j + "].length", t, groups[j].length);
                for (int k = 0; k < t; k++) {
                    groups[j][k] = vector[splitGroups[i][j][k]];
                }
            }
            return groups;
        }
    }

    /**
     * Combines groups into vector.
     *
     * @param groups groups.
     * @param i      layer index.
     * @return vector.
     */
    @Override
    public byte[][] combineGroups(byte[][][] groups, int i) {
        MathPreconditions.checkNonNegativeInRange("i", i, d);
        if (i == 0 || i == d - 1) {
            MathPreconditions.checkEqual("g", "groups.length", gInFirstLayer, groups.length);
            byte[][] vector = new byte[n][];
            for (int j = 0; j < gInFirstLayer; j++) {
                MathPreconditions.checkEqual("t", "group[" + j + "].length", tInFirstLayer, groups[j].length);
                for (int k = 0; k < tInFirstLayer; k++) {
                    vector[splitGroups[i][j][k]] = groups[j][k];
                }
            }
            return vector;
        } else {
            MathPreconditions.checkEqual("g", "groups.length", g, groups.length);
            byte[][] vector = new byte[n][];
            for (int j = 0; j < g; j++) {
                MathPreconditions.checkEqual("t", "group[" + j + "].length", t, groups[j].length);
                for (int k = 0; k < t; k++) {
                    vector[splitGroups[i][j][k]] = groups[j][k];
                }
            }
            return vector;
        }
    }

    /**
     * Sets sub-permutations. We have d layers, each contains N / T groups, each group corresponds a [T] → [T]
     * sub-permutations.
     *
     * @param permutation permutation.
     */
    @Override
    public void setPermutation(final int[] permutation) {
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(permutation));
        MathPreconditions.checkEqual("n", "permutation.length", n, permutation.length);
        int[][] splitLayerPermutation = computeSplitLayerPermutation(permutation);
        subPermutations = new int[d][][];
        for (int i = 0; i < d; i++) {
            int currentT = (i == 0 || i == d - 1) ? tInFirstLayer : t;
            subPermutations[i] = new int[n / currentT][currentT];
            int[] mp = new int[n];
            for (int j = 0; j < n / currentT; j++) {
                for (int k = 0; k < currentT; k++) {
                    mp[splitLayerPermutation[i][splitGroups[i][j][k]]] = k;
                }
            }
            for (int j = 0; j < n / currentT; j++) {
                for (int k = 0; k < currentT; k++) {
                    subPermutations[i][j][k] = mp[splitLayerPermutation[i + 1][splitGroups[i][j][k]]];
                }
            }
        }
        setPermutation = true;
    }

    private int[][] computeSplitLayerPermutation(int[] permutation) {
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
         * We split the permutation in the in-outside way
         * There are 2 * ceil(log(N) / log(T)) split permutation results, each layer contains n elements. For example,
         * when N = 2^5, T = 2^4, we have 2 * ceil(5 / 4) - 1 = 3 sub-permutations and 4 sub-permutation results.
         * ( 0) (1) (2) (3) (4) (5) (6) (7) (8) ( 9 )
         *  00   □   □   □   ■   ■   □   □   □  π(00)
         *  01   □   □   □   ■   ■   □   □   □  π(01)
         *  02   □   □   □   ■   ■   □   □   □  π(02)
         *  ...
         *  30   □   □   □   ■   ■   □   □   □  π(30)
         *  31   □   □   □   ■   ■   □   □   □  π(31)
         *  |<-1->|<-    2 * log(T) = 8    ->|<-1->|
         *  |<-          2 * log(N) = 10         ->|
         */
        int[][] splitLayerPermutation = new int[d + 1][n];
        // copy assigned layer permutation (except the last one)
        int middleLayerIndex = (d + 1) / 2;
        for (int i = 0; i < (int) Math.ceil((double) logN / logT); i++) {
            int leftIndex = i == middleLayerIndex - 1 ? 0 : logN - (i + 1) * logT;
            int rightIndex = i == middleLayerIndex - 1 ? level : logN + (i + 1) * logT - 1;
            System.arraycopy(eachLayerPermutation[leftIndex], 0, splitLayerPermutation[middleLayerIndex - i - 1], 0, n);
            System.arraycopy(eachLayerPermutation[rightIndex], 0, splitLayerPermutation[middleLayerIndex + i], 0, n);
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

    /**
     * Permutations.
     *
     * @param inputGroups input groups.
     * @param i           layer index.
     * @return output groups.
     */
    @Override
    public byte[][][] permutation(byte[][][] inputGroups, int i) {
        Preconditions.checkArgument(setPermutation, "Please set permutation");
        MathPreconditions.checkNonNegativeInRange("i", i, d);
        int currentG = (i == 0 || i == d - 1) ? gInFirstLayer : g;
        int currentT = (i == 0 || i == d - 1) ? tInFirstLayer : t;
        MathPreconditions.checkEqual("g", "input_groups.length", currentG, inputGroups.length);
        byte[][][] outputGroups = new byte[currentG][currentT][];
        for (int j = 0; j < currentG; j++) {
            MathPreconditions.checkEqual("t", "input_groups[" + j + "].length", currentT, inputGroups[j].length);
            outputGroups[j] = PermutationNetworkUtils.permutation(subPermutations[i][j], inputGroups[j]);
        }
        return outputGroups;
    }

    /**
     * Permutations.
     *
     * @param inputVector input vector.
     * @param i           layer index.
     * @return output vector.
     */
    @Override
    public byte[][] permutation(byte[][] inputVector, int i) {
        Preconditions.checkArgument(setPermutation, "Please set permutation");
        MathPreconditions.checkEqual("n", "input_vector.length", n, inputVector.length);
        MathPreconditions.checkNonNegativeInRange("i", i, d);
        byte[][][] inputGroups = splitVector(inputVector, i);
        byte[][][] outputGroups = permutation(inputGroups, i);
        return combineGroups(outputGroups, i);
    }

    /**
     * Gets the number of groups in each layer, i.e, N / T.
     *
     * @param level the index of level
     * @return the number of groups in each layer.
     */
    @Override
    public int getG(int level) {
        return level == 0 || level == d - 1 ? gInFirstLayer : g;
    }

    /**
     * Gets T, the sub-permutation size.
     *
     * @param level the index of level
     * @return T.
     */
    @Override
    public int getT(int level) {
        return level == 0 || level == d - 1 ? tInFirstLayer : t;
    }
}
