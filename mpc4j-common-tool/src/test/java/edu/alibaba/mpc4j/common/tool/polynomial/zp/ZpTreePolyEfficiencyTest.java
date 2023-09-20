package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Zp二叉树多项式性能测试。
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
@Ignore
public class ZpTreePolyEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpTreePolyEfficiencyTest.class);
    /**
     * l
     */
    private static final int[] L_ARRAY = new int[] {128, 256};
    /**
     * point num
     */
    private static final int[] POINT_NUM_ARRAY = new int[] {1 << 8, 1 << 9, 1 << 10};
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * Zp二叉树多项式插值测试类型
     */
    private static final ZpPolyFactory.ZpTreePolyType[] ZP_TREE_POLY_TYPES = new ZpPolyFactory.ZpTreePolyType[]{
        // NTL_TREE
        ZpPolyFactory.ZpTreePolyType.NTL_TREE,
        // RINGS_TREE
        ZpPolyFactory.ZpTreePolyType.RINGS_TREE,
    };
    /**
     * Zp多项式插值测试类型
     */
    private static final ZpPolyFactory.ZpPolyType[] ZP_POLY_TYPES = new ZpPolyFactory.ZpPolyType[]{
        // NTL
        ZpPolyFactory.ZpPolyType.NTL,
        // RINGS_NEWTON
        ZpPolyFactory.ZpPolyType.RINGS_NEWTON,
        // JDK_NEWTON
        ZpPolyFactory.ZpPolyType.JDK_NEWTON,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "         l", "  # points",
            "PIter.(ms)", " Iter.(ms)", "FIter.(ms)", "PEval.(ms)", " Eval.(ms)", "FEval.(ms)"
        );
        for (int l : L_ARRAY) {
            for (int pointNum : POINT_NUM_ARRAY) {
                testEfficiency(l, pointNum);
            }
        }
    }

    private void testEfficiency(int l, int pointNum) {
        BigInteger prime = ZpManager.getPrime(l);
        // 创建插值点
        BigInteger[] xArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(prime, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, pointNum)
            .mapToObj(index -> BigIntegerUtils.randomNonNegative(prime, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        for (ZpPolyFactory.ZpPolyType type : ZP_POLY_TYPES) {
            ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
            // 插值时间
            STOP_WATCH.start();
            zpPoly.interpolate(pointNum, xArray, yArray);
            STOP_WATCH.stop();
            double interpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();
            // 求值时间
            BigInteger[] coefficients = zpPoly.interpolate(pointNum, xArray, yArray);
            STOP_WATCH.start();
            zpPoly.evaluate(coefficients, xArray);
            double evaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(l), 10),
                StringUtils.leftPad(String.valueOf(pointNum), 10),
                StringUtils.leftPad("-", 10),
                StringUtils.leftPad("-", 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(interpolateTime), 10),
                StringUtils.leftPad("-", 10),
                StringUtils.leftPad("-", 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(evaluateTime), 10)
            );
        }
        for (ZpPolyFactory.ZpTreePolyType type : ZP_TREE_POLY_TYPES) {
            ZpTreePoly zpTreePoly = ZpPolyFactory.createTreeInstance(type, l);
            STOP_WATCH.start();
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            STOP_WATCH.stop();
            double prepareInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();
            STOP_WATCH.start();
            zpTreePoly.interpolate(yArray);
            zpTreePoly.destroyInterpolateBinaryTree();
            STOP_WATCH.stop();
            double interpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();
            double fullInterpolateTime = prepareInterpolateTime + interpolateTime;
            // 求值时间
            zpTreePoly.prepareInterpolateBinaryTree(xArray);
            BigInteger[] coefficients = zpTreePoly.interpolate(yArray);
            zpTreePoly.destroyInterpolateBinaryTree();
            STOP_WATCH.start();
            zpTreePoly.prepareEvaluateBinaryTrees(xArray.length, xArray);
            STOP_WATCH.stop();
            double prepareEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();
            STOP_WATCH.start();
            zpTreePoly.evaluate(coefficients);
            zpTreePoly.destroyEvaluateBinaryTree();
            STOP_WATCH.stop();
            double evaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / 1000;
            STOP_WATCH.reset();
            double fullEvaluateTime = prepareEvaluateTime + evaluateTime;
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(l), 10),
                StringUtils.leftPad(String.valueOf(pointNum), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(prepareInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(interpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(prepareEvaluateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(evaluateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullEvaluateTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
