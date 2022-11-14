package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * 稀疏数组相关代数操作类，用于支持稀疏矩阵以及LDPC编码。
 * 稀疏数组是指 只有少数点为1，多数点为0的 比特数组。
 * 对 int[] 的封装。 [1,2,5] 表示 该数组 第1、2、5位置为1，其余为0。
 * 所有稀疏数组均已排序。
 *
 * @author Hanwen Feng
 * @date 2022.3.2
 */

public class SparseBitVector {
    /**
     * 被封装的数组，记录稀疏向量中值为1的位置
     */
    private int[] nonZeroIndexArray;

    /**
     * 对应的比特向量的长度。例如pureVector=【1，2，5】，bitSize = 8. 则表示长度为8的0-1向量， 其中1,2,5的位置为1，其他为0
     */
    private int bitSize;

    /**
     * 将int[] 数组转换为 SparseArray对象。
     *
     * @param indexArray 指定的数组。
     * @param bitSize    对应比特向量的长度。
     */
    public static SparseBitVector create(int[] indexArray, int bitSize) {
        if (indexArray.length != 0) {
            Arrays.sort(indexArray);
            if (indexArray[indexArray.length - 1] >= bitSize) {
                throw new IllegalArgumentException("All indexes in int[] r must be smaller than bitSize.");
            }
        }
        SparseBitVector sparseBitVector = new SparseBitVector();
        sparseBitVector.nonZeroIndexArray = indexArray;
        sparseBitVector.bitSize = bitSize;
        return sparseBitVector;
    }

    /**
     * 私有构造函数。
     */
    private SparseBitVector() {
        // empty
    }

    /**
     * 将已排序的int[] 数组转换为 SparseArray对象，节省后续排序开销。
     *
     */
    public static SparseBitVector createUnCheck(int[] indexArray, int bitSize) {
        SparseBitVector sparseBitVector = new SparseBitVector();
        sparseBitVector.nonZeroIndexArray = indexArray;
        sparseBitVector.bitSize = bitSize;
        return sparseBitVector;
    }


    /**
     * 创建指定大小的数组，封装为SparseArray
     *
     * @param sparseArraySize 稀疏数组大小
     * @param bitArraySize    比特数组大小
     */
    public static SparseBitVector createEmpty(int sparseArraySize, int bitArraySize) {
        assert sparseArraySize <= bitArraySize
            : "sparseArraySize: " + sparseArraySize + " should be smaller than bitArraySize: " + bitArraySize;
        SparseBitVector sparseBitVector = new SparseBitVector();
        sparseBitVector.nonZeroIndexArray = new int[sparseArraySize];
        sparseBitVector.bitSize = bitArraySize;
        return sparseBitVector;
    }

    /**
     * 实现稀疏向量的循环移位。
     * 例如，若bitSize=10, 则 [3,4,9] 移位得到 [0,4,5]
     *
     * @return 返回移位一次后的稀疏向量
     */
    public SparseBitVector cyclicMove() {
        SparseBitVector shiftArray = createEmpty(getSize(), getBitSize());
        if (getValue(getSize() - 1) == getBitSize() - 1) {
            for (int i = getSize() - 1; i > 0; i--) {
                shiftArray.setValue(i, getValue(i - 1) + 1);
            }
            shiftArray.setValue(0, 0);
        } else {
            for (int i = 0; i < getSize(); i++) {
                shiftArray.setValue(i, getValue(i) + 1);
            }
        }
        return shiftArray;
    }

    /**
     * 深拷贝稀疏向量
     *
     * @return 拷贝的稀疏向量
     */
    public SparseBitVector copyOf() {
        int[] nPureVector = Arrays.copyOf(nonZeroIndexArray, nonZeroIndexArray.length);
        return createUnCheck(nPureVector, bitSize);
    }

