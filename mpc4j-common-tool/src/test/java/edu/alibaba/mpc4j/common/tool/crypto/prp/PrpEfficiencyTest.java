package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
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
 * PRP性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/18
 */
@Ignore
public class PrpEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrpEfficiencyTest.class);
    /**
     * 低性能算法log(n)
     */
    private static final int SLOW_LOG_N = 10;
    /**
     * 高性能算法log(n)
     */
    private static final int FAST_LOG_N = 22;
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("0");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * 全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 全0明文
     */
    private static final byte[] ZERO_PLAINTEXT = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 全0密文
     */
    private static final byte[] ZERO_CIPHERTEXT = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 高性能类型
     */
    private static final PrpType[] FAST_TYPES = new PrpType[] {
        // AES
        PrpType.JDK_AES,
        PrpType.NATIVE_AES,
        // SM4
        PrpType.BC_SM4,
    };
    /**
     * 低性能类型
     */
    private static final PrpType[] SLOW_TYPES = new PrpType[] {
        // LowMC_20
        PrpType.JDK_BYTES_LOW_MC_20,
        PrpType.JDK_LONGS_LOW_MC_20,
        // LowMC_21
        PrpType.JDK_BYTES_LOW_MC_21,
        PrpType.JDK_LONGS_LOW_MC_21,
        // LowMC_23
        PrpType.JDK_BYTES_LOW_MC_23,
        PrpType.JDK_LONGS_LOW_MC_23,
        // LowMC_32
        PrpType.JDK_BYTES_LOW_MC_32,
        PrpType.JDK_LONGS_LOW_MC_32,
        // LowMC_192
        PrpType.JDK_BYTES_LOW_MC_192,
        PrpType.JDK_LONGS_LOW_MC_192,
        // LowMC_208
        PrpType.JDK_BYTES_LOW_MC_208,
        PrpType.JDK_LONGS_LOW_MC_208,
        // LowMC_287
        PrpType.JDK_BYTES_LOW_MC_287,
        PrpType.JDK_LONGS_LOW_MC_287,
    };

    @Test
    public void testFastEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "   PRP(us)", "InvPRP(us)");
        for (PrpType type : FAST_TYPES) {
            testEfficiency(type,  FAST_LOG_N);
        }
    }

    @Test
    public void testSlowEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "    log(n)", "   PRP(us)", "InvPRP(us)");
        for (PrpType type : SLOW_TYPES) {
            testEfficiency(type, SLOW_LOG_N);
        }
    }

    private void testEfficiency(PrpType type, int logN) {
        assert logN > 0 : "log(n) must be greater than 0";
        int n = 1 << logN;
        Prp prp = PrpFactory.createInstance(type);
        prp.setKey(ZERO_KEY);
        // 预热
        IntStream.range(0, n).forEach(index -> prp.prp(ZERO_PLAINTEXT));
        IntStream.range(0, n).forEach(index -> prp.invPrp(ZERO_CIPHERTEXT));
        // Prp性能
        STOP_WATCH.start();
        IntStream.range(0, n).forEach(index -> prp.prp(ZERO_PLAINTEXT));
        STOP_WATCH.stop();
        double prpTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
        STOP_WATCH.reset();
        // InvPrp性能
        STOP_WATCH.start();
        IntStream.range(0, n).forEach(index -> prp.invPrp(ZERO_CIPHERTEXT));
        STOP_WATCH.stop();
        double invPrpTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
        STOP_WATCH.reset();
        LOGGER.info("{}\t{}\t{}\t{}",
            StringUtils.leftPad(type.name(), 20),
            StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(prpTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invPrpTime), 10)
        );
    }
}
