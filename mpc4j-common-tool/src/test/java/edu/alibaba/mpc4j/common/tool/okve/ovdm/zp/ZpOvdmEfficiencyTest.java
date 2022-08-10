package edu.alibaba.mpc4j.common.tool.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Zp-OVDM性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class ZpOvdmEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpOvdmEfficiencyTest.class);
    /**
     * 最大元素数量对数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final ZpOvdmType[] TYPES = new ZpOvdmType[] {
        ZpOvdmType.H2_TWO_CORE_GCT,
        ZpOvdmType.H2_SINGLETON_GCT,
        ZpOvdmType.H3_SINGLETON_GCT,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "      logN", " encode(s)", " decode(s)");
        // 2^8个元素
        testEfficiency(8);
        // 2^10个元素
        testEfficiency(10);
        // 2^12个元素
        testEfficiency(12);
        // 2^14个元素
        testEfficiency(14);
        // 2^16个元素
        testEfficiency(16);
        // 2^18个元素
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (ZpOvdmType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(ZpOvdmFactory.getHashNum(type), ZpOvdmTestUtils.SECURE_RANDOM);
            // 创建OKVS实例
            ZpOvdm<ByteBuffer> ovdm = ZpOvdmFactory.createInstance(
                EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, n, keys
            );
            // 生成随机键值对
            Map<ByteBuffer, BigInteger> keyValueMap = ZpOvdmTestUtils.randomKeyValueMap(n);
            STOP_WATCH.start();
            BigInteger[] storage = ovdm.encode(keyValueMap);
            STOP_WATCH.stop();
            double encodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // 解码
            STOP_WATCH.start();
            keyValueMap.keySet().forEach(key -> ovdm.decode(storage, key));
            STOP_WATCH.stop();
            double decodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(encodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(decodeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
