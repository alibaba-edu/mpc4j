package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.ExtremeSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.LowerTriSquareSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.NaiveSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.SparseBitVector;


import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * OnlineLdpcCreator类, 适用于分块矩阵中 Ep已经提前生成并写在了LdpcCreatorUtils的情况
 * 仅按列生成矩阵A、B,C,D,F，满足LdpcEncoder的需要。
 *
 * @author Hanwen Feng
 * @date 2022.3.15
 */

class OnlineSilverCodeCreator extends AbstractSilverCodeCreator {
    /**
     * 构造函数
     *
     * @param silverCodeType 类型
     * @param ceilLogN 目标输出OT数量
     */
    OnlineSilverCodeCreator(SilverCodeType silverCodeType, int ceilLogN) throws IllegalStateException {
        if (ceilLogN < SilverCodeCreatorUtils.MIN_LOG_N || ceilLogN > SilverCodeCreatorUtils.MAX_LOG_N) {
            throw new IllegalArgumentException("OnlineLdpcCreator ONLY supports ceilLogN in: " +
                "[" + SilverCodeCreatorUtils.MIN_LOG_N + ", " + SilverCodeCreatorUtils.MAX_LOG_N + "]");
        }
        initParams(silverCodeType, ceilLogN);
        // 读取生成对应的参数和种子。
        initFromFile();
        kValue = lpnParams.getK();
        // 分别执行左矩阵初始化和右矩阵初始化。
        leftCodeInit();
        rightCodeInit();
    }

    /**
     * 生成H的左矩阵，含A,B,D
     */
    private void leftCodeInit() {
        // 左矩阵为循环阵，种子为第一列的稀疏向量，直接读取。
        int[] firstCol = new int[leftSeed.length];
        for (int i = 0; i < firstCol.length; ++i) {
            firstCol[i] = (int) (kValue * leftSeed[i]) % kValue;
        }
        // 检验第一列是否regular （预置的种子满足此要求）。
        assert isRegular(firstCol, kValue, kValue - gapValue);
        // 将列向量封装为sparsevector。
        SparseBitVector currentVector = SparseBitVector.createUncheck(firstCol, kValue);
        // 创建存储矩阵A 列向量和矩阵B列向量的list。
        ArrayList<SparseBitVector> aColsList = new ArrayList<>(kValue - gapValue);
        aColsList.ensureCapacity(kValue - gapValue);
        ArrayList<SparseBitVector> bColsList = new ArrayList<>(gapValue);
        bColsList.ensureCapacity(gapValue);
        // 创建存储矩阵D非空列向量list，以及记录非空列向量位置的list。
        ArrayList<SparseBitVector> dColsList = new ArrayList<>();
        ArrayList<Integer> dNonEmptyIndexList = new ArrayList<>();
        // 将首列循环移位kvalue-gapvalue次，得到的矩阵前kvalue-gapvalue行构成A，后gapValue行构成D。
        for (int colIndex = 0; colIndex < kValue - gapValue; colIndex++) {
            // 由于首列是regular的，每次移位后最多只有一个元素超过了kvalue-gapvalue，直接判断最后一个元素。
            if (currentVector.getLastPosition() < kValue - gapValue) {
                // 若最后一个元素也没有超过kvalue-gapvalue，将该列复制，加入矩阵A的colsList。
                SparseBitVector aCol = currentVector.copyOfRange(0, currentVector.getSize(), kValue - gapValue);
                aColsList.add(aCol);
            } else {
                // 若最后一个元素超过了kvalue-gapvalue,则将当前列的前getSize()-1个元素复制，加入A的colsList。
                SparseBitVector aCol = currentVector.copyOfRange(0, currentVector.getSize() - 1, kValue - gapValue);
                aColsList.add(aCol);
                // 当前列的最后一个元素加入矩阵D的非空colsList, 并记录当前索引值。
                dNonEmptyIndexList.add(colIndex);
                int dColElement = currentVector.getLastPosition() - (kValue - gapValue);
                int[] dCol = {dColElement};
                dColsList.add(SparseBitVector.createUncheck(dCol, gapValue));
            }
            // 将当前列循环移位。
            currentVector = currentVector.cyclicShiftRight();
        }
        // 将list转为数组。
        int[] dNonEmptyIndex = dNonEmptyIndexList.stream().mapToInt(k -> k).toArray();
        // 根据计算的colsList创建矩阵A和D。
        matrixA = NaiveSparseBitMatrix.createFromColumnList(aColsList);
        matrixD = ExtremeSparseBitMatrix.createUncheck(gapValue, kValue - gapValue, dNonEmptyIndex, dColsList);
        // 继续循环移位当前列gapValue次，得到矩阵B。
        for (int colIndex = 0; colIndex < gapValue; colIndex++) {
            if (currentVector.getLastPosition() < kValue - gapValue) {
                SparseBitVector bCol = currentVector.copyOfRange(0, currentVector.getSize(), kValue - gapValue);
                bColsList.add(bCol);
            } else {
                SparseBitVector bCol = currentVector.copyOfRange(0, currentVector.getSize() - 1, kValue - gapValue);
                bColsList.add(bCol);
            }
            currentVector = currentVector.cyclicShiftRight();
        }
        // 创建矩阵B。
        matrixB = NaiveSparseBitMatrix.createFromColumnList(bColsList);
    }

