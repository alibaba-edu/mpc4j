package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

import java.util.Arrays;
import java.util.Stack;
import java.util.stream.IntStream;

/**
 * JDK Waksman network.
 *
 * @author Weiran Liu
 * @date 2024/3/21
 */
@SuppressWarnings({"AlibabaUndefineMagicConstant", "AlibabaAvoidNegationOperator", "AlibabaMethodTooLong"})
class JdkWaksmanNetwork<T> extends AbstractWaksmanNetwork<T> {
    /**
     * Creates a network.
     *
     * @param permutationMap permutation map.
     */
    JdkWaksmanNetwork(final int[] permutationMap) {
        super(permutationMap);
        // creates the source map
        int[] sourceMap = IntStream.range(0, n).toArray();
        // iteratively create the Benes network
        genWaksmanRoute(sourceMap, permutationMap);
        // update widths
        updateWidths();
    }

    /**
     * Creates a network by directly setting the network.
     *
     * @param n       number of inputs.
     * @param network network.
     */
    JdkWaksmanNetwork(final int n, final byte[][] network) {
        super(n, network);
    }

    private void genWaksmanRoute(final int[] sourceMap, final int[] permutationMap) {
        int logN = LongUtils.ceilLog2(n);
        genWaksmanRoute(logN, 0, 0, sourceMap, permutationMap);
    }

