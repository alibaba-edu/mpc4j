package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19.Rss19Zp64CoreMtgConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSS19-Zp64 multiplication triple test.
 *
 * @author Weiran Liu
 * @date 2023/2/15
 */
@Ignore
public class Rss19Zp64CoreMtgConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rss19Zp64CoreMtgConfigTest.class);
    /**
     * polynomial modulus degrees
     */
    private static final int[] POLY_MODULUS_DEGREES = new int[] {
        2048, 4096, 8192
    };

    @Test
    public void testValidL() {
        for (int polyModulusDegree : POLY_MODULUS_DEGREES) {
            for (int l = 1; l < LongUtils.MAX_L; l++) {
                try {
                    Rss19Zp64CoreMtgConfig config = new Rss19Zp64CoreMtgConfig.Builder(l)
                        .setPolyModulusDegree(polyModulusDegree)
                        .build();
                    Zp64 zp64 = config.getZp64();
                    LOGGER.info("modulus degree  = {}: l = {}: p = {}", polyModulusDegree, l, zp64.getPrime());
                } catch (Exception ignored) {

                }
            }
        }
    }
}
