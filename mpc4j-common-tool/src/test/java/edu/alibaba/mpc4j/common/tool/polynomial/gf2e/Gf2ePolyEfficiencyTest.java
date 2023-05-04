package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
 * GF2X多项式性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/27
 */
@Ignore
public class Gf2ePolyEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2ePolyEfficiencyTest.class);
    /**
     * l取值
     */
    private static final int[] L_ARRAY = new int[] {40, 50, 60, 70, 80};
    /**
     * 点数量取值
     */
    private static final int[] POINT_NUM_ARRAY = new int[] {10, 20, 30, 40, 50};
    /**
     * log(n)
     */
    private static final int LOG_N = 4;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.00");
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
    private static final Gf2ePolyFactory.Gf2ePolyType[] TYPES = new Gf2ePolyFactory.Gf2ePolyType[] {
        Gf2ePolyFactory.Gf2ePolyType.NTL,
        Gf2ePolyFactory.Gf2ePolyType.RINGS_NEWTON,
        Gf2ePolyFactory.Gf2ePolyType.RINGS_LAGRANGE,
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
        for (Gf2ePolyFactory.Gf2ePolyType type : TYPES) {
            Gf2ePoly gf2ePoly = Gf2ePolyFactory.createInstance(type, l);
            int byteL = gf2ePoly.getByteL();
            // 创建全量插值点
            byte[][] xFullArray = IntStream.range(0, pointNum)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yFullArray = IntStream.range(0, pointNum)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[] yFull = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
            // 创建半数插值点
            byte[][] xHalfArray = IntStream.range(0, pointNum / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[][] yHalfArray = IntStream.range(0, pointNum / 2)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                .toArray(byte[][]::new);
            byte[] yHalf = BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
            // 全量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2ePoly.interpolate(pointNum, xFullArray, yFullArray));
            STOP_WATCH.stop();
            double fullInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 全量根差值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2ePoly.rootInterpolate(pointNum, xFullArray, yFull));
            STOP_WATCH.stop();
            double fullRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2ePoly.interpolate(pointNum, xHalfArray, yHalfArray));
            STOP_WATCH.stop();
            double halfInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量根差值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2ePoly.rootInterpolate(pointNum, xHalfArray, yHalf));
            STOP_WATCH.stop();
            double halfRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 单一求值时间
            byte[][] coefficients = gf2ePoly.rootInterpolate(pointNum, xFullArray, yFull);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index ->
                Arrays.stream(xFullArray).forEach(x -> gf2ePoly.evaluate(coefficients, x))
            );
            STOP_WATCH.stop();
            double singleEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 批量求值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2ePoly.evaluate(coefficients, xFullArray));
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
