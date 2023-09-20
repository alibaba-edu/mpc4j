package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
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
 * GF(2^128) consistency test.
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
@RunWith(Parameterized.class)
@Ignore
public class Gf2kConsistencyTest {
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

        // BC V.S. COMBINED
        configurations.add(new Object[]{"BC V.S. COMBINED", Gf2kType.BC, Gf2kType.COMBINED});
        // BC V.S. NTL
        configurations.add(new Object[]{"BC V.S. NTL", Gf2kType.BC, Gf2kType.NTL});
        // BC V.S. JDK
        configurations.add(new Object[]{"BC V.S. JDK", Gf2kType.BC, Gf2kType.JDK});
        // BC V.S. RINGS
        configurations.add(new Object[]{"BC V.S. RINGS", Gf2kType.BC, Gf2kType.RINGS});

        return configurations;
    }

    /**
     * this GF(2^128)
     */
    private final Gf2k thisGf2k;
    /**
     * that GF(2^128)
     */
    private final Gf2k thatGf2k;

    public Gf2kConsistencyTest(String name, Gf2kType thisType, Gf2kType thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        thisGf2k = Gf2kFactory.createInstance(EnvType.STANDARD, thisType);
        thatGf2k = Gf2kFactory.createInstance(EnvType.STANDARD, thatType);
    }

    @Test
    public void testMulConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] a = thisGf2k.createRandom(SECURE_RANDOM);
            byte[] b = thisGf2k.createRandom(SECURE_RANDOM);
            byte[] thisResult = thisGf2k.mul(a, b);
            byte[] thatResult = thatGf2k.mul(a, b);
            Assert.assertArrayEquals(thisResult, thatResult);
        }
    }

    @Test
    public void testMuliConsistency() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] thisA = thisGf2k.createRandom(SECURE_RANDOM);
            byte[] thatA = BytesUtils.clone(thisA);
            byte[] b = thisGf2k.createRandom(SECURE_RANDOM);
            thisGf2k.muli(thisA, b);
            thatGf2k.muli(thatA, b);
            Assert.assertArrayEquals(thisA, thatA);
        }
    }
}
