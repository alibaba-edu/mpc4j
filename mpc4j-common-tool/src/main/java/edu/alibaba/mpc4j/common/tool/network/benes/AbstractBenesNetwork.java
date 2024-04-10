package edu.alibaba.mpc4j.common.tool.network.benes;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.Vector;

/**
 * abstract Benes network.
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
abstract class AbstractBenesNetwork<T> implements BenesNetwork<T> {
    /**
     * number of inputs
     */
    protected final int n;
    /**
     * level
     */
    protected final int level;
    /**
     * max width
     */
    protected final int maxWidth;
    /**
     * network
     */
    protected byte[][] network;
    /**
     * widths
     */
    private final int[] widths;

    /**
     * Creates a Benes network. The permutation is represented by an array. The length of the array is the number of
     * inputs, each number in the array represents a map. For example, [7, 4, 8, 6, 2, 1, 0, 3, 5] corresponds to map
     * a vector
     * <p>a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]</p>
     * to
     * <p>a[7], a[4], a[8], a[6], a[2], a[1], a[0], a[3], a[5]</p>
     *
     * @param permutationMap the permutation map.
     */
    AbstractBenesNetwork(final int[] permutationMap) {
        assert PermutationNetworkUtils.validPermutation(permutationMap);
        n = permutationMap.length;
        level = PermutationNetworkUtils.getLevel(n);
        maxWidth = PermutationNetworkUtils.getMaxWidth(n);
        network = new byte[level][maxWidth];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            Arrays.fill(network[levelIndex], (byte) -1);
        }
        widths = new int[level];
    }

    /**
     * Creates a Benes network by directly setting the network.
     *
     * @param network Waksman network.
     */
    AbstractBenesNetwork(final int n, final byte[][] network) {
        assert n > 1;
        this.n = n;
        // level must be an odd number
        assert network.length % 2 == 1;
        assert network.length == PermutationNetworkUtils.getLevel(n);
        level = network.length;
        maxWidth = network[0].length;
        // verify the network
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            assert network[levelIndex].length == maxWidth;
        }
        assert maxWidth == PermutationNetworkUtils.getMaxWidth(n);
        this.network = network;
        widths = new int[level];
        updateWidths();
    }

    protected void updateWidths() {
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            int width = 0;
            for (int widthIndex = 0; widthIndex < network[levelIndex].length; widthIndex++) {
                assert network[levelIndex][widthIndex] == -1 || network[levelIndex][widthIndex] == 0
                    || network[levelIndex][widthIndex] == 1 || network[levelIndex][widthIndex] == 2;
                if (network[levelIndex][widthIndex] == -1) {
                    network[levelIndex][widthIndex] = 2;
                }
                if (network[levelIndex][widthIndex] != 2) {
                    width++;
                }
            }
            widths[levelIndex] = width;
        }
    }

    @Override
    public byte[] getGates(int levelIndex) {
        return network[levelIndex];
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getMaxWidth() {
        return maxWidth;
    }

    @Override
    public int getWidth(int levelIndex) {
        return widths[levelIndex];
    }

    @Override
    public Vector<T> permutation(final Vector<T> inputVector) {
        assert inputVector.size() == n;
        int logN = LongUtils.ceilLog2(n);
        Vector<T> outputVector = new Vector<>(inputVector);
        permutation(logN, 0, 0, outputVector);

        return outputVector;
    }

    private void permutation(int subLogN, int levelIndex, int permIndex, Vector<T> subSrcs) {
        int subN = subSrcs.size();
        if (subN == 2) {
            assert (subLogN == 1 || subLogN == 2);
            permuteSingleLevel(subLogN, levelIndex, permIndex, subSrcs);
        } else if (subN == 3) {
            assert subLogN == 2;
            permuteTripleLevel(levelIndex, permIndex, subSrcs);
        } else {
            int subLevel = 2 * subLogN - 1;
            // top subnetwork map, with size Math.floor(n / 2)
            int subTopN = subN / 2;
            Vector<T> subTopSrcs = new Vector<>(subTopN);
            // bottom subnetwork map, with size Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            Vector<T> subBottomSrcs = new Vector<>(subBottomN);
            // evaluate left-part of the Benes network
            for (int i = 0; i < subN - 1; i += 2) {
                int s = network[levelIndex][permIndex + i / 2] == 1 ? 1 : 0;
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ s, subLogN);
                    if (x < subN / 2) {
                        subTopSrcs.add(subSrcs.elementAt(i | j));
                    } else {
                        subBottomSrcs.add(subSrcs.elementAt(i | j));
                    }
                }
            }
            // add more gate for the bottom subnetwork with an odd number of inputs.
            if (subN % 2 == 1) {
                subBottomSrcs.add(subSrcs.elementAt(subN - 1));
            }
            // iteratively evaluate the middle network
            permutation(subLogN - 1, levelIndex + 1, permIndex, subTopSrcs);
            permutation(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomSrcs);
            // evaluate right-part of the network
            for (int i = 0; i < subN - 1; i += 2) {
                int s = network[levelIndex + subLevel - 1][permIndex + i / 2] == 1 ? 1 : 0;
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ s, subLogN);
                    if (x < subN / 2) {
                        subSrcs.set(i | j, subTopSrcs.elementAt(x));
                    } else {
                        subSrcs.set(i | j, subBottomSrcs.elementAt(i / 2));
                    }
                }
            }
            // add more gate fot the bottom subnetwork there is an odd number of inputs.
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subSrcs.set(subN - 1, subBottomSrcs.elementAt(idx - 1));
            }
        }
    }

    private void permuteSingleLevel(int subLogN, int levelIndex, int permIndex, Vector<T> subSrcs) {
        if (subLogN == 1) {
            // level-1 single gate, the gate must be a switching gate (█)
            assert network[levelIndex][permIndex] != 2;
            // switch according to level-1 single gate (█)
            if (network[levelIndex][permIndex] == 1) {
                T temp = subSrcs.elementAt(0);
                subSrcs.set(0, subSrcs.elementAt(1));
                subSrcs.set(1, temp);
            }
        } else {
            // level-3 single gate, the left and the right gate must be an empty gate (□)
            assert (network[levelIndex][permIndex] == 2)
                && (network[levelIndex + 2][permIndex] == 2)
                && (network[levelIndex + 1][permIndex] != 2);
            // switch according to level-3 single gate (□ █ □)
            if (network[levelIndex + 1][permIndex] == 1) {
                T temp = subSrcs.elementAt(0);
                subSrcs.set(0, subSrcs.elementAt(1));
                subSrcs.set(1, temp);
            }
        }
    }

    private void permuteTripleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // level-3 triple gates, all gates must be in the form (█ □ █)
        //                                                      □ █ □
        assert (network[levelIndex][permIndex] != 2)
            && (network[levelIndex + 1][permIndex] != 2)
            && (network[levelIndex + 2][permIndex] != 2);
        // switch according to level-3 triple gate (█ □ █)
        //                                          □ █ □
        if (network[levelIndex][permIndex] == 1) {
            T temp = subSrcs.elementAt(0);
            subSrcs.set(0, subSrcs.elementAt(1));
            subSrcs.set(1, temp);
        }
        if (network[levelIndex + 1][permIndex] == 1) {
            T temp = subSrcs.elementAt(1);
            subSrcs.set(1, subSrcs.elementAt(2));
            subSrcs.set(2, temp);
        }
        if (network[levelIndex + 2][permIndex] == 1) {
            T temp = subSrcs.elementAt(0);
            subSrcs.set(0, subSrcs.elementAt(1));
            subSrcs.set(1, temp);
        }
    }

    /**
     * Cyclic shift right (i mod N) for N = 2^n and 0 <= i < N. For example:
     * <p>i = 00010011, log(N) = 8, rightCycleShift(i, n) = 10001001.</p>
     *
     * @param num  the integer i.
     * @param logN n = log(N).
     * @return cyclic shift right result.
     */
    protected int rightCycleShift(int num, int logN) {
        return ((num & 1) << (logN - 1)) | (num >> 1);
    }
}
