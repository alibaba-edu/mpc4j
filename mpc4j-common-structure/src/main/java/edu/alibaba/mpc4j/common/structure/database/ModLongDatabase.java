package edu.alibaba.mpc4j.common.structure.database;

/**
 * mod long database. Elements in such database are in [0, n) where n is a long value.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public interface ModLongDatabase extends Database {
    /**
     * Splits the database with the split rows.
     *
     * @param splitRows the split rows.
     * @return a new database with the split rows.
     */
    @Override
    ModLongDatabase split(int splitRows);
}
