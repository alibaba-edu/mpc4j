package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.LowerTriSquareSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.NaiveSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.UpperTriSquareSparseBitMatrix;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.SparseBitVector;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * FullLdpcCreator 类
 * 按照论文 Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes
 * （http://eprint.iacr.org/2021/1150）的描述生成Ldpc。
 * 不要求Ep提前生成，不要求已知当前ceilLogN对应的LPN参数
 *
 * @author Hanwen Feng
 * @date 2022/03/14
 */
public class FullSilverCodeCreator extends AbstractSilverCodeCreator {
    /**
     * 由于给定参数下 Ep未必存在，需要多次调整参数尝试。引入临时矩阵，最终确定后写入到A~F。
     */
    private NaiveSparseBitMatrix tempA, tempB, tempD, tempE, tempF;

    private LowerTriSquareSparseBitMatrix tempC;
    /**
     * 最大尝试次数
     */
    private static final int MAX_TRY_TIME = 100;

    /**
     * 构造函数
     *
     * @param silverCodeType Ldpc类型
     * @param ceilLogN 目标输出OT数量
     */
    public FullSilverCodeCreator(SilverCodeType silverCodeType, int ceilLogN) {
        initParams(silverCodeType, ceilLogN);
        // 首先计算一组使得LPN假设成立，且可以输出足够数量OT的LPN参数。
        LpnParams lpnParams = new SilverCodeLpnParamsFinder(silverCodeType).computeLpnParams(ceilLogN);
        // 读取LPN的k，t值，作为临时的LDPC参数。
        int tryKvalue = lpnParams.getK();
        int tryTvalue = lpnParams.getT();
        // 尝试tryKvalue，直到找到能够使Ep存在的值。
        int count = 0;
        while (true) {
            try {
                leftCodeInit(tryKvalue);
                rightCodeInit(tryKvalue);
                // 若不可逆，computeMatrixEp() 抛出异常。
                computeMatrixEp();
                // 对应可逆的Ep参数，设置LPN参数。
                setLpnParams(tryKvalue, tryTvalue);
                // 确定k值。
                kValue = tryKvalue;
                // 若顺利计算出Ep，则将临时矩阵赋给所需的分块矩阵。
                matrixA = tempA;
                matrixB = tempB;
                matrixC = tempC;
                matrixD = tempD.toExtremeSparseBitMatrix();
                matrixF = tempF.toExtremeSparseBitMatrix();
                break;
            } catch (ArithmeticException e) {
                // 当Ep 不存在时，将tryKvalue 加1。
                tryKvalue++;
                count++;
                if (count > MAX_TRY_TIME) {
                    throw new IllegalStateException("For k = " + tryKvalue + ": hard to find lpn params");
                }
            }
        }
    }

    /**
     * 生成左矩阵
     *
     * @param kValue 当前尝试的kvalue
     */
    private void leftCodeInit(int kValue) {
        // 根据种子，计算初始列向量。
        int[] firstCol = new int[weight];
        HashSet<Integer> ss = new HashSet<>();
        int trials = 0;
        for (int i = 0; i < weight; ++i) {
            firstCol[i] = (int) (kValue * leftSeed[i]) % kValue;
            while (ss.contains(firstCol[i])) {
                ++firstCol[i];
                if (++trials > MAX_TRY_TIME) {
                    throw new IllegalArgumentException("Too many collisions");
                }
            }
            ss.add(firstCol[i]);
        }
        // 根据初始列向量，创建循环左矩阵。
        NaiveSparseBitMatrix leftMtx = NaiveSparseBitMatrix.createFromCyclic(kValue, kValue, firstCol);
        // 提取矩阵A,B，D,E。
        tempA = leftMtx.subMatrix(0, kValue - gapValue, 0, kValue - gapValue);
        tempB = leftMtx.subMatrix(kValue - gapValue, kValue, 0, kValue - gapValue);
        tempD = leftMtx.subMatrix(0, kValue - gapValue,
            kValue - gapValue, kValue);
        tempE = leftMtx.subMatrix(kValue - gapValue, kValue, kValue - gapValue, kValue);
    }

    /**
     * 生成右矩阵，使用了OnlineLdpcCreator的改进方法。
     *
     * @param kValue 当前尝试的kvalue。
     */
    private void rightCodeInit(int kValue) {
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
        tempC = LowerTriSquareSparseBitMatrix.createUncheck(cColsList);
        ArrayList<SparseBitVector> fColsList = Stream.of(fColsArray).collect(Collectors.toCollection(ArrayList::new));
        tempF = NaiveSparseBitMatrix.createFromColumnList(fColsList);
    }

    /**
     * 计算Ep = (F*C^{-1}*B）+ E)^{-1}。
     */
    private void computeMatrixEp() {
        UpperTriSquareSparseBitMatrix cTranspose = tempC.transpose();
        NaiveSparseBitMatrix fTranspose = tempF.transpose();
        NaiveSparseBitMatrix bTranspose = tempB.transpose();
        NaiveSparseBitMatrix eTranspose = tempE.transpose();

        matrixEp = ByteDenseBitMatrix.createFromDense(
            gapValue, fTranspose.lExtMul(cTranspose.invLextMul(bTranspose.transposeDense().getByteArrayData()))
            )
            .xor(eTranspose.transposeDense())
            .inverse();
    }

    /**
     * 调整t取值，创建符合安全要求的LPN参数。
     *
     * @param tryK 当前k值。
     * @param tryT 当前t值。
     */
    private void setLpnParams(int tryK, int tryT) {
        // 按照Ldpc形式要求，计算n。
        int n = 2 * tryK - gapValue;
        int count = 0;
        // 检测当前取值是否满足安全要求。若安全性不足，则增加t值，直到满足条件。
        while (true) {
            try {
                lpnParams = LpnParams.create(n, tryK, tryT);
                break;
            } catch (IllegalArgumentException e) {
                tryT++;
                count++;
                if (count > MAX_TRY_TIME) {
                    throw new IllegalStateException("Cannot Find t");
                }
            }
        }
    }
}
