package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OKVS性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class OkvsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkvsEfficiencyTest.class);
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
    private static final OkvsType[] TYPES = new OkvsType[]{
        // 多项式OKVS性能过差，不测试
        OkvsType.GBF,
        OkvsType.MEGA_BIN,
        OkvsType.H2_TWO_CORE_GCT,
        OkvsType.H2_DFS_GCT,
        OkvsType.H2_SINGLETON_GCT,
        OkvsType.H3_SINGLETON_GCT,
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
        for (OkvsType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM);
            // 创建OKVS实例
            Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(EnvType.STANDARD, type, n, OkvsTestUtils.DEFAULT_L, keys);
            // 生成随机键值对
            Map<ByteBuffer, byte[]> keyValueMap = OkvsTestUtils.randomKeyValueMap(n);
            STOP_WATCH.start();
            byte[][] storage = okvs.encode(keyValueMap);
            STOP_WATCH.stop();
            double encodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // 解码
            STOP_WATCH.start();
            keyValueMap.keySet().forEach(key -> okvs.decode(storage, key));
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
