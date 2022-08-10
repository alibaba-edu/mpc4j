package edu.alibaba.mpc4j.common.tool.lpn.matrix;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * 稀疏数组相关代数操作类，用于支持稀疏矩阵以及LDPC编码
 * 稀疏数组是指 只有少数点为1，多数点为0的 比特数组。
 * 对 int[] 的封装。 [1,2,5] 表示 该数组 第1、2、5位置为1，其余为0
 *
 * @author Hanwen Feng
 * @date 2022.3.2
 */

public class SparseVector {
    /**
     * 被封装的数组，记录稀疏向量中值为1的位置
     */
    private final int[] pureVector;

    /**
     * 对应的比特向量的长度。例如pureVector=【1，2，5】，bitSize = 8. 则表示长度为8的0-1向量， 其中1,2,5的位置为1，其他为0
     */
    private final int bitSize;

    /**
     * 标记是否已经排序
     */
    private boolean sorted;

    /**
     * 将int[] 数组转换为 SparseArray对象
     *
     * @param r       指定的数组
     * @param bitSize 对应比特向量的长度
     */
    public SparseVector(int[] r, int bitSize) {
        pureVector = r;
        this.bitSize = bitSize;
    }

    /**
     * 同上，支持在初始化阶段表明数组已排序，节省后续排序开销
     *
     * @param sorted 表明数组是否已经排序
     */
    public SparseVector(int[] r, int bitArraySize, boolean sorted) {
        this(r, bitArraySize);
        this.sorted = sorted;
    }


    /**
     * 创建指定大小的数组，封装为SparseArray
     *
     * @param sparseArraySize 稀疏数组大小
     * @param bitArraySize    比特数组大小
     */
    public SparseVector(int sparseArraySize, int bitArraySize) {
        pureVector = new int[sparseArraySize];
        this.bitSize = bitArraySize;
    }

    /**
     * 实现稀疏向量的循环移位。
     * 例如，若bitSize=10, 则 [3,4,9] 移位得到 [0,4,5]
     *
     * @return 返回移位一次后的稀疏向量
     */
    public SparseVector cyclicMove() {
        assert sorted;
        SparseVector shiftArray = new SparseVector(getSize(), getBitSize());
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
        shiftArray.setSorted();
        return shiftArray;
    }

