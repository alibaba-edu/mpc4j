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
        // We thank Qixian Zhou for pointing out that
        // the maximal number of RPC calling is negative when codeword byte length is less than or equal to 49.
        for (int codewordByteLength = 50; codewordByteLength < 65; codewordByteLength++) {
            testMaxCallTime(codewordByteLength);
        }
    }

    private void testMaxCallTime(int codewordByteLength) {
        int maxCallTime = RandomCoderUtils.getLogMaxCallTime(codewordByteLength);
        LOGGER.info("{}\t\t{}\t\t2^{}", codewordByteLength, codewordByteLength * Byte.SIZE, maxCallTime);
    }
}
