package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory.PrfType;
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
 * PRF性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class PrfEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrfEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 16;
    /**
     * 输出字节长度输出格式
     */
    private static final DecimalFormat OUTPUT_BYTE_LENGTH_DECIMAL_FORMAT = new DecimalFormat("000");
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.0000");
    /**
     * 全0消息
     */
    private static final byte[] ZERO_MESSAGE = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final PrfType[] TYPES = new PrfType[] {
        PrfType.JDK_AES_CBC,
        PrfType.BC_SIP_HASH,
        PrfType.BC_SIP128_HASH,
        PrfType.BC_SM4_CBC,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "output_len", "   prg(us)");
        // PRF函数最大测试到2^9 = 512比特，即2^6 = 64字节
        testEfficiency(1);
        testEfficiency(1 << 1);
        testEfficiency(1 << 2);
        testEfficiency(1 << 3);
        testEfficiency(1 << 4);
        testEfficiency(1 << 5);
        testEfficiency(1 << 6);
    }

    private void testEfficiency(int outputByteLength) {
        int n = 1 << LOG_N;
        for (PrfType type : TYPES) {
            Prf prf = PrfFactory.createInstance(type, outputByteLength);
            prf.setKey(ZERO_KEY);
            // 预热
            IntStream.range(0, n).forEach(index -> prf.getBytes(ZERO_MESSAGE));
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> prf.getBytes(ZERO_MESSAGE));
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(OUTPUT_BYTE_LENGTH_DECIMAL_FORMAT.format(outputByteLength), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