    /**
     * 深拷贝稀疏向量
     *
     * @return 拷贝的稀疏向量
     */
    public SparseVector copyOf() {
        int[] nPureVector = Arrays.copyOf(pureVector, pureVector.length);
        SparseVector nArray = new SparseVector(nPureVector, bitSize);
        if (sorted) {
            nArray.setSorted();
        }
        return nArray;
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
    public SparseVector copyOfRange(int from, int to, int newBitSize) {
        int[] nPureVector = Arrays.copyOfRange(pureVector, from, to);
        SparseVector nVector = new SparseVector(nPureVector, newBitSize);
        if (sorted) {
            nVector.setSorted();
        }
        return nVector;
    }

    /**
     * 将向量移动常数位数，非循环移位
     * 例如[1,3,5]，bitSize为6, 移位1, 得到[2,4]
     *
     * @param constant 移动的位数
     * @return 返回移位后的向量
     */
    public SparseVector shiftConstant(int constant) {
        assert sorted;

        int[] nPureArray = new int[pureVector.length];
        int lengthCount = 0;
        for (int i = 0; i < pureVector.length; i++) {
            int element = pureVector[i] + constant;
            if (element < bitSize) {
                nPureArray[i] = element;
                lengthCount++;
            } else {
                break;
            }
        }
        int[] tPureArray = Arrays.copyOf(nPureArray, lengthCount);
        return new SparseVector(tPureArray, bitSize, true);
    }

    /**
     * 得到稀疏数组大小
     *
     * @return 数组大小
     */
    public int getSize() {
        return pureVector.length;
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
    public SparseVector getSubArray(int startValue, int endValue) {
        assert endValue >= startValue && startValue >= 0;
        assert endValue <= getBitSize();
        assert sorted;

        int targetBitSize = endValue - startValue;

        if (getSize() == 0) {
            return new SparseVector(0, targetBitSize);
        }

        int startIndexI = Arrays.binarySearch(pureVector, startValue);

        if (startIndexI < 0) {
            startIndexI = (startIndexI + 1) * (-1);
        }

        int endIndexI = Arrays.binarySearch(pureVector, endValue);
        if (endIndexI < 0) {
            endIndexI = (endIndexI + 1) * (-1);
        }

        int[] nPureArray = Arrays.copyOfRange(pureVector, startIndexI, endIndexI);

        for (int i = 0; i < nPureArray.length; i++) {
            nPureArray[i] = nPureArray[i] - startValue;
        }

        return new SparseVector(nPureArray, targetBitSize, true);
    }

    /**
     * 两个sparseArray 相加
     *
     * @param xArray 待加和的另一个sparseArray
     * @return 加和
     */
    public SparseVector addSparseArray(SparseVector xArray) {
        assert bitSize == xArray.bitSize;
        assert sorted && xArray.sorted;

        int[] temp = new int[getSize() + xArray.getSize()];
        int tempIndex = 0;

        int index0 = 0;
        int index1 = 0;

        while (index0 != getSize() && index1 != xArray.getSize()) {
            if (getValue(index0) < xArray.getValue(index1)) {
                temp[tempIndex] = getValue(index0++);
                tempIndex++;

            } else if (getValue(index0) > xArray.getValue(index1)) {
                temp[tempIndex] = xArray.getValue(index1++);
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
        while (index1 != xArray.getSize()) {
            temp[tempIndex] = xArray.getValue(index1++);
            tempIndex++;
        }

        int[] nArray = Arrays.copyOf(temp, tempIndex);

        return new SparseVector(nArray, bitSize, true);
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
        for (int index : pureVector) {
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
        for (int index : pureVector) {
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
        for (int index : pureVector) {
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
    public void multiplyAdd(final byte[][] xVec, byte[] y) {
        assert bitSize == xVec.length;
        assert xVec[0].length == y.length;
        for (int index : pureVector) {
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
    public boolean multiply(SparseVector sVec) {
        boolean result = false;

        int rowIndex = 0;
        int colIndex = 0;

        // 乘积矩阵（i,j）位置等于 第i行和第i列做内积。
        while (rowIndex != getSize() && colIndex != sVec.getSize()) {
            if (getValue(rowIndex) < sVec.getValue(colIndex))
                ++rowIndex;
            else if (sVec.getValue(colIndex) < getValue(rowIndex))
                ++colIndex;
            else {
                result = !result;
                ++rowIndex;
                ++colIndex;
            }
        }
        return result;
    }

    /**
     * 统计本稀疏数组记录的位置
     *
     * @param indexCounterArray 用于记录数量的向量
     *                          例如，本稀疏数组为 [2,4]， 则将 indexCounterArray[2]，indexCounterArray[4] 分别加一。
     */
    public void indexCounter(int[] indexCounterArray) {
        for (int index : pureVector) {
            indexCounterArray[index]++;
        }
    }


    /**
     * 设置数组指定位置的值
     *
     * @param index 位置
     * @param value 值
     */
    public void setValue(int index, int value) {
        assert index < getSize();
        assert value < bitSize;
        pureVector[index] = value;
    }

    /**
     * 返回数组指定位置取值
     *
     * @param index 位置
     * @return 值
     */
    public int getValue(int index) {
        return pureVector[index];
    }

    /**
     * 返回pureVector的最后一个元素
     *
     * @return 返回最后一个元素
     */
    public int getLastValue() {
        return pureVector[pureVector.length - 1];
    }

    /**
     * 对数组排序
     */
    public void sort() {
        Arrays.sort(pureVector);
        sorted = true;
    }

    /**
     * 判断给定index是否包含在稀疏数组中
     *
     * @param index 需要判断的index
     * @return 返回是或否
     */
    public boolean checkIndex(int index) {
        assert sorted;
        return (Arrays.binarySearch(pureVector, index) >= 0);
    }

    /**
     * 判断数组是否已经排序（升序）
     *
     * @return 若已经排序 返回true
     */
    public boolean isSorted() {
        for (int i = 0; i < getSize() - 1; i++) {
            if (getValue(i) > getValue(i + 1)) {
                return false;
            }
        }
        setSorted();
        return true;
    }

    private void setSorted() {
        sorted = true;
    }

    public void setSortedWithCheck() {
        assert (isSorted());
        sorted = true;
    }

    /**
     * 得到对应的bitArray.
     * 数据类型为byte[]，每一个byte存储0或者1
     *
     * @return bitArray
     */
    public byte[] getBitVector() {
        byte[] output = new byte[bitSize];
        Arrays.stream(pureVector).forEach(i -> output[i] = 1);
        return output;
    }

    public int[] getPureVector() {
        return Arrays.copyOf(pureVector, pureVector.length);
    }

    @Override
    public String toString() {
        return Arrays.toString(pureVector);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseVector)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SparseVector that = (SparseVector) obj;

        return Arrays.equals(this.pureVector, that.pureVector);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(pureVector).toHashCode();
    }
}