    /**
     * 生成H的右矩阵，含C和F
     * 经观察发现，如果将右矩阵的第i列都向上移动i位，那么右矩阵的各列是按周期重复的。
     * 我们将一个周期记录为 rightImprovedSeed，并存储
     */
    private void rightCodeInit() {
        /*
         * 创建存储矩阵C和矩阵F各个列的数组。
         * 由于并发访问每列，选择用数组存储而不是arrayList。
         */
        SparseBitVector[] cColsArray = new SparseBitVector[kValue - gapValue];
        SparseBitVector[] fColsArray = new SparseBitVector[kValue - gapValue];
        // 根据rightImprovedSeed 生成左矩阵各列。
        IntStream.range(0, kValue - gapValue).parallel()
            .forEach(colIndex -> {
                int rem = colIndex % gapValue;
                SparseBitVector fullCol = SparseBitVector.createUncheck(rightSeed[rem], kValue).shiftRight(colIndex);
                // 矩阵C的列为左矩阵每列的前 k - gap 项。
                cColsArray[colIndex] = fullCol.sub(0, kValue - gapValue);
                // 矩阵F的列为左矩阵每列的后 gap 项。
                fColsArray[colIndex] = fullCol.sub(kValue - gapValue, kValue);
            });
        // 将数组转为ArrayList，然后生成对应的稀疏矩阵。
        ArrayList<SparseBitVector> cColsList = Stream.of(cColsArray).collect(Collectors.toCollection(ArrayList::new));
        matrixC = LowerTriSquareSparseBitMatrix.createUncheck(cColsList);
        ArrayList<SparseBitVector> fColsList = Stream.of(fColsArray).collect(Collectors.toCollection(ArrayList::new));
        matrixF = NaiveSparseBitMatrix.createFromColumnList(fColsList).toExtremeSparseBitMatrix();
    }

    /**
     * 判断初始向量是否regular，即向量任意两个相邻的元素的差都大于 cyclicLength -rows。这意味着每一次移位最多有一个元素不属于该矩阵
     *
     * @param initVector   初始向量
     * @param cyclicLength 循环维度
     * @param rows         行数
     * @return 判断是否regular
     */
    private boolean isRegular(int[] initVector, int cyclicLength, int rows) {
        int minIntervalLength = cyclicLength - rows;
        for (int i = 0; i < initVector.length - 1; i++) {
            int intervalLength = initVector[i + 1] - initVector[i];
            if (intervalLength <= minIntervalLength) {
                return false;
            }
        }
        return initVector[0] - initVector[initVector.length - 1] + cyclicLength > minIntervalLength;
    }
}
