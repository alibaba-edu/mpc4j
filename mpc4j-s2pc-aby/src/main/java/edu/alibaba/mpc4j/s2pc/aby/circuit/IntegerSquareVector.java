package edu.alibaba.mpc4j.s2pc.aby.circuit;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.stream.IntStream;

/**
 * 整数方括号向量。
 *
 * @author Weiran Liu
 * @date 2022/12/13
 */
public class IntegerSquareVector {
    /**
     * 比特长度
     */
    public static final int SIZE = Integer.SIZE;
    /**
     * 数量
     */
    private int num;
    /**
     * 二进制方括号向量
     */
    private SquareSbitVector[] binaryVectors;
    /**
     * 是否为明文状态
     */
    private boolean isPublic;

    /**
     * 构造明文整数方括号向量。
     *
     * @param values 整数值向量。
     * @return 明文整数方括号向量。
     */
    public static IntegerSquareVector create(int[] values) {
        return create(EnvType.STANDARD, true, values);
    }

    /**
     * 构造明文整数方括号向量。
     *
     * @param envType 环境类型。
     * @param parallel 是否并行处理。
     * @param values 整数值向量。
     * @return 明文整数方括号向量。
     */
    public static IntegerSquareVector create(EnvType envType, boolean parallel, int[] values) {
        assert values.length > 0 : "values.length must be greater than 0: " + values.length;
        // encode and transpose
        TransBitMatrix bitMatrix = TransBitMatrixFactory.createInstance(envType, SIZE, values.length, parallel);
        IntStream.range(0, values.length).forEach(index -> {
            byte[] byteValue = IntUtils.intToByteArray(values[index]);
            bitMatrix.setColumn(index, byteValue);
        });
        TransBitMatrix transBitMatrix = bitMatrix.transpose();
        // create IntegerSquareVector
        IntegerSquareVector integerSquareVector = new IntegerSquareVector();
        integerSquareVector.num = values.length;
        integerSquareVector.isPublic = true;
        integerSquareVector.binaryVectors = new SquareSbitVector[SIZE];
        for (int index = 0; index < SIZE; index++) {
            integerSquareVector.binaryVectors[index] = SquareSbitVector.create(
                integerSquareVector.num, transBitMatrix.getColumn(index), true
            );
        }
        return integerSquareVector;
    }

    /**
     * 构造密文整数方括号向量。
     *
     * @param binaryVectors 布尔向量。
     * @return 密文整数方括号向量。
     */
    public static IntegerSquareVector create(SquareSbitVector[] binaryVectors) {
        // 允许输入长度小于SIZE，前面补明文0
        assert binaryVectors.length > 0 && binaryVectors.length <= SIZE
            : "the length of binary vectors must be in range (0, " + SIZE + "]: " + binaryVectors.length;
        // create IntegerSquareVector
        IntegerSquareVector integerSquareVector = new IntegerSquareVector();
        integerSquareVector.num = binaryVectors[0].bitNum();
        integerSquareVector.isPublic = binaryVectors[0].isPlain();
        integerSquareVector.binaryVectors = new SquareSbitVector[SIZE];
        for (int index = 0; index < binaryVectors.length; index++) {
            assert binaryVectors[index].bitNum() == integerSquareVector.num
                : "the " + index + "-th binary vector must contain the same bits "
                + "(" + integerSquareVector.num + "): " + binaryVectors[index].bitNum();
            assert binaryVectors[index].isPlain() == integerSquareVector.isPublic
                : "the " + index + "-th binary vector must have the same public state "
                + "(" + integerSquareVector.isPublic + "): " + binaryVectors[index].isPlain();
            integerSquareVector.binaryVectors[index] = binaryVectors[index];
        }
        // padding remaining binary vectors with 0
        for (int index = binaryVectors.length; index < SIZE; index++) {
            integerSquareVector.binaryVectors[index] = SquareSbitVector.createZeros(integerSquareVector.num);
        }
        return integerSquareVector;
    }

    /**
     * 私有构造函数。
     */
    private IntegerSquareVector() {
        // empty
    }
}
