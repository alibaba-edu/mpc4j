package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ECC-OVDM性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class EccOvdmEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EccOvdmEfficiencyTest.class);
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
    private static final EccOvdmType[] TYPES = new EccOvdmType[] {
        EccOvdmType.H2_TWO_CORE_GCT,
        EccOvdmType.H2_SINGLETON_GCT,
        EccOvdmType.H3_SINGLETON_GCT,
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
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (EccOvdmType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(EccOvdmFactory.getHashNum(type), EccOvdmTestUtils.SECURE_RANDOM);
            // 创建OKVS实例
            EccOvdm<ByteBuffer> ovdm = EccOvdmFactory.createInstance(EnvType.STANDARD, type, EccOvdmTestUtils.ECC, n, keys);
            // 生成随机键值对
            Map<ByteBuffer, ECPoint> keyValueMap = EccOvdmTestUtils.randomKeyValueMap(n);
            STOP_WATCH.start();
            ECPoint[] storage = ovdm.encode(keyValueMap);
            STOP_WATCH.stop();
            double encodeTime = (double)STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // 解码
            STOP_WATCH.start();
            keyValueMap.keySet().forEach(key -> ovdm.decode(storage, key));
            STOP_WATCH.stop();
            double decodeTime = (double)STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
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
