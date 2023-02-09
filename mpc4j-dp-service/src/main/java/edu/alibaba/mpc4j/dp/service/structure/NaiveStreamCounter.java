package edu.alibaba.mpc4j.dp.service.structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Naive stream counter, recording the counter using a map.
 *
 * @author Weiran Liu
 * @date 2022/11/16
 */
public class NaiveStreamCounter implements StreamCounter {
    /**
     * counter map
     */
    private final Map<String, Integer> countMap;
    /**
     * the total number of insert items
     */
    private int num;

    public NaiveStreamCounter() {
        countMap = new HashMap<>();
        num = 0;
    }

    @Override
    public boolean insert(String item) {
        num++;
        if (countMap.containsKey(item)) {
            int count = countMap.get(item);
            count++;
            countMap.put(item, count);
        } else {
            countMap.put(item, 1);
        }
        return true;
    }

    @Override
    public int query(String item) {
        if (countMap.containsKey(item)) {
            return countMap.get(item);
        }
        return 0;
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public Set<String> getRecordItemSet() {
        return countMap.keySet();
    }
}
