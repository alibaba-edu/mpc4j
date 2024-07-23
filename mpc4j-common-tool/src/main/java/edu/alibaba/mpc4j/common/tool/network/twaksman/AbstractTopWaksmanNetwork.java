package edu.alibaba.mpc4j.common.tool.network.twaksman;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetwork;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;

/**
 * Waksman network but assign values according to the highest bit
 *
 * @author Feng Han
 * @date 2024/6/14
 */
abstract class AbstractTopWaksmanNetwork<T> implements PermutationNetwork<T> {
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
     * parallel
     */
    protected boolean parallel = true;
    /**
     * thread pool
     */
    protected ForkJoinPool forkJoinPool;

    /**
     * Creates a Waksman network. The permutation is represented by an array. The length of the array is the number of
     * inputs, each number in the array represents a map. For example, [7, 4, 8, 6, 2, 1, 0, 3, 5] corresponds to map
     * a vector
     * <p>a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]</p>
     * to
     * <p>a[7], a[4], a[8], a[6], a[2], a[1], a[0], a[3], a[5]</p>
     *
     * @param permutationMap the permutation map.
     */
    AbstractTopWaksmanNetwork(final int[] permutationMap) {
        assert PermutationNetworkUtils.validPermutation(permutationMap);
        n = permutationMap.length;
        level = PermutationNetworkUtils.getLevel(n);
        maxWidth = PermutationNetworkUtils.getMaxWidth(n);
        network = new byte[level][maxWidth];
        for (int levelIndex = 0; levelIndex < level; levelIndex++) {
            Arrays.fill(network[levelIndex], (byte) -1);
        }
        widths = new int[level];
        forkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
    }

    /**
     * Creates a Waksman network by directly setting the network.
     *
     * @param n       number of inputs.
     * @param network Waksman network.
     */
    AbstractTopWaksmanNetwork(final int n, final byte[][] network) {
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
        forkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
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
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
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
            if (subLogN == 1) {
                permuteSingleLevel(levelIndex, permIndex, subSrcs);
            } else {
                permutePadSingleLevel(levelIndex, permIndex, subSrcs);
            }
        } else if (subN == 3) {
            assert subLogN == 2;
            permuteTripleLevel(levelIndex, permIndex, subSrcs);
        } else if (subN == 4) {
            assert (subLogN == 2 || subLogN == 3);
            if (subLogN == 2) {
                permuteQuadrupleLevel(levelIndex, permIndex, subSrcs);
            } else {
                permutePadQuadrupleLevel(levelIndex, permIndex, subSrcs);
            }
        } else {
            int subLevel = 2 * subLogN - 1;
            // top subnetwork map, with size Math.floor(n / 2)
            int subTopN = subN / 2;
            Vector<T> subTopSrcs = new Vector<>(subTopN);
            // bottom subnetwork map, with size Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            Vector<T> subBottomSrcs = new Vector<>(subBottomN);
            // evaluate left-part of the network
            for (int halfIndex = 0; halfIndex < subTopN; halfIndex++) {
                if (network[levelIndex][permIndex + halfIndex] == 1) {
                    subTopSrcs.add(subSrcs.elementAt(halfIndex + subTopN));
                    subBottomSrcs.add(subSrcs.elementAt(halfIndex));
                } else {
                    subTopSrcs.add(subSrcs.elementAt(halfIndex));
                    subBottomSrcs.add(subSrcs.elementAt(halfIndex + subTopN));
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
            byte[] net = network[levelIndex + subLevel - 1];
            for (int halfIndex = 0; halfIndex < subTopN; halfIndex++) {
                if (net[permIndex + halfIndex] == 1) {
                    subSrcs.set(halfIndex + subTopN, subTopSrcs.elementAt(halfIndex));
                    subSrcs.set(halfIndex, subBottomSrcs.elementAt(halfIndex));
                } else {
                    subSrcs.set(halfIndex, subTopSrcs.elementAt(halfIndex));
                    subSrcs.set(halfIndex + subTopN, subBottomSrcs.elementAt(halfIndex));
                }
            }
            // add more gate fot the bottom subnetwork there is an odd number of inputs.
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subSrcs.set(subN - 1, subBottomSrcs.elementAt(idx - 1));
            }
        }
    }

