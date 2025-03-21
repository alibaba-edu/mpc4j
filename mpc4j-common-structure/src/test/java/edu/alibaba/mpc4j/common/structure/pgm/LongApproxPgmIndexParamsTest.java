package edu.alibaba.mpc4j.common.structure.pgm;

import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * test parameters generated by PGM-index.
 *
 * @author Weiran Liu
 * @date 2024/9/1
 */
@Ignore
public class LongApproxPgmIndexParamsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongApproxPgmIndexParamsTest.class);
    /**
     * round
     */
    private static final int ROUND = 10000;
    /**
     * epsilon used in the paper
     */
    private static final int EPSILON = 4;
    /**
     * epsilon recursive used in the paper
     */
    private static final int EPSILON_RECURSIVE = 2;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public LongApproxPgmIndexParamsTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testParamsLogNum18() throws IOException {
        int logNum = 18;
        testParams(logNum);
    }

    @Test
    public void testParamsLogNum20() throws IOException {
        int logNum = 20;
        testParams(logNum);
    }

    @Test
    public void testParamsLogNum22() throws IOException {
        int logNum = 22;
        testParams(logNum);
    }

    private void testParams(int logNum) throws IOException {
        LOGGER.info("logNum = {}, epsilon = {}", logNum, EPSILON);
        String filePath = "PGM_INDEX_PARAMS"
            + "_EPSILON_" + EPSILON
            + "_RECURSIVE_" + EPSILON_RECURSIVE
            + "_LOGNUM_" + logNum
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "ID\tSegments\tSize(B)";
        printWriter.println(tab);
        int[][] results = new int[ROUND][];
        IntStream.range(0, ROUND).parallel().forEach(index -> {
            LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder();
            builder.setEpsilon(EPSILON);
            builder.setEpsilonRecursive(EPSILON_RECURSIVE);
            long[] sortedKeys = randomDistinctSortedKeys(logNum);
            builder.setSortedKeys(sortedKeys);
            LongApproxPgmIndex longApproxPgmIndex = builder.build();
            int segmentNum = longApproxPgmIndex.firstLevelSegmentNum();
            int size = longApproxPgmIndex.toByteArray().length;
            results[index] = new int[] {index + 1, segmentNum, size};
        });
        for (int index = 0; index < ROUND; index++) {
            printWriter.println(results[index][0] + "\t" + results[index][1] + "\t" + results[index][2]);
        }
        printWriter.close();
        fileWriter.close();
    }

    private long[] randomDistinctSortedKeys(int logNum) {
        int num = 1 << logNum;
        long[] keys = new long[num];
        boolean success = false;
        while (!success) {
            for (int i = 0; i < num; i++) {
                keys[i] = secureRandom.nextLong();
            }
            success = (Arrays.stream(keys).distinct().count() == num);
        }
        Arrays.sort(keys);
        return keys;
    }
}
