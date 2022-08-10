package edu.alibaba.mpc4j.common.tool.crypto.tcrhf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
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
 * 可调抗关联哈希函数性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/27
 */
@Ignore
public class TcrhfEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcrhfEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 24;
    /**
     * 全0明文
     */
    private static final byte[] ZERO_MESSAGE = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.0000");
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final TcrhfFactory.TcrhfType[] TYPES = new TcrhfFactory.TcrhfType[] {
        TcrhfFactory.TcrhfType.TMMO,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                name", "    log(n)", " tcrhf(us)");
        int n = 1 << LOG_N;
        for (TcrhfFactory.TcrhfType type : TYPES) {
            Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> tcrhf.hash(0, ZERO_MESSAGE));
            STOP_WATCH.stop();
            double time = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
    }
}
