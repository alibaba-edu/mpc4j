package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
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
 * 抗关联哈希函数性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/26
 */
@Ignore
public class CrhfEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrhfEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 24;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public CrhfEfficiencyTest() {
        stopWatch = new StopWatch();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "  parallel", "  crhf(us)");
        for (CrhfType type : CrhfType.values()) {
            Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
            testEfficiency(crhf, false);
            testEfficiency(crhf, true);
        }
    }

    private void testEfficiency(Crhf crhf, boolean parallel) {
        int n = 1 << LOG_N;
        byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // warm-up
        IntStream.range(0, n).forEach(index -> crhf.hash(message));
        IntStream intStream = parallel ? IntStream.range(0, n).parallel() : IntStream.range(0, n);
        stopWatch.start();
        intStream.forEach(index -> crhf.hash(message));
        stopWatch.stop();
        double crhfTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
        stopWatch.reset();
        LOGGER.info(
            "{}\t{}\t{}\t{}",
            StringUtils.leftPad(crhf.getCrhfType().name(), 20),
            StringUtils.leftPad(Integer.toString(LOG_N), 10),
            StringUtils.leftPad(Boolean.toString(parallel), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(crhfTime), 10)
        );
    }
}
