package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory.PrgType;
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
 * PRG efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/18
 */
@Ignore
public class PrgEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrgEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 16;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * 全0种子
     */
    private static final byte[] ZERO_SEED = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final PrgType[] TYPES = new PrgType[] {
        PrgType.JDK_SECURE_RANDOM,
        PrgType.JDK_AES_ECB,
        PrgType.BC_SM4_ECB,
        PrgType.JDK_AES_CTR,
        PrgType.BC_SM4_CTR,
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
        for (PrgType type : TYPES) {
            Prg prg = PrgFactory.createInstance(type, outputByteLength);
            // 预热
            IntStream.range(0, n).forEach(index -> prg.extendToBytes(ZERO_SEED));
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> prg.extendToBytes(ZERO_SEED));
            STOP_WATCH.stop();
            double time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(LOG_N), 10),
                StringUtils.leftPad(String.valueOf(outputByteLength), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
