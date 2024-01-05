package edu.alibaba.mpc4j.common.structure.database;

/**
 * mod bit database. Elements in such database are in {0, 1}^l.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public interface ModBitNumDatabase extends Database {
    /**
     * Splits the database with the split rows.
     *
     * @param splitRows the split rows.
     * @return a new database with the split rows.
     */
    @Override
    ModBitNumDatabase split(int splitRows);
}
