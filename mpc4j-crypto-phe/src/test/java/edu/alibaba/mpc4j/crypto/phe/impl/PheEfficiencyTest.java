package edu.alibaba.mpc4j.crypto.phe.impl;

import edu.alibaba.mpc4j.crypto.phe.PheEngine;
import edu.alibaba.mpc4j.crypto.phe.PheEngineTestConfiguration;
import edu.alibaba.mpc4j.crypto.phe.PheTestUtils;
import edu.alibaba.mpc4j.crypto.phe.params.PhePrivateKey;
import edu.alibaba.mpc4j.crypto.phe.params.PhePublicKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 半同态加密性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/28
 */
@Ignore
public class PheEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PheEfficiencyTest.class);
    /**
     * 性能测试轮数
     */
    private static final int LOG_N = 8;
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
    private static final PheEngineTestConfiguration[] CONFIGURATIONS = new PheEngineTestConfiguration[] {
        // OU98
        (PheEngineTestConfiguration)PheEngineTestConfiguration.OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40[1],
        (PheEngineTestConfiguration)PheEngineTestConfiguration.OU98_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80[1],
        // Pai99
        (PheEngineTestConfiguration)PheEngineTestConfiguration.PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_40[1],
        (PheEngineTestConfiguration)PheEngineTestConfiguration.PAI99_NAME_CONFIGURATION_SIGNED_FULL_PRECISION_80[1],
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                name", "   pLength", " PkEnc(ms)", " SkEnc(ms)", "  Add.(ms)", "  Mul.(ms)", "  Dec.(ms)"
        );
        int n = 1 << LOG_N;
        for (PheEngineTestConfiguration configuration : CONFIGURATIONS) {
            PheEngine pheEngine = configuration.getPheEngine();
            PhePrivateKey sk = configuration.getPrivateKey();
            PhePublicKey pk = sk.getPublicKey();
            // 生成明文
            int plaintextBitLength = pk.getModulus().bitLength() - 2;
            BigInteger[] plaintexts = IntStream.range(0, n)
                .mapToObj(index -> new BigInteger(plaintextBitLength, PheTestUtils.SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            // 预热
            Arrays.stream(plaintexts).forEach(plaintext -> pheEngine.rawEncrypt(pk, plaintext));
            // 公钥加密
            STOP_WATCH.start();
            BigInteger[] ciphertext1s = Arrays.stream(plaintexts)
                .map(plaintext -> pheEngine.rawEncrypt(pk, plaintext))
                .toArray(BigInteger[]::new);
            STOP_WATCH.stop();
            double pkEncTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / (double) n;
            STOP_WATCH.reset();
            // 私钥加密
            STOP_WATCH.start();
            BigInteger[] ciphertext2s = Arrays.stream(plaintexts)
                .map(plaintext -> pheEngine.rawEncrypt(sk, plaintext))
                .toArray(BigInteger[]::new);
            STOP_WATCH.stop();
            double skEncTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / (double) n;
            STOP_WATCH.reset();
            // 加法运算
            STOP_WATCH.start();
            BigInteger[] ciphertext3s = IntStream.range(0, n)
                .mapToObj(index -> pheEngine.rawAdd(pk, ciphertext1s[index], ciphertext2s[index]))
                .toArray(BigInteger[]::new);
            STOP_WATCH.stop();
            double addTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / (double) n;
            STOP_WATCH.reset();
            // 乘法运算
            STOP_WATCH.start();
            BigInteger[] ciphertext4s = IntStream.range(0, n)
                .mapToObj(index -> pheEngine.rawMultiply(pk, ciphertext3s[index], plaintexts[index]))
                .toArray(BigInteger[]::new);
            STOP_WATCH.stop();
            double mulTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / (double) n;
            STOP_WATCH.reset();
            // 解密运算
            STOP_WATCH.start();
            Arrays.stream(ciphertext4s).forEach(ciphertext -> pheEngine.rawDecrypt(sk, ciphertext));
            STOP_WATCH.stop();
            double decTime = STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / (double) n;
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(pheEngine.getPheType().name(), 20),
                StringUtils.leftPad(String.valueOf(pheEngine.primeBitLength(pk)), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(pkEncTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(skEncTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(addTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(mulTime) , 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(decTime), 10)
            );
        }
    }
}