    private void genWaksmanRoute(int subLogN, int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        int subN = subSrcs.length;
        if (subN == 2) {
            assert (subLogN == 1 || subLogN == 2);
            if (subLogN == 1) {
                genSingleLevel(levelIndex, permIndex, subSrcs, subDests);
            } else {
                genPadSingleLevel(levelIndex, permIndex, subSrcs, subDests);
            }
        } else if (subN == 3) {
            assert subLogN == 2;
            genTripleLevel(levelIndex, permIndex, subSrcs, subDests);
        } else if (subN == 4) {
            assert (subLogN == 2 || subLogN == 3);
            if (subLogN == 2) {
                genQuadrupleLevel(levelIndex, permIndex, subSrcs, subDests);
            } else {
                genPadQuadrupleLevel(levelIndex, permIndex, subSrcs, subDests);
            }
        } else {
            int subLevel = 2 * subLogN - 1;
            // top subnetwork map, with size Math.floor(n / 2)
            int subTopN = subN / 2;
            // bottom subnetwork map, with size Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            // create forward/backward lookup tables
            // subSrcList stores the position map. For example, src = [2, 4, 6], dest = [6, 4, 2].
            // We re-organize the map to the form [0, subN - 1) -> [0, subN - 1)
            int[] fullPerms = new int[n];
            int[] fullInvPerms = new int[n];
            IntStream.range(0, subN).forEach(i -> fullInvPerms[subSrcs[i]] = i);
            IntStream.range(0, subN).forEach(i -> fullPerms[i] = fullInvPerms[subDests[i]]);
            IntStream.range(0, subN).forEach(i -> fullInvPerms[fullPerms[i]] = i);
            // shorten the array
            int[] perms = Arrays.copyOf(fullPerms, subN);
            int[] invPerms = Arrays.copyOf(fullInvPerms, subN);
            // path, initialized by -1, we use 2 for empty node
            int[] path = new int[subN];
            Arrays.fill(path, -1);
            if (subN % 2 == 1) {
                // handling odd n, the last node directly links to the bottom subnetwork.
                path[subN - 1] = 1;
                path[perms[subN - 1]] = 1;
                // if values - 1 == perm[values - 1], then the last one is also a direct link. Handle other cases.
                if (perms[subN - 1] != subN - 1) {
                    int idx = perms[invPerms[subN - 1] ^ 1];
                    depthFirstSearch(path, perms, invPerms, idx);
                }
            } else {
                // handling even n
                evenDepthFirstSearch(path, perms, invPerms);
            }
            // set other switches
            for (int i = 0; i < subN; ++i) {
                if (path[i] < 0) {
                    depthFirstSearch(path, perms, invPerms, i);
                }
            }
            // create left part of the network.
            TIntList subTopSrcQueue = new TIntLinkedList();
            TIntList subBottomSrcQueue = new TIntLinkedList();
            for (int i = 0; i < subN - 1; i += 2) {
                network[levelIndex][permIndex + i / 2] = (byte) path[i];
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ path[i], subLogN);
                    if (x < subN / 2) {
                        subTopSrcQueue.add(subSrcs[i | j]);
                    } else {
                        subBottomSrcQueue.add(subSrcs[i | j]);
                    }
                }
            }
            if (subN % 2 == 1) {
                // add one more switch for the odd case.
                subBottomSrcQueue.add(subSrcs[subN - 1]);
            }
            // create right part of the subnetwork.
            int[] subTopDests = new int[subTopN];
            int[] subBottomDests = new int[subBottomN];
            for (int i = 0; i < subN - 1; i += 2) {
                network[levelIndex + subLevel - 1][permIndex + i / 2] = (byte) path[perms[i]];
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ path[perms[i]], subLogN);
                    if (x < subN / 2) {
                        subTopDests[i / 2] = subSrcs[perms[i | j]];
                    } else {
                        subBottomDests[i / 2] = subSrcs[perms[i | j]];
                    }
                }
            }
            if (subN % 2 == 1) {
                // add one more switch for the odd case.
                subBottomDests[subN / 2] = subDests[subN - 1];
            } else {
                // remove one switch for the even case.
                network[levelIndex + subLevel - 1][permIndex + subN / 2 - 1] = 2;
            }
            int[] subTopSrcs = subTopSrcQueue.toArray();
            int[] subBottomSrcs = subBottomSrcQueue.toArray();
            // create top subnetwork, with (log(N) - 1) levels
            genWaksmanRoute(subLogN - 1, levelIndex + 1, permIndex, subTopSrcs, subTopDests);
            // create bottom subnetwork with (log(N) - 1) levels.
            genWaksmanRoute(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomSrcs, subBottomDests);
        }
    }

    private void genSingleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        // logN == 1, we have 2 * log(N) - 1 = 1 level (█)
        network[levelIndex][permIndex] = !(subSrcs[0] == subDests[0]) ? (byte) 1 : (byte) 0;
    }

    private void genPadSingleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        // logN == 2，we have 2 * logN - 1 = 3 levels (□ █ □).
        network[levelIndex][permIndex] = 2;
        network[levelIndex + 1][permIndex] = !(subSrcs[0] == subDests[0]) ? (byte) 1 : (byte) 0;
        network[levelIndex + 2][permIndex] = 2;
    }

    private void genTripleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        if (subSrcs[0] == subDests[0]) {
            /*
             * [0, 1, 2] -> [0, 1, 2], █ □ █ = 0   0
             *                         □ █ □     0
             *
             * [0, 1, 2] -> [0, 2, 1], █ □ █ = 0   0
             *                         □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = !(subSrcs[1] == subDests[1]) ? (byte) 1 : (byte) 0;
            network[levelIndex + 2][permIndex] = 0;
        } else if (subSrcs[0] == subDests[1]) {
            /*
             * [0, 1, 2] -> [1, 0, 2], █ □ █ = 0   1
             *                         □ █ □     0
             *
             * [0, 1, 2] -> [1, 2, 0], █ □ █ = 0   1
             *                         □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = !(subSrcs[1] == subDests[0]) ? (byte) 1 : (byte) 0;
            network[levelIndex + 2][permIndex] = 1;
        } else {
            /*
             * [0, 1, 2] -> [2, 0, 1], █ □ █ = 1   0
             *                         □ █ □     1
             *
             * [0, 1, 2] -> [2, 1, 0], █ □ █ = 1   1
             *                         □ █ □     1
             */
            network[levelIndex][permIndex] = 1;
            network[levelIndex + 1][permIndex] = 1;
            network[levelIndex + 2][permIndex] = !(subSrcs[1] == subDests[0]) ? (byte) 1 : (byte) 0;
        }
    }

    private void genQuadrupleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        byte[] switches = genQuadrupleSwitches(subSrcs, subDests);
        network[levelIndex][permIndex] = switches[0];
        network[levelIndex][permIndex + 1] = switches[1];
        network[levelIndex + 1][permIndex] = switches[2];
        network[levelIndex + 1][permIndex + 1] = switches[3];
        network[levelIndex + 2][permIndex] = switches[4];
        network[levelIndex + 2][permIndex + 1] = 2;
    }

    private void genPadQuadrupleLevel(int levelIndex, int permIndex, int[] subSrcs, int[] subDests) {
        byte[] switches = genQuadrupleSwitches(subSrcs, subDests);
        network[levelIndex][permIndex] = switches[0];
        network[levelIndex][permIndex + 1] = switches[1];
        network[levelIndex + 1][permIndex] = 2;
        network[levelIndex + 1][permIndex + 1] = 2;
        network[levelIndex + 2][permIndex] = switches[2];
        network[levelIndex + 2][permIndex + 1] = switches[3];
        network[levelIndex + 3][permIndex] = 2;
        network[levelIndex + 3][permIndex + 1] = 2;
        network[levelIndex + 4][permIndex] = switches[4];
        network[levelIndex + 4][permIndex + 1] = 2;
    }

    private byte[] genQuadrupleSwitches(int[] subSrcs, int[] subDests) {
        assert subSrcs.length == 4;
        assert subDests.length == 4;
        if (subDests[0] == subSrcs[0]) {
            // [0, 1, 2, 3] -> [0, ?, ?, ?]
            if (subDests[1] == subSrcs[1]) {
                // [0, 1, 2, 3] -> [0, 1, ?, ?]
                if (subDests[2] == subSrcs[2]) {
                    /*
                     * [0, 1, 2, 3] -> [0, 1, 2, 3], █ █ █ = 0 0 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 0, 0, 0};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [0, 1, 3, 2], █ █ █ = 0 0 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 0, 0, 0};
                }
            } else if (subDests[1] == subSrcs[2]) {
                // [0, 1, 2, 3] -> [0, 2, ?, ?]
                if (subDests[2] == subSrcs[1]) {
                    /*
                     * [0, 1, 2, 3] -> [0, 2, 1, 3], █ █ █ = 1 1 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{1, 0, 1, 0, 1};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [0, 2, 3, 1], █ █ █ = 0 0 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 0, 1, 0};
                }
            } else {
                assert subDests[1] == subSrcs[3];
                // [0, 1, 2, 3] -> [0, 3, ?, ?]
                if (subDests[2] == subSrcs[1]) {
                    /*
                     * [0, 1, 2, 3] -> [0, 3, 1, 2], █ █ █ = 1 1 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{1, 1, 1, 0, 1};
                } else {
                    assert subDests[2] == subSrcs[2];
                    /*
                     * [0, 1, 2, 3] -> [0, 3, 2, 1], █ █ █ = 0 0 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 0, 1, 0};
                }
            }
        } else if (subDests[0] == subSrcs[1]) {
            // [0, 1, 2, 3] -> [1, ?, ?, ?]
            if (subDests[1] == subSrcs[0]) {
                // [0, 1, 2, 3] -> [1, 0, ?, ?]
                if (subDests[2] == subSrcs[2]) {
                    /*
                     * [0, 1, 2, 3] -> [1, 0, 2, 3], █ █ █ = 0 0 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 0, 0, 1};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [1, 0, 3, 2], █ █ █ = 0 0 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 0, 0, 1};
                }
            } else if (subDests[1] == subSrcs[2]) {
                // [0, 1, 2, 3] -> [1, 2, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [1, 2, 0, 3], █ █ █ = 0 1 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 1, 0, 1};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [1, 2, 3, 0], █ █ █ = 1 0 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{1, 1, 0, 1, 0};
                }
            } else {
                assert subDests[1] == subSrcs[3];
                // [0, 1, 2, 3] -> [1, 3, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [1, 3, 0, 2], █ █ █ = 0 1 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 1, 0, 1};
                } else {
                    assert subDests[2] == subSrcs[2];
                    /*
                     * [0, 1, 2, 3] -> [1, 3, 2, 0], █ █ █ = 1 0 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 0, 1, 0};
                }
            }
        } else if (subDests[0] == subSrcs[2]) {
            // [0, 1, 2, 3] -> [2, ?, ?, ?]
            if (subDests[1] == subSrcs[0]) {
                // [0, 1, 2, 3] -> [2, 0, ?, ?]
                if (subDests[2] == subSrcs[1]) {
                    /*
                     * [0, 1, 2, 3] -> [2, 0, 1, 3], █ █ █ = 1 1 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{1, 0, 1, 0, 0};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [2, 0, 3, 1], █ █ █ = 0 0 1
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 0, 1, 1};
                }
            } else if (subDests[1] == subSrcs[1]) {
                // [0, 1, 2, 3] -> [2, 1, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [2, 1, 0, 3], █ █ █ = 0 1 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 1, 0, 0};
                } else {
                    assert subDests[2] == subSrcs[3];
                    /*
                     * [0, 1, 2, 3] -> [2, 1, 3, 0], █ █ █ = 1 0 1
                     *                               █ █ □   1 1
                     */
                    return new byte[]{1, 1, 0, 1, 1};
                }
            } else {
                assert subDests[1] == subSrcs[3];
                // [0, 1, 2, 3] -> [2, 3, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [2, 3, 0, 1], █ █ █ = 0 1 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 1, 1, 0};
                } else {
                    assert subDests[2] == subSrcs[1];
                    /*
                     * [0, 1, 2, 3] -> [2, 3, 1, 0], █ █ █ = 1 1 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 1, 1, 0};
                }
            }
        } else {
            assert subDests[0] == subSrcs[3];
            // [0, 1, 2, 3] -> [3, ?, ?, ?]
            if (subDests[1] == subSrcs[0]) {
                // [0, 1, 2, 3] -> [3, 0, ?, ?]
                if (subDests[2] == subSrcs[1]) {
                    /*
                     * [0, 1, 2, 3] -> [3, 0, 1, 2], █ █ █ = 1 1 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{1, 1, 1, 0, 0};
                } else {
                    assert subDests[2] == subSrcs[2];
                    /*
                     * [0, 1, 2, 3] -> [3, 0, 2, 1], █ █ █ = 0 0 1
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 0, 1, 1};
                }
            } else if (subDests[1] == subSrcs[1]) {
                // [0, 1, 2, 3] -> [3, 1, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [3, 1, 0, 2], █ █ █ = 0 1 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 1, 0, 0};
                } else {
                    assert subDests[2] == subSrcs[2];
                    /*
                     * [0, 1, 2, 3] -> [3, 1, 2, 0], █ █ █ = 1 0 1
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 0, 1, 1};
                }
            } else {
                assert subDests[1] == subSrcs[2];
                // [0, 1, 2, 3] -> [3, 2, ?, ?]
                if (subDests[2] == subSrcs[0]) {
                    /*
                     * [0, 1, 2, 3] -> [3, 2, 0, 1], █ █ █ = 0 1 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 1, 1, 0};
                } else {
                    assert subDests[2] == subSrcs[1];
                    /*
                     * [0, 1, 2, 3] -> [3, 2, 1, 0], █ █ █ = 1 1 1
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 1, 1, 1};
                }
            }
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

    private void evenDepthFirstSearch(int[] path, int[] perms, int[] invPerms) {
        assert path.length > 4 && path.length % 2 == 0;
        // set the last path to be 0
        int idx = perms[path.length - 1];
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{idx, 1});
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
    public WaksmanNetworkType getWaksmanType() {
        return WaksmanNetworkType.JDK;
    }

    @Override
    public PermutationNetworkType getType() {
        return PermutationNetworkType.WAKSMAN_JDK;
    }
}
