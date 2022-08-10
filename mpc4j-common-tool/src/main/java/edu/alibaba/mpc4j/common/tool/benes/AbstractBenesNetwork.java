package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.util.Vector;

/**
 * 贝奈斯网络（Benes Network）抽象类。
 *
 * @author Weiran Liu
 * @date 2021/09/26
 */
public abstract class AbstractBenesNetwork<T> implements BenesNetwork<T> {
    /**
     * 映射数量
     */
    protected final int n;
    /**
     * 层数
     */
    protected final int level;
    /**
     * 网络
     */
    protected boolean[][] network;

    /**
     * 构造Benes网络。置换表用数组表示，其中数组长度表示置换表的大小，数组中的每一个数字表示原始表映射方式。
     * 例如：[7, 4, 8, 6, 2, 1, 0, 3, 5]表示的映射关系为：
     * [0, 1, 2, 3, 4, 5, 6, 7, 8]
     * |  |  |  |  |  |  |  |  |
     * [7, 4, 8, 6, 2, 1, 0, 3, 5]
     *
     * @param permutationMap 置换表。
     */
    public AbstractBenesNetwork(final int[] permutationMap) {
        assert BenesNetworkUtils.validPermutation(permutationMap);
        n = permutationMap.length;
        // 设置层数：level = 2 * log(n) - 1
        level = 2 * (int) Math.ceil(DoubleUtils.log2(n)) - 1;
    }

    @Override
    public boolean[] getNetworkLevel(int levelIndex) {
        assert levelIndex >= 0 && levelIndex < level;
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
    public int getWidth() {
        return n / 2;
    }

    @Override
    public Vector<T> permutation(final Vector<T> inputVector) {
        assert inputVector.size() == n;
        int logN = (int) Math.ceil(DoubleUtils.log2(n));
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
            // 上方子Benes网络的输入和输出映射表，大小为Math.floor(n / 2)
            int subTopN = subN / 2;
            Vector<T> subTopSrcs = new Vector<>(subTopN);
            // 下方子Benes网络的输入和输出映射表，大小为Math.ceil(n / 2)
            int subBottomN = subN - subTopN;
            Vector<T> subBottomSrcs = new Vector<>(subBottomN);
            // 对Benes网络的左侧部分求值
            for (int i = 0; i < subN - 1; i += 2) {
                int s = network[levelIndex][permIndex + i / 2] ? 1 : 0;
                for (int j = 0; j < 2; ++j) {
                    int x = rightCycleShift((i | j) ^ s, subLogN);
                    if (x < subN / 2) {
                        subTopSrcs.add(subSrcs.elementAt(i | j));
                    } else {
                        subBottomSrcs.add(subSrcs.elementAt(i | j));
                    }
                }
            }
            // 如果是奇数个输入，则下方子Benes网络需要再增加一个输入
            if (subN % 2 == 1) {
                subBottomSrcs.add(subSrcs.elementAt(subN - 1));
            }
            // 迭代对Benes网络的中间部分求值
            permutation(subLogN - 1, levelIndex + 1, permIndex, subTopSrcs);
            permutation(subLogN - 1, levelIndex + 1, permIndex + subN / 4, subBottomSrcs);
            // 对Benes网络的右侧部分求值
            for (int i = 0; i < subN - 1; i += 2) {
                int s = network[levelIndex + subLevel - 1][permIndex + i / 2] ? 1 : 0;
                for (int j = 0; j < 2; j++) {
                    int x = rightCycleShift((i | j) ^ s, subLogN);
                    if (x < subN / 2) {
                        subSrcs.set(i | j, subTopSrcs.elementAt(x));
                    } else {
                        subSrcs.set(i | j, subBottomSrcs.elementAt(i / 2));
                    }
                }
            }
            // 如果是奇数个输入，则下方子Benes网络需要多处理一个输入
            int idx = (int) (Math.ceil(subN * 0.5));
            if (subN % 2 == 1) {
                subSrcs.set(subN - 1, subBottomSrcs.elementAt(idx - 1));
            }
        }
    }

    private void permuteSingleLevel(int subLogN, int levelIndex, int permIndex, Vector<T> subSrcs) {
        if (subLogN == 1) {
            // 根据1层交换门(█)交换位置
            if (network[levelIndex][permIndex]) {
                T temp = subSrcs.elementAt(0);
                subSrcs.set(0, subSrcs.elementAt(1));
                subSrcs.set(1, temp);
            }
        } else {
            // 3层交换门，左侧和右侧的悬空门必然不交换（false）
            assert (!network[levelIndex][permIndex]) && (!network[levelIndex + 2][permIndex]);
            // 根据3层交换门(█ █ █)交换位置
            if (network[levelIndex + 1][permIndex]) {
                T temp = subSrcs.elementAt(0);
                subSrcs.set(0, subSrcs.elementAt(1));
                subSrcs.set(1, temp);
            }
        }
    }

    private void permuteTripleLevel(int levelIndex, int permIndex, Vector<T> subSrcs) {
        // 根据三层交换门（█ □ █）交换位置
        //              □ █ □
        if (network[levelIndex][permIndex]) {
            T temp = subSrcs.elementAt(0);
            subSrcs.set(0, subSrcs.elementAt(1));
            subSrcs.set(1, temp);
        }
        if (network[levelIndex + 1][permIndex]) {
            T temp = subSrcs.elementAt(1);
            subSrcs.set(1, subSrcs.elementAt(2));
            subSrcs.set(2, temp);
        }
        if (network[levelIndex + 2][permIndex]) {
            T temp = subSrcs.elementAt(0);
            subSrcs.set(0, subSrcs.elementAt(1));
            subSrcs.set(1, temp);
        }
    }

    /**
     * 以n比特为单位，对数字i右循环移位。
     * 例如：n = 8，      i = 00010011
     * 则有：rightCycleShift(i, n) = 10001001
     *
     * @param i 整数i。
     * @param n 单位长度。
     * @return 以n为单位长度，将i右循环移位。
     */
    protected int rightCycleShift(int i, int n) {
        return ((i & 1) << (n - 1)) | (i >> 1);
    }
}