    /**
     * 按指定index 深拷贝向量
     * 例如 pureVector = [1,4,6,8]，指定拷贝位置from: 1 to: 3， 则拷贝出 [4,6]。
     *
     * @param from       拷贝起始位置
     * @param to         拷贝的终止位置
     * @param newBitSize 拷贝得到向量的bitSize。
     * @return 拷贝的稀疏向量
     */
    public SparseBitVector copyOfRange(int from, int to, int newBitSize) {
        int[] nPureVector = Arrays.copyOfRange(nonZeroIndexArray, from, to);
        return createUnCheck(nPureVector, newBitSize);
    }

    /**
     * 将向量移动常数位数，非循环移位
     * 例如[1,3,5]，bitSize为6, 移位1, 得到[2,4]
     *
     * @param constant 移动的位数
     * @return 返回移位后的向量
     */
    public SparseBitVector shiftConstant(int constant) {
        int[] nPureArray = new int[nonZeroIndexArray.length];
        int lengthCount = 0;
        for (int i = 0; i < nonZeroIndexArray.length; i++) {
            int element = nonZeroIndexArray[i] + constant;
            if (element < bitSize) {
                nPureArray[i] = element;
                lengthCount++;
            } else {
                break;
            }
        }
        int[] tPureArray = Arrays.copyOf(nPureArray, lengthCount);
        return createUnCheck(tPureArray, bitSize);
    }

    /**
     * 得到稀疏数组大小
     *
     * @return 数组大小
     */
    public int getSize() {
        return nonZeroIndexArray.length;
    }

    /**
     * 得到对应比特数组的大小
     *
     * @return 比特数组大小
     */
    public int getBitSize() {
        return bitSize;
    }

    /**
     * 提取子稀疏数组
     *
     * @param startValue 提取的起始值
     * @param endValue   结束值
     * @return 子稀疏数组
     * 例如： 从（2，4，5，8，10），指定 (start = 3, end =6), 首先提取得到 (4,5)；
     * 由于初始位置为3, 平移得到 (1,2), 返回 (1,2)
     */
    public SparseBitVector getSubArray(int startValue, int endValue) {
        assert endValue >= startValue && startValue >= 0;
        assert endValue <= getBitSize();

        int targetBitSize = endValue - startValue;

        if (getSize() == 0) {
            return createEmpty(0, targetBitSize);
        }

        int startIndexI = Arrays.binarySearch(nonZeroIndexArray, startValue);

        if (startIndexI < 0) {
            startIndexI = (startIndexI + 1) * (-1);
        }

        int endIndexI = Arrays.binarySearch(nonZeroIndexArray, endValue);
        if (endIndexI < 0) {
            endIndexI = (endIndexI + 1) * (-1);
        }

        int[] nPureArray = Arrays.copyOfRange(nonZeroIndexArray, startIndexI, endIndexI);

        for (int i = 0; i < nPureArray.length; i++) {
            nPureArray[i] = nPureArray[i] - startValue;
        }

        return createUnCheck(nPureArray, targetBitSize);
    }

    /**
     * 两个sparseArray 相加
     *
     * @param that 待加和的另一个sparseArray
     * @return 加和
     */
    public SparseBitVector add(SparseBitVector that) {
        assert bitSize == that.bitSize;

        int[] temp = new int[getSize() + that.getSize()];
        int tempIndex = 0;

        int index0 = 0;
        int index1 = 0;

        while (index0 != getSize() && index1 != that.getSize()) {
            if (getValue(index0) < that.getValue(index1)) {
                temp[tempIndex] = getValue(index0++);
                tempIndex++;

            } else if (getValue(index0) > that.getValue(index1)) {
                temp[tempIndex] = that.getValue(index1++);
                tempIndex++;
            } else {
                ++index0;
                ++index1;
            }
        }
        while (index0 != getSize()) {
            temp[tempIndex] = getValue(index0++);
            tempIndex++;
        }
        while (index1 != that.getSize()) {
            temp[tempIndex] = that.getValue(index1++);
            tempIndex++;
        }
        int[] nArray = Arrays.copyOf(temp, tempIndex);
        return createUnCheck(nArray, bitSize);
    }

