package edu.alibaba.mpc4j.common.structure.database;

/**
 * mod BigInteger database. Elements in such database are in [0, n) where n is a BigInteger.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public interface ModBigIntDatabase extends Database {
    /**
     * Splits the database with the split rows.
     *
     * @param splitRows the split rows.
     * @return a new database with the split rows.
     */
    @Override
    ModBigIntDatabase split(int splitRows);
}
