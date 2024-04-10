package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Zp-DOKVS efficiency test.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
@Ignore
public class ZpDokvsEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpDokvsEfficiencyTest.class);
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final ZpDokvsType[] TYPES = ZpDokvsType.values();
    /**
     * default prime
     */
    private static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH * 2);
    /**
     * default Zp
     */
    private static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, DEFAULT_PRIME);

    @Test
    public void testEfficiency() {
        LOGGER.info(
            "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                          name", "         m", "        lm", "        rm", "  parallel",
            " encode(s)", " decode(s)", "dEncode(s)", "dDecode(s)"
        );
        testEfficiency(8);
        testEfficiency(10);
        testEfficiency(12);
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        testEfficiency(logN, false);
        testEfficiency(logN, true);
    }

    private void testEfficiency(int logN, boolean parallelEncode) {
        int n = 1 << logN;
        for (ZpDokvsType type : TYPES) {
            int hashNum = ZpDokvsFactory.getHashKeyNum(type);
            byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
            ZpDokvs<ByteBuffer> dokvs = ZpDokvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_PRIME, n, keys);
            dokvs.setParallelEncode(parallelEncode);
            Map<ByteBuffer, BigInteger> keyValueMap = ZpDokvsTest.randomKeyValueMap(DEFAULT_ZP, n);
            STOP_WATCH.start();
            BigInteger[] nonDoublyStorage = dokvs.encode(keyValueMap, false);
            STOP_WATCH.stop();
            double nonDoublyEncodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            Stream<ByteBuffer> nonDoublyKeyStream = keyValueMap.keySet().stream();
            nonDoublyKeyStream = parallelEncode ? nonDoublyKeyStream.parallel() : nonDoublyKeyStream;
            STOP_WATCH.start();
            Map<ByteBuffer, BigInteger> nonDoublyDecodeKeyValueMap = nonDoublyKeyStream
                .collect(Collectors.toMap(key -> key, key -> dokvs.decode(nonDoublyStorage, key)));
            STOP_WATCH.stop();
            double nonDoublyDecodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            keyValueMap.keySet().forEach(key -> Assert.assertEquals(keyValueMap.get(key), nonDoublyDecodeKeyValueMap.get(key)));
            STOP_WATCH.reset();
            STOP_WATCH.start();
            BigInteger[] doublyStorage = dokvs.encode(keyValueMap, true);
            STOP_WATCH.stop();
            double doublyEncodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            Stream<ByteBuffer> doublyKeyStream = keyValueMap.keySet().stream();
            doublyKeyStream = parallelEncode ? doublyKeyStream.parallel() : doublyKeyStream;
            STOP_WATCH.start();
            Map<ByteBuffer, BigInteger> doublyDecodeKeyValueMap = doublyKeyStream
                .collect(Collectors.toMap(key -> key, key -> dokvs.decode(doublyStorage, key)));
            STOP_WATCH.stop();
            double doublyDecodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            keyValueMap.keySet().forEach(key -> Assert.assertEquals(keyValueMap.get(key), doublyDecodeKeyValueMap.get(key)));
            STOP_WATCH.reset();
            String lm;
            String rm;
            if (dokvs instanceof SparseZpDokvs) {
                SparseZpDokvs<ByteBuffer> sparseDokvs = (SparseZpDokvs<ByteBuffer>) dokvs;
                int m = sparseDokvs.getM();
                lm = String.valueOf(sparseDokvs.sparsePositionRange());
                rm = String.valueOf(m - sparseDokvs.sparsePositionRange());
            } else {
                lm = "-";
                rm = "-";
            }
            LOGGER.info(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 30),
                StringUtils.leftPad(String.valueOf(dokvs.getM()), 10),
                StringUtils.leftPad(lm, 10),
                StringUtils.leftPad(rm, 10),
                StringUtils.leftPad(String.valueOf(parallelEncode), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(nonDoublyDecodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyEncodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(doublyDecodeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
