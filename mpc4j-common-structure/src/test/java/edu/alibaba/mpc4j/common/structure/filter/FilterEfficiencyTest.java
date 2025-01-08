package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Filter efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class FilterEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEfficiencyTest.class);
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

    public FilterEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                          name", "    log(n)", "   time(s)", "     ratio");
        testEfficiency(4);
        testEfficiency(8);
        testEfficiency(12);
        testEfficiency(16);
        testEfficiency(20);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (FilterType type : FilterType.values()) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), secureRandom);
            byte[][] items = BytesUtils.randomByteArrayVector(n, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, n, keys);
            stopWatch.start();
            Arrays.stream(items).forEach(item -> filter.put(ByteBuffer.wrap(item)));
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

    @Test
    public void testCuckooFilterEstimateSize() {
        LOGGER.info("{}\t{}\t{}", "                          name", "    log(n)", "     size");
        for (int logN : new int[] {16, 18, 20, 22, 24, 26, 28}) {
            testCuckooFilterEstimateSize(logN);
        }
    }

    private void testCuckooFilterEstimateSize(int logN) {
        int n = 1 << logN;
        for (CuckooFilterType type : CuckooFilterType.values()) {
            LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(String.valueOf(CuckooFilterFactory.estimateByteSize(type, n)), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    @Test
    public void testCuckooFilterEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                          name", "    log(n)", "   time(s)", "     ratio");
        for (int logN : new int[] {16, 18, 20}) {
            testCuckooFilterEfficiency(logN);
        }
    }

    private void testCuckooFilterEfficiency(int logN) {
        int n = 1 << logN;
        for (CuckooFilterType type : new CuckooFilterType[] {CuckooFilterType.MOBILE_CUCKOO_FILTER, CuckooFilterType.MOBILE_VACUUM_FILTER}) {
            byte[][] keys = CommonUtils.generateRandomKeys(CuckooFilter.getHashKeyNum(), secureRandom);
            byte[][] items = IntStream.range(0, n)
                .mapToObj(IntUtils::intToByteArray)
                .toArray(byte[][]::new);
            Filter<ByteBuffer> filter = CuckooFilterFactory.createCuckooFilter(EnvType.STANDARD, type, n, keys);
            stopWatch.start();
            Arrays.stream(items).forEach(item -> filter.put(ByteBuffer.wrap(item)));
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
