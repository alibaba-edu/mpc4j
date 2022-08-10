package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CM20-MPOPRF工具类测试。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cm20MpOprfUtilsTest.class);

    @Test
    public void testSearchW() {
        LOGGER.info("-----search w-----");
        LOGGER.info("n\tw");
        // 计算log(n) = 8到log(n) = 24的取值
        for (int nLogValue = 8; nLogValue <= 24; nLogValue++) {
            LOGGER.info("2^{}\t{}", nLogValue, Cm20MpOprfUtils.searchW(1 << nLogValue));
        }
    }
}
