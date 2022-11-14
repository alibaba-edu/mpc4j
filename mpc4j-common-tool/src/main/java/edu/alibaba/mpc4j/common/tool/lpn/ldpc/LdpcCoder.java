package edu.alibaba.mpc4j.common.tool.lpn.ldpc;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.ExtremeSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.LowerTriangularSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.SparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * LdpcCoder 类，实现按照给定的Ldpc，将boolean[] 和 byte[][] 类型的消息完成转置编码
 * 参考论文 Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes
 * （http://eprint.iacr.org/2021/1150）P.32 Fig.12
 *
 * Ldpc由稀疏矩阵H定义，H由分块矩阵 A, B，C，D, E，F 构成, 上半区 (A,B,C)，下半区 （D,E,F）
 * 其中各分块矩阵的维度由参数 k 和 参数 gap确定，具体：
 *  矩阵A, 行 k - gap，列 k - gap
 *  矩阵B，行 k - gap, 列 gap
 *  矩阵C，行 k - gap, 列 k - gap
 *  矩阵D，行 gap, 列 k - gap
 *  矩阵E，行 gap, 列 gap
 *  矩阵F，行 gap， 列 k - gap
 * 以上矩阵均为稀疏矩阵 SparseMatrix，其中 D,F 大部分列为空，属于极度稀疏矩阵 ExtremeSparseBitMatrix.
 *
 * 对于消息e, 转置编码过程是计算 e*G^T， 其中矩阵G是矩阵H的奇偶校验矩阵。 使用矩阵H的分块矩阵信息可以完成转置编码的计算。
 * 矩阵Ep = (F*C^{-1}*B）+ E)^{-1} 可预先计算完成，因此将其作为LdpcCoder的成员变量进行初始化
 *
 * @author Hanwen Feng
 * @date 2022/03/18
 */
public class LdpcCoder {
    /**
     * 分块矩阵A
     */
    private final SparseBitMatrix matrixA;
    /**
     * 分块矩阵B
     */
    private final SparseBitMatrix matrixB;
    /**
     * 分块矩阵C
     */
    private final LowerTriangularSparseBitMatrix matrixC;
    /**
     * 分块矩阵D
     */
    private final ExtremeSparseBitMatrix matrixD;
    /**
     * 分块矩阵F
     */
    private final ExtremeSparseBitMatrix matrixF;
    /**
     * 矩阵Ep = (F*C^{-1}*B）+ E)^{-1}
     */
    private final DenseBitMatrix matrixEp;
    /**
     * Ldpc 参数gap
     */
    private final int gapValue;
    /**
     * Ldpc 参数k
     */
    private final int kValue;
    /**
     * 包私有构造函数
     * 传入所有成员变量，完成初始化。
     */
    LdpcCoder(SparseBitMatrix matrixA, SparseBitMatrix matrixB, LowerTriangularSparseBitMatrix matrixC, ExtremeSparseBitMatrix matrixD,
              ExtremeSparseBitMatrix matrixF, DenseBitMatrix matrixEp, int gapValue, int kValue) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.matrixC = matrixC;
        this.matrixD = matrixD;
        this.matrixEp = matrixEp;
        this.matrixF = matrixF;
        this.gapValue = gapValue;
        this.kValue = kValue;
    }
    /**
     * 指定LdpcCoder是否并行
     * @param parallel 是否并行
     */
    public void setParallel(boolean parallel) {
        matrixA.setParallel(parallel);
    }
    /**
     * 转置编码运算
     * 计算 e*G^T
     * @param messageE 待编码消息
     * @return 返回编码结果
     */
    public boolean[] transEncode(boolean[] messageE) {
        // 判断是否消息长度是否符合。
        assert messageE.length == 2 * kValue - gapValue;
        /*
        * 根据分块矩阵大小，将待编码消息进行切割.
        * pp 对应论文的变量 p'；
        * ppp 对应论文变量 p''。
         */
        boolean[] x = Arrays.copyOfRange(messageE, 0, kValue - gapValue);
        boolean[] p = Arrays.copyOfRange(messageE, kValue - gapValue, kValue);
        boolean[] pp = Arrays.copyOfRange(messageE, kValue, messageE.length);
        boolean[] ppp = new boolean[kValue - gapValue];
        // step 1 计算 pp = pp * C^{-1}。
        pp = matrixC.invLmul(pp);
        // step2  计算 p = pp * B + p。
        matrixB.lmulAddi(pp, p);
        // step 3， 计算 p = p*E'^{-1}。
        p = matrixEp.lmul(p);
        //step 4, 计算 x = p *D + x。
        //noinspection SuspiciousNameCombination
        matrixD.lmulAddi(p, x);
        // step 5 计算 ppp = p * F + ppp。
        matrixF.lmulAddi(p, ppp);
        // step 6, 计算 pp = ppp * C^{-1} + pp。
        matrixC.invLmulAddi(ppp, pp);
        // step 7, 计算 x = pp * A + x
        //noinspection SuspiciousNameCombination
        matrixA.lmulAddi(pp, x);
        return x;
    }

    /**
     * 转置编码运算
     * @param messageE 待编码消息
     * @return 返回编码结果
     */
    public byte[][] transEncode(byte[][] messageE) {
        assert messageE.length == 2 * kValue - gapValue;
        /*
         * 根据分块矩阵大小，将待编码消息进行切割.
         * pp 对应论文的变量 p'；
         * ppp 对应论文变量 p''。
         */
        int byteLength = messageE[0].length;
        byte[][] x = new byte[kValue - gapValue][];
        for (int i = 0; i < x.length; i++) {
            x[i] = BytesUtils.clone(messageE[i]);
        }
        byte[][] p = new byte[gapValue][];
        for (int i = 0; i < p.length; i++) {
            p[i] = BytesUtils.clone(messageE[i + kValue - gapValue]);
        }
        byte[][] pp = new byte[kValue - gapValue][];
        for (int i = 0; i < pp.length; i++) {
            pp[i] = BytesUtils.clone(messageE[i + kValue]);
        }
        byte[][] ppp = new byte[kValue - gapValue][byteLength];
        // 各步骤定义和对 boolean[] 的transEncode相同。
        pp = matrixC.invLextMul(pp);
        matrixB.lExtMulAddi(pp, p);
        p = matrixEp.lExtMul(p);
        //noinspection SuspiciousNameCombination
        matrixD.lExtMulAddi(p, x);
        matrixF.lExtMulAddi(p, ppp);
        matrixC.invLextMulAddi(ppp, pp);
        //noinspection SuspiciousNameCombination
        matrixA.lExtMulAddi(pp, x);
        return x;
    }
}