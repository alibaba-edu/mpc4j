package edu.alibaba.mpc4j.common.tool.network.waksman;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

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
        // iteratively create the Benes network
        genWaksmanRoute(permutationMap);
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

    private void genWaksmanRoute(final int[] permutationMap) {
        int logN = LongUtils.ceilLog2(n);
        genWaksmanRoute(logN, 0, 0, permutationMap);
    }

    private void genWaksmanRoute(int subLogN, int levelIndex, int permIndex, int[] perms) {
        int subN = perms.length;
        if (subN == 2) {
            assert (subLogN == 1 || subLogN == 2);
            if (subLogN == 1) {
                genSingleLevel(levelIndex, permIndex, perms);
            } else {
                genPadSingleLevel(levelIndex, permIndex, perms);
            }
        } else if (subN == 3) {
            assert subLogN == 2;
            genTripleLevel(levelIndex, permIndex, perms);
        } else if (subN == 4) {
            assert (subLogN == 2 || subLogN == 3);
            if (subLogN == 2) {
                genQuadrupleLevel(levelIndex, permIndex, perms);
            } else {
                genPadQuadrupleLevel(levelIndex, permIndex, perms);
            }
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
            if (subN % 2 == 1) {
                // handling odd n, the last node directly links to the bottom subnetwork.
                path[subN - 1] = 1;
                path[perms[subN - 1]] = 1;
                // if values - 1 == perm[values - 1], then the last one is also a direct link. Handle other cases.
                if (perms[subN - 1] != subN - 1) {
                    int idx = perms[subN - 1] ^ 1;
                    depthFirstSearch(path, perms, invPerms, idx);
                }
            } else {
                int index = perms[subN - 1];
                path[index] = 1;
                depthFirstSearch(path, perms, invPerms, index ^ 1);
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
                if (rightFromTop == 0) {
                    subTopDests[partSrcIndex] = perms[i++] >> 1;
                    subBottomDests[partSrcIndex] = perms[i++] >> 1;
                } else {
                    subBottomDests[partSrcIndex] = perms[i++] >> 1;
                    subTopDests[partSrcIndex] = perms[i++] >> 1;
                }
            }
            if (subN % 2 == 1) {
                // add one more switch for the odd case.
                subBottomDests[subN / 2] = perms[subN - 1] >> 1;
            } else {
                // remove one switch for the even case.
                network[levelIndex + subLevel - 1][permIndex + subN / 2 - 1] = 2;
            }
            // create top subnetwork, with (log(N) - 1) levels
            genWaksmanRoute(subLogN - 1, levelIndex + 1, permIndex, subTopDests);
            // create bottom subnetwork with (log(N) - 1) levels.
            genWaksmanRoute(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomDests);
        }
    }

    private void genSingleLevel(int levelIndex, int permIndex, int[] subDests) {
        // logN == 1, we have 2 * log(N) - 1 = 1 level (█)
        network[levelIndex][permIndex] = subDests[0] == 0 ? (byte) 0 : (byte) 1;
    }

    private void genPadSingleLevel(int levelIndex, int permIndex, int[] subDests) {
        // logN == 2，we have 2 * logN - 1 = 3 levels (□ █ □).
        network[levelIndex][permIndex] = 2;
        network[levelIndex + 1][permIndex] = subDests[0] == 0 ? (byte) 0 : (byte) 1;
        network[levelIndex + 2][permIndex] = 2;
    }

    private void genTripleLevel(int levelIndex, int permIndex, int[] subDests) {
        if (subDests[0] == 0) {
            /*
             * [0, 1, 2] -> [0, 1, 2], █ □ █ = 0   0
             *                         □ █ □     0
             *
             * [0, 1, 2] -> [0, 2, 1], █ □ █ = 0   0
             *                         □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = subDests[1] == 1 ? (byte) 0 : (byte) 1;
            network[levelIndex + 2][permIndex] = 0;
        } else if (subDests[1] == 0) {
            /*
             * [0, 1, 2] -> [1, 0, 2], █ □ █ = 0   1
             *                         □ █ □     0
             *
             * [0, 1, 2] -> [1, 2, 0], █ □ █ = 0   1
             *                         □ █ □     1
             */
            network[levelIndex][permIndex] = 0;
            network[levelIndex + 1][permIndex] = subDests[0] == 1 ? (byte) 0 : (byte) 1;
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
            network[levelIndex + 2][permIndex] = subDests[0] == 1 ? (byte) 0 : (byte) 1;
        }
    }

    private void genQuadrupleLevel(int levelIndex, int permIndex, int[] subDests) {
        byte[] switches = genQuadrupleSwitches(subDests);
        network[levelIndex][permIndex] = switches[0];
        network[levelIndex][permIndex + 1] = switches[1];
        network[levelIndex + 1][permIndex] = switches[2];
        network[levelIndex + 1][permIndex + 1] = switches[3];
        network[levelIndex + 2][permIndex] = switches[4];
        network[levelIndex + 2][permIndex + 1] = 2;
    }

    private void genPadQuadrupleLevel(int levelIndex, int permIndex, int[] subDests) {
        byte[] switches = genQuadrupleSwitches(subDests);
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

    private byte[] genQuadrupleSwitches(int[] subDests) {
        assert subDests.length == 4;
        if (subDests[0] == 0) {
            // [0, 1, 2, 3] -> [0, ?, ?, ?]
            if (subDests[1] == 1) {
                // [0, 1, 2, 3] -> [0, 1, ?, ?]
                if (subDests[2] == 2) {
                    /*
                     * [0, 1, 2, 3] -> [0, 1, 2, 3], █ █ █ = 0 0 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 0, 0, 0};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [0, 1, 3, 2], █ █ █ = 0 0 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 0, 0, 0};
                }
            } else if (subDests[1] == 2) {
                // [0, 1, 2, 3] -> [0, 2, ?, ?]
                if (subDests[2] == 1) {
                    /*
                     * [0, 1, 2, 3] -> [0, 2, 1, 3], █ █ █ = 1 1 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{1, 0, 1, 0, 1};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [0, 2, 3, 1], █ █ █ = 0 0 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 0, 1, 0};
                }
            } else {
                assert subDests[1] == 3;
                // [0, 1, 2, 3] -> [0, 3, ?, ?]
                if (subDests[2] == 1) {
                    /*
                     * [0, 1, 2, 3] -> [0, 3, 1, 2], █ █ █ = 1 1 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{1, 1, 1, 0, 1};
                } else {
                    assert subDests[2] == 2;
                    /*
                     * [0, 1, 2, 3] -> [0, 3, 2, 1], █ █ █ = 0 0 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 0, 1, 0};
                }
            }
        } else if (subDests[0] == 1) {
            // [0, 1, 2, 3] -> [1, ?, ?, ?]
            if (subDests[1] == 0) {
                // [0, 1, 2, 3] -> [1, 0, ?, ?]
                if (subDests[2] == 2) {
                    /*
                     * [0, 1, 2, 3] -> [1, 0, 2, 3], █ █ █ = 0 0 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 0, 0, 1};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [1, 0, 3, 2], █ █ █ = 0 0 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 0, 0, 1};
                }
            } else if (subDests[1] == 2) {
                // [0, 1, 2, 3] -> [1, 2, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [1, 2, 0, 3], █ █ █ = 0 1 1
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 1, 0, 1};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [1, 2, 3, 0], █ █ █ = 1 0 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{1, 1, 0, 1, 0};
                }
            } else {
                assert subDests[1] == 3;
                // [0, 1, 2, 3] -> [1, 3, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [1, 3, 0, 2], █ █ █ = 0 1 1
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 1, 0, 1};
                } else {
                    assert subDests[2] == 2;
                    /*
                     * [0, 1, 2, 3] -> [1, 3, 2, 0], █ █ █ = 1 0 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 0, 1, 0};
                }
            }
        } else if (subDests[0] == 2) {
            // [0, 1, 2, 3] -> [2, ?, ?, ?]
            if (subDests[1] == 0) {
                // [0, 1, 2, 3] -> [2, 0, ?, ?]
                if (subDests[2] == 1) {
                    /*
                     * [0, 1, 2, 3] -> [2, 0, 1, 3], █ █ █ = 1 1 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{1, 0, 1, 0, 0};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [2, 0, 3, 1], █ █ █ = 0 0 1
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 0, 1, 1};
                }
            } else if (subDests[1] == 1) {
                // [0, 1, 2, 3] -> [2, 1, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [2, 1, 0, 3], █ █ █ = 0 1 0
                     *                               █ █ □   0 0
                     */
                    return new byte[]{0, 0, 1, 0, 0};
                } else {
                    assert subDests[2] == 3;
                    /*
                     * [0, 1, 2, 3] -> [2, 1, 3, 0], █ █ █ = 1 0 1
                     *                               █ █ □   1 1
                     */
                    return new byte[]{1, 1, 0, 1, 1};
                }
            } else {
                assert subDests[1] == 3;
                // [0, 1, 2, 3] -> [2, 3, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [2, 3, 0, 1], █ █ █ = 0 1 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 1, 1, 0};
                } else {
                    assert subDests[2] == 1;
                    /*
                     * [0, 1, 2, 3] -> [2, 3, 1, 0], █ █ █ = 1 1 0
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 1, 1, 0};
                }
            }
        } else {
            assert subDests[0] == 3;
            // [0, 1, 2, 3] -> [3, ?, ?, ?]
            if (subDests[1] == 0) {
                // [0, 1, 2, 3] -> [3, 0, ?, ?]
                if (subDests[2] == 1) {
                    /*
                     * [0, 1, 2, 3] -> [3, 0, 1, 2], █ █ █ = 1 1 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{1, 1, 1, 0, 0};
                } else {
                    assert subDests[2] == 2;
                    /*
                     * [0, 1, 2, 3] -> [3, 0, 2, 1], █ █ █ = 0 0 1
                     *                               █ █ □   0 1
                     */
                    return new byte[]{0, 0, 0, 1, 1};
                }
            } else if (subDests[1] == 1) {
                // [0, 1, 2, 3] -> [3, 1, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [3, 1, 0, 2], █ █ █ = 0 1 0
                     *                               █ █ □   1 0
                     */
                    return new byte[]{0, 1, 1, 0, 0};
                } else {
                    assert subDests[2] == 2;
                    /*
                     * [0, 1, 2, 3] -> [3, 1, 2, 0], █ █ █ = 1 0 1
                     *                               █ █ □   0 1
                     */
                    return new byte[]{1, 0, 0, 1, 1};
                }
            } else {
                assert subDests[1] == 2;
                // [0, 1, 2, 3] -> [3, 2, ?, ?]
                if (subDests[2] == 0) {
                    /*
                     * [0, 1, 2, 3] -> [3, 2, 0, 1], █ █ █ = 0 1 0
                     *                               █ █ □   1 1
                     */
                    return new byte[]{0, 1, 1, 1, 0};
                } else {
                    assert subDests[2] == 1;
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

    @Override
    public WaksmanNetworkType getWaksmanType() {
        return WaksmanNetworkType.JDK;
    }

    @Override
    public PermutationNetworkType getType() {
        return PermutationNetworkType.WAKSMAN_JDK;
    }
}
