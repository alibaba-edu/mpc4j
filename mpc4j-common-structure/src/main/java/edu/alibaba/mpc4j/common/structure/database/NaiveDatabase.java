package edu.alibaba.mpc4j.common.structure.database;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import edu.alibaba.mpc4j.common.structure.database.DatabaseFactory.DatabaseType;
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * naive database. Each data is an element in Z_{2^l} represented by BigInteger where l > 0.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public class NaiveDatabase implements ModBitNumDatabase {
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
    private BigInteger[] data;

    /**
     * Creates a database.
     *
     * @param l    element bit length.
     * @param data data.
     * @return a database.
     */
    public static NaiveDatabase create(int l, byte[][] data) {
        NaiveDatabase database = new NaiveDatabase(l);
        MathPreconditions.checkPositive("rows", data.length);
        database.data = Arrays.stream(data)
            .peek(element ->
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(element, database.byteL, database.l))
            )
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        return database;
    }

    /**
     * Creates a database.
     *
     * @param l    element bit length.
     * @param data data.
     * @return a database.
     */
    public static NaiveDatabase create(int l, BigInteger[] data) {
        NaiveDatabase database = new NaiveDatabase(l);
        MathPreconditions.checkPositive("rows", data.length);
        database.data = Arrays.stream(data)
            .peek(element -> {
                Preconditions.checkArgument(element.signum() >= 0);
                Preconditions.checkArgument(element.bitLength() <= l);
            })
            .toArray(BigInteger[]::new);
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
    public static NaiveDatabase createRandom(int l, int rows, SecureRandom secureRandom) {
        NaiveDatabase database = new NaiveDatabase(l);
        MathPreconditions.checkPositive("rows", rows);
        database.data = IntStream.range(0, rows)
            .mapToObj(index -> new BigInteger(l, secureRandom))
            .toArray(BigInteger[]::new);
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
    public static NaiveDatabase create(EnvType envType, boolean parallel, BitVector... bitVectors) {
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
        BigInteger[] data = IntStream.range(0, rows)
            .mapToObj(transBitMatrix::getColumn)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);

        return NaiveDatabase.create(l, data);
    }

    /**
     * Creates an empty database.
     *
     * @param l element bit length.
     * @return a database.
     */
    public static NaiveDatabase createEmpty(int l) {
        NaiveDatabase database = new NaiveDatabase(l);
        database.data = new BigInteger[0];

        return database;
    }

    private NaiveDatabase(int l) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
    }

    @Override
    public DatabaseFactory.DatabaseType getType() {
        return DatabaseFactory.DatabaseType.NAIVE;
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
        byte[][] byteArrayData = Arrays.stream(data)
            .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
            .toArray(byte[][]::new);
        DenseBitMatrix byteDenseBitMatrix = ByteDenseBitMatrix.createFromDense(l, byteArrayData);
        DenseBitMatrix transByteDenseBitMatrix = byteDenseBitMatrix.transpose(envType, parallel);
        return IntStream.range(0, l)
            .mapToObj(index -> BitVectorFactory.create(rows, transByteDenseBitMatrix.getByteArrayRow(index)))
            .toArray(BitVector[]::new);
    }

    @Override
    public ModBitNumDatabase split(int splitRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("split rows", splitRows, rows);
        BigInteger[] subData = new BigInteger[splitRows];
        BigInteger[] remainData = new BigInteger[rows - splitRows];
        System.arraycopy(data, 0, subData, 0, splitRows);
        System.arraycopy(data, splitRows, remainData, 0, rows - splitRows);
        data = remainData;
        return NaiveDatabase.create(l, subData);
    }

    @Override
    public void reduce(int reduceRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("reduce rows", reduceRows, rows);
        if (reduceRows < rows) {
            // reduce if the reduced rows is less than rows.
            BigInteger[] remainData = new BigInteger[reduceRows];
            System.arraycopy(data, 0, remainData, 0, reduceRows);
            data = remainData;
        }
    }

    @Override
    public void merge(Database other) {
        NaiveDatabase that = (NaiveDatabase) other;
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
        BigInteger[] mergeData = new BigInteger[this.data.length + that.data.length];
        System.arraycopy(this.data, 0, mergeData, 0, this.data.length);
        System.arraycopy(that.data, 0, mergeData, this.data.length, that.data.length);
        data = mergeData;
    }

    @Override
    public byte[][] getBytesData() {
        return Arrays.stream(data)
            .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL))
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getBytesData(int index) {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(data[index], byteL);
    }

    @Override
    public BigInteger[] getBigIntegerData() {
        return data;
    }

    @Override
    public BigInteger getBigIntegerData(int index) {
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
        if (obj instanceof NaiveDatabase) {
            NaiveDatabase that = (NaiveDatabase) obj;
            return new EqualsBuilder()
                .append(this.l, that.l)
                .append(this.data, that.data)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(data, Math.min(data.length, MatrixUtils.DISPLAY_NUM)))
            .map(element -> element.toString(16))
            .map(element -> element.toUpperCase(Locale.ROOT))
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (l = " + l + "): " + Arrays.toString(stringData);
    }

    /**
     * Partitions the database by the assigned partition L. Note that each L of the partitioned database is the
     * assigned partition L. For example, when the current L is 3, and the partition L is 9, then we create 1 partition
     * database with L = 9 (byteL = 2), but all first byte in the partitioned database are 0.
     *
     * @param partitionL the partition L.
     * @return the partition result.
     */
    public ZlDatabase[] partitionZl(int partitionL) {
        MathPreconditions.checkPositive("partitionL", partitionL);
        int partitionNum = CommonUtils.getUnitNum(l, partitionL);
        ZlDatabase[] partitionDatabases = new ZlDatabase[partitionNum];
        // and = 2^l - 1, where l is the partition L.
        BigInteger and = BigInteger.ONE.shiftLeft(partitionL).subtract(BigInteger.ONE);
        int partitionByteL = CommonUtils.getByteLength(partitionL);
        int rows = rows();
        // copy the data
        BigInteger[] tempData = new BigInteger[rows];
        System.arraycopy(data, 0, tempData, 0, rows);
        // we need to partition in reverse order so that we can then combine
        for (int partitionIndex = partitionNum - 1; partitionIndex >= 0; partitionIndex--) {
            byte[][] partitionData = new byte[rows][partitionByteL];
            for (int index = 0; index < rows; index++) {
                BigInteger element = tempData[index].and(and);
                tempData[index] = tempData[index].shiftRight(partitionL);
                partitionData[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(element, partitionByteL);
            }
            partitionDatabases[partitionIndex] = ZlDatabase.create(partitionL, partitionData);
        }
        return partitionDatabases;
    }

    /**
     * Creates a database by combining Zl databases.
     *
     * @param l         element bit length.
     * @param databases the combining databases.
     * @return a bytes vector.
     */
    public static NaiveDatabase createFromZl(int l, ZlDatabase... databases) {
        // check databases.length > 0
        MathPreconditions.checkPositive("databases.length", databases.length);
        int rows = databases[0].rows();
        // check all databases have the same rows
        Arrays.stream(databases).forEach(database ->
            MathPreconditions.checkEqual("rows", "database.rows", rows, database.rows())
        );
        // combine each database
        BigInteger[] data = new BigInteger[rows];
        Arrays.fill(data, BigInteger.ZERO);
        for (Database database : databases) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                BigInteger partitionData = database.getBigIntegerData(rowIndex);
                data[rowIndex] = data[rowIndex]
                    .shiftLeft(database.getL())
                    .or(partitionData);
            }
        }
        // verify that all combined vectors has at most upper-bound bit length
        Arrays.stream(data).forEach(element -> Preconditions.checkArgument(element.bitLength() <= l));

        return NaiveDatabase.create(l, data);
    }

    /**
     * Partitions the database by the assigned partition L. Note that each L of the partitioned database is the
     * assigned partition L. For example, when the current L is 3, and the partition L is 9, then we create 1 partition
     * database with L = 9 (byteL = 2), but all first byte in the partitioned database are 0.
     *
     * @param partitionL the partition L.
     * @return the partition result.
     */
    public Zl64Database[] partitionZl64(int partitionL) {
        MathPreconditions.checkPositiveInRangeClosed("partitionL", partitionL, DatabaseFactory.maxBitDatabaseL(DatabaseType.ZL64));
        int partitionNum = CommonUtils.getUnitNum(l, partitionL);
        Zl64Database[] partitionDatabases = new Zl64Database[partitionNum];
        // and = 2^l - 1, where l is the partition L.
        BigInteger and = BigInteger.ONE.shiftLeft(partitionL).subtract(BigInteger.ONE);
        int rows = rows();
        // copy the data
        BigInteger[] tempData = new BigInteger[rows];
        System.arraycopy(data, 0, tempData, 0, rows);
        // we need to partition in reverse order so that we can then combine
        for (int partitionIndex = partitionNum - 1; partitionIndex >= 0; partitionIndex--) {
            long[] partitionData = new long[rows];
            for (int index = 0; index < rows; index++) {
                BigInteger element = tempData[index].and(and);
                tempData[index] = tempData[index].shiftRight(partitionL);
                partitionData[index] = element.longValue();
            }
            partitionDatabases[partitionIndex] = Zl64Database.create(partitionL, partitionData);
        }
        return partitionDatabases;
    }

    /**
     * Creates a database by combining Zl64 databases.
     *
     * @param l         element bit length.
     * @param databases the combining databases.
     * @return a bytes vector.
     */
    public static NaiveDatabase createFromZl64(int l, Zl64Database... databases) {
        // check databases.length > 0
        MathPreconditions.checkPositive("databases.length", databases.length);
        int rows = databases[0].rows();
        // check all databases have the same rows
        Arrays.stream(databases).forEach(database ->
            MathPreconditions.checkEqual("rows", "database.rows", rows, database.rows())
        );
        // combine each database
        BigInteger[] data = new BigInteger[rows];
        Arrays.fill(data, BigInteger.ZERO);
        for (Database database : databases) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                BigInteger partitionData = database.getBigIntegerData(rowIndex);
                data[rowIndex] = data[rowIndex]
                    .shiftLeft(database.getL())
                    .or(partitionData);
            }
        }
        // verify that all combined vectors has at most upper-bound bit length
        Arrays.stream(data).forEach(element -> Preconditions.checkArgument(element.bitLength() <= l));

        return NaiveDatabase.create(l, data);
    }

    /**
     * Partitions the database by the assigned partition L. Note that each L of the partitioned database is the
     * assigned partition L. For example, when the current L is 3, and the partition L is 9, then we create 1 partition
     * database with L = 9 (byteL = 2), but all first byte in the partitioned database are 0.
     *
     * @param partitionL the partition L.
     * @return the partition result.
     */
    public Zl32Database[] partitionZl32(int partitionL) {
        MathPreconditions.checkPositiveInRangeClosed("partitionL", partitionL, DatabaseFactory.maxBitDatabaseL(DatabaseType.ZL32));
        int partitionNum = CommonUtils.getUnitNum(l, partitionL);
        Zl32Database[] partitionDatabases = new Zl32Database[partitionNum];
        // and = 2^l - 1, where l is the partition L.
        BigInteger and = BigInteger.ONE.shiftLeft(partitionL).subtract(BigInteger.ONE);
        int rows = rows();
        // copy the data
        BigInteger[] tempData = new BigInteger[rows];
        System.arraycopy(data, 0, tempData, 0, rows);
        // we need to partition in reverse order so that we can then combine
        for (int partitionIndex = partitionNum - 1; partitionIndex >= 0; partitionIndex--) {
            int[] partitionData = new int[rows];
            for (int index = 0; index < rows; index++) {
                BigInteger element = tempData[index].and(and);
                tempData[index] = tempData[index].shiftRight(partitionL);
                partitionData[index] = element.intValue();
            }
            partitionDatabases[partitionIndex] = Zl32Database.create(partitionL, partitionData);
        }
        return partitionDatabases;
    }

    /**
     * Creates a database by combining Zl32 databases.
     *
     * @param l         element bit length.
     * @param databases the combining databases.
     * @return a bytes vector.
     */
    public static NaiveDatabase createFromZl32(int l, Zl32Database... databases) {
        // check databases.length > 0
        MathPreconditions.checkPositive("databases.length", databases.length);
        int rows = databases[0].rows();
        // check all databases have the same rows
        Arrays.stream(databases).forEach(database ->
            MathPreconditions.checkEqual("rows", "database.rows", rows, database.rows())
        );
        // combine each database
        BigInteger[] data = new BigInteger[rows];
        Arrays.fill(data, BigInteger.ZERO);
        for (Database database : databases) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                BigInteger partitionData = database.getBigIntegerData(rowIndex);
                data[rowIndex] = data[rowIndex]
                    .shiftLeft(database.getL())
                    .or(partitionData);
            }
        }
        // verify that all combined vectors has at most upper-bound bit length
        Arrays.stream(data).forEach(element -> Preconditions.checkArgument(element.bitLength() <= l));

        return NaiveDatabase.create(l, data);
    }
}
