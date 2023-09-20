package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 整数布谷鸟哈希桶性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class IntCuckooHashBinEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntCuckooHashBinEfficiencyTest.class);
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

    private static final IntCuckooHashBinType[] TYPES = new IntCuckooHashBinType[] {
        IntCuckooHashBinType.NO_STASH_NAIVE,
        IntCuckooHashBinType.NO_STASH_PSZ18_3_HASH,
        IntCuckooHashBinType.NO_STASH_PSZ18_4_HASH,
        IntCuckooHashBinType.NO_STASH_PSZ18_5_HASH,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                 name", "      type", "      logN", " insert(s)");
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
        for (IntCuckooHashBinType type : TYPES) {
            int[] intItems = HashBinTestUtils.randomIntItems(n);
            // 测试整数布谷鸟哈希
            byte[][] intKeys = CommonUtils.generateRandomKeys(
                IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
            );
            IntNoStashCuckooHashBin intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, n, intKeys);
            // 插入数据
            double time = 0;
            boolean success = false;
            while (!success) {
                try {
                    STOP_WATCH.start();
                    intHashBin.insertItems(intItems);
                    STOP_WATCH.stop();
                    time = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
                    STOP_WATCH.reset();
                    success = true;
                } catch (ArithmeticException ignored) {
                    STOP_WATCH.stop();
                    STOP_WATCH.reset();
                    intKeys = CommonUtils.generateRandomKeys(
                        IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                    );
                    intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, n, intKeys);
                }
            }
            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 21),
                StringUtils.leftPad("int", 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
            // 测试普通布谷鸟哈希
            List<Integer> items = Arrays.stream(intItems).boxed().collect(Collectors.toList());
            CuckooHashBinFactory.CuckooHashBinType relatedType = IntCuckooHashBinFactory.relateCuckooHashBinType(type);
            byte[][] keys = CommonUtils.generateRandomKeys(
                CuckooHashBinFactory.getHashNum(relatedType), HashBinTestUtils.SECURE_RANDOM
            );
            CuckooHashBin<Integer> hashBin = CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, relatedType, n, keys
            );
            // 插入数据
            time = 0;
            success = false;
            while (!success) {
                try {
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
                        IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                    );
                    hashBin = CuckooHashBinFactory.createCuckooHashBin(EnvType.STANDARD, relatedType, n, keys);
                }
            }
            LOGGER.info("{}\t{}\t{}\t{}",
                StringUtils.leftPad(relatedType.name(), 21),
                StringUtils.leftPad("object", 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
