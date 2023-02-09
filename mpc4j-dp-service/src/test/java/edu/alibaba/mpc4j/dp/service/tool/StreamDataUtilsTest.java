package edu.alibaba.mpc4j.dp.service.tool;

import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.structure.StreamCounterTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * stream data utility functions test.
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class StreamDataUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamDataUtilsTest.class);

    @Test
    public void testChess() throws IOException {
        String path = Objects.requireNonNull(
            StreamCounterTest.class.getClassLoader().getResource("chess.dat")
        ).getPath();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(path);
        assertData("chess", dataStream);
        dataStream.close();
    }

    @Test
    public void testConnect() throws IOException {
        String path = Objects.requireNonNull(
            StreamCounterTest.class.getClassLoader().getResource("connect.dat")
        ).getPath();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(path);
        assertData("connect", dataStream);
        dataStream.close();
    }

    @Test
    public void testMushroom() throws IOException {
        String path = Objects.requireNonNull(
            StreamCounterTest.class.getClassLoader().getResource("mushroom.dat")
        ).getPath();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(path);
        assertData("mushroom", dataStream);
        dataStream.close();
    }

    private void assertData(String name, Stream<String> itemStream) {
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        itemStream.forEach(streamCounter::insert);
        // get count map
        Map<String, Integer> countMap = streamCounter.getRecordItemSet().stream()
            .collect(Collectors.toMap(item -> item, streamCounter::query));
        // get count list
        List<Map.Entry<String, Integer>> countList = new ArrayList<>(countMap.entrySet());
        countList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(countList);
        // get count domain
        int minDomainValue = countList.stream()
            .mapToInt(entry -> Integer.parseInt(entry.getKey()))
            .min()
            .orElse(Integer.MIN_VALUE);
        int maxDomainValue = countList.stream()
            .mapToInt(entry -> Integer.parseInt(entry.getKey()))
            .max()
            .orElse(Integer.MAX_VALUE);
        LOGGER.info(
            "{}: [{}, {}], # items = {}, # distinct items = {}, max items = <{}, {}>",
            name, minDomainValue, maxDomainValue,
            streamCounter.getNum(), streamCounter.getRecordItemSet().size(),
            countList.get(0).getKey(), countList.get(0).getValue()
        );
    }
}
