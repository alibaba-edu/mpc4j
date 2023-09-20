package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^64) consistency test.
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
@RunWith(Parameterized.class)
@Ignore
public class Gf64ConsistencyTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 1000;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // NTL V.S. COMBINED
        configurations.add(new Object[]{"BC V.S. COMBINED", Gf64Type.NTL, Gf64Type.COMBINED});
        // NTL V.S. JDK
        configurations.add(new Object[]{"BC V.S. JDK", Gf64Type.NTL, Gf64Type.JDK});
        // NTL V.S. RINGS
        configurations.add(new Object[]{"BC V.S. RINGS", Gf64Type.NTL, Gf64Type.RINGS});

        return configurations;
    }

    /**
     * this GF(2^64)
     */
    private final Gf64 thisGf64;
    /**
     * that GF(2^64)
     */
    private final Gf64 thatGf64;

    public Gf64ConsistencyTest(String name, Gf64Type thisType, Gf64Type thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        thisGf64 = Gf64Factory.createInstance(EnvType.STANDARD, thisType);
        thatGf64 = Gf64Factory.createInstance(EnvType.STANDARD, thatType);
    }

    @Test
    public void testMulConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] a = thisGf64.createRandom(SECURE_RANDOM);
            byte[] b = thisGf64.createRandom(SECURE_RANDOM);
            byte[] thisResult = thisGf64.mul(a, b);
            byte[] thatResult = thatGf64.mul(a, b);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }

    @Test
    public void testMuliConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] thisA = thisGf64.createRandom(SECURE_RANDOM);
            byte[] thatA = BytesUtils.clone(thisA);
            byte[] b = thisGf64.createRandom(SECURE_RANDOM);
            thisGf64.muli(thisA, b);
            thatGf64.muli(thatA, b);
            Assert.assertArrayEquals(thisA, thatA);
        }
    }
}
