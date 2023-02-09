package edu.alibaba.mpc4j.common.tool.hash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory.IntHashType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * efficieny tests for IntHash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class IntHashEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntHashEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 20;
    /**
     * decimal format for log(n)
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * decimal format for time
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.0000");
    /**
     * all 0 data
     */
    private static final byte[] ZERO_DATA = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * the type
     */
    private static final IntHashType[] TYPES = new IntHashType[] {
        IntHashType.XX_HASH_32,
        IntHashType.BOB_HASH_32,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                name", "    log(n)", "  hash(us)");
        int n = 1 << LOG_N;
        for (IntHashType type : TYPES) {
            IntHash intHash = IntHashFactory.createInstance(type);
            // warmup
            IntStream.range(0, n).forEach(index -> intHash.hash(ZERO_DATA));
            STOP_WATCH.start();
            // efficiency test
            IntStream.range(0, n).forEach(index -> intHash.hash(ZERO_DATA));
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
    }
}
