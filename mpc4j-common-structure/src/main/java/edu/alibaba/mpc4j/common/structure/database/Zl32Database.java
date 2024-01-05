package edu.alibaba.mpc4j.common.structure.database;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zl32 database. Each data is an element in Z_{2^l} represented by int where l ∈ (0, IntUtil.MAX_L].
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Zl32Database implements ModBitNumDatabase {
    /**
     * element bit length
     */
    private final int l;
    /**
     * element byte length
     */
    private final int byteL;
    /**
     * 2^l
     */
    private final int rangeBound;
    /**
     * offset (in bit)
     */
    private final int offset;
    /**
     * offset (in byte)
     */
    private final int byteOffset;
    /**
     * data
     */
    private int[] data;

    /**
     * Creates a database.
     *
     * @param l    element bit length.
     * @param data data.
     * @return a database.
     */
    public static Zl32Database create(int l, byte[][] data) {
        Zl32Database database = new Zl32Database(l);
        MathPreconditions.checkPositive("rows", data.length);
        database.data = Arrays.stream(data)
            .peek(element ->
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(element, database.byteL, database.l))
            )
            .mapToInt(reducedBytesData -> {
                int byteOffset = Integer.BYTES - reducedBytesData.length;
                byte[] bytesData = new byte[Integer.BYTES];
                System.arraycopy(reducedBytesData, 0, bytesData, byteOffset, reducedBytesData.length);
                return IntUtils.byteArrayToInt(bytesData);
            })
            .toArray();
        return database;
    }

    /**
     * Creates a database.
     *
     * @param l    element bit length.
     * @param data data.
     * @return a database.
     */
    public static Zl32Database create(int l, int[] data) {
        Zl32Database database = new Zl32Database(l);
        MathPreconditions.checkPositive("rows", data.length);
        database.data = Arrays.stream(data)
            .peek(element -> MathPreconditions.checkNonNegativeInRange("element", element, database.rangeBound))
            .toArray();
        return database;
    }

    /**
     * Creates a random database.
     *
     * @param l            element bit length.
     * @param rows         number of rows.
     * @param secureRandom the random state.
     * @return a database.
     */
    public static Zl32Database createRandom(int l, int rows, SecureRandom secureRandom) {
        Zl32Database database = new Zl32Database(l);
        MathPreconditions.checkPositive("rows", rows);
        database.data = IntStream.range(0, rows)
            .map(index -> IntUtils.randomNonNegative(database.rangeBound, secureRandom))
            .toArray();
        return database;
    }

    /**
     * Creates a database by combining bit vectors.
     *
     * @param bitVectors the bit vectors.
     * @return a database.
     */
    public static Zl32Database create(EnvType envType, boolean parallel, BitVector... bitVectors) {
        // check BitVectors.length is in range (0, MAX_L]
        MathPreconditions.checkPositiveInRangeClosed("BitVectors.length", bitVectors.length, IntUtils.MAX_L);
        int l = bitVectors.length;
        // check all bit vectors has the same bit num
        int rows = bitVectors[0].bitNum();
        MathPreconditions.checkPositive("rows", rows);
        Arrays.stream(bitVectors).forEach(bitVector ->
            MathPreconditions.checkEqual("rows", "BitVector.bitNum", rows, bitVector.bitNum())
        );
        int offset = Integer.SIZE - l;
        TransBitMatrix bitMatrix = TransBitMatrixFactory.createInstance(envType, rows, Integer.SIZE, parallel);
        for (int columnIndex = 0; columnIndex < l; columnIndex++) {
            bitMatrix.setColumn(columnIndex + offset, bitVectors[columnIndex].getBytes());
        }
        TransBitMatrix transBitMatrix = bitMatrix.transpose();
        int[] data = IntStream.range(0, rows)
            .mapToObj(transBitMatrix::getColumn)
            .mapToInt(IntUtils::byteArrayToInt)
            .toArray();
        // create the result
        return Zl32Database.create(l, data);
    }

    /**
     * Creates an empty database.
     *
     * @param l element bit length.
     * @return a database.
     */
    public static Zl32Database createEmpty(int l) {
        Zl32Database database = new Zl32Database(l);
        database.data = new int[0];

        return database;
    }

    private Zl32Database(int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1 << l;
        offset = Integer.SIZE - l;
        byteOffset = Integer.BYTES - byteL;
    }


    @Override
    public DatabaseFactory.DatabaseType getType() {
        return DatabaseFactory.DatabaseType.ZL32;
    }

    @Override
    public int rows() {
        return data.length;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public BitVector[] bitPartition(EnvType envType, boolean parallel) {
        int rows = rows();
        byte[][] bytesData = Arrays.stream(data).mapToObj(IntUtils::intToByteArray).toArray(byte[][]::new);
        DenseBitMatrix byteDenseBitMatrix = ByteDenseBitMatrix.createFromDense(Integer.SIZE, bytesData);
        DenseBitMatrix transByteDenseBitMatrix = byteDenseBitMatrix.transpose(envType, parallel);
        return IntStream.range(offset, Integer.SIZE)
            .mapToObj(index -> BitVectorFactory.create(rows, transByteDenseBitMatrix.getByteArrayRow(index)))
            .toArray(BitVector[]::new);
    }

    @Override
    public Zl32Database split(int splitRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("split rows", splitRows, rows);
        int[] subData = new int[splitRows];
        int[] remainData = new int[rows - splitRows];
        System.arraycopy(data, 0, subData, 0, splitRows);
        System.arraycopy(data, splitRows, remainData, 0, rows - splitRows);
        data = remainData;
        return Zl32Database.create(l, subData);
    }

    @Override
    public void reduce(int reduceRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("reduce rows", reduceRows, rows);
        if (reduceRows < rows) {
            // reduce if the reduced rows is less than rows.
            int[] remainData = new int[reduceRows];
            System.arraycopy(data, 0, remainData, 0, reduceRows);
            data = remainData;
        }
    }

    @Override
    public void merge(Database other) {
        Zl32Database that = (Zl32Database) other;
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
        int[] mergeData = new int[this.data.length + that.data.length];
        System.arraycopy(this.data, 0, mergeData, 0, this.data.length);
        System.arraycopy(that.data, 0, mergeData, this.data.length, that.data.length);
        data = mergeData;
    }

    @Override
    public byte[][] getBytesData() {
        return Arrays.stream(data)
            .mapToObj(IntUtils::intToByteArray)
            .map(element -> {
                byte[] reducedBytesData = new byte[byteL];
                System.arraycopy(element, byteOffset, reducedBytesData, 0, byteL);
                return reducedBytesData;
            })
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getBytesData(int index) {
        byte[] bytesData = IntUtils.intToByteArray(data[index]);
        byte[] reducedBytesData = new byte[byteL];
        System.arraycopy(bytesData, byteOffset, reducedBytesData, 0, byteL);
        return reducedBytesData;
    }

    @Override
    public BigInteger[] getBigIntegerData() {
        return Arrays.stream(data)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
    }

    @Override
    public BigInteger getBigIntegerData(int index) {
        return BigInteger.valueOf(data[index]);
    }

    /**
     * Gets the data.
     *
     * @return the data.
     */
    public int[] getData() {
        return data;
    }

    /**
     * Gets the element.
     *
     * @param index the index.
     * @return the element.
     */
    public int getData(int index) {
        return data[index];
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(l)
            .append(data)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zl32Database) {
            Zl32Database that = (Zl32Database) obj;
            return new EqualsBuilder()
                .append(this.l, that.l)
                .append(this.data, that.data)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (l = " + l + "): "
            + Arrays.toString(Arrays.copyOf(data, Math.min(data.length, MatrixUtils.DISPLAY_NUM)));
    }
}
