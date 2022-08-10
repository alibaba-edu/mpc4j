package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import edu.alibaba.mpc4j.common.tool.lpn.matrix.*;

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

class OnlineLdpcCreator extends AbstractLdpcCreator {
    /**
     * 优化了右矩阵 （A,F）的生成逻辑，对应的种子信息被记录为improvedSeed， 在LdpcCreatorUtils.
     */
    private final int[][] rightImprovedSeed;

    /**
     * 构造函数
     *
     * @param codeType 类型
     * @param ceilLogN 目标输出OT数量
     */
    public OnlineLdpcCreator(LdpcCreatorUtils.CodeType codeType, int ceilLogN) {
        super(codeType, ceilLogN);
        // 读取生成对应的参数和种子。
        lpnParams = LdpcCreatorUtils.getLpnParams(ceilLogN, codeType);
        kValue = lpnParams.getK();
        rightImprovedSeed = LdpcCreatorUtils.getImprovedRightSeed(codeType);
        // 矩阵Ep已经写在了内存，直接读取，并创建DenseMatrix对象。
        matrixEp = new DenseMatrix(gapValue, gapValue, LdpcCreatorUtils.getMatrixEp(ceilLogN, codeType));
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
        assert CyclicSparseMatrix.isRegular(firstCol, kValue, kValue - gapValue);
        // 将列向量封装为sparsevector。
        SparseVector currentVector = new SparseVector(firstCol, kValue, true);
        // 创建存储矩阵A 列向量和矩阵B列向量的list。
        ArrayList<SparseVector> aColsList = new ArrayList<>(kValue - gapValue);
        aColsList.ensureCapacity(kValue - gapValue);
        ArrayList<SparseVector> bColsList = new ArrayList<>(gapValue);
        bColsList.ensureCapacity(gapValue);
        // 创建存储矩阵D非空列向量list，以及记录非空列向量位置的list。
        ArrayList<SparseVector> dColsList = new ArrayList<>();
        ArrayList<Integer> dNonEmptyIndexList = new ArrayList<>();
        // 将首列循环移位kvalue-gapvalue次，得到的矩阵前kvalue-gapvalue行构成A，后gapValue行构成D。
        for (int colIndex = 0; colIndex < kValue - gapValue; colIndex++) {
            // 由于首列是regular的，每次移位后最多只有一个元素超过了kvalue-gapvalue，直接判断最后一个元素。
            if (currentVector.getLastValue() < kValue - gapValue) {
                // 若最后一个元素也没有超过kvalue-gapvalue，将该列复制，加入矩阵A的colsList。
                SparseVector aCol = currentVector.copyOfRange(0, currentVector.getSize(), kValue - gapValue);
                aColsList.add(aCol);
            } else {
                // 若最后一个元素超过了kvalue-gapvalue,则将当前列的前getSize()-1个元素复制，加入A的colsList。
                SparseVector aCol = currentVector.copyOfRange(0, currentVector.getSize() - 1, kValue - gapValue);
                aColsList.add(aCol);
                // 当前列的最后一个元素加入矩阵D的非空colsList, 并记录当前索引值。
                dNonEmptyIndexList.add(colIndex);
                int dColElement = currentVector.getLastValue() - (kValue - gapValue);
                int[] dCol = {dColElement};
                dColsList.add(new SparseVector(dCol, gapValue, true));
            }
            // 将当前列循环移位。
            currentVector = currentVector.cyclicMove();
        }
        // 将list转为数组。
        int[] dNonEmptyIndex = dNonEmptyIndexList.stream().mapToInt(k -> k).toArray();
        // 根据计算的colsList创建矩阵A和D。
        matrixA = new SparseMatrix(aColsList, SparseMatrix.WorkType.ColsOnly, kValue - gapValue, kValue - gapValue);
        matrixD = new ExtremeSparseMatrix(dColsList, dNonEmptyIndex, gapValue, kValue - gapValue);
        // 继续循环移位当前列gapValue次，得到矩阵B。
        for (int colIndex = 0; colIndex < gapValue; colIndex++) {
            if (currentVector.getLastValue() < kValue - gapValue) {
                SparseVector bCol = currentVector.copyOfRange(0, currentVector.getSize(), kValue - gapValue);
                bColsList.add(bCol);
            } else {
                SparseVector bCol = currentVector.copyOfRange(0, currentVector.getSize() - 1, kValue - gapValue);
                bColsList.add(bCol);
            }
            currentVector = currentVector.cyclicMove();
        }
        // 创建矩阵B。
        matrixB = new SparseMatrix(bColsList, SparseMatrix.WorkType.ColsOnly, kValue - gapValue, gapValue);
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
        SparseVector[] cColsArray = new SparseVector[kValue - gapValue];
        SparseVector[] fColsArray = new SparseVector[kValue - gapValue];
        // 根据rightImprovedSeed 生成左矩阵各列。
        IntStream.range(0, kValue - gapValue).parallel()
                .forEach(colIndex -> {
                    int rem = colIndex % gapValue;
                    SparseVector fullCol = new SparseVector(rightImprovedSeed[rem], kValue, true).shiftConstant(colIndex);
                    // 矩阵C的列为左矩阵每列的前 k - gap 项。
                    cColsArray[colIndex] = fullCol.getSubArray(0, kValue - gapValue);
                    // 矩阵F的列为左矩阵每列的后 gap 项。
                    fColsArray[colIndex] = fullCol.getSubArray(kValue - gapValue, kValue);
                });
        // 将数组转为ArrayList，然后生成对应的稀疏矩阵。
        ArrayList<SparseVector> cColsList = Stream.of(cColsArray).collect(Collectors.toCollection(ArrayList::new));
        matrixC = new SparseMatrix(cColsList, SparseMatrix.WorkType.ColsOnly, kValue - gapValue, kValue - gapValue);
        matrixC.setDiagMtx();
        ArrayList<SparseVector> fColsList = Stream.of(fColsArray).collect(Collectors.toCollection(ArrayList::new));
        SparseMatrix sparseMatrixF = new SparseMatrix(fColsList, SparseMatrix.WorkType.ColsOnly, gapValue, kValue - gapValue);
        matrixF = sparseMatrixF.getExtremeSparseMatrix();
    }
}
