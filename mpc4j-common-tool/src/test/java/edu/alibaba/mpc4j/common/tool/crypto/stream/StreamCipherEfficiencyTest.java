package edu.alibaba.mpc4j.common.tool.crypto.stream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory.StreamCipherType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 流密码性能测试。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
@Ignore
public class StreamCipherEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamCipherEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int LOG_N = 18;
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("000.0000");
    /**
     * 全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 高性能类型
     */
    private static final StreamCipherType[] TYPES = new StreamCipherType[] {
        // JDK_AES_OFB
        StreamCipherType.JDK_AES_OFB,
        // BC_AES_OFB
        StreamCipherType.BC_AES_OFB,
        // BC_SM4_OFB
        StreamCipherType.BC_SM4_OFB,
        // BC_ZUC_128
        StreamCipherType.BC_ZUC_128,
    };
    /**
     * 明文字节长度
     */
    private static final int[] BYTE_LENGTH_ARRAY = new int[] {1, 16, 32, 64};

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}", "                name", "     bytes", "   enc(us)", "iv_enc(us)");
        for (int byteLength : BYTE_LENGTH_ARRAY) {
            for (StreamCipherType type : TYPES) {
                testEfficiency(type, byteLength);
            }
            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }

    private void testEfficiency(StreamCipherType type, int byteLength) {
        int n = 1 << LOG_N;
        StreamCipher streamCipher = StreamCipherFactory.createInstance(type);
        byte[] iv = new byte[streamCipher.ivByteLength()];
        byte[] plaintext = new byte[byteLength];
        // 预热
        IntStream.range(0, n).forEach(index -> streamCipher.encrypt(ZERO_KEY, iv, plaintext));
        // enc性能
        STOP_WATCH.start();
        IntStream.range(0, n).forEach(index -> streamCipher.encrypt(ZERO_KEY, iv, plaintext));
        STOP_WATCH.stop();
        double encTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
        STOP_WATCH.reset();
        // ivEnc性能
        STOP_WATCH.start();
        IntStream.range(0, n).forEach(index -> streamCipher.ivEncrypt(ZERO_KEY, iv, plaintext));
        STOP_WATCH.stop();
        double ivEncTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n;
        STOP_WATCH.reset();
        LOGGER.info("{}\t{}\t{}\t{}",
            StringUtils.leftPad(type.name(), 20),
            StringUtils.leftPad(String.valueOf(byteLength), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(encTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(ivEncTime), 10)
        );
    }
}
