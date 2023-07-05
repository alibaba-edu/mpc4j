package edu.alibaba.mpc4j.dp.service.tool;

import java.util.Comparator;
import java.util.Map;

/**
 * Bucket Comparator.
 *
 * @author Weiran Liu
 * @date 2023/6/13
 */
public class BucketDoubleComparator implements Comparator<Map.Entry<String, Double>> {
    @Override
    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
        return o1.getValue().compareTo(o2.getValue());
    }
}
