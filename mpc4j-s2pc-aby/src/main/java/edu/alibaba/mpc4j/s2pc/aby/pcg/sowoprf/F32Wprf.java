package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import org.bouncycastle.util.Arrays;

import java.security.SecureRandom;

/**
 * (F3, F2)-wPRF, which is defined in the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating-Moduli PRFs and
 * Post-quantum Signatures. CRYPTO 2024, pp. 274-308. Cham: Springer Nature Switzerland, 2024.
 * </p>
 * Section 3.3 shows the (F3, F2)-wPRF construction.
 * <p>
 * In particular, we will evaluate F(k, x) = B_2 ·_2 (A_3 ·_3 [k ⊙ x]), where k ∈ F_2^n, x ∈ F_3^n, A ∈ F_3^{m×n},
 * B ∈ F_2{m×t}.
 * </p>
 * The parameter is n = 4λ, m = 2λ, t = λ. Therefore, the key space is k ∈ Z_2^{4λ} (512 bits), the message space is
 * x ∈ Z_3^n (512 elements in Z_3), and the output space is Z_2^λ (128 elements in Z_2).
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
    public static final int T = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * t byte length
     */
    private static final int T_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;

    /**
     * Gets input length. The input space is Z_2^{4λ}.
     *
     * @return input length.
     */
    public static int getInputLength() {
        return N;
    }

    /**
     * Gets output byte length. The output space is Z_2^{λ}, we can represent inputs as <code>byte[]</code>.
     *
     * @return output byte length.
     */
    public static int getOutputByteLength() {
        return T_BYTE_LENGTH;
    }

    /**
     * Z3-field
     */
    private final Z3ByteField z3Field;
    /**
     * A_3 with 4λ rows and 2λ columns
     */
    private final F32WprfMatrix matrixA;
    /**
     * B_2 with 2λ rows and λ columns
     */
    private final DenseBitMatrix matrixB;
    /**
     * binary key
     */
    private boolean[] binaryKey;

    public F32Wprf(Z3ByteField z3Field, byte[] seedA, byte[] seedB, F32WprfMatrixType matrixType) {
        this.z3Field = z3Field;
        matrixA = F32WprfMatrixFactory.createRandom(z3Field, seedA, matrixType);
        matrixB = DenseBitMatrixFactory.createRandom(DenseBitMatrixType.BYTE_MATRIX, M, T, seedB);
    }

    /**
     * Gets matrix A.
     *
     * @return matrix A.
     */
    public F32WprfMatrix getMatrixA() {
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
     * Generates a random key. The key space is k ∈ Z_2^4λ.
     *
     * @param secureRandom random state.
     * @return a random key.
     */
    public byte[] keyGen(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(N_BYTE_LENGTH, secureRandom);
    }

    /**
     * Initializes the key.
     *
     * @param key key.
     */
    public void init(byte[] key) {
        MathPreconditions.checkEqual("n", "key.length", N_BYTE_LENGTH, key.length);
        Preconditions.checkArgument(!Arrays.areAllZeroes(key, 0, key.length), "key must be random");
        binaryKey = new boolean[N];
        for (int i = 0; i < N; i++) {
            binaryKey[i] = BinaryUtils.getBoolean(key, i);
        }
    }

    /**
     * Computes PRF.
     *
     * @param input input.
     * @return PRF.
     */
    public byte[] prf(byte[] input) {
        Preconditions.checkNotNull(binaryKey);
        // F(k, x) = B_2 ·_2 (A_3 ·_3 [k ⊙_3 x])
        // here ·_3 is multiplication modulo 3, and ⊙_3 is component-wise multiplication modulo 3
        MathPreconditions.checkEqual("n", "input.length", N, input.length);
        // input must not be all zero
        Preconditions.checkArgument(!Arrays.areAllZeroes(input, 0, input.length), "input must be random");
        // input must be in Z3
        for (byte b : input) {
            Preconditions.checkArgument(z3Field.validateElement(b));
        }
        // k ⊙_3 x, where ⊙_3 is component-wise multiplication modulo 3
        byte[] kx3 = new byte[N];
        for (int i = 0; i < N; i++) {
            kx3[i] = binaryKey[i] ? input[i] : 0;
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
