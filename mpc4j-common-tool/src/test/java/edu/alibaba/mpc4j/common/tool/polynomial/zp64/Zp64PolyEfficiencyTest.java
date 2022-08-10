package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory.Zp64PolyType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zp64多项式性能测试。
 *
 * @author Weiran Liu
 * @date 2022/8/5
 */
@Ignore
public class Zp64PolyEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64PolyEfficiencyTest.class);
    /**
     * l取值
     */
    private static final int[] L_ARRAY = new int[]{20, 30, 40, 50};
    /**
     * 点数量取值
     */
    private static final int[] POINT_NUM_ARRAY = new int[]{10, 20, 30, 40, 50};
    /**
     * log(n)
     */
    private static final int LOG_N = 10;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("00.0000");
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
    private static final Zp64PolyType[] TYPES = new Zp64PolyType[]{
        Zp64PolyType.NTL,
        Zp64PolyType.RINGS_NEWTON,
        Zp64PolyType.RINGS_LAGRANGE,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type",
            "         l", "  # points", "    log(n)",
            "  Full(ms)", " rFull(ms)", "  Half(ms)", "  rHalf(ms)", " Eval.(ms)", "bEval.(ms)"
        );
        for (int l : L_ARRAY) {
            for (int pointNum : POINT_NUM_ARRAY) {
                testEfficiency(l, pointNum);
            }
        }
    }

    private void testEfficiency(int l, int pointNum) {
        int n = 1 << LOG_N;
        for (Zp64PolyType type : TYPES) {
            Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(type, l);
            long p = zp64Poly.getPrime();
            // 创建插值点
            long[] xFullArray = IntStream.range(0, pointNum)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            long[] yFullArray = IntStream.range(0, pointNum)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            long yFull = LongUtils.randomNonNegative(p, SECURE_RANDOM);
            // 创建一半的插值点
            long[] xHalfArray = IntStream.range(0, pointNum / 2)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            long[] yHalfArray = IntStream.range(0, pointNum / 2)
                .mapToLong(index -> LongUtils.randomNonNegative(p, SECURE_RANDOM))
                .toArray();
            long yHalf = LongUtils.randomNonNegative(p, SECURE_RANDOM);
            // 全量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> zp64Poly.interpolate(pointNum, xFullArray, yFullArray));
            STOP_WATCH.stop();
            double fullInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 全量根插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> zp64Poly.rootInterpolate(pointNum, xFullArray, yFull));
            STOP_WATCH.stop();
            double fullRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> zp64Poly.interpolate(pointNum, xHalfArray, yHalfArray));
            STOP_WATCH.stop();
            double halfInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量根插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> zp64Poly.rootInterpolate(pointNum, xHalfArray, yHalf));
            STOP_WATCH.stop();
            double halfRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 单一求值时间
            long[] coefficients = zp64Poly.interpolate(pointNum, xFullArray, yFullArray);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index ->
                Arrays.stream(xFullArray).forEach(x -> zp64Poly.evaluate(coefficients, x))
            );
            STOP_WATCH.stop();
            double singleEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 批量求值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> zp64Poly.evaluate(coefficients, xFullArray));
            double multiEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(l), 10),
                StringUtils.leftPad(String.valueOf(pointNum), 10),
                StringUtils.leftPad(String.valueOf(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullRootInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(halfInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(halfRootInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(singleEvaluateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(multiEvaluateTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
