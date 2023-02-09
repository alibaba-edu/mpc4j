package edu.alibaba.mpc4j.common.tool.coder.linear;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Hadamard Coder. Hadamard coder is a linear code that can encode a k-bit dataword to 2^k-bit codeword.
 * For the hamming distances between all codewords (except for the codeword for dataword = 0) are  2^{k-1}.
 * <p>
 * The following paper introduces standard properties of Hadamard matrices.
 * </p>
 * <p>
 * Acharya, Jayadev, Ziteng Sun, and Huanyu Zhang. Hadamard response: Estimating distributions privately, efficiently,
 * and with little communication. In The 22nd International Conference on Artificial Intelligence and Statistics, pp.
 * 1120-1129. PMLR, 2019.
 * </p>
 * <li>The number of +1’s in each row except the first is K/2.</li>
 * <li>Any two rows agree (and disagree) on exactly K/2 locations.</li>
 * <li>Vector multiplication with HK is possible in time O(K log K) with Fast Walsh Hadamard transform.</li>
 * <li>We can uniformly sample from the +1’s the -1’s) in any row in time O(log K).</li>
 * All properties are additionally implemented.
 * <li>The referenced source code is from https://github.com/rick7661/HadamardCoder/blob/master/Hadamard.java</li>
 * <li>The Hadamard algorithm description is from http://introcs.cs.princeton.edu/java/14array/Hadamard.java.html</li>
 * <li>The property algorithm description is from https://github.com/Samuel-Maddock/pure-LDP/</li>
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public class HadamardCoder implements LinearCoder {
    /**
     * Creates a Hadamard matrix with size 2^k * 2^k.
     *
     * @param k the maximal bit length of the integer for encoding.
     * @return the Hadamard matrix.
     */
    public static boolean[][] createHadamardMatrix(int k) {
        // we need 0 < k < 31, otherwise the matrix size would be greater than Integer.MAX_VALUE.
        MathPreconditions.checkNonNegativeInRange("k", k, Integer.SIZE - 1);
        if (k == 0) {
            /*
             * H(2^0) = H(1)
             *     -----
             *       T
             *     -----
             */
            boolean[][] matrix = new boolean[1][1];
            matrix[0] = new boolean[]{true};
            return matrix;
        } else if (k == 1) {
            /*
             * H(2^1) = H(2)
             *     -----
             *      T T
             *      T F
             *     -----
             */
            boolean[][] matrix = new boolean[2][2];
            matrix[0] = new boolean[]{true, true};
            matrix[1] = new boolean[]{true, false};
            return matrix;
        } else {
            /*
             * H(1) is a 1-by-1 matrix with the single entry true,
             * and for n > 1, H(2n) is obtained by aligning four copies of H(n) in a large square,
             * and then inverting all entries in the lower right n-by-n copy, as shown in the following example.
             * H(1)  H(2)    H(4)
             * -------------------
             *  T    T T   T T T T
             *       T F   T F T F
             *             T T F F
             *             T F F T
             * -------------------
             * See https://introcs.cs.princeton.edu/java/14array/，Creative Exercises 29 for details.
             */
            int n = 1 << k;
            boolean[][] matrix = new boolean[n][n];
            matrix[0][0] = true;
            /*
             * i1 = 1
             * while i1 < k:
             *     for i2 in range(i1):
             *         for i3 in range(i1):
             *             H[i2+i1][i3]    = H[i2][i3]
             *             H[i2][i3+i1]    = H[i2][i3]
             *             H[i2+i1][i3+i1] = not H[i2][i3]
             *     i1 += i1
             * return H
             */
            for (int i1 = 1; i1 < n; i1 += i1) {
                for (int i2 = 0; i2 < i1; i2++) {
                    for (int i3 = 0; i3 < i1; i3++) {
                        matrix[i2 + i1][i3] = matrix[i2][i3];
                        matrix[i2][i3 + i1] = matrix[i2][i3];
                        matrix[i2 + i1][i3 + i1] = (!matrix[i2][i3]);
                    }
                }
            }
            return matrix;
        }
    }

    /**
     * Checks if the Hadamard entry in the Hadamard matrix is 1.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @return true if the entry is 1.
     */
    public static boolean checkParity(int x, int y) {
        assert x >= 0 && x <= IntUtils.MAX_SIGNED_POWER_OF_TWO
            : "x must be in range [0, " + IntUtils.MAX_SIGNED_POWER_OF_TWO + "): " + x;
        assert y >= 0 && y <= IntUtils.MAX_SIGNED_POWER_OF_TWO
            : "y must be in range [0, " + IntUtils.MAX_SIGNED_POWER_OF_TWO + "): " + y;
        // the parity equals to \sum_i (x_i y_i) (mod 2)
        int z = x & y;
        return Integer.bitCount(z) % 2 == 0;
    }

    /**
     * In-place multiplies the input vector with the Hadamard matrix using fast Walsh-Hadamard transformation.
     * <p>
     * Given an input vector, its fast Walsh-Hadamard transformation multiplication computes v · H(n), where Ts are
     * treated as +1, and Fs are treated as -1. For example:
     * <li>[1, 1, 1, 1, 1, 1, 1, 1] · H(8) = [8, 0, 0, 0, 0, 0, 0, 0].</li>
     * <li>[1, 0, 1, 0, 0, 1, 1, 0] · H(8) = [4, 2, 0, -2, 0, 2, 0, 2].</li>
     * </p>
     *
     * @param array the given array.
     */
    public static void inplaceFastWalshHadamardTrans(int[] array) {
        int n = array.length;
        Preconditions.checkArgument((n & (n - 1)) == 0, "n must be a power of 2: %s", n);
        int h = 1;
        while (h < n) {
            for (int i = 0; i < array.length; i += (h * 2)) {
                for (int j = i; j < i + h; j++) {
                    int x = array[j];
                    int y = array[j + h];
                    array[j] = x + y;
                    array[j + h] = x - y;
                }
            }
            h *= 2;
        }
    }

    /**
     * In-place multiplies the input vector with the Hadamard matrix using fast Walsh-Hadamard transformation.
     * <p>
     * Given an input vector, its fast Walsh-Hadamard transformation multiplication computes v · H(n), where Ts are
     * treated as +1, and Fs are treated as -1. For example:
     * <li>[1, 1, 1, 1, 1, 1, 1, 1] · H(8) = [8, 0, 0, 0, 0, 0, 0, 0].</li>
     * <li>[1, 0, 1, 0, 0, 1, 1, 0] · H(8) = [4, 2, 0, -2, 0, 2, 0, 2].</li>
     * </p>
     *
     * @param array the given array.
     */
    public static void inplaceFastWalshHadamardTrans(double[] array) {
        int n = array.length;
        Preconditions.checkArgument((n & (n - 1)) == 0, "n must be a power of 2: %s", n);
        int h = 1;
        while (h < n) {
            for (int i = 0; i < array.length; i += (h * 2)) {
                for (int j = i; j < i + h; j++) {
                    double x = array[j];
                    double y = array[j + h];
                    array[j] = x + y;
                    array[j + h] = x - y;
                }
            }
            h *= 2;
        }
    }

    /**
     * Multiplies the input vector with the Hadamard matrix using fast Walsh-Hadamard transformation.
     * <p>
     * Given an input vector, its fast Walsh-Hadamard transformation multiplication computes v · H(n), where Ts are
     * treated as +1, and Fs are treated as -1. For example:
     * <li>[1, 1, 1, 1, 1, 1, 1, 1] · H(8) = [8, 0, 0, 0, 0, 0, 0, 0].</li>
     * <li>[1, 0, 1, 0, 0, 1, 1, 0] · H(8) = [4, 2, 0, -2, 0, 2, 0, 2].</li>
     * </p>
     *
     * @param inputArray the input array.
     * @return the result of v · H(n).
     */
    public static int[] fastWalshHadamardTrans(int[] inputArray) {
        int n = inputArray.length;
        Preconditions.checkArgument((n & (n - 1)) == 0, "n must be a power of 2: %s", n);
        return innerFastWalshHadamardTrans(inputArray);
    }

    private static int[] innerFastWalshHadamardTrans(int[] inputArray) {
        int n = inputArray.length;
        assert n > 0 : "n must be greater than 0: " + n;
        assert (n & (n - 1)) == 0 : "n must be a power of 2: " + n;
        /*
         * if k == 1:
         *     return dist
         * dist1 = dist[0 : k//2]
         * dist2 = dist[k//2 : k]
         * trans1 = FWHT_A(k//2, dist1)
         * trans2 = FWHT_A(k//2, dist2)
         * trans = np.concatenate((trans1 + trans2, trans1 - trans2))
         * return trans
         */
        if (n == 1) {
            return inputArray;
        }
        int halfN = n / 2;
        int[] leftInputVector = new int[halfN];
        int[] rightInputVector = new int[n - halfN];
        System.arraycopy(inputArray, 0, leftInputVector, 0 , leftInputVector.length);
        System.arraycopy(inputArray, halfN, rightInputVector, 0, rightInputVector.length);
        int[] leftOutputVector = innerFastWalshHadamardTrans(leftInputVector);
        int[] rightOutputVector = innerFastWalshHadamardTrans(rightInputVector);
        int[] outputVector = new int[n];
        for (int i = 0; i < halfN; i++) {
            outputVector[i] = leftOutputVector[i] + rightOutputVector[i];
        }
        for (int i = halfN; i < n; i++) {
            outputVector[i] = leftOutputVector[i - halfN] - rightOutputVector[i - halfN];
        }
        return outputVector;
    }

    /**
     * Multiplies the input vector with the Hadamard matrix using fast Walsh-Hadamard transformation.
     * <p>
     * Given an input vector, its fast Walsh-Hadamard transformation multiplication computes v · H(n), where Ts are
     * treated as +1, and Fs are treated as -1. For example:
     * <li>[1, 1, 1, 1, 1, 1, 1, 1] · H(8) = [8, 0, 0, 0, 0, 0, 0, 0].</li>
     * <li>[1, 0, 1, 0, 0, 1, 1, 0] · H(8) = [4, 2, 0, -2, 0, 2, 0, 2].</li>
     * </p>
     *
     * @param inputArray the input array.
     * @return the result of v · H(n).
     */
    public static double[] fastWalshHadamardTrans(double[] inputArray) {
        int n = inputArray.length;
        Preconditions.checkArgument((n & (n - 1)) == 0, "n must be a power of 2: %s", n);
        return innerFastWalshHadamardTrans(inputArray);
    }

    private static double[] innerFastWalshHadamardTrans(double[] inputArray) {
        int n = inputArray.length;
        assert n > 0 : "n must be greater than 0: " + n;
        assert (n & (n - 1)) == 0 : "n must be a power of 2: " + n;
        /*
         * if k == 1:
         *     return dist
         * dist1 = dist[0 : k//2]
         * dist2 = dist[k//2 : k]
         * trans1 = FWHT_A(k//2, dist1)
         * trans2 = FWHT_A(k//2, dist2)
         * trans = np.concatenate((trans1 + trans2, trans1 - trans2))
         * return trans
         */
        if (n == 1) {
            return inputArray;
        }
        int halfN = n / 2;
        double[] leftInputVector = new double[halfN];
        double[] rightInputVector = new double[n - halfN];
        System.arraycopy(inputArray, 0, leftInputVector, 0 , leftInputVector.length);
        System.arraycopy(inputArray, halfN, rightInputVector, 0, rightInputVector.length);
        double[] leftOutputVector = innerFastWalshHadamardTrans(leftInputVector);
        double[] rightOutputVector = innerFastWalshHadamardTrans(rightInputVector);
        double[] outputVector = new double[n];
        for (int i = 0; i < halfN; i++) {
            outputVector[i] = leftOutputVector[i] + rightOutputVector[i];
        }
        for (int i = halfN; i < n; i++) {
            outputVector[i] = leftOutputVector[i - halfN] - rightOutputVector[i - halfN];
        }
        return outputVector;
    }

    /**
     * n比特 * n比特的哈达码矩阵
     */
    private final byte[][] hadamardCode;
    /**
     * 数据字比特长度
     */
    private final int datawordBitLength;
    /**
     * 数据字字节长度
     */
    private final int datawordByteLength;
    /**
     * 码字比特长度
     */
    private final int codewordBitLength;
    /**
     * 码字字节长度
     */
    private final int codewordByteLength;

    /**
     * Creates a Hadamard coder.
     *
     * @param k the dataword bit length.
     */
    public HadamardCoder(int k) {
        MathPreconditions.checkNonNegativeInRange("k", k, Integer.SIZE - 1);
        datawordBitLength = k;
        datawordByteLength = CommonUtils.getByteLength(datawordBitLength);
        int n = 1 << k;
        codewordBitLength = n;
        codewordByteLength = CommonUtils.getByteLength(codewordBitLength);
        if (k == 1) {
            hadamardCode = new byte[][]{
                {(byte) 0b00000000,},
                {(byte) 0b00000001,},
            };
        } else if (k == (1 << 1)) {
            hadamardCode = new byte[][]{
                {(byte) 0b00000000,},
                {(byte) 0b00000101,},
                {(byte) 0b00000011,},
                {(byte) 0b00000110,},
            };
        } else {
            // create a hadamard matrix with n = 2^k
            boolean[][] matrix = createHadamardMatrix(k);
            // we need to flip all elements in the hadamard matrix for ensuring linearity
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = !matrix[i][j];
                }
            }
            hadamardCode = Arrays.stream(matrix)
                .map(BinaryUtils::binaryToByteArray)
                .toArray(byte[][]::new);
        }
    }

    @Override
    public int getDatawordBitLength() {
        return datawordBitLength;
    }

    @Override
    public int getDatawordByteLength() {
        return datawordByteLength;
    }

    @Override
    public int getCodewordBitLength() {
        return codewordBitLength;
    }

    @Override
    public int getCodewordByteLength() {
        return codewordByteLength;
    }

    @Override
    public int getMinimalHammingDistance() {
        return codewordBitLength / 2;
    }

    @Override
    public byte[] encode(byte[] input) {
        assert input.length <= codewordByteLength;
        assert BytesUtils.isReduceByteArray(input, datawordBitLength);
        int index = IntUtils.fixedByteArrayToNonNegInt(input);
        return BytesUtils.clone(hadamardCode[index]);
    }

    @Override
    public String toString() {
        return binaryToString(hadamardCode);
    }

    /**
     * Represents the Hadamard matrix to a String.
     *
     * @param matrix the Hadamard matrix.
     * @return the String representation.
     */
    private String binaryToString(byte[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (byte[] rowBytes : matrix) {
            boolean[] row = BinaryUtils.byteArrayToBinary(rowBytes);
            for (boolean column : row) {
                builder.append(column ? 1 : 0);
                builder.append(' ');
            }
            builder.append('\n');
        }
        // 要减去最后一个\n
        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HadamardCoder)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        HadamardCoder that = (HadamardCoder) obj;
        return new EqualsBuilder().append(this.datawordBitLength, that.datawordBitLength).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(datawordBitLength).toHashCode();
    }
}
