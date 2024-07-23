package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.matrix.Z3ByteMatrix;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.Arrays;

import java.security.SecureRandom;

/**
 * (F3, F2)-wPRF with (n, m, t) = (4λ, 2λ, λ), where the k ∈ Z_2^4λ, x ∈ Z_3^4λ, f(k,x) Z_2^λ
 *
 * @author Weiran Liu
 * @date 2024/5/24
 */
public class F32Wprf {
    /**
     * n = 4λ
     */
    public static final int N = 4 * CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * n byte length
     */
    public static final int N_BYTE_LENGTH = 4 * CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * m = 2λ
     */
    public static final int M = 2 * CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * m byte length
     */
    public static final int M_BYTE_LENGTH = 2 * CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * t = λ
     */
    private static final int T = CommonConstants.BLOCK_BIT_LENGTH;

    /**
     * Gets input length.
     *
     * @return input length.
     */
    public static int getInputLength() {
        return N;
    }

    /**
     * Gets output byte length.
     *
     * @return output byte length.
     */
    public static int getOutputByteLength() {
        return CommonConstants.BLOCK_BYTE_LENGTH;
    }
    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * A_3 with 4λ rows and 2λ columns
     */
    private final Z3ByteMatrix matrixA;
    /**
     * B_2 with 2λ rows and λ columns
     */
    private final DenseBitMatrix matrixB;

    public F32Wprf(Z3ByteField z3Field, byte[] seedA, byte[] seedB) {
        this.z3Field = z3Field;
        matrixA = Z3ByteMatrix.createRandom(z3Field, N, M, seedA);
        matrixB = DenseBitMatrixFactory.createRandom(DenseBitMatrixType.BYTE_MATRIX, M, T, seedB);
    }

    /**
     * Gets matrix A.
     *
     * @return matrix A.
     */
    public Z3ByteMatrix getMatrixA() {
        return matrixA;
    }

    /**
     * Gets matrix B.
     *
     * @return matrix B.
     */
    public DenseBitMatrix getMatrixB() {
        return matrixB;
    }

    /**
     * Generates a random key.
     *
     * @param secureRandom random state.
     * @return a random key.
     */
    public byte[] keyGen(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(N_BYTE_LENGTH, secureRandom);
    }

    /**
     * Computes PRF.
     *
     * @param key   key.
     * @param input input.
     * @return PRF.
     */
    public byte[] prf(byte[] key, byte[] input) {
        // F(k, x) = B_2 ·_2 (A_3 ·_3 [k ⊙_3 x])
        // here ·_3 is multiplication modulo 3, and ⊙_3 is component-wise multiplication modulo 3
        MathPreconditions.checkEqual("n", "key.length", N_BYTE_LENGTH, key.length);
        MathPreconditions.checkEqual("n", "input.length", N, input.length);
        // key and input must not be all zero
        Preconditions.checkArgument(!Arrays.areAllZeroes(key, 0, key.length), "key must be random");
        Preconditions.checkArgument(!Arrays.areAllZeroes(input, 0, input.length), "input must be random");
        // input must be in Z3
        for (byte b : input) {
            Preconditions.checkArgument(z3Field.validateElement(b));
        }
        // k ⊙_3 x, where ⊙_3 is component-wise multiplication modulo 3
        byte[] kx3 = new byte[N];
        for (int i = 0; i < N; i++) {
            kx3[i] = BinaryUtils.getBoolean(key, i) ? input[i] : 0;
        }
        // A_3 ·_3 [k ⊙_3 x]
        byte[] akx3 = matrixA.leftMul(kx3);
        // Z3 -> Z2 moduli conversion: each 0 and 2 are mapped to 0 while each 1 is mapped to 1.
        byte[] akx2 = new byte[M_BYTE_LENGTH];
        for (int i = 0; i < M; i++) {
            if (akx3[i] == 1) {
                BinaryUtils.setBoolean(akx2, i, true);
            }
        }
        // B_2 ·_2 (A_3 ·_3 [k ⊙_3 x])
        return matrixB.leftMultiply(akx2);
    }
}
