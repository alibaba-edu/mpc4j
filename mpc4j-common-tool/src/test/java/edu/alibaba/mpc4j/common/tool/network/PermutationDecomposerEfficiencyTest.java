package edu.alibaba.mpc4j.common.tool.network;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * permutation decomposer efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/3/28
 */
public class PermutationDecomposerEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationNetworkEfficiencyTest.class);
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * numbers of inputs
     */
    private static final int[] LOG_N_ARRAY = new int[]{12, 14, 16, 18};

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "                name", "         N", "         T", "create(us)", " perm.(us)");
        for (int logN : LOG_N_ARRAY) {
            for (int logT = logN / 2; logT <= logN; logT += 2) {
                testEfficiency(logN, logT);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(int logN, int logT) {
        int n = 1 << logN;
        int t = 1 << logT;

        List<Integer> shuffle = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(shuffle, SECURE_RANDOM);
        int[] permutation = shuffle.stream().mapToInt(index -> index).toArray();

        STOP_WATCH.start();
        PermutationDecomposer decomposer = new PermutationDecomposer(permutation, t);
        STOP_WATCH.stop();
        long createTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
        STOP_WATCH.reset();

        Vector<Integer> inputVector = IntStream.range(0, n).boxed().collect(Collectors.toCollection(Vector::new));
        STOP_WATCH.start();
        Vector<Integer> outputVector = new Vector<>(inputVector);
        int[][][] splitGroups = decomposer.getSplitGroups();
        int[][][] subPermutations = decomposer.getSubPermutations();
        for (int i = 0; i < decomposer.getD(); i++) {
            for (int j = 0; j < decomposer.getSubNum(); j++) {
                outputVector = PermutationNetworkUtils.permutation(splitGroups[i][j], subPermutations[i][j], outputVector);
            }
        }
        STOP_WATCH.stop();
        long permuteTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
        STOP_WATCH.reset();

        LOGGER.info("{}\t{}\t{}\t{}\t{}",
            StringUtils.leftPad("JDK", 20),
            StringUtils.leftPad(String.valueOf(n), 10),
            StringUtils.leftPad(String.valueOf(t), 10),
            StringUtils.leftPad(String.valueOf(createTime), 10),
            StringUtils.leftPad(String.valueOf(permuteTime), 10)
        );
    }
}
