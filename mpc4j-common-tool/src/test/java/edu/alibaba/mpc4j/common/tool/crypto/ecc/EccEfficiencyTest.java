package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory.EccType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 椭圆曲线性能测试。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class EccEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EccEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 10;
    /**
     * 次数输出格式
     */
    private static final DecimalFormat LOG_N_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 输出格式
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
     * 椭圆曲线测试类型
     */
    private static final EccType[] ECC_TYPES = new EccType[] {
        EccType.SEC_P256_K1_MCL,
        EccType.SEC_P256_K1_OPENSSL,
        EccType.SEC_P256_K1_BC,
        EccType.SEC_P256_R1_MCL,
        EccType.SEC_P256_R1_OPENSSL,
        EccType.SEC_P256_R1_BC,
        EccType.SM2_P256_V1_OPENSSL,
        EccType.SM2_P256_V1_BC,
        EccType.CURVE25519_BC,
        EccType.ED25519_BC
    };

    /**
     * 字节椭圆曲线测试类型
     */
    private static final ByteEccFactory.ByteEccType[] BYTE_MUL_ECC_TYPES = new ByteEccFactory.ByteEccType[] {
        ByteEccFactory.ByteEccType.X25519_SODIUM,
        ByteEccFactory.ByteEccType.X25519_BC,
        ByteEccFactory.ByteEccType.ED25519_SODIUM,
        ByteEccFactory.ByteEccType.ED25519_BC,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", "                name", "    log(n)",
            "  Hash(ms)", "RndPt.(ms)", " sMul.(ms)", " bMul.(ms)", "sPMul.(ms)", "bPMul.(ms)"
        );
        int n = 1 << LOG_N;
        for (EccType type : ECC_TYPES) {
            Ecc ecc = EccFactory.createInstance(type);
            // 生成随机消息
            byte[][] messages = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(message);
                    return message;
                })
                .toArray(byte[][]::new);
            // 预热
            Arrays.stream(messages).forEach(ecc::hashToCurve);
            // 单次调用HashToCurve
            STOP_WATCH.start();
            Arrays.stream(messages).forEach(ecc::hashToCurve);
            STOP_WATCH.stop();
            double hashToCurveTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            // 单次生成随机数
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> ecc.randomPoint(SECURE_RANDOM));
            STOP_WATCH.stop();
            double randomPointTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();

            // 生成一个非生成元的点
            BigInteger gr = BigIntegerUtils.randomPositive(ecc.getN(), SECURE_RANDOM);
            ECPoint h = ecc.multiply(ecc.getG(), gr);
            // 生成幂指数
            BigInteger[] rs = IntStream.range(0, n)
                .mapToObj(index -> BigIntegerUtils.randomPositive(ecc.getN(), SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            // 单次幂运算
            STOP_WATCH.start();
            Arrays.stream(rs).forEach(r -> ecc.multiply(h, r));
            STOP_WATCH.stop();
            double singleMultiplyTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            // 批量幂运算
            STOP_WATCH.start();
            ecc.multiply(h, rs);
            STOP_WATCH.stop();
            double batchMultiplyTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();

            // 预计算
            ecc.precompute(h);
            // 预计算单次幂运算
            STOP_WATCH.start();
            Arrays.stream(rs).forEach(r -> ecc.multiply(h, r));
            STOP_WATCH.stop();
            double singlePrecomputeMulTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            // 预计算批量幂运算
            STOP_WATCH.start();
            ecc.multiply(h, rs);
            STOP_WATCH.stop();
            double batchPrecomputeMulTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            // 销毁预计算
            ecc.destroyPrecompute(h);
            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(hashToCurveTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(randomPointTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(singleMultiplyTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(batchMultiplyTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(singlePrecomputeMulTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(batchPrecomputeMulTime), 10)
            );
        }
        for (ByteEccFactory.ByteEccType type : BYTE_MUL_ECC_TYPES) {
            ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(type);
            // 生成随机消息
            byte[][] messages = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(message);
                    return message;
                })
                .toArray(byte[][]::new);
            // 预热
            Arrays.stream(messages).forEach(byteMulEcc::hashToCurve);
            // 单次调用HashToCurve
            STOP_WATCH.start();
            Arrays.stream(messages).forEach(byteMulEcc::hashToCurve);
            STOP_WATCH.stop();
            double hashToCurveTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            // 单次生成随机数
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> byteMulEcc.randomPoint(SECURE_RANDOM));
            STOP_WATCH.stop();
            double randomPointTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();

            // 生成一个非生成元的点
            byte[] hr = byteMulEcc.randomScalar(SECURE_RANDOM);
            byte[] h = byteMulEcc.mul(byteMulEcc.getG(), hr);
            // 生成幂指数
            byte[][] rs = IntStream.range(0, n)
                .mapToObj(index -> byteMulEcc.randomScalar(SECURE_RANDOM))
                .toArray(byte[][]::new);
            // 单次幂运算
            STOP_WATCH.start();
            Arrays.stream(rs).forEach(r -> byteMulEcc.mul(h, r));
            STOP_WATCH.stop();
            double mulTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad("(B) " + type.name(), 20),
                StringUtils.leftPad(LOG_N_DECIMAL_FORMAT.format(LOG_N), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(hashToCurveTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(randomPointTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime), 10),
                StringUtils.leftPad("    --    ", 10),
                StringUtils.leftPad("    --    ", 10),
                StringUtils.leftPad("    --    ", 10)
            );
        }
    }
}
