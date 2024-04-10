package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory.EccType;
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
 * ECC efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class EccEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EccEfficiencyTest.class);
    /**
     * log(n) for most operations
     */
    private static final int LOG_N = 10;
    /**
     * log(n) for addition
     */
    private static final int ADD_LOG_N = 12;
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * ECC type
     */
    private static final EccType[] ECC_TYPES = new EccType[]{
        EccType.SEC_P256_K1_OPENSSL,
        EccType.SEC_P256_K1_BC,
        EccType.SEC_P256_R1_OPENSSL,
        EccType.SEC_P256_R1_BC,
        EccType.SM2_P256_V1_OPENSSL,
        EccType.SM2_P256_V1_BC,
        EccType.CURVE25519_BC,
        EccType.ED25519_BC
    };

    /**
     * Byte ECC type
     */
    private static final ByteEccFactory.ByteEccType[] BYTE_MUL_ECC_TYPES = new ByteEccFactory.ByteEccType[]{
        ByteEccFactory.ByteEccType.X25519_SODIUM,
        ByteEccFactory.ByteEccType.X25519_BC,
        ByteEccFactory.ByteEccType.ED25519_SODIUM,
        ByteEccFactory.ByteEccType.ED25519_BC,
        ByteEccFactory.ByteEccType.FOUR_Q,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                name", "  Hash(ms)", "RndPt.(ms)", " Add.(ms)", " Mul.(ms)", "Pre.(ms)", "PMul.(ms)"
        );
        int n = 1 << LOG_N;
        int addN = 1 << ADD_LOG_N;
        for (EccType type : ECC_TYPES) {
            Ecc ecc = EccFactory.createInstance(type);
            // generate random messages
            byte[][] messages = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(message);
                    return message;
                })
                .toArray(byte[][]::new);

            // warmup
            Arrays.stream(messages).forEach(ecc::hashToCurve);

            // hash
            STOP_WATCH.start();
            Arrays.stream(messages).forEach(ecc::hashToCurve);
            STOP_WATCH.stop();
            String hashToCurveTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            // random point
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> ecc.randomPoint(SECURE_RANDOM));
            STOP_WATCH.stop();
            String randomPointTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            // generate two random points for addition
            ECPoint[] h1s = IntStream.range(0, addN).mapToObj(index -> ecc.randomPoint(SECURE_RANDOM)).toArray(ECPoint[]::new);
            ECPoint[] h2s = IntStream.range(0, addN).mapToObj(index -> ecc.randomPoint(SECURE_RANDOM)).toArray(ECPoint[]::new);
            STOP_WATCH.start();
            IntStream.range(0, addN).forEach(index -> ecc.add(h1s[index], h2s[index]));
            STOP_WATCH.stop();
            String addTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / addN);
            STOP_WATCH.reset();

            // generate random points and random scalars for multiplication
            ECPoint[] hs = IntStream.range(0, n).mapToObj(index -> ecc.randomPoint(SECURE_RANDOM)).toArray(ECPoint[]::new);
            BigInteger[] rs = IntStream.range(0, n).mapToObj(index -> ecc.randomZn(SECURE_RANDOM)).toArray(BigInteger[]::new);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> ecc.multiply(hs[index], rs[index]));
            STOP_WATCH.stop();
            String multiplyTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            // generate a random point for pre-computation
            ECPoint h = ecc.randomPoint(SECURE_RANDOM);
            // precompute
            STOP_WATCH.start();
            ecc.precompute(h);
            STOP_WATCH.stop();
            String precomputeTime = String.valueOf(STOP_WATCH.getTime(TimeUnit.MILLISECONDS));
            STOP_WATCH.reset();
            // fix-point multiplication
            STOP_WATCH.start();
            Arrays.stream(rs).forEach(r -> ecc.multiply(h, r));
            STOP_WATCH.stop();
            String fixMultiplyTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();
            // destroy precompute
            ecc.destroyPrecompute(h);

            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(hashToCurveTime, 10),
                StringUtils.leftPad(randomPointTime, 10),
                StringUtils.leftPad(addTime, 10),
                StringUtils.leftPad(multiplyTime, 10),
                StringUtils.leftPad(precomputeTime, 10),
                StringUtils.leftPad(fixMultiplyTime, 10)
            );
        }
        for (ByteEccType type : BYTE_MUL_ECC_TYPES) {
            ByteMulEcc byteMulEcc = ByteEccFactory.createMulInstance(type);
            // generate random messages
            byte[][] messages = IntStream.range(0, n)
                .mapToObj(index -> {
                    byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    SECURE_RANDOM.nextBytes(message);
                    return message;
                })
                .toArray(byte[][]::new);

            // warmup
            Arrays.stream(messages).forEach(byteMulEcc::hashToCurve);

            // hash
            STOP_WATCH.start();
            Arrays.stream(messages).forEach(byteMulEcc::hashToCurve);
            STOP_WATCH.stop();
            String hashToCurveTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            // random point
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> byteMulEcc.randomPoint(SECURE_RANDOM));
            STOP_WATCH.stop();
            String randomPointTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            String addTime = "--";
            if (ByteEccFactory.isByteFullEcc(type)) {
                ByteFullEcc byteFullEcc = (ByteFullEcc) byteMulEcc;
                // generate two random points for addition
                byte[][] h1s = IntStream.range(0, addN).mapToObj(index -> byteMulEcc.randomPoint(SECURE_RANDOM)).toArray(byte[][]::new);
                byte[][] h2s = IntStream.range(0, addN).mapToObj(index -> byteMulEcc.randomPoint(SECURE_RANDOM)).toArray(byte[][]::new);
                STOP_WATCH.start();
                IntStream.range(0, addN).forEach(index -> byteFullEcc.add(h1s[index], h2s[index]));
                STOP_WATCH.stop();
                addTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / addN);
                STOP_WATCH.reset();
            }

            // generate random points and random scalars for multiplication
            byte[][] hs = IntStream.range(0, n).mapToObj(index -> byteMulEcc.randomPoint(SECURE_RANDOM)).toArray(byte[][]::new);
            byte[][] rs = IntStream.range(0, n).mapToObj(index -> byteMulEcc.randomScalar(SECURE_RANDOM)).toArray(byte[][]::new);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> byteMulEcc.mul(hs[index], rs[index]));
            STOP_WATCH.stop();
            String multiplyTime = TIME_DECIMAL_FORMAT.format((double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / n);
            STOP_WATCH.reset();

            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad("(B) " + type.name(), 20),
                StringUtils.leftPad(hashToCurveTime, 10),
                StringUtils.leftPad(randomPointTime, 10),
                StringUtils.leftPad(addTime, 10),
                StringUtils.leftPad(multiplyTime, 10),
                StringUtils.leftPad("--", 10),
                StringUtils.leftPad("--", 10)
            );
        }
    }
}
