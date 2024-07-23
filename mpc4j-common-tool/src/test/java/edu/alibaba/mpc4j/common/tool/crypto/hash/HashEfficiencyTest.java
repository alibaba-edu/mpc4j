package edu.alibaba.mpc4j.common.tool.crypto.hash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Hash性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class HashEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HashEfficiencyTest.class);
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

    public HashEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "                name", "    log(n)", "   out_len", "  parallel", "  hash(us)");
        for (int logN = 0; logN <= 10; logN++) {
            testEfficiency(1 << logN);
            testEfficiency(1 << logN);
        }
    }

    private void testEfficiency(int outputByteLength) {
        for (HashType type : HashType.values()) {
            testEfficiency(type, outputByteLength, false);
            testEfficiency(type, outputByteLength, true);
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }

    private void testEfficiency(HashType type, int outputByteLength, boolean parallel) {
        int n = 1 << LOG_N;
        byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        if (outputByteLength <= HashFactory.getUnitByteLength(type)) {
            Hash hash = HashFactory.createInstance(type, outputByteLength);
            // warmup
            IntStream.range(0, n).forEach(index -> hash.digestToBytes(message));
            IntStream intStream = parallel ? IntStream.range(0, n).parallel() : IntStream.range(0, n);
            stopWatch.start();
            intStream.forEach(index -> hash.digestToBytes(message));
            stopWatch.stop();
            double time = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
            stopWatch.reset();
            LOGGER.info("{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(Integer.toString(LOG_N), 10),
                StringUtils.leftPad(String.valueOf(outputByteLength), 10),
                StringUtils.leftPad(Boolean.toString(parallel), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
    }
}
