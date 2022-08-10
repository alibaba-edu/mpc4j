package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GF(2^128)性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
@Ignore
public class Gf2kEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2kEfficiencyTest.class);
    /**
     * 乘法性能测试轮数
     */
    private static final int LOG_N = 16;
    /**
     * 点数量输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.00");
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final Gf2kType[] TYPES = new Gf2kType[] {
        Gf2kType.BC,
        Gf2kType.SSE,
        Gf2kType.NTL,
        Gf2kType.RINGS,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "      type", "    log(n)", "   mul(us)", "  muli(us)");
        int n = 1 << LOG_N;
        for (Gf2kType type : TYPES) {
            Gf2k gf2k = Gf2kFactory.createInstance(type);
            // 创建数据
            byte[][] aArray = new byte[n][];
            byte[][] bArray = new byte[n][];
            IntStream.range(0, n).forEach(index -> {
                aArray[index] = gf2k.createRandom(SECURE_RANDOM);
                bArray[index] = gf2k.createRandom(SECURE_RANDOM);
            });
            // 预热
            IntStream.range(0, n).forEach(index -> gf2k.mul(aArray[index], bArray[index]));
            // mul性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2k.mul(aArray[index], bArray[index]));
            STOP_WATCH.stop();
            double mulTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            // muli性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2k.muli(aArray[index], bArray[index]));
            STOP_WATCH.stop();
            double muliTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(muliTime), 10)
            );
        }
    }
}
