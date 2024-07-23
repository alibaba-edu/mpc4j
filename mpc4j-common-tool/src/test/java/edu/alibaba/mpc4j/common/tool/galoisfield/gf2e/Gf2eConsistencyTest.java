package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.GaloisfieldTestUtils;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * GF(2^l) consistency test.
 *
 * @author Weiran Liu
 * @date 2022/5/19
 */
@RunWith(Parameterized.class)
public class Gf2eConsistencyTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int l : GaloisfieldTestUtils.GF2E_L_ARRAY) {
            // STANDARD
            configurations.add(new Object[]{
                EnvType.STANDARD.name() + " v.s. " + EnvType.STANDARD_JDK.name() + "(l = " + l + ")",
                Gf2eFactory.getType(EnvType.STANDARD, l), Gf2eFactory.getType(EnvType.STANDARD_JDK, l), l
            });
            // INLAND
            configurations.add(new Object[]{
                EnvType.INLAND.name() + " v.s. " + EnvType.INLAND_JDK.name() + "(l = " + l + ")",
                Gf2eFactory.getType(EnvType.INLAND, l), Gf2eFactory.getType(EnvType.INLAND_JDK, l), l
            });
            Gf2eType thatType = Gf2eType.NTL;
            Gf2eType thisType;
            // COMBINED V.S. NTL
            thisType = Gf2eType.COMBINED;
            if (Gf2eFactory.available(thisType, l)) {
                configurations.add(new Object[]{
                    thisType + " v.s. " + thatType + " (l = " + l + ")", thisType, thatType, l
                });
            }
            // JDK V.S. NTL
            if (Gf2eFactory.available(thisType, l)) {
                configurations.add(new Object[]{
                    thisType + " v.s. " + thatType + " (l = " + l + ")", thisType, thatType, l
                });
            }
            thisType = Gf2eType.JDK;
            if (Gf2eFactory.available(thisType, l)) {
                configurations.add(new Object[]{
                    thisType + " v.s. " + thatType + " (l = " + l + ")", thisType, thatType, l
                });
            }
            // Rings V.S. NTL
            thisType = Gf2eType.RINGS;
            configurations.add(new Object[]{
                thisType + " v.s. " + thatType + " (l = " + l + ")", thisType, thatType, l
            });
        }

        return configurations;
    }

    /**
     * this GF(2^l)
     */
    private final Gf2e thisGf2e;
    /**
     * that GF(2^l)
     */
    private final Gf2e thatGf2e;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2eConsistencyTest(String name, Gf2eType thisType, Gf2eType thatType, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        thisGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, thisType, l);
        thatGf2e = Gf2eFactory.createInstance(EnvType.STANDARD, thatType, l);
        Assert.assertEquals(thisGf2e.getL(), thatGf2e.getL());
        Assert.assertEquals(thisGf2e.getByteL(), thatGf2e.getByteL());
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConsistency() {
        byte[] p, q, thisR, thatR;
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // mul
            p = thisGf2e.createNonZeroRandom(secureRandom);
            q = thatGf2e.createNonZeroRandom(secureRandom);
            thisR = thisGf2e.mul(p, q);
            thatR = thatGf2e.mul(p, q);
            Assert.assertArrayEquals(thisR, thatR);
            // muli
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2e.muli(thisR, q);
            thatGf2e.muli(thatR, q);
            Assert.assertArrayEquals(thisR, thatR);
            // inv
            thisR = thisGf2e.inv(p);
            thatR = thatGf2e.inv(p);
            Assert.assertArrayEquals(thisR, thatR);
            // invi
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2e.invi(thisR);
            thatGf2e.invi(thatR);
            Assert.assertArrayEquals(thisR, thatR);
            // div
            thisR = thisGf2e.div(p, q);
            thatR = thatGf2e.div(p, q);
            Assert.assertArrayEquals(thisR, thatR);
            // divi
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2e.divi(thisR, q);
            thatGf2e.divi(thatR, q);
            Assert.assertArrayEquals(thisR, thatR);
        }
    }
}
