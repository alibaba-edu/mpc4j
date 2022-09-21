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
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final HashType[] TYPES = new HashType[] {
        HashType.JDK_SHA256,
        HashType.NATIVE_SHA256,
        HashType.BC_SHA3_256,
        HashType.BC_SHA3_512,
        HashType.BC_BLAKE_2B_160,
        HashType.NATIVE_BLAKE_2B_160,
        HashType.NATIVE_BLAKE_3,
        HashType.BC_SHAKE_128,
        HashType.BC_SHAKE_256,
        HashType.BC_SM3,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "   out_len", "  hash(us)");
        for (int logN = 0; logN <= 10; logN++) {
            testEfficiency(1 << logN);
        }
    }

    private void testEfficiency(int outputByteLength) {
        int n = 1 << LOG_N;
        for (HashType type : TYPES) {
            if (outputByteLength <= HashFactory.getUnitByteLength(type)) {
                Hash hash = HashFactory.createInstance(type, outputByteLength);
                // 预热
                IntStream.range(0, n).forEach(index -> hash.digestToBytes(ZERO_MESSAGE));
                STOP_WATCH.start();
                IntStream.range(0, n).forEach(index -> hash.digestToBytes(ZERO_MESSAGE));
                STOP_WATCH.stop();
                double time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
                STOP_WATCH.reset();
                LOGGER.info("{}\t{}\t{}\t{}",
                    StringUtils.leftPad(type.name(), 20),
                    StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                    StringUtils.leftPad(String.valueOf(outputByteLength), 10),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
                );
            }
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
