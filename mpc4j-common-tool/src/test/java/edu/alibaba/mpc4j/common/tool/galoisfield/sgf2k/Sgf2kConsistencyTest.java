package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
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
 * Subfield GF2K consistency test.
 *
 * @author Weiran Liu
 * @date 2024/6/3
 */
@RunWith(Parameterized.class)
public class Sgf2kConsistencyTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int subfieldL : new int[]{2, 4, 8, 16, 32, 64}) {
            // STANDARD
            configurations.add(new Object[]{
                EnvType.STANDARD.name() + " v.s. " + EnvType.STANDARD_JDK.name() + " (subfieldL = " + subfieldL + ")",
                Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL), Sgf2kFactory.getInstance(EnvType.STANDARD_JDK, subfieldL)
            });
            // INLAND
            configurations.add(new Object[]{
                EnvType.INLAND.name() + " v.s. " + EnvType.INLAND_JDK.name() + " (subfieldL = " + subfieldL + ")",
                Sgf2kFactory.getInstance(EnvType.INLAND, subfieldL), Sgf2kFactory.getInstance(EnvType.INLAND_JDK, subfieldL)
            });
        }

        for (int subfieldL : new int[]{2, 4, 8, 16, 32, 64}) {
            // NTL vs. Rings
            NtlSubSgf2k ntlSgf2k = new NtlSubSgf2k(EnvType.STANDARD, subfieldL);
            RingsSubSgf2k ringsSgf2k = new RingsSubSgf2k(EnvType.STANDARD, subfieldL);
            configurations.add(new Object[]{"NTL vs. Rings (subfield L = " + subfieldL + ")", ntlSgf2k, ringsSgf2k});
        }

        return configurations;
    }

    /**
     * this Subfield GF2K
     */
    private final Sgf2k thisSgf2k;
    /**
     * that Subfield GF2K
     */
    private final Sgf2k thatSgf2k;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Sgf2kConsistencyTest(String name, Sgf2k thisSgf2k, Sgf2k thatSgf2k) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.thisSgf2k = thisSgf2k;
        this.thatSgf2k = thatSgf2k;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testConsistency() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] p = thisSgf2k.createNonZeroRandom(secureRandom);
            byte[] q = thatSgf2k.createNonZeroRandom(secureRandom);
            // mul
            byte[] thisMulR = thisSgf2k.mul(p, q);
            byte[] thatMulR = thatSgf2k.mul(p, q);
            Assert.assertArrayEquals(thisMulR, thatMulR);
            // inv
            byte[] thisInvR = thisSgf2k.inv(p);
            byte[] thatInvR = thatSgf2k.inv(p);
            Assert.assertArrayEquals(thisInvR, thatInvR);
            // div
            byte[] thisDivR = thisSgf2k.div(p, q);
            byte[] thatDivR = thatSgf2k.div(p, q);
            Assert.assertArrayEquals(thisDivR, thatDivR);
            // muli
            byte[] thisMuliR = BytesUtils.clone(p);
            thisSgf2k.muli(thisMuliR, q);
            byte[] thatMuliR = BytesUtils.clone(p);
            thatSgf2k.muli(thatMuliR, q);
            Assert.assertArrayEquals(thisMuliR, thatMuliR);
            // invi
            byte[] thisInviR = BytesUtils.clone(p);
            thisSgf2k.invi(thisMuliR);
            byte[] thatInviR = BytesUtils.clone(p);
            thatSgf2k.invi(thatMuliR);
            Assert.assertArrayEquals(thisInviR, thatInviR);
            // divi
            // div
            byte[] thisDiviR = BytesUtils.clone(p);
            thisSgf2k.divi(thisDiviR, q);
            byte[] thatDiviR = BytesUtils.clone(p);
            thatSgf2k.divi(thatDiviR, q);
            Assert.assertArrayEquals(thisDiviR, thatDiviR);
        }
    }
}
