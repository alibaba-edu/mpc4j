package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
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
 * GF(2^128) consistency test.
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
@RunWith(Parameterized.class)
public class Gf2kConsistencyTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // STANDARD
        configurations.add(new Object[]{
            EnvType.STANDARD.name() + " v.s. " + EnvType.STANDARD_JDK.name(),
            Gf2kFactory.getType(EnvType.STANDARD), Gf2kFactory.getType(EnvType.STANDARD_JDK)
        });
        // INLAND
        configurations.add(new Object[]{
            EnvType.INLAND.name() + " v.s. " + EnvType.INLAND_JDK.name(),
            Gf2kFactory.getType(EnvType.INLAND), Gf2kFactory.getType(EnvType.INLAND_JDK)
        });
        // COMBINED V.S. NTL
        configurations.add(new Object[]{Gf2kType.COMBINED + " v.s. " + Gf2kType.NTL, Gf2kType.COMBINED, Gf2kType.NTL});
        // COMBINED V.S. JDK
        configurations.add(new Object[]{Gf2kType.COMBINED + " v.s. " + Gf2kType.JDK, Gf2kType.COMBINED, Gf2kType.JDK});
        // COMBINED V.S. RINGS
        configurations.add(new Object[]{Gf2kType.COMBINED + " v.s. " + Gf2kType.RINGS, Gf2kType.COMBINED, Gf2kType.RINGS});

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
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2kConsistencyTest(String name, Gf2kType thisType, Gf2kType thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        thisGf2k = Gf2kFactory.createInstance(EnvType.STANDARD, thisType);
        thatGf2k = Gf2kFactory.createInstance(EnvType.STANDARD, thatType);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConsistency() {
        byte[] p, q, thisR, thatR;
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // mul
            p = thisGf2k.createNonZeroRandom(secureRandom);
            q = thatGf2k.createNonZeroRandom(secureRandom);
            thisR = thisGf2k.mul(p, q);
            thatR = thatGf2k.mul(p, q);
            Assert.assertArrayEquals(thisR, thatR);
            // muli
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2k.muli(thisR, q);
            thatGf2k.muli(thatR, q);
            Assert.assertArrayEquals(thisR, thatR);
            // inv
            thisR = thisGf2k.inv(p);
            thatR = thatGf2k.inv(p);
            Assert.assertArrayEquals(thisR, thatR);
            // invi
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2k.invi(thisR);
            thatGf2k.invi(thatR);
            Assert.assertArrayEquals(thisR, thatR);
            // div
            thisR = thisGf2k.div(p, q);
            thatR = thatGf2k.div(p, q);
            Assert.assertArrayEquals(thisR, thatR);
            // divi
            thisR = BytesUtils.clone(p);
            thatR = BytesUtils.clone(p);
            thisGf2k.divi(thisR, q);
            thatGf2k.divi(thatR, q);
            Assert.assertArrayEquals(thisR, thatR);
        }
    }
}
