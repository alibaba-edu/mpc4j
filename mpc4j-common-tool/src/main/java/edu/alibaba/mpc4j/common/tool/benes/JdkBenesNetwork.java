package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkFactory.BenesNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * JDK贝奈斯网络（Benes Network）。论文来源：
 * Chang C, Melhem R. Arbitrary size benes networks[J]. Parallel Processing Letters, 1997, 7(03): 279-284.
 * <p>
 * 实现参考：
 * https://github.com/osu-crypto/PSI-analytics/blob/master/psi_analytics_eurocrypt19/common/benes.cpp
 *
 * @author Weiran Liu
 * @date 2021/09/18
 */
class JdkBenesNetwork<T> extends AbstractBenesNetwork<T> {
    /**
     * 构建JDK贝奈斯网络。
     *
     * @param permutationMap 置换表。
     */
    JdkBenesNetwork(final int[] permutationMap) {
        super(permutationMap);
        // 初始化网络
        network = new boolean[level][n / 2];
        // 构建原点列表
        int[] sourceMap = IntStream.range(0, n).toArray();
        // 迭代构建Benes网络
        genBenesRoute(sourceMap, permutationMap);
    }

    private void genBenesRoute(final int[] sourceMap, final int[] permutationMap) {
        // 初始化迭代过程中的全局变量
        int logN = (int) Math.ceil(DoubleUtils.log2(n));
        genBenesRoute(logN, 0, 0, sourceMap, permutationMap);
    }