    /**
     * 稀疏数组(对应的比特向量)和布尔向量做内积。
     *
     * @param xVec 布尔向量
     * @return 返回boolean值
     */
    public boolean multiply(final boolean[] xVec) {
        assert bitSize == xVec.length;

        boolean r = false;
        for (int index : nonZeroIndexArray) {
            r = xVec[index] != r;
        }
        return r;
    }

    /**
     * 稀疏数组(对应的比特向量)和byte向量做内积。
     *
     * @param xVec byte向量
     * @return 返回byte
     */
    public byte multiply(final byte[] xVec) {
        assert bitSize == xVec.length;

        byte r = 0;
        for (int index : nonZeroIndexArray) {
            r ^= xVec[index];
        }
        return r;
    }

    /**
     * 稀疏数组(对应的比特向量)和byte[][]做内积。
     *
     * @param xVec byte数组
     * @return byte数组
     */
    public byte[] multiply(final byte[][] xVec) {
        assert bitSize == xVec.length;
        byte[] r = new byte[xVec[0].length];
        for (int index : nonZeroIndexArray) {
            BytesUtils.xori(r, xVec[index]);
        }
        return r;
    }

    /**
     * 稀疏数组（对应的比特向量）和 byte[][] xvec做内积，然后将结果和byte[] y 相加，返回到y
     *
     * @param xVec 向量
     * @param y    返回向量
     */
    public void multiplyAddi(final byte[][] xVec, byte[] y) {
        assert bitSize == xVec.length;
        assert xVec[0].length == y.length;
        for (int index : nonZeroIndexArray) {
            //noinspection SuspiciousNameCombination
            BytesUtils.xori(y, xVec[index]);
        }
    }

    /**
     * 稀疏向量（表示的boolean向量）和稀疏向量（表示的boolean向量）做内积，得到boolean值
     *
     * @param sVec 稀疏向量
     * @return 内积结果boolean值
     */
    public boolean multiply(SparseBitVector sVec) {
        boolean result = false;

        int rowIndex = 0;
        int colIndex = 0;

        // 乘积矩阵（i,j）位置等于 第i行和第i列做内积。
        while (rowIndex != getSize() && colIndex != sVec.getSize()) {
            if (getValue(rowIndex) < sVec.getValue(colIndex)) {
                ++rowIndex;
            }
            else if (sVec.getValue(colIndex) < getValue(rowIndex)) {
                ++colIndex;
            }
            else {
                result = !result;
                ++rowIndex;
                ++colIndex;
            }
        }
        return result;
    }

    /**
     * 统计本稀疏数组记录的位置。
     * 例如，本稀疏数组为 [2,4]， 则将 indexCounterArray[2]，indexCounterArray[4] 分别加一。
     *
     * @param indexCounterArray 用于记录数量的向量
     */
    public void indexCounter(int[] indexCounterArray) {
        for (int index : nonZeroIndexArray) {
            indexCounterArray[index]++;
        }
    }


    /**
     * 设置数组指定位置的值
     *
     * @param index 位置
     * @param value 值
     */
    private void setValue(int index, int value) {
        assert index < getSize();
        assert value < bitSize;
        nonZeroIndexArray[index] = value;
    }

    /**
     * 返回数组指定位置取值
     *
     * @param index 位置
     * @return 值
     */
    public int getValue(int index) {
        return nonZeroIndexArray[index];
    }

    /**
     * 返回pureVector的最后一个元素
     *
     * @return 返回最后一个元素
     */
    public int getLastValue() {
        return nonZeroIndexArray[nonZeroIndexArray.length - 1];
    }

    private static boolean isSorted(int[] indexArray) {
        for (int i = 0; i < indexArray.length; i++) {
            if (indexArray[i] > indexArray[i + 1]) {
                return false;
            }
        }
        return true;
    }

    public int[] getNonZeroIndexArray() {
        return Arrays.copyOf(nonZeroIndexArray, nonZeroIndexArray.length);
    }

    @Override
    public String toString() {
        return Arrays.toString(nonZeroIndexArray);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseBitVector)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SparseBitVector that = (SparseBitVector) obj;

        return Arrays.equals(this.nonZeroIndexArray, that.nonZeroIndexArray);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nonZeroIndexArray).toHashCode();
    }
}