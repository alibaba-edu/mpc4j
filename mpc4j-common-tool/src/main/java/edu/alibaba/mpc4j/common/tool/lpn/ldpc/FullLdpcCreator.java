package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.lpn.matrix.SparseMatrix;

import java.util.HashSet;

/**
 * FullLdpcCreator 类
 * 按照论文 Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes
 * （http://eprint.iacr.org/2021/1150）的描述生成Ldpc。
 * 不要求Ep提前生成，不要求已知当前ceilLogN对应的LPN参数
 *
 * @author Hanwen Feng
 * @date 2022/03/14
 */
public class FullLdpcCreator extends AbstractLdpcCreator {
    /**
     * 由于给定参数下 Ep未必存在，需要多次调整参数尝试。引入临时矩阵，最终确定后写入到A~F。
     */
    private SparseMatrix tempA, tempB, tempC, tempD, tempE, tempF;
    /**
     * 最大尝试次数
     */
    private static final int MAX_TRY_TIME = 100;

    /**
     * 构造函数
     *
     * @param codeType Ldpc类型
     * @param ceilLogN 目标输出OT数量
     */
    public FullLdpcCreator(LdpcCreatorUtils.CodeType codeType, int ceilLogN) {
        super(codeType, ceilLogN);
        // 首先计算一组使得LPN假设成立，且可以输出足够数量OT的LPN参数。
        LpnParams lpnParams = new LdpcLpnParamsFinder(codeType).computeLpnParams(ceilLogN);
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
                matrixA.setType(SparseMatrix.WorkType.ColsOnly);
                matrixB = tempB;
                matrixB.setType(SparseMatrix.WorkType.ColsOnly);
                matrixC = tempC;
                matrixC.setType(SparseMatrix.WorkType.ColsOnly);
                matrixD = tempD.getExtremeSparseMatrix();
                matrixF = tempF.getExtremeSparseMatrix();
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
        SparseMatrix leftMtx = SparseMatrix.createFromCyclicCol(kValue, kValue, firstCol, SparseMatrix.WorkType.Full);
        // 提取矩阵A,B，D,E。
        tempA = leftMtx.getSubMatrix(0, 0, kValue - gapValue, kValue - gapValue, SparseMatrix.WorkType.Full);
        tempB = leftMtx.getSubMatrix(0, kValue - gapValue, kValue - gapValue, gapValue, SparseMatrix.WorkType.Full);
        tempD = leftMtx.getSubMatrix(kValue - gapValue, 0,
            gapValue, kValue - gapValue, SparseMatrix.WorkType.ColsOnly);
        tempE = leftMtx.getSubMatrix(kValue - gapValue, kValue - gapValue, gapValue, gapValue, SparseMatrix.WorkType.RowsOnly);

    }

    /**
     * 生成右矩阵。
     *
     * @param kValue 当前尝试的kvalue。
     */
    private void rightCodeInit(int kValue) {
        SparseMatrix.PointList pnts = new SparseMatrix.PointList(kValue, kValue - gapValue);
        // 添加主对角线。
        for (int i = 0; i < kValue - gapValue; i++) {
            pnts.addPoint(new SparseMatrix.Point(i, i));
        }
        // 添加gap区元素。
        for (int i = 0; i < kValue; i++) {
            int rem = i % gapValue;
            int startIndex = 0;
            while (i - gapValue + rightSeed[rem][startIndex] < 0) {
                startIndex++;
                if (startIndex >= weight - 1)
                    break;
            }
            for (int k = startIndex; k < weight - 1; k++) {
                if (i - gapValue + rightSeed[rem][k] >= kValue - gapValue)
                    break;
                pnts.addPoint(new SparseMatrix.Point(i, i - gapValue + rightSeed[rem][k]));
            }
        }
        // 添加辅助对角线1。
        for (int i = LdpcCreatorUtils.BAND_POSITION[0] + gapValue, j = 0; i < kValue; i++, j++) {
            pnts.addPoint(new SparseMatrix.Point(i, j));
        }
        // 添加辅助对角线2。
        for (int i = LdpcCreatorUtils.BAND_POSITION[1] + gapValue, j = 0; i < kValue; i++, j++) {
            pnts.addPoint(new SparseMatrix.Point(i, j));
        }
        // 创建右矩阵。
        SparseMatrix rightMtx = SparseMatrix.createFromPointsList(pnts, SparseMatrix.WorkType.Full);
        // 从右矩阵中提取出分块矩阵C。
        tempC = rightMtx.getSubMatrix(0, 0, kValue - gapValue, kValue - gapValue, SparseMatrix.WorkType.Full);
        // 设置C为下三角矩阵。
        tempC.setDiagMtx();
        // 提取分块矩阵F。
        tempF = rightMtx.getSubMatrix(kValue - gapValue, 0, gapValue, kValue - gapValue, SparseMatrix.WorkType.Full);
    }

    /**
     * 计算Ep = (F*C^{-1}*B）+ E)^{-1}。
     */
    private void computeMatrixEp() {
        matrixEp = tempF.rmul(tempC.diagInvRmul(tempB.getDense())).add(tempE.getDense()).getInverse();
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
