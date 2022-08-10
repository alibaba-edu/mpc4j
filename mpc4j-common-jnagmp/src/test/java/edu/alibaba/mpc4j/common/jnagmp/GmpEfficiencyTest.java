package edu.alibaba.mpc4j.common.jnagmp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * GMP性能测试。
 *
 * @author Weiran Liu
 * @date 2022/8/5
 */
@Ignore
public class GmpEfficiencyTest implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GmpEfficiencyTest.class);
    /**
     * 随机测试轮数
     */
    private static final int N = 100;
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0000.00");
    /**
     * 有限域比特长度
     */
    private static final int[] L_ARRAY = new int[] {1 << 10, 1 << 11, 1 << 12};

    @BeforeClass
    public static void checkLoaded() {
        Gmp.checkLoaded();
    }

    @Override
    public void close() throws Exception {
        Gmp.INSTANCE.remove();
        final AtomicBoolean gcHappened = new AtomicBoolean(false);
        gcHappened.set(true);
        while (!gcHappened.get()) {
            System.gc();
            //noinspection BusyWait
            Thread.sleep(100);
        }
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "      name", "         l", "modPow(us)", "modInv(us)");
        for (int bitLength : L_ARRAY) {
            testEfficiency(bitLength);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void testEfficiency(int l) {
        // 随机选择l比特长的质数
        BigInteger modulus = BigInteger.probablePrime(l, SECURE_RANDOM);
        // 随机选择MAX_ITERATION个底数
        BigInteger[] bases = IntStream.range(0, N)
            .mapToObj(index -> new BigInteger(l, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        // 随机选择幂，有正有负
        BigInteger[] exponents = IntStream.range(0, N)
            .mapToObj(index -> {
                BigInteger exponent = new BigInteger(l, SECURE_RANDOM);
                return SECURE_RANDOM.nextBoolean() ? exponent.negate() : exponent;
            })
            .toArray(BigInteger[]::new);
        STOP_WATCH.start();
        IntStream.range(0, N).forEach(index -> bases[index].modPow(exponents[index], modulus));
        STOP_WATCH.stop();
        double jdkModPowTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / N;
        STOP_WATCH.reset();

        // 用Gmp.modPow计算
        STOP_WATCH.start();
        IntStream.range(0, N).forEach(index -> Gmp.modPowInsecure(bases[index], exponents[index], modulus));
        STOP_WATCH.stop();
        double gmpModPowTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / N;
        STOP_WATCH.reset();

        // 用Java的modInverse计算
        STOP_WATCH.start();
        IntStream.range(0, N).forEach(index -> bases[index].modInverse(modulus));
        STOP_WATCH.stop();
        double jdkModInvTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / N;
        STOP_WATCH.reset();

        // 用Gmp的modInverse计算
        STOP_WATCH.start();
        IntStream.range(0, N).forEach(index -> Gmp.modInverse(bases[index], modulus));
        STOP_WATCH.stop();
        double gmpModInvTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / N;
        STOP_WATCH.reset();

        LOGGER.info("{}\t{}\t{}\t{}",
            StringUtils.leftPad("GMP", 10),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(gmpModPowTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(gmpModInvTime), 10)
        );
        LOGGER.info("{}\t{}\t{}\t{}",
            StringUtils.leftPad("JDK", 10),
            StringUtils.leftPad(String.valueOf(l), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(jdkModPowTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(jdkModInvTime), 10)
        );
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
