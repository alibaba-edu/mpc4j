package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 布谷鸟哈希桶性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class CuckooHashBinEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuckooHashBinEfficiencyTest.class);
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
    private static final CuckooHashBinType[] TYPES = new CuckooHashBinType[]{
        // 单哈希函数布谷鸟哈希不需要测试
        CuckooHashBinType.NAIVE_2_HASH,
        CuckooHashBinType.NAIVE_3_HASH,
        CuckooHashBinType.NAIVE_4_HASH,
        CuckooHashBinType.NAIVE_5_HASH,
        CuckooHashBinType.NO_STASH_NAIVE,
        CuckooHashBinType.NO_STASH_PSZ18_3_HASH,
        CuckooHashBinType.NO_STASH_PSZ18_4_HASH,
        CuckooHashBinType.NO_STASH_PSZ18_5_HASH,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}", "                 name", "      logN", " insert(s)");
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
        // 2^20个元素
        testEfficiency(20);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (CuckooHashBinType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(
                CuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
            );
            // 创建OKVS实例
            CuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createCuckooHashBin(EnvType.STANDARD, type, n, keys);
            // 生成随机元素
            List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(n);
            double time = 0;
            boolean success = false;
            while (!success) {
                try {
                    // 插入数据
                    STOP_WATCH.start();
                    hashBin.insertItems(items);
                    STOP_WATCH.stop();
                    time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
                    STOP_WATCH.reset();
                    success = true;
                } catch (ArithmeticException ignored) {
                    STOP_WATCH.stop();
                    STOP_WATCH.reset();
                    keys = CommonUtils.generateRandomKeys(
                        CuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                    );
                    hashBin = CuckooHashBinFactory.createCuckooHashBin(EnvType.STANDARD, type, n, keys);
                }
            }
            LOGGER.info("{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 21),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
