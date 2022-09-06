package edu.alibaba.mpc4j.common.tool.coder.linear;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Conversion;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

/**
 * BCH编码器。参考libOTe中LinearCode的实现：https://github.com/osu-crypto/libOTe/blob/master/libOTe/Tools/LinearCode.cpp。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public abstract class AbstractBchCoder implements LinearCoder {
    /**
     * 生成矩阵的存储路径
     */
    private static final String FILE_PATH = "gen_matrix" + File.separator;
    /**
     * 生成矩阵的分组长度
     */
    private static final int CODE_BLOCK_BIT_LENGTH = 128;
    /**
     * 明文的比特长度
     */
    private final int datawordBitLength;
    /**
     * 明文的字节长度
     */
    private final int datawordByteLength;
    /**
     * 码字的比特长度
     */
    private final int codewordBitLength;
    /**
     * 码字的字节长度
     */
    private final int codewordByteLength;
    /**
     * 按照码字长度存储的生成矩阵
     */
    private final byte[][] generateMatrix;
    /**
     * 查找表
     */
    private final ArrayList<byte[][]> lookupTable;

    /**
     * 以文件的形式输入字节数组，构造线性码。
     *
     * @param datawordBitLength 明文比特长度。
     * @param codewordBitLength 码字比特长度。
     */
    AbstractBchCoder(int datawordBitLength, int codewordBitLength) {
        // 初始化参数
        this.datawordBitLength = datawordBitLength;
        datawordByteLength = CommonUtils.getByteLength(this.datawordBitLength);
        this.codewordBitLength = codewordBitLength;
        codewordByteLength = CommonUtils.getByteLength(this.codewordBitLength);
        generateMatrix = new byte[this.datawordByteLength * Byte.SIZE][this.codewordByteLength];
        lookupTable = new ArrayList<>(datawordByteLength);
        // 读取生成矩阵文件
        String fileName = FILE_PATH + "mx" + datawordBitLength + "by" + codewordBitLength + ".txt";
        InputStream inputStream = Objects.requireNonNull(
            AbstractBchCoder.class.getClassLoader().getResourceAsStream(fileName)
        );
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        // 每行128比特，不够长度补0，先计算码字分组长度，向上取整，码字分组长度 * 明文比特长度 * 码字分组字节长度 = 生成矩阵字节数
        int codewordBlockSize = CommonUtils.getUnitNum(codewordBitLength, CODE_BLOCK_BIT_LENGTH);
        int codewordRoundBitLength = codewordBlockSize * datawordBitLength;
        int codewordRoundByteLength = codewordRoundBitLength * CODE_BLOCK_BIT_LENGTH / Byte.SIZE;
        byte[] flattenGenerateMatrix = new byte[codewordRoundByteLength];
        try {
            String line;
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {
                // 每行按逗号分隔，依次读取成byte
                String[] splits = line.split(",");
                Preconditions.checkArgument(splits.length == CODE_BLOCK_BIT_LENGTH / Byte.SIZE);
                for (int i = 0; i < CODE_BLOCK_BIT_LENGTH / Byte.SIZE; i++) {
                    flattenGenerateMatrix[count * CODE_BLOCK_BIT_LENGTH / Byte.SIZE + i]
                        = Hex.decode(splits[i].trim().substring(2))[0];
                }
                count++;
            }
            // 如果读取的数据行数不等于码字归约比特长度，意味着文件中生成矩阵的长度不正确
            assert count == codewordRoundBitLength;
            // 初始化线性编码
            initLinearCoder(flattenGenerateMatrix);
            // 生成查找表
            generateLookupTable();
        } catch (IOException e) {
            throw new IllegalStateException("ERROR: Failed to read generate matrix from file:" + fileName);
        }
    }

    /**
     * 初始化线性编码构造函数。
     *
     * @param flattenGenerateMatrix 平铺的生成矩阵。
     */
    private void initLinearCoder(byte[] flattenGenerateMatrix) {
        // 传进来的是plaintextBitLength * codewordBitLength大的矩阵，每个分组存一行
        // 为了保证一致性，把codewordBitLength向上取整到分组长度，再乘以传进来的是plaintextBitLength
        int codewordBlockSize = CommonUtils.getUnitNum(codewordBitLength, CODE_BLOCK_BIT_LENGTH);
        // 因此，生成矩阵总行数 = 分组长度 * 明文比特长度
        int size = codewordBlockSize * datawordBitLength;
        // 总字节数 = 生成矩阵总行数 * 每行16个字节
        Preconditions.checkArgument(flattenGenerateMatrix.length == size * CODE_BLOCK_BIT_LENGTH / Byte.SIZE);
        // C++和Java对于byte的比特表示顺序不一样，需要转一下
        for (int i = 0; i < flattenGenerateMatrix.length; i++) {
            flattenGenerateMatrix[i] = BytesUtils.reverseBit(flattenGenerateMatrix[i]);
        }
        // 先把前面缺的位置补上0
        int offset = datawordByteLength * Byte.SIZE - datawordBitLength;
        int codewordOffset = codewordByteLength * Byte.SIZE - codewordBitLength;
        for (int offsetIndex = 0; offsetIndex < offset; offsetIndex++) {
            generateMatrix[offsetIndex] = new byte[codewordByteLength];
        }
        for (int row = 0; row < this.datawordBitLength; row++) {
            generateMatrix[row + offset] = new byte[codewordByteLength];
            // 每轮读取codewordBlockSize * 16的字节，拷贝到generateMatrix
            System.arraycopy(flattenGenerateMatrix, row * codewordBlockSize * CODE_BLOCK_BIT_LENGTH / Byte.SIZE,
                generateMatrix[row + offset], 0, codewordByteLength);
            // C++里面的编码是右侧补0，所以Java要向右移动，使得左侧补0
            BytesUtils.shiftRighti(generateMatrix[row + offset], codewordOffset);
        }
    }

    private void generateLookupTable() {
        // 将输入按照字节拆分，每个字节构造一个大小为2^{Byte.Size}的查找表
        int stride = 1 << Byte.SIZE;
        for (int index = 0; index < datawordByteLength; index++) {
            byte[][] indexLookupTable = new byte[stride][];
            // 遍历所有的取值，每个取值计算一个中间结果
            for (int intValue = 0; intValue < stride; intValue++) {
                boolean[] binaryValue = new boolean[Byte.SIZE];
                Conversion.intToBinary(intValue, 0, binaryValue, 0, binaryValue.length);
                ArrayUtils.reverse(binaryValue);
                indexLookupTable[intValue] = new byte[codewordByteLength];
                for (int binaryIndex = 0; binaryIndex < binaryValue.length; binaryIndex++) {
                    if (binaryValue[binaryIndex]) {
                        BytesUtils.xori(indexLookupTable[intValue], generateMatrix[index * Byte.SIZE + binaryIndex]);
                    }
                }
            }
            lookupTable.add(indexLookupTable);
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
        // BCH编码默认汉明距离为128比特
        return CommonConstants.BLOCK_BIT_LENGTH;
    }

    @Override
    public byte[] encode(byte[] input) {
        assert input.length <= codewordByteLength;
        assert BytesUtils.isReduceByteArray(input, datawordBitLength);
        byte[] paddingInput = new byte[datawordByteLength];
        System.arraycopy(input, 0, paddingInput, paddingInput.length - input.length, input.length);
        byte[] codeword = new byte[codewordByteLength];
        for (int inputIndex = 0; inputIndex < paddingInput.length; inputIndex++) {
            BytesUtils.xori(codeword, lookupTable.get(inputIndex)[paddingInput[inputIndex] & 0xFF]);
        }

        return codeword;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte[] generateMatrixRow : this.generateMatrix) {
            String binary = BigIntegerUtils.byteArrayToNonNegBigInteger(generateMatrixRow).toString(2);
            int padding = this.codewordBitLength - binary.length();
            while (padding > 0) {
                stringBuilder.append("0");
                padding--;
            }
            stringBuilder.append(binary).append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractBchCoder)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        AbstractBchCoder that = (AbstractBchCoder)obj;
        return new EqualsBuilder()
            .append(this.datawordBitLength, that.datawordBitLength)
            .append(this.codewordBitLength, that.codewordBitLength)
            .append(this.generateMatrix, that.generateMatrix)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(datawordBitLength)
            .append(codewordBitLength)
            .append(generateMatrix)
            .toHashCode();
    }
}
