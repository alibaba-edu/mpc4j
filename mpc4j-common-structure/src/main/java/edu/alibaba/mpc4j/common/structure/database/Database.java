package edu.alibaba.mpc4j.common.structure.database;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.math.BigInteger;

/**
 * database interface.
 *
 * @author Weiran Liu
 * @date 2023/4/4
 */
public interface Database {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    DatabaseFactory.DatabaseType getType();

    /**
     * Gets number of rows.
     *
     * @return number of rows.
     */
    int rows();

    /**
     * Gets the maximal l (in bits) so that all elements in {0, 1}^l is a valid element.
     *
     * @return the maximal l (in bits).
     */
    int getL();

    /**
     * Gets the maximal l (in bytes) so that all elements in {0, 1}^l is a valid element.
     *
     * @return the maximal l (in bytes).
     */
    int getByteL();

    /**
     * Partitions the bytes vector by columns.
     *
     * @param envType  the environment.
     * @param parallel parallel operation.
     * @return the partition result.
     */
    BitVector[] bitPartition(EnvType envType, boolean parallel);

    /**
     * Splits the database with the split rows.
     *
     * @param splitRows the split rows.
     * @return a new database with the split rows.
     */
    Database split(int splitRows);

    /**
     * Reduces the database to the reduced rows.
     *
     * @param reduceRows the reduced rows.
     */
    void reduce(int reduceRows);

    /**
     * Merges two databases.
     *
     * @param other the other database.
     */
    void merge(Database other);

    /**
     * Gets the data in byte[].
     *
     * @return the data in byte[].
     */
    byte[][] getBytesData();

    /**
     * Gets the data in byte[].
     *
     * @param index the index.
     * @return the data in byte[].
     */
    byte[] getBytesData(int index);

    /**
     * Gets the data in BigInteger.
     *
     * @return the data in BigInteger.
     */
    BigInteger[] getBigIntegerData();

    /**
     * Gets the data in BigInteger.
     *
     * @param index the index.
     * @return the data in BigInteger.
     */
    BigInteger getBigIntegerData(int index);
}
