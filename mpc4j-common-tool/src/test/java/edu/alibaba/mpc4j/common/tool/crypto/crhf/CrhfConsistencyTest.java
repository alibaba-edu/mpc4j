package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * CRHF consistency test.
 *
 * @author Weiran Liu
 * @date 2024/6/24
 */
@RunWith(Parameterized.class)
public class CrhfConsistencyTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MMO_SIGMA
        configurations.add(new Object[] {"MMO_SIGMA", CrhfType.JDK_MMO_SIGMA, CrhfType.SIMD_MMO_SIGMA});

        return configurations;
    }

    /**
     * this type
     */
    private final CrhfType thisType;
    /**
     * that type
     */
    private final CrhfType thatType;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public CrhfConsistencyTest(String name, CrhfType thisType, CrhfType thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.thisType = thisType;
        this.thatType = thatType;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConsistency() {
        Crhf thisCrhf = CrhfFactory.createInstance(EnvType.STANDARD, thisType);
        Crhf thatCrhf = CrhfFactory.createInstance(EnvType.STANDARD, thatType);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] message = BlockUtils.randomBlock(secureRandom);
            byte[] thisResult = thisCrhf.hash(message);
            byte[] thatResult = thatCrhf.hash(message);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }
}
