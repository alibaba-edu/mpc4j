package edu.alibaba.mpc4j.common.tool.coder;

import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoderUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 随机编码工具类测试。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
@Ignore
public class RandomCoderUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomCoderUtilsTest.class);

    @Test
    public void testMaxCallTime() {
        LOGGER.info("l(B)\tl(b)\tmax call time");
        // 经过测试，输出51字节长度的伪随机数编码支持的PRC调用次数已经为负数了
        for (int codewordByteLength = 52; codewordByteLength < 65; codewordByteLength++) {
            testMaxCallTime(codewordByteLength);
        }
    }

    private void testMaxCallTime(int codewordByteLength) {
        int maxCallTime = RandomCoderUtils.getLogMaxCallTime(codewordByteLength);
        LOGGER.info("{}\t\t{}\t\t2^{}", codewordByteLength, codewordByteLength * Byte.SIZE, maxCallTime);
    }
}
