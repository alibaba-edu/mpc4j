package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSS19-核布尔三元组生成协议配置项测试。
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@Ignore
public class Rss19Zp64CoreMtgConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rss19Zp64CoreMtgConfigTest.class);

    @Test
    public void testConfigSetPrimeBitLength() {
        for (int size = 1; size < Long.SIZE - 1; size++) {
            try {
                Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder()
                    .setPrimeBitLength(size)
                    .build();
                long prime = config.getZp();
                LOGGER.info("config build success for plain bit length: {}, prime = {}", size, prime);
            } catch (Exception e) {
                LOGGER.info("config build  failed for plain bit length: {}", size);
            }
        }
    }
}
