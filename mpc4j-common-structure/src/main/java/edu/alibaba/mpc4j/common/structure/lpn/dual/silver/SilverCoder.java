package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.dual.DualLpnCoder;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.ExtremeSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.LowerTriSquareSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.sparse.NaiveSparseBitMatrix;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * SilverCoder. SilverCoder encodes e = (e_1, ..., e_{2k-g}) to w = (w_1, ..., w_k). The construction comes from the
 * following paper:
 * <p></p>
 * Couteau, Geoffroy, Peter Rindal, and Srinivasan Raghuraman. Silver: silent VOLE and oblivious transfer from hardness
 * of decoding structured LDPC codes. CRYPTO 2021, pp. 502-534. Cham: Springer International Publishing, 2021.
 * <p></p>
 * SilverCoder is defined by a parity check matrix H = [A, B, C // D, E, F]. The size of each sub-matrix is decided by
 * the parameter k and g (gap), here we denote s = k - g:
 * <p>A: s · s</p>
 * <p>B: s · g</p>
 * <p>C: s · s</p>
 * <p>D: g · s</p>
 * <p>E: g · g</p>
 * <p>F: g · s</p>
 * All above sub-matrices are sparse bit matrix. D and F are extremely sparse bit matrix (some rows are all-zero).
 * <p></p>
 * Given the message e, the coder computes e · G^T, where G^T · H = 0. The message size is k, the code size is 2k - g.
 *
 * @author Hanwen Feng
 * @date 2022/3/18
 */
public class SilverCoder implements DualLpnCoder {
    /**
     * sub-matrix A
     */
    private final NaiveSparseBitMatrix matrixA;
    /**
     * sub-matrix B
     */
    private final NaiveSparseBitMatrix matrixB;
    /**
     * sub-matrix C
     */
    private final LowerTriSquareSparseBitMatrix matrixC;
    /**
     * sub-matrix D
     */
    private final ExtremeSparseBitMatrix matrixD;
    /**
     * sub-matrix F
     */
    private final ExtremeSparseBitMatrix matrixF;
    /**
     * sub-matrix E' = (F · C^{-1} · B）+ E)^{-1}
     */
    private final DenseBitMatrix matrixEp;
    /**
     * gap
     */
    private final int gapValue;
    /**
     * k
     */
    private final int kValue;
    /**
     * code size
     */
    private final int codeSize;

    SilverCoder(NaiveSparseBitMatrix matrixA, NaiveSparseBitMatrix matrixB, LowerTriSquareSparseBitMatrix matrixC,
                ExtremeSparseBitMatrix matrixD, ExtremeSparseBitMatrix matrixF, DenseBitMatrix matrixEp,
                int gapValue, int kValue) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.matrixC = matrixC;
        this.matrixD = matrixD;
        this.matrixEp = matrixEp;
        this.matrixF = matrixF;
        this.gapValue = gapValue;
        this.kValue = kValue;
        codeSize = 2 * kValue - gapValue;
    }

    @Override
    public void setParallel(boolean parallel) {
        matrixA.setParallel(parallel);
    }

    @Override
    public boolean getParallel() {
        return matrixA.getParallel();
    }

    @Override
    public boolean[] dualEncode(boolean[] e) {
        assert e.length == codeSize;
        // initialize w = [x | p | p' | p''], where x ∈ {0,1}^s, p ∈ {0,1}^g, p' ∈ {0,1}^s, p'' ∈ {0,1}^s
        boolean[] x = Arrays.copyOfRange(e, 0, kValue - gapValue);
        boolean[] p = Arrays.copyOfRange(e, kValue - gapValue, kValue);
        boolean[] pp = Arrays.copyOfRange(e, kValue, e.length);
        boolean[] ppp = new boolean[kValue - gapValue];
        // Step 1: compute p' = p' · C^{-1}
        pp = matrixC.invLmul(pp);
        // Step 2: compute p = p' · B + p
        matrixB.lmulAddi(pp, p);
        // Step 3: compute p = p · E'^{-1}
        p = matrixEp.leftMultiply(p);
        // Step 4: compute x = p · D + x
        matrixD.lmulAddi(p, x);
        // Step 5: compute p'' = p · F + p''
        matrixF.lmulAddi(p, ppp);
        // Step 6: compute p' = p'' · C^{-1} + p'
        matrixC.invLmulAddi(ppp, pp);
        // Step 7, compute x = p' · A + x
        matrixA.lmulAddi(pp, x);

        return x;
    }

    @Override
    public byte[][] dualEncode(byte[][] e) {
        assert e.length == codeSize;
        // initialize w = [x | p | p' | p''], where x ∈ {0,1}^s, p ∈ {0,1}^g, p' ∈ {0,1}^s, p'' ∈ {0,1}^s
        int byteLength = e[0].length;
        byte[][] x = new byte[kValue - gapValue][];
        for (int i = 0; i < x.length; i++) {
            x[i] = BytesUtils.clone(e[i]);
        }
        byte[][] p = new byte[gapValue][];
        for (int i = 0; i < p.length; i++) {
            p[i] = BytesUtils.clone(e[i + kValue - gapValue]);
        }
        byte[][] pp = new byte[kValue - gapValue][];
        for (int i = 0; i < pp.length; i++) {
            pp[i] = BytesUtils.clone(e[i + kValue]);
        }
        byte[][] ppp = new byte[kValue - gapValue][byteLength];
        // Step 1: compute p' = p' · C^{-1}
        pp = matrixC.invLextMul(pp);
        // Step 2: compute p = p' · B + p
        matrixB.lExtMulAddi(pp, p);
        // Step 3: compute p = p · E'^{-1}
        p = matrixEp.leftGf2lMultiply(p);
        // Step 4: compute x = p · D + x
        matrixD.lExtMulAddi(p, x);
        // Step 5: compute p'' = p · F + p''
        matrixF.lExtMulAddi(p, ppp);
        // Step 6: compute p' = p'' · C^{-1} + p'
        matrixC.invLextMulAddi(ppp, pp);
        // Step 7, compute x = p' · A + x
        matrixA.lExtMulAddi(pp, x);

        return x;
    }

    @Override
    public int getCodeSize() {
        return codeSize;
    }

    @Override
    public int getMessageSize() {
        return kValue;
    }
}