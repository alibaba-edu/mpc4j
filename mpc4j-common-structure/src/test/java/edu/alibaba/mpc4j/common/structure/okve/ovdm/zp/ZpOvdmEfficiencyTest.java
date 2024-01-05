package edu.alibaba.mpc4j.common.structure.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.structure.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Zp-OVDM efficiency test.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@Ignore
public class ZpOvdmEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZpOvdmEfficiencyTest.class);
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * types
     */
    private static final ZpOvdmType[] TYPES = new ZpOvdmType[] {
        ZpOvdmType.H2_TWO_CORE_GCT,
        ZpOvdmType.H2_SINGLETON_GCT,
        ZpOvdmType.H3_SINGLETON_GCT,
        ZpOvdmType.LPRST21_GBF,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "      logN", " encode(s)", " decode(s)");
        testEfficiency(8);
        testEfficiency(10);
        testEfficiency(12);
        testEfficiency(14);
        testEfficiency(16);
        testEfficiency(18);
    }

    private void testEfficiency(int logN) {
        int n = 1 << logN;
        for (ZpOvdmType type : TYPES) {
            byte[][] keys = CommonUtils.generateRandomKeys(ZpOvdmFactory.getHashNum(type), ZpOvdmTestUtils.SECURE_RANDOM);
            // create instance
            ZpOvdm<ByteBuffer> ovdm = ZpOvdmFactory.createInstance(
                EnvType.STANDARD, type, ZpOvdmTestUtils.DEFAULT_PRIME, n, keys
            );
            // generate random key-value pairs
            Map<ByteBuffer, BigInteger> keyValueMap = ZpOvdmTestUtils.randomKeyValueMap(n);
            // encode
            STOP_WATCH.start();
            BigInteger[] storage = ovdm.encode(keyValueMap);
            STOP_WATCH.stop();
            double encodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            // decode
            STOP_WATCH.start();
            keyValueMap.keySet().forEach(key -> ovdm.decode(storage, key));
            STOP_WATCH.stop();
            double decodeTime = (double) STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
            STOP_WATCH.reset();
            LOGGER.info(
                "{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(encodeTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(decodeTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
