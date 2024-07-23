package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory.BenesNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

/**
 * JDK Benes network. The implementation is inspired by:
 * <p><a href="https://github.com/osu-crypto/PSI-analytics/blob/master/psi_analytics_eurocrypt19/common/benes.cpp">...</a></p>
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
@SuppressWarnings({"AlibabaUndefineMagicConstant", "AlibabaAvoidNegationOperator"})
class JdkBenesNetwork<T> extends AbstractBenesNetwork<T> {
    /**
     * Creates a network.
     *
     * @param permutationMap permutation map.
     */
    JdkBenesNetwork(final int[] permutationMap) {
        super(permutationMap);
        // iteratively create the Benes network
        genBenesRoute(permutationMap);
        // update widths
        updateWidths();
    }

    /**
     * Creates a network by directly setting the network.
     *
     * @param n       number of inputs.
     * @param network network.
     */
    JdkBenesNetwork(int n, final byte[][] network) {
        super(n, network);
    }

    private void genBenesRoute(final int[] permutationMap) {
        int logN = LongUtils.ceilLog2(n);
        genBenesRoute(logN, 0, 0, permutationMap);
    }

    private void genBenesRoute(int subLogN, int levelIndex, int permIndex, int[] perms) {
        int subN = perms.length;
        if (subN == 2) {
            assert (subLogN == 1 || subLogN == 2);
            genSingleLevel(subLogN, levelIndex, permIndex, perms);
        } else if (subN == 3) {
            assert subLogN == 2;
            genTripleLevel(levelIndex, permIndex, perms);
        } else {
            int subLevel = 2 * subLogN - 1;
            // top subnetwork map, with size Math.floor(n / 2)
            int subTopN = subN / 2;
            // bottom subnetwork map, with size Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            // create forward/backward lookup tables
            int[] invPerms = new int[subN];
            IntStream.range(0, subN).forEach(i -> invPerms[perms[i]] = i);
            // path, initialized by -1, we use 2 for empty node
            int[] path = new int[subN];
            Arrays.fill(path, -1);
            // handling odd n
            if (subN % 2 == 1) {
                // the last node directly links to the bottom subnetwork.
                path[subN - 1] = 1;
                path[perms[subN - 1]] = 1;
                // if values - 1 == perm[values - 1], then the last one is also a direct link. Handle other cases.
                if (perms[subN - 1] != subN - 1) {
                    int idx = perms[invPerms[subN - 1] ^ 1];
                    depthFirstSearch(path, perms, invPerms, idx);
                }
            }
            // set other switches
            for (int i = 0; i < subN; ++i) {
                if (path[i] < 0) {
                    depthFirstSearch(path, perms, invPerms, i);
                }
            }
            // create the subnetworks.
            int[] subTopDests = new int[subTopN];
            int[] subBottomDests = new int[subBottomN];
            byte[] leftNet = network[levelIndex];
            byte[] rightNet = network[levelIndex + subLevel - 1];
            for (int i = 0, partSrcIndex = 0; i < subN - 1; partSrcIndex++) {
                leftNet[permIndex + partSrcIndex] = (byte) path[i];
                // 对应的index是不是来自于上半个网络
                int rightFromTop = path[perms[i]];
                rightNet[permIndex + partSrcIndex] = (byte) rightFromTop;
                if(rightFromTop == 0){
                    subTopDests[partSrcIndex] = perms[i++] >> 1;
                    subBottomDests[partSrcIndex] = perms[i++] >> 1;
                }else{
                    subBottomDests[partSrcIndex] = perms[i++] >> 1;
                    subTopDests[partSrcIndex] = perms[i++] >> 1;
                }
            }
            // add one more switch for the odd case.
            if (subN % 2 == 1) {
                subBottomDests[subN / 2] = perms[subN - 1] >> 1;
            }
            if (parallel && n > PermutationNetworkFactory.PARALLEL_THRESHOLD && forkJoinPool.getParallelism() - forkJoinPool.getActiveThreadCount() > 0) {
                ForkJoinTask<?> topTask = forkJoinPool.submit(() ->
                    genBenesRoute(subLogN - 1, levelIndex + 1, permIndex, subTopDests));
                ForkJoinTask<?> subTask = forkJoinPool.submit(() ->
                    genBenesRoute(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomDests)
                );
                topTask.join();
                subTask.join();
            } else {
                // create top subnetwork, with (log(N) - 1) levels
                genBenesRoute(subLogN - 1, levelIndex + 1, permIndex, subTopDests);
                // create bottom subnetwork with (log(N) - 1) levels.
                genBenesRoute(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomDests);
            }
        }
    }

    private void genSingleLevel(int subLogN, int levelIndex, int permIndex, int[] subDests) {
        if (subLogN == 1) {
            // logN == 1, we have 2 * log(N) - 1 = 1 level (█)
            network[levelIndex][permIndex] = !(subDests[0] == 0) ? (byte) 1 : (byte) 0;
        } else {
            // logN == 2，we have 2 * logN - 1 = 3 levels (□ █ □).
            network[levelIndex][permIndex] = 2;
            network[levelIndex + 1][permIndex] = !(subDests[0] == 0) ? (byte) 1 : (byte) 0;
            network[levelIndex + 2][permIndex] = 2;
        }
    }

    private void genTripleLevel(int levelIndex, int permIndex, int[] subDests) {
        if (subDests[0] == 0) {
            /*
             * 0 -> 0，1 -> 1，2 -> 2, the network is:
             * █ □ █ = 0   0
             * □ █ □     0
             *
             * 0 -> 0，1 -> 2，2 -> 1, the network is:
             * █ □ █ = 0   0
             * □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = !(subDests[1] == 1) ? (byte) 1 : (byte) 0;
            network[levelIndex + 2][permIndex] = 0;
        } else if (subDests[1] == 0) {
            /*
             * 0 -> 1，1 -> 0，2 -> 2, the network is:
             * █ □ █ = 0   1
             * □ █ □     0
             *
             * 0 -> 1，1 -> 2，2 -> 0, the network is:
             * █ □ █ = 0   1
             * □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = !(subDests[0] == 1) ? (byte) 1 : (byte) 0;
            network[levelIndex + 2][permIndex] = 1;
        } else {
            /*
             * 0 -> 2，1 -> 0，2 -> 1, the network is:
             * █ □ █ = 1   0
             * □ █ □     1
             *
             * 0 -> 2，1 -> 1，2 -> 0, the network is:
             * █ □ █ = 1   1
             * □ █ □     1
             */
            network[levelIndex][permIndex] = 1;
            network[levelIndex + 1][permIndex] = 1;
            network[levelIndex + 2][permIndex] = !(subDests[0] == 1) ? (byte) 1 : (byte) 0;
        }
    }

    private void depthFirstSearch(int[] path, int[] perms, int[] invPerms, int idx) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{idx, 0});
        while (!stack.empty()) {
            int[] pair = stack.pop();
            path[pair[0]] = pair[1];
            // if the next item in the vertical array is unassigned
            int neighbor = pair[0] ^ 1;
            if (path[neighbor] < 0) {
                // the next item is always assigned the opposite of this item,
                // unless it was part of path/cycle of previous node
                stack.push(new int[]{neighbor, pair[1] ^ 1});
            }
            idx = perms[invPerms[pair[0]] ^ 1];
            if (path[idx] < 0) {
                stack.push(new int[]{idx, pair[1] ^ 1});
            }
        }
    }

    @Override
    public BenesNetworkType getBenesType() {
        return BenesNetworkType.JDK;
    }

    @Override
    public PermutationNetworkType getType() {
        return PermutationNetworkType.BENES_JDK;
    }
}
