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
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zl database. Each data is an element in Z_{2^l} represented by byte[] where l > 0.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
public class ZlDatabase implements ModBitNumDatabase {
    /**
     * element bit length
     */
    private final int l;
    /**
     * element byte length
     */
    private final int byteL;
    /**
     * data
     */
    private byte[][] data;

    /**
     * Creates a database.
     *
     * @param l    element bit length.
     * @param data data.
     * @return a database.
     */
    public static ZlDatabase create(int l, byte[][] data) {
        ZlDatabase database = new ZlDatabase(l);
        MathPreconditions.checkPositive("rows", data.length);
        database.data = Arrays.stream(data)
            .peek(bytes -> Preconditions.checkArgument(
                BytesUtils.isFixedReduceByteArray(bytes, database.byteL, database.l)
            ))
            .toArray(byte[][]::new);
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
    public static ZlDatabase createRandom(int l, int rows, SecureRandom secureRandom) {
        ZlDatabase database = new ZlDatabase(l);
        MathPreconditions.checkPositive("rows", rows);
        database.data = IntStream.range(0, rows)
            .mapToObj(index -> BytesUtils.randomByteArray(database.byteL, database.l, secureRandom))
            .toArray(byte[][]::new);
        return database;
    }

    /**
     * Creates a database by combining bit vectors.
     *
     * @param envType    the environment.
     * @param parallel   parallel combination.
     * @param bitVectors the combining bit vectors.
     * @return a database.
     */
    public static ZlDatabase create(EnvType envType, boolean parallel, BitVector... bitVectors) {
        MathPreconditions.checkPositive("BitVectors.length", bitVectors.length);
        int l = bitVectors.length;
        int rows = bitVectors[0].bitNum();
        // check all bit vectors has the same bit num
        Arrays.stream(bitVectors).forEach(bitVector ->
            MathPreconditions.checkEqual("rows", "BitVector.bitNum", rows, bitVector.bitNum())
        );
        TransBitMatrix bitMatrix = TransBitMatrixFactory.createInstance(envType, rows, l, parallel);
        for (int columnIndex = 0; columnIndex < l; columnIndex++) {
            bitMatrix.setColumn(columnIndex, bitVectors[columnIndex].getBytes());
        }
        TransBitMatrix transBitMatrix = bitMatrix.transpose();
        byte[][] data = IntStream.range(0, rows)
            .mapToObj(transBitMatrix::getColumn)
            .toArray(byte[][]::new);

        return create(l, data);
    }

    /**
     * Creates an empty database.
     *
     * @param l element bit length.
     * @return a database.
     */
    public static ZlDatabase createEmpty(int l) {
        ZlDatabase database = new ZlDatabase(l);
        database.data = new byte[0][];

        return database;
    }

    private ZlDatabase(int l) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
    }

    @Override
    public DatabaseFactory.DatabaseType getType() {
        return DatabaseFactory.DatabaseType.ZL;
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
        DenseBitMatrix byteDenseBitMatrix = ByteDenseBitMatrix.createFromDense(l, data);
        DenseBitMatrix transByteDenseBitMatrix = byteDenseBitMatrix.transpose(envType, parallel);
        return IntStream.range(0, l)
            .mapToObj(index -> BitVectorFactory.create(rows, transByteDenseBitMatrix.getByteArrayRow(index)))
            .toArray(BitVector[]::new);
    }

    @Override
    public ModBitNumDatabase split(int splitRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("split rows", splitRows, rows);
        byte[][] subData = new byte[splitRows][];
        byte[][] remainData = new byte[rows - splitRows][];
        System.arraycopy(data, 0, subData, 0, splitRows);
        System.arraycopy(data, splitRows, remainData, 0, rows - splitRows);
        data = remainData;
        return ZlDatabase.create(l, subData);
    }

    @Override
    public void reduce(int reduceRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("reduce rows", reduceRows, rows);
        if (reduceRows < rows) {
            // reduce if the reduced rows is less than rows.
            byte[][] remainData = new byte[reduceRows][];
            System.arraycopy(data, 0, remainData, 0, reduceRows);
            data = remainData;
        }
    }

    @Override
    public void merge(Database other) {
        ZlDatabase that = (ZlDatabase) other;
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
        byte[][] mergeData = new byte[this.data.length + that.data.length][];
        System.arraycopy(this.data, 0, mergeData, 0, this.data.length);
        System.arraycopy(that.data, 0, mergeData, this.data.length, that.data.length);
        data = mergeData;
    }

    @Override
    public byte[][] getBytesData() {
        return data;
    }

    @Override
    public byte[] getBytesData(int index) {
        return data[index];
    }

    @Override
    public BigInteger[] getBigIntegerData() {
        return Arrays.stream(data)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
    }

    @Override
    public BigInteger getBigIntegerData(int index) {
        return BigIntegerUtils.byteArrayToNonNegBigInteger(data[index]);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(l);
        Arrays.stream(data).forEach(hashCodeBuilder::append);
        return hashCodeBuilder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZlDatabase) {
            ZlDatabase that = (ZlDatabase) obj;
            if (this.rows() != that.rows()) {
                return false;
            }
            int rows = rows();
            EqualsBuilder equalsBuilder = new EqualsBuilder();
            equalsBuilder.append(this.l, that.l);
            IntStream.range(0, rows).forEach(index -> equalsBuilder.append(this.data[index], that.data[index]));
            return equalsBuilder.isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(data, Math.min(data.length, MatrixUtils.DISPLAY_NUM)))
            .map(Hex::toHexString)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (l = " + l + "): " + Arrays.toString(stringData);
    }
}
