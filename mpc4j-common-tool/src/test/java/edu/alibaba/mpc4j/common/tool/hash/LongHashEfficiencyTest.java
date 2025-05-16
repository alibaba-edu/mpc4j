package edu.alibaba.mpc4j.common.tool.hash;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
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
 * efficiency tests for LongHash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class LongHashEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongHashEfficiencyTest.class);
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
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * all 0 data
     */
    private static final byte[] ZERO_DATA = BlockUtils.zeroBlock();
    /**
     * the type
     */
    private static final LongHashType[] TYPES = new LongHashType[] {
        LongHashType.XX_HASH_64,
        LongHashType.BOB_HASH_64,
    };

    /**
     * the stop watch
     */
    private final StopWatch stopWatch;
    /**
     * we also want to compare efficiency with PRF
     */
    private final Prf prf;

    public LongHashEfficiencyTest() {
        stopWatch = new StopWatch();
        prf = PrfFactory.createInstance(EnvType.STANDARD, Long.BYTES);
        prf.setKey(BlockUtils.zeroBlock());
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                name", "    log(n)", "  hash(us)");
        int n = 1 << LOG_N;
        for (LongHashType type : TYPES) {
            LongHash longHash = LongHashFactory.createInstance(type);
            // warmup
            IntStream.range(0, n).forEach(index -> longHash.hash(ZERO_DATA));
            stopWatch.start();
            // efficiency test
            IntStream.range(0, n).forEach(index -> longHash.hash(ZERO_DATA));
            stopWatch.stop();
            double time = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
            stopWatch.reset();
            LOGGER.info("{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        // compare with PRF, warmup
        IntStream.range(0, n).forEach(index -> prf.getLong(ZERO_DATA, Long.MAX_VALUE));
        stopWatch.start();
        // efficiency test
        IntStream.range(0, n).forEach(index -> prf.getLong(ZERO_DATA, Long.MAX_VALUE));
        stopWatch.stop();
        double time = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad(prf.getPrfType().name(), 20),
            StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
        );
    }
}
