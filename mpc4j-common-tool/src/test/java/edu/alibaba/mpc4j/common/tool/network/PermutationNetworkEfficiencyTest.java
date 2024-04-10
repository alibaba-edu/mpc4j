package edu.alibaba.mpc4j.common.tool.network;

import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
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
 * permutation network efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/3/22
 */
@Ignore
public class PermutationNetworkEfficiencyTest {
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
    private static final int[] PERMUTATION_NUM_ARRAY = new int[] {1 << 12, 1 << 14, 1 << 16, 1 << 18};

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", " perm. num", "create(us)", " perm.(us)");
        for (int num : PERMUTATION_NUM_ARRAY) {
            testEfficiency(num);
        }
    }

    private void testEfficiency(int num) {
        for (PermutationNetworkType type : PermutationNetworkType.values()) {
            List<Integer> shufflePermutationMap = IntStream.range(0, num)
                .boxed()
                .collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream()
                .mapToInt(permutation -> permutation)
                .toArray();

            STOP_WATCH.start();
            PermutationNetwork<Integer> network = PermutationNetworkFactory.createInstance(type, permutationMap);
            STOP_WATCH.stop();
            long createTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();

            Vector<Integer> inputVector = IntStream.range(0, num)
                .boxed().
                collect(Collectors.toCollection(Vector::new));
            STOP_WATCH.start();
            network.permutation(inputVector);
            STOP_WATCH.stop();
            long permuteTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(num), 10),
                StringUtils.leftPad(String.valueOf(createTime), 10),
                StringUtils.leftPad(String.valueOf(permuteTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