    private void permuteSingleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // level-1 single gate, the gate must be a switching gate (█)
        assert network[levelIndex][permIndex] != 2;
        // switch according to level-1 single gate (█)
        if (network[levelIndex][permIndex] == 1) {
            T temp0 = subSrcs.elementAt(0);
            T temp1 = subSrcs.elementAt(1);
            subSrcs.set(0, temp1);
            subSrcs.set(1, temp0);
        }
    }

    private void permutePadSingleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // level-3 single gate, the left and the right gate must be an empty gate (□)
        assert (network[levelIndex][permIndex] == 2)
            && (network[levelIndex + 2][permIndex] == 2)
            && (network[levelIndex + 1][permIndex] != 2);
        // switch according to level-3 single gate (□ █ □)
        if (network[levelIndex + 1][permIndex] == 1) {
            T temp0 = subSrcs.elementAt(0);
            T temp1 = subSrcs.elementAt(1);
            subSrcs.set(0, temp1);
            subSrcs.set(1, temp0);
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
            T temp0 = subSrcs.elementAt(0);
            T temp1 = subSrcs.elementAt(1);
            subSrcs.set(0, temp1);
            subSrcs.set(1, temp0);
        }
        if (network[levelIndex + 1][permIndex] == 1) {
            T temp1 = subSrcs.elementAt(1);
            T temp2 = subSrcs.elementAt(2);
            subSrcs.set(1, temp2);
            subSrcs.set(2, temp1);
        }
        if (network[levelIndex + 2][permIndex] == 1) {
            T temp0 = subSrcs.elementAt(0);
            T temp1 = subSrcs.elementAt(1);
            subSrcs.set(0, temp1);
            subSrcs.set(1, temp0);
        }
    }

    @SuppressWarnings("AlibabaMethodTooLong")
    private void permuteQuadrupleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // level-3 quadruple gates, all gates must be in the form (█ █ █)
        //                                                         █ █ □
        assert (network[levelIndex][permIndex] != 2) && (network[levelIndex][permIndex + 1] != 2)
            && (network[levelIndex + 1][permIndex] != 2) && (network[levelIndex + 1][permIndex + 1] != 2)
            && (network[levelIndex + 2][permIndex] != 2) && (network[levelIndex + 2][permIndex + 1] == 2);
        // level 1
        if (network[levelIndex][permIndex] == 1) {
            T temp00 = subSrcs.elementAt(0);
            T temp02 = subSrcs.elementAt(2);
            subSrcs.set(0, temp02);
            subSrcs.set(2, temp00);
        }
        if (network[levelIndex][permIndex + 1] == 1) {
            T temp01 = subSrcs.elementAt(1);
            T temp03 = subSrcs.elementAt(3);
            subSrcs.set(1, temp03);
            subSrcs.set(3, temp01);
        }
        // level 2
        byte[] level2 = network[levelIndex + 1];
        if (level2[permIndex] == 1) {
            T temp20 = subSrcs.elementAt(0);
            T temp21 = subSrcs.elementAt(1);
            subSrcs.set(0, temp21);
            subSrcs.set(1, temp20);
        }
        if (level2[permIndex + 1] == 1) {
            T temp22 = subSrcs.elementAt(2);
            T temp23 = subSrcs.elementAt(3);
            subSrcs.set(2, temp23);
            subSrcs.set(3, temp22);
        }
        // level 2
        if (network[levelIndex + 2][permIndex] == 1) {
            T temp40 = subSrcs.elementAt(0);
            T temp42 = subSrcs.elementAt(2);
            subSrcs.set(0, temp42);
            subSrcs.set(2, temp40);
        }
    }

    @SuppressWarnings("AlibabaMethodTooLong")
    private void permutePadQuadrupleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // level-5 quadruple gates, all gates must be in the form (█ □ █ □ █)
        //                                                         █ □ █ □ □
        assert (network[levelIndex][permIndex] != 2) && (network[levelIndex][permIndex + 1] != 2)
            && (network[levelIndex + 1][permIndex] == 2) && (network[levelIndex + 1][permIndex + 1] == 2)
            && (network[levelIndex + 2][permIndex] != 2) && (network[levelIndex + 2][permIndex + 1] != 2)
            && (network[levelIndex + 3][permIndex] == 2) && (network[levelIndex + 3][permIndex + 1] == 2)
            && (network[levelIndex + 4][permIndex] != 2) && (network[levelIndex + 4][permIndex + 1] == 2);
        // level 1
        if (network[levelIndex][permIndex] == 1) {
            T temp00 = subSrcs.elementAt(0);
            T temp02 = subSrcs.elementAt(2);
            subSrcs.set(0, temp02);
            subSrcs.set(2, temp00);
        }
        if (network[levelIndex][permIndex + 1] == 1) {
            T temp01 = subSrcs.elementAt(1);
            T temp03 = subSrcs.elementAt(3);
            subSrcs.set(1, temp03);
            subSrcs.set(3, temp01);
        }
        // level 3
        byte[] level3 = network[levelIndex + 2];
        if (level3[permIndex] == 1) {
            T temp20 = subSrcs.elementAt(0);
            T temp21 = subSrcs.elementAt(1);
            subSrcs.set(0, temp21);
            subSrcs.set(1, temp20);
        }
        if (level3[permIndex + 1] == 1) {
            T temp22 = subSrcs.elementAt(2);
            T temp23 = subSrcs.elementAt(3);
            subSrcs.set(2, temp23);
            subSrcs.set(3, temp22);
        }
        // level 5
        if (network[levelIndex + 4][permIndex] == 1) {
            T temp40 = subSrcs.elementAt(0);
            T temp42 = subSrcs.elementAt(2);
            subSrcs.set(0, temp42);
            subSrcs.set(2, temp40);
        }
    }
}
