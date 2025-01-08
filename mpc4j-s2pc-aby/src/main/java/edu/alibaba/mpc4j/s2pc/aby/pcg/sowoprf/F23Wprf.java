package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import org.bouncycastle.util.Arrays;

import java.security.SecureRandom;

/**
 * (F2, F3)-wPRF, which is defined in the following paper:
 * <p>
 * Navid Alamati, Guru-Vamsi Policharla, Srinivasan Raghuraman, and Peter Rindal. Improved Alternating-Moduli PRFs and
 * Post-quantum Signatures. CRYPTO 2024, pp. 274-308. Cham: Springer Nature Switzerland, 2024.
 * </p>
 * Definition 3.1 shows the (F2, F3)-wPRF construction.
 * <p>
 * Let n, m, t ∈ N, our (F_2, F_3)-wPRF construction is F(k, x) := B_3 ·_3 (A_2 ·_2 [k ⊙_2 x]) where x, k ∈ F_2^n, and
 * A_2 ∈ F_2^{m×n},B_3 ∈ F_3^{t×m} are uniformly distributed.
 * </p>
 * The parameter is n = 4λ, m = 2λ, t = λ / log2(3). Therefore, the key space is k ∈ Z_2^4λ (512 bits), the message
 * space is x ∈ Z_2^n (512 bits), and the output space is Z_3^{λ / log2(3)} (81 elements in Z_3).
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class F23Wprf {
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
     * t = λ / log2(3) = 81
     */
    public static final int T = (int) Math.ceil(CommonConstants.BLOCK_BIT_LENGTH / (Math.log(3) / Math.log(2)));

    /**
     * Gets input byte length. The input space is Z_2^{n}, we can represent inputs as <code>byte[]</code>.
     *
     * @return input byte length.
     */
    public static int getInputByteLength() {
        return N_BYTE_LENGTH;
    }

    /**
     * Gets output length. The output space is Z_3^{λ / log2(3)}.
     *
     * @return output length.
     */
    public static int getOutputLength() {
        return T;
    }

    /**
     * A_2 with 4λ rows and 2λ columns
     */
    private final DenseBitMatrix matrixA;
    /**
     * B_3 with 2λ rows and λ / log2(3) columns
     */
    private final F23WprfMatrix matrixB;
    /**
     * key
     */
    private byte[] key;

    public F23Wprf(Z3ByteField z3Field, byte[] seedA, byte[] seedB, F23WprfMatrixType matrixType) {
        matrixA = DenseBitMatrixFactory.createRandom(DenseBitMatrixType.BYTE_MATRIX, N, M, seedA);
        matrixB = F23WprfMatrixFactory.createRandom(z3Field, seedB, matrixType);
    }

    /**
     * Gets matrix A.
     *
     * @return matrix A.
     */
    public DenseBitMatrix getMatrixA() {
        return matrixA;
    }

    /**
     * Gets matrix B.
     *
     * @return matrix B.
     */
    public F23WprfMatrix getMatrixB() {
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
        this.key = BytesUtils.clone(key);
    }

    /**
     * Computes PRF.
     *
     * @param input input.
     * @return PRF.
     */
    public byte[] prf(byte[] input) {
        Preconditions.checkNotNull(key);
        // F(k, x) = B_3 ·_3 (A_2 ·_2 [k ⊙_2 x])
        // here ·_3 is multiplication modulo 3.
        MathPreconditions.checkEqual("n (byte length)", "input.length", N_BYTE_LENGTH, input.length);
        // input must not be all zero
        Preconditions.checkArgument(!Arrays.areAllZeroes(input, 0, input.length), "input must be random");
        // k ⊙_2 x
        byte[] kx2 = BytesUtils.and(input, key);
        // A_2 ·_2 [k ⊙_2 x]
        byte[] akx2 = matrixA.leftMultiply(kx2);
        // B_3 ·_3 (A_2 ·_2 [k ⊙_2 x])
        return matrixB.leftBinaryMul(akx2);
    }
}
