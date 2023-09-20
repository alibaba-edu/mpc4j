package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import org.junit.Assert;
import org.junit.Test;

/**
 * CM20-MP-OPRF protocol description test.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Cm20MpOprfPtoDescTest {

    @Test
    public void testSearchW() {
        for (int logN = Cm20MpOprfPtoDesc.MIN_LOG_N_FOR_W; logN <= Cm20MpOprfPtoDesc.MAX_LOG_N_FOR_W; logN++) {
            int n = 1 << logN;
            int w = Cm20MpOprfUtils.searchW(n);
            Assert.assertEquals(w, Cm20MpOprfPtoDesc.LOG_N_W_MAP.get(logN));
        }
    }
}
