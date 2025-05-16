package edu.alibaba.mpc4j.common.tool.f3hash;

import edu.alibaba.mpc4j.common.tool.f3hash.F3HashFactory.F3HashType;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory.LongHashType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * F3Hash efficiency test
 *
 * @author Feng Han
 * @date 2024/10/21
 */
public class F3HashEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(F3HashEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 16;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public F3HashEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testLongF3HashEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                    name", "    log(n)", "  parallel", "  hash(us)");
        for (LongHashType type : LongHashType.values()) {
            testEfficiency(type, false);
            testEfficiency(type, true);
        }
    }

    private void testEfficiency(LongHashType type, boolean parallel) {
        int n = 1 << LOG_N;
        byte[] message = BlockUtils.zeroBlock();
        F3Hash hash = F3HashFactory.createInstance(F3HashType.LONG_F3_HASH, type);
        // warmup
        IntStream.range(0, n).forEach(index -> hash.digestToBytes(message));
        IntStream intStream = parallel ? IntStream.range(0, n).parallel() : IntStream.range(0, n);
        stopWatch.start();
        intStream.forEach(index -> hash.digestToBytes(message));
        stopWatch.stop();
        double time = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}\t{}",
            StringUtils.leftPad(F3HashType.LONG_F3_HASH + "_" + type.name(), 25),
            StringUtils.leftPad(Integer.toString(LOG_N), 10),
            StringUtils.leftPad(Boolean.toString(parallel), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
        );

    }
}
