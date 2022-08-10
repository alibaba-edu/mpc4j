package edu.alibaba.mpc4j.common.tool.coder.linear;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * 哈达码（Hadamard）编码器。哈达码是一个线性码，将n比特长的输入编码为长度为2^n的码字，且任意两个非0码字的汉明距离均为2^{n-1}。
 * 参考代码来自于：https://github.com/rick7661/HadamardCoder/blob/master/Hadamard.java.
 * 算法来自于：http://introcs.cs.princeton.edu/java/14array/Hadamard.java.html
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public class HadamardCoder implements LinearCoder {
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
     * 构造哈达码编码器。
     *
     * @param k 哈达码编码的输入比特长度。
     */
    public HadamardCoder(int k) {
        // 要求0 < k < 31，否则矩阵长度会超过int的最大长度
        assert k > 0 && k < Integer.SIZE - 1;
        datawordBitLength = k;
        datawordByteLength = CommonUtils.getByteLength(datawordBitLength);
        int n = 1 << k;
        codewordBitLength = n;
        codewordByteLength = CommonUtils.getByteLength(codewordBitLength);
        if (k == 1) {
            hadamardCode = new byte[][] {
                {(byte)0b00000000,},
                {(byte)0b00000001,},
            };
        } else if (k == 2) {
            hadamardCode = new byte[][] {
                {(byte)0b00000000,},
                {(byte)0b00000101,},
                {(byte)0b00000011,},
                {(byte)0b00000110,},
            };
        } else {
            // 初始化维度为n = 2^k的哈达码矩阵
            boolean[][] hadamardBinaryMatrix = new boolean[n][n];
            hadamardBinaryMatrix[0][0] = true;
            /*
             * 构建哈达码矩阵上半部分，原理参见https://introcs.cs.princeton.edu/java/14array/，Creative Exercises第29题的描述：
             * H(1) is a 1-by-1 matrix with the single entry true,
             * and for n > 1, H(2n) is obtained by aligning four copies of H(n) in a large square,
             * and then inverting all entries in the lower right n-by-n copy, as shown in the following example.
             * H(1)  H(2)    H(4)
             * -------------------
             *  T    T T   T T T T
             *       T F   T F T F
             *             T T F F
             * -------------------
             */
            for (int m = 1; m < n; m += m) {
                for (int i = 0; i < m; i++) {
                    for (int j = 0; j < m; j++) {
                        hadamardBinaryMatrix[i + m][j] = hadamardBinaryMatrix[i][j];
                        hadamardBinaryMatrix[i][j + m] = hadamardBinaryMatrix[i][j];
                        hadamardBinaryMatrix[i + m][j + m] = (!hadamardBinaryMatrix[i][j]);
                    }
                }
            }
            // 还需要把结果翻转一遍，否则无法满足线性关系
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    hadamardBinaryMatrix[i][j] = !hadamardBinaryMatrix[i][j];
                }
            }

            hadamardCode = Arrays.stream(hadamardBinaryMatrix)
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
     * 将哈达码矩阵表示为字符串形式。
     *
     * @param matrix 矩阵。
     * @return 字符串表示的哈达码矩阵。
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
        HadamardCoder that = (HadamardCoder)obj;
        return new EqualsBuilder().append(this.datawordBitLength, that.datawordBitLength).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(datawordBitLength).toHashCode();
    }
}
