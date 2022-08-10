package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;
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
 * KDF性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class KdfEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KdfEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 16;
    /**
     * 种子字节长度输出格式
     */
    private static final DecimalFormat SEND_BYTE_LENGTH_DECIMAL_FORMAT = new DecimalFormat("000");
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.0000");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final KdfType[] TYPES = new KdfType[] {
        KdfType.JDK_SHA256,
        KdfType.NATIVE_SHA256,
        KdfType.BC_BLAKE_2B,
        KdfType.NATIVE_BLAKE_2B,
        KdfType.NATIVE_BLAKE_3,
        KdfType.BC_SM3,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "  seed_len", "   prg(us)");
        // KDF函数最大测试到2^8 = 256比特，即2^5 = 32字节
        testEfficiency(1);
        testEfficiency(1 << 1);
        testEfficiency(1 << 2);
        testEfficiency(1 << 3);
        testEfficiency(1 << 4);
        testEfficiency(1 << 5);
    }

    private void testEfficiency(int seedByteLength) {
        int n = 1 << LOG_N;
        for (KdfType type : TYPES) {
            Kdf kdf = KdfFactory.createInstance(type);
            byte[] zeroSeed = new byte[seedByteLength];
            // 预热
            IntStream.range(0, n).forEach(index -> kdf.deriveKey(zeroSeed));
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> kdf.deriveKey(zeroSeed));
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(SEND_BYTE_LENGTH_DECIMAL_FORMAT.format(seedByteLength), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