    private void genBenesRoute(int subLogN, int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        int subN = subSrcs.length;
        if (subN == 2) {
            assert (subLogN == 1 || subLogN == 2);
            genSingleLevel(subLogN, levelIndex, permIndex, subSrcs, subDests);
        } else if (subN == 3) {
            assert subLogN == 2;
            genTripleLevel(levelIndex, permIndex, subSrcs, subDests);
        } else {
            int subLevel = 2 * subLogN - 1;
            // 上方子Benes网络的输入和输出映射表，大小为Math.floor(n / 2)
            int subTopN = subN / 2;
            // 下方子Benes网络的输入和输出映射表，大小为（Math.ceil(n / 2)）
            int subBottomN = subN - subTopN;
            // 构建正向/反向查找表
            // subSrcList中存储的是位置标签，例如，如果src = [2, 4, 6]，dest = [6, 4, 2]，则左侧2的位置0的元素要换到右侧2的位置2上
            // 为了获得正确的映射，需要先整理映射表，将其修改为[0, subN - 1) -> [0, subN - 1)的格式
            int[] fullPerms = new int[n];
            int[] fullInvPerms = new int[n];
            IntStream.range(0, subN).forEach(i -> fullInvPerms[subSrcs[i]] = i);
            IntStream.range(0, subN).forEach(i -> fullPerms[i] = fullInvPerms[subDests[i]]);
            IntStream.range(0, subN).forEach(i -> fullInvPerms[fullPerms[i]] = i);
            // 整理好格式后，再把数组长度剪短
            int[] perms = Arrays.copyOf(fullPerms, subN);
            int[] invPerms = Arrays.copyOf(fullInvPerms, subN);
            // 路径，初始时均设置为-1
            int[] path = new int[subN];
            Arrays.fill(path, -1);
            // 如果n为奇数，则最后一个节点需要特殊处理
            if (subN % 2 == 1) {
                // 最后一个点和lower sub-network是直连，所以path[values - 1] = 1，且perm的最后一个也为1
                path[subN - 1] = 1;
                path[perms[subN - 1]] = 1;
                if (perms[subN - 1] != subN - 1) {
                    // 如果values - 1 = perm[values - 1]，意味着最后一个映射是直连的，不涉及到其他点；否则要特殊处理
                    int idx = perms[invPerms[subN - 1] ^ 1];
                    depthFirstSearch(path, perms, invPerms, idx);
                }
            }
            // 设置其他节点
            for (int i = 0; i < subN; ++i) {
                if (path[i] < 0) {
                    depthFirstSearch(path, perms, invPerms, i);
                }
            }
            // 构建子Benes网络的输入
            Queue<Integer> subTopSrcQueue = new LinkedList<>();
            Queue<Integer> subBottomSrcQueue = new LinkedList<>();
            for (int i = 0; i < subN - 1; i += 2) {
                network[levelIndex][permIndex + i / 2] = (path[i] == 1);
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ path[i], subLogN);
                    if (x < subN / 2) {
                        subTopSrcQueue.add(subSrcs[i | j]);
                    } else {
                        subBottomSrcQueue.add(subSrcs[i | j]);
                    }
                }
            }
            // 如果是奇数，需要在下方增加一个直连节点
            if (subN % 2 == 1) {
                subBottomSrcQueue.add(subSrcs[subN - 1]);
            }
            // 构建子Benes网络的输出
            int[] subTopDests = new int[subTopN];
            int[] subBottomDests = new int[subBottomN];
            for (int i = 0; i < subN - 1; i += 2) {
                network[levelIndex + subLevel - 1][permIndex + i / 2] = (path[perms[i]] == 1);
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ path[perms[i]], subLogN);
                    if (x < subN / 2) {
                        subTopDests[x] = subSrcs[perms[i | j]];
                    } else {
                        subBottomDests[i / 2] = subSrcs[perms[i | j]];
                    }
                }
            }
            int idx = (int) (Math.ceil(subN * 0.5));
            // 如果是奇数，要在下方增加一个直连节点
            if (subN % 2 == 1) {
                subBottomDests[idx - 1] = subDests[subN - 1];
            }
            int[] subTopSrcs = subTopSrcQueue.stream().mapToInt(src -> src).toArray();
            int[] subBottomSrcs = subBottomSrcQueue.stream().mapToInt(src -> src).toArray();
            // 构建上方的子Benes Network，包含logN - 1层，这logN - 1层位于上一层加1的位置。
            genBenesRoute(subLogN - 1, levelIndex + 1, permIndex, subTopSrcs, subTopDests);
            // 构建下方的子Benes Network，包含logN - 1层，这logN - 1层位于上一层加1的位置。
            genBenesRoute(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomSrcs, subBottomDests);
        }
    }

    /**
     * 生成单层网络。虽然是单层网络，但实际可能包含三层，这样才能与标准三层网络的层数保持一致。
     *
     * @param subLogN    当前logN，只可能等于1（单层）或等于2（三层）。
     * @param levelIndex 层数索引值。
     * @param permIndex  置换索引值。
     * @param subSrcs    原点表。
     * @param subDests   置换表。
     */
    private void genSingleLevel(int subLogN, int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        // 当N = 2时，只有2个输入和输出，网络只包含1个交换门，但层数要求有两种情况
        if (subLogN == 1) {
            // logN = 1，有2 * logN - 1 = 1层交换门(█)，是否交换取决于输入和输出的[2] -> [2]映射
            network[levelIndex][permIndex] = !(subSrcs[0] == subDests[0]);
        } else {
            // logN = 2，有2 * logN - 1 = 3层交换门(█ █ █），此时只设置中间的交换门，取决于输入和输出的[2] -> [2]映射
            network[levelIndex + 1][permIndex] = !(subSrcs[0] == subDests[0]);
        }
    }

    /**
     * 生成三层网络。
     *
     * @param levelIndex 层索引值。
     * @param permIndex  置换索引值。
     * @param subSrcs    原点表。
     * @param subDests   置换表。
     */
    private void genTripleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        /*
         * N = 3时的网络结构为（█表示交换门、□表示直连门）：
         * █ □ █
         * □ █ □
         */
        if (subSrcs[0] == subDests[0]) {
            /*
             * 0 -> 0，1 -> 1，2 -> 2，网络结构为：
             * █ □ █ = 0   0
             * □ █ □     0
             *
             * 0 -> 0，1 -> 2，2 -> 1，网络结构为：
             * █ □ █ = 0   0
             * □ █ □     1
             */
            network[levelIndex][permIndex] = false;
            network[levelIndex + 1][permIndex] = !(subSrcs[1] == subDests[1]);
            network[levelIndex + 2][permIndex] = false;
        } else if (subSrcs[0] == subDests[1]) {
            /*
             * 0 -> 1，1 -> 0，2 -> 2，网络结构为：
             * █ □ █ = 0   1
             * □ █ □     0
             *
             * 0 -> 1，1 -> 2，2 -> 0，网络结构为：
             * █ □ █ = 0   1
             * □ █ □     1
             */
            network[levelIndex][permIndex] = false;
            network[levelIndex + 1][permIndex] = !(subSrcs[1] == subDests[0]);
            network[levelIndex + 2][permIndex] = true;
        } else {
            /*
             * 0 -> 2，1 -> 0，2 -> 1，网络结构为：
             * █ □ █ = 1   0
             * □ █ □     1
             *
             * 0 -> 2，1 -> 1，2 -> 0，网络结构为：
             * █ □ █ = 1   1
             * □ █ □     1
             */
            network[levelIndex][permIndex] = true;
            network[levelIndex + 1][permIndex] = true;
            network[levelIndex + 2][permIndex] = !(subSrcs[1] == subDests[0]);
        }
    }

    private void depthFirstSearch(int[] path, int[] perms, int[] invPerms, int idx) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{idx, 0});
        while (!stack.empty()) {
            int[] pair = stack.pop();
            path[pair[0]] = pair[1];
            // if the next item in the vertical array is unassigned
            if (path[pair[0] ^ 1] < 0) {
                // the next item is always assigned the opposite of this item,
                // unless it was part of path/cycle of previous node
                stack.push(new int[]{pair[0] ^ 1, pair[1] ^ 1});
            }
            idx = perms[invPerms[pair[0]] ^ 1];
            if (path[idx] < 0) {
                stack.push(new int[]{idx, pair[1] ^ 1});
            }
        }
    }

    @Override
    public BenesNetworkType getBenesNetworkType() {
        return BenesNetworkType.JDK_BENES_NETWORK;
    }
}
