package edu.alibaba.mpc4j.common.structure.rcfilter;

import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Random Cuckoo Filter efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
@Ignore
public class RandomFilterEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomFilterEfficiencyTest.class);
    /**
     * decimal format
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public RandomFilterEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testEstimateSize() {
        LOGGER.info("{}\t{}\t{}", "                          name", "    log(n)", "     size");
        for (int logN : new int[]{16, 18, 20, 22, 24, 26, 28}) {
            testEstimateSize(logN);
        }
    }

    private void testEstimateSize(int logN) {
        int n = 1 << logN;
        for (RandomCuckooFilterType type : RandomCuckooFilterType.values()) {
            LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(String.valueOf(RandomCuckooFilterFactory.estimateByteSize(type, n)), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                          name", "    log(n)", "   time(s)", "     ratio");
        for (int logN : new int[]{16, 18, 20, 22, 24,}) {
            testEfficiency(logN);
        }
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (RandomCuckooFilterType type : RandomCuckooFilterType.values()) {
            long[] items = IntStream.range(0, n)
                .mapToLong(i -> secureRandom.nextLong())
                .toArray();
            RandomCuckooFilter filter = RandomCuckooFilterFactory.createCuckooFilter(type, n);
            stopWatch.start();
            Arrays.stream(items).forEach(filter::put);
            stopWatch.stop();
            double time = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            long byteSize = filter.byteSize();
            stopWatch.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(DECIMAL_FORMAT.format(time), 10),
                StringUtils.leftPad(String.valueOf(byteSize), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
