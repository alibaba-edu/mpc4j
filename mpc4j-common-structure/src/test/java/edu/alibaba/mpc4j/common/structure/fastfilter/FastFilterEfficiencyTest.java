package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Fast Cuckoo Filter efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/11/7
 */
@Ignore
public class FastFilterEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastFilterEfficiencyTest.class);
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

    public FastFilterEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testEstimateSize() {
        LOGGER.info("{}\t{}\t{}", "                          name", "    log(n)", "     size");
        for (int n : new int[]{(1 << 16), (1 << 18), (1 << 20), (1 << 22), (1 << 24)}) {
            testEstimateSize(n);
        }
    }

    private void testEstimateSize(int n) {
        for (FastCuckooFilterType type : FastCuckooFilterType.values()) {
            LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(n), 10),
                StringUtils.leftPad(String.valueOf(FastCuckooFilterFactory.estimateByteSize(type, n)), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                          name", "        n", "   time(s)", "      size");
        for (int n : new int[]{(1 << 16), (1 << 18), (1 << 20), (1 << 22), (1 << 24)}) {
            testEfficiency(n);
        }
    }

    private void testEfficiency(int n) {
        for (FastCuckooFilterType type : FastCuckooFilterType.values()) {
            ArrayList<byte[]> items = randomItems(n);
            long hashSeed = secureRandom.nextLong();
            FastCuckooFilter<byte[]> filter = FastCuckooFilterFactory.createCuckooFilter(type, n, hashSeed);
            stopWatch.start();
            items.forEach(filter::put);
            stopWatch.stop();
            double time = (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000;
            long byteSize = filter.byteSize();
            stopWatch.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(n), 10),
                StringUtils.leftPad(DECIMAL_FORMAT.format(time), 10),
                StringUtils.leftPad(String.valueOf(byteSize), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    private ArrayList<byte[]> randomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom))
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
