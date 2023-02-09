package edu.alibaba.mpc4j.dp.service;

import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.structure.StreamCounterTest;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * LDP test data utilities.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class LdpTestDataUtils {
    /**
     * File path for stream_counter_example_data.txt
     */
    public static final String EXAMPLE_DATA_PATH = Objects.requireNonNull(
        StreamCounterTest.class.getClassLoader().getResource("stream_counter_example_data.txt")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    public static final Set<String> EXAMPLE_DATA_DOMAIN = IntStream
        .rangeClosed(480, 520)
        .mapToObj(String::valueOf)
        .collect(Collectors.toSet());
    /**
     * domain size for stream_counter_example_data.txt
     */
    public static final int EXAMPLE_DATA_D = EXAMPLE_DATA_DOMAIN.size();
    /**
     * large key set for stream_counter_example_data.txt
     */
    public static final Set<String> EXAMPLE_DATA_LARGE_DOMAIN = IntStream
        .range(0, 2000)
        .mapToObj(String::valueOf)
        .collect(Collectors.toSet());
    /**
     * large domain size for stream_counter_example_data.txt
     */
    public static final int EXAMPLE_LARGE_D = EXAMPLE_DATA_LARGE_DOMAIN.size();
    /**
     * total num for stream_counter_example_data.txt
     */
    public static final int EXAMPLE_TOTAL_NUM;
    /**
     * warmup num for stream_counter_example_data.txt
     */
    public static final int EXAMPLE_WARMUP_NUM;
    /**
     * correct count map for stream_counter_example_data.txt
     */
    public static final Map<String, Integer> CORRECT_EXAMPLE_COUNT_MAP;
    /**
     * correct count ordered list for stream_counter_example_data.txt
     */
    public static final List<Map.Entry<String, Integer>> CORRECT_EXAMPLE_COUNT_ORDERED_LIST;

    static {
        try {
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
            EXAMPLE_TOTAL_NUM = (int)dataStream.count();
            EXAMPLE_WARMUP_NUM = (int)Math.round(EXAMPLE_TOTAL_NUM* 0.01);
            dataStream.close();
            NaiveStreamCounter streamCounter = new NaiveStreamCounter();
            dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
            dataStream.forEach(streamCounter::insert);
            dataStream.close();
            CORRECT_EXAMPLE_COUNT_MAP = EXAMPLE_DATA_DOMAIN.stream()
                .collect(Collectors.toMap(item -> item, streamCounter::query));
            CORRECT_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(CORRECT_EXAMPLE_COUNT_MAP.entrySet());
            // descending sort
            CORRECT_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_EXAMPLE_COUNT_ORDERED_LIST);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }
    /**
     * File path for connect.dat
     */
    public static final String CONNECT_DATA_PATH = Objects.requireNonNull(
        StreamCounterTest.class.getClassLoader().getResource("connect.dat")
    ).getPath();
    /**
     * Key set for connect.dat
     */
    public static final Set<String> CONNECT_DATA_DOMAIN = IntStream.rangeClosed(1, 129)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * total num for connect.dat
     */
    public static final int CONNECT_TOTAL_NUM;
    /**
     * warmup num for connect.dat
     */
    public static final int CONNECT_WARMUP_NUM;
    /**
     * correct count map for connect.dat
     */
    public static final Map<String, Integer> CORRECT_CONNECT_COUNT_MAP;
    /**
     * correct count ordered list for connect.dat
     */
    public static final List<Map.Entry<String, Integer>> CORRECT_CONNECT_COUNT_ORDER_LIST;

    static {
        try {
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
            CONNECT_TOTAL_NUM = (int)dataStream.count();
            CONNECT_WARMUP_NUM = (int)Math.round(CONNECT_TOTAL_NUM * 0.01);
            dataStream.close();
            NaiveStreamCounter streamCounter = new NaiveStreamCounter();
            dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
            dataStream.forEach(streamCounter::insert);
            dataStream.close();
            CORRECT_CONNECT_COUNT_MAP = CONNECT_DATA_DOMAIN.stream()
                .collect(Collectors.toMap(item -> item, streamCounter::query));
            CORRECT_CONNECT_COUNT_ORDER_LIST = new ArrayList<>(CORRECT_CONNECT_COUNT_MAP.entrySet());
            // descending sort
            CORRECT_CONNECT_COUNT_ORDER_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_CONNECT_COUNT_ORDER_LIST);

        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    public static double getVariance(Map<String, Double> estimates, Map<String, Integer> corrects) {
        // compute the variance
        double variance = 0;
        for (String item : corrects.keySet()) {
            double estimate = estimates.get(item);
            int correct = corrects.get(item);
            variance += Math.pow(estimate - correct, 2);
        }
        return variance;
    }
}
