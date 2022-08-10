package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

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
 * GF(2^l)性能测试。
 *
 * @author Weiran Liu
 * @date 2022/5/19
 */
@Ignore
public class Gf2eEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2eEfficiencyTest.class);
    /**
     * 乘法性能测试轮数
     */
    private static final int LOG_N = 10;
    /**
     * l输出格式
     */
    private static final DecimalFormat L_DECIMAL_FORMAT = new DecimalFormat("000");
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
    private static final Gf2eFactory.Gf2eType[] TYPES = new Gf2eFactory.Gf2eType[] {
        Gf2eFactory.Gf2eType.NTL,
        Gf2eFactory.Gf2eType.RINGS,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", "      type", "         l", "    log(n)",
            "   mul(us)", "  muli(us)", "   div(us)", "  divi(us)", "   inv(us)", "  invi(us)"
        );
        testEfficiency(1);
        testEfficiency(2);
        testEfficiency(3);
        testEfficiency(4);
        testEfficiency(40);
        testEfficiency(128);
        testEfficiency(256);
    }

    private void testEfficiency(int l) {
        int n = 1 << LOG_N;
        for (Gf2eFactory.Gf2eType type : TYPES) {
            Gf2e gf2e = Gf2eFactory.createInstance(type, l);
            // 创建数据
            byte[][] arrayA = new byte[n][];
            byte[][] arrayB = new byte[n][];
            IntStream.range(0, n).forEach(index -> {
                arrayA[index] = gf2e.createNonZeroRandom(SECURE_RANDOM);
                arrayB[index] = gf2e.createNonZeroRandom(SECURE_RANDOM);
            });
            // 预热
            IntStream.range(0, n).forEach(index -> {
                gf2e.mul(arrayA[index], arrayB[index]);
                gf2e.div(arrayA[index], arrayB[index]);
                gf2e.inv(arrayA[index]);
            });
            // mul性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.mul(arrayA[index], arrayB[index]));
            STOP_WATCH.stop();
            double mulTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            // muli性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.muli(arrayA[index], arrayB[index]));
            STOP_WATCH.stop();
            double muliTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            // div性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.div(arrayA[index], arrayB[index]));
            STOP_WATCH.stop();
            double divTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            // divi性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.divi(arrayA[index], arrayB[index]));
            STOP_WATCH.stop();
            double diviTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            // inv性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.inv(arrayA[index]));
            STOP_WATCH.stop();
            double invTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            // invi性能
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2e.invi(arrayA[index]));
            STOP_WATCH.stop();
            double inviTime = (double)STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
            STOP_WATCH.reset();

            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 10),
                StringUtils.leftPad(L_DECIMAL_FORMAT.format(l), 10),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(muliTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(divTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(diviTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(invTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(inviTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
