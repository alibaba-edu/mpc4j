package edu.alibaba.mpc4j.common.tool.bristol;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

/**
 * Block Boolean Matrix Multiplication lookup table. The lookup table partitions the Block Boolean Matrix into 32 * 128
 * sub-matrices, each of which is of size 4 * 1.
 * <p>
 * WINDOW_SIZE = 16, lookup table (in Gray order) has 4 input wires (w0, w1, w2, w3) and 16 output wires (l0 ... l15):
 * <ul>
 *     <li>l0 = 0</li>
 *     <li>l1 = l0 ⊕ w0 = w0                </li>
 *     <li>l2 = l1 ⊕ w1 = w0 ⊕ w1           </li>
 *     <li>l3 = l2 ⊕ w0 =      w1           </li>
 *     <li>l4 = l3 ⊕ w2 = w0      ⊕ w2      </li>
 *     <li>l5 = l4 ⊕ w0 = w0 ⊕ w1 ⊕ w2      </li>
 *     <li>l6 = l5 ⊕ w1 = w0      ⊕ w2      </li>
 *     <li>l7 = l6 ⊕ w0 =           w2      </li>
 *     <li>l8 = l7 ⊕ w3 =           w2 ⊕ w3 </li>
 *     <li>...</li>
 * </ul>
 * Given a sub-matrix [b0, b1, b2, b3]^T, we treat it as a 4-bit number (b3, b2, b1, b0), and the output is exactly
 * the inverse Gray code of that number. For example, if the sub-matrix is [0, 0, 1, 1]^T (0b1100 = 6), then the output
 * is w2 ⊕ w3. We need to find the lookup table wire corresponding to w2 ⊕ w3. Note that l8 = l7 ⊕ w3 = w2 ⊕ w3, so
 * the code should be 8, which is the 6-th inverse Gray code.
 *
 * @author Weiran Liu
 * @date 2025/4/7
 */
public class BlockBmmLookupCode {
    /**
     * block bit size = 128
     */
    static final int BLOCK_BIT_SIZE = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * block byte size = 128 / 8 = 16
     */
    static final int BLOCK_BYTE_SIZE = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * window bit size = 4
     */
    static final int WINDOW_BIT_SIZE = 4;
    /**
     * window size = 2^4 = 16
     */
    static final int WINDOW_SIZE = 1 << WINDOW_BIT_SIZE;
    /**
     * window num = 128 / 4 = 32
     */
    static final int WINDOW_NUM = BLOCK_BIT_SIZE / WINDOW_BIT_SIZE;
    /**
     * lookup tables
     */
    private final byte[][] codes;

    public BlockBmmLookupCode(byte[][] byteBitMatrix) {
        MathPreconditions.checkEqual("rows", Integer.toString(BLOCK_BIT_SIZE), byteBitMatrix.length, BLOCK_BIT_SIZE);
        for (byte[] rowVector : byteBitMatrix) {
            MathPreconditions.checkEqual("columns", Integer.toString(BLOCK_BYTE_SIZE), rowVector.length, BLOCK_BYTE_SIZE);
        }
        // the easiest way to compute the lookup table is to do computation based on the boolean matrix.
        boolean[][] binaryBitMatrix = new boolean[BLOCK_BIT_SIZE][BLOCK_BIT_SIZE];
        // Suppose the sub-matrix is [0, 0, 1, 1]^T (0b1100 = 6). This means the output is w2 ⊕ w3.
        // What we need to do is to find the lookup table wire corresponding to w2 ⊕ w3.
        // Note that l8 = l7 ⊕ w3 = w2 ⊕ w3, so the code should be 8, which is the 6-th inverse Gray code.
        int[] grayCode = GrayCodeGenerator.generate(WINDOW_SIZE);
        int[] inverseGrayCode = new int[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            inverseGrayCode[grayCode[i]] = i;
        }
        for (int i = 0; i < BLOCK_BIT_SIZE; i++) {
            for (int j = 0; j < BLOCK_BIT_SIZE; j++) {
                binaryBitMatrix[i][j] = BinaryUtils.getBoolean(byteBitMatrix[i], j);
            }
        }
        codes = new byte[WINDOW_NUM][BLOCK_BIT_SIZE];
        for (int iRow = 0; iRow < WINDOW_NUM; iRow++) {
            for (int jCol = 0; jCol < BLOCK_BIT_SIZE; jCol++) {
                if (binaryBitMatrix[iRow * 4][jCol]) {
                    codes[iRow][jCol] |= 0b0001;
                }
                if (binaryBitMatrix[iRow * 4 + 1][jCol]) {
                    codes[iRow][jCol] |= 0b0010;
                }
                if (binaryBitMatrix[iRow * 4 + 2][jCol]) {
                    codes[iRow][jCol] |= 0b0100;
                }
                if (binaryBitMatrix[iRow * 4 + 3][jCol]) {
                    codes[iRow][jCol] |= 0b1000;
                }
                codes[iRow][jCol] = (byte) (inverseGrayCode[codes[iRow][jCol]]);
            }
        }
    }

    /**
     * Left multiplies the input vector with the lookup table code.
     *
     * @param v input vector.
     * @return the result of left multiplication.
     */
    public byte[] leftMultiply(byte[] v) {
        MathPreconditions.checkEqual("v.length", "16", v.length, 16);
        boolean[] binaryV = BinaryUtils.byteArrayToBinary(v);
        // we use lookup tables to search the answer
        boolean[][] lookups = new boolean[WINDOW_NUM][BLOCK_BIT_SIZE];
        for (int i = 0; i < WINDOW_NUM; i++) {
            for (int j = 0; j < BLOCK_BIT_SIZE; j++) {
                // for each column, we create a lookup table in Gray order
                boolean[] tables = new boolean[WINDOW_SIZE];
                tables[0] = false;
                for (int k = 1; k < WINDOW_SIZE; k++) {
                    tables[k] = tables[k - 1] ^ binaryV[i * WINDOW_BIT_SIZE + GrayCodeGenerator.ctz(k)];
                }
                // we use the code to find the output of that lookup table
                byte code = codes[i][j];
                lookups[i][j] = tables[code];
            }
        }
        // we need to xor all the lookup tables
        boolean[] result = new boolean[BLOCK_BIT_SIZE];
        for (int iRow = 0; iRow < WINDOW_NUM; iRow++) {
            for (int jCol = 0; jCol < BLOCK_BIT_SIZE; jCol++) {
                result[jCol] ^= lookups[iRow][jCol];
            }
        }
        return BinaryUtils.binaryToByteArray(result);
    }

    /**
     * Gets code.
     *
     * @param i i.
     * @param j j.
     * @return code.
     */
    public byte getCode(int i, int j) {
        return codes[i][j];
    }
}
