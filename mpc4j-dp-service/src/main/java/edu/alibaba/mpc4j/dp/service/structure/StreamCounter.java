package edu.alibaba.mpc4j.dp.service.structure;

import java.util.Set;

/**
 * Streaming-data counter interface.
 *
 * @author Weiran Liu
 * @date 2022/11/16
 */
public interface StreamCounter {
    /**
     * Insert an item.
     *
     * @param item the item.
     * @return return true if the item is not ignored and successfully inserted.
     */
    boolean insert(String item);

    /**
     * Query an item.
     *
     * @param item the item.
     * @return the query count, or 0 if no item matches.
     */
    int query(String item);

    /**
     * Return the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();

    /**
     * Return the recorded item set.
     *
     * @return the recorded item set.
     */
    Set<String> getRecordItemSet();
}
