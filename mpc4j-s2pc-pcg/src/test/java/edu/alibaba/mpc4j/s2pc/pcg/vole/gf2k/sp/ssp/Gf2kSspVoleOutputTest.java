package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Single single-point GF2K-VOLE tests.
 *
 * @author Weiran Liu
 * @date 2024/5/30
 */
@RunWith(Parameterized.class)
public class Gf2kSspVoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int l : new int[]{2, 4, 8, 16, 32, 64, 128}) {
            configurations.add(new Object[]{"l = " + l, l});
        }

        return configurations;
    }

    /**
     * field
     */
    private final Sgf2k field;
    /**
     * subfield
     */
    private final Gf2e subfield;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2kSspVoleOutputTest(String name, int subfieldL) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
        subfield = field.getSubfield();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testSenderIllegalInputs() {
        // create a sender output with length 0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
            Gf2kSspVoleSenderOutput.create(field, 0, xAlpha, new byte[0][]);
        });
        int subfieldByteL = subfield.getByteL();
        int subfieldL = subfield.getL();
        int fieldByteL = field.getByteL();
        int fieldL = field.getL();
        // create a sender output with negative α
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleSenderOutput.create(field, -1, xAlpha, t);
        });
        // create a sender output with large α
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleSenderOutput.create(field, MAX_NUM, xAlpha, t);
        });
        // create a sender output with x[α] = 0
        Assert.assertThrows(AssertionError.class, () -> {
            int alpha = secureRandom.nextInt(MAX_NUM);
            byte[] xAlpha = subfield.createZero();
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleSenderOutput.create(field, alpha, xAlpha, t);
        });
        // create a sender output with small length x
        if (subfieldByteL > 1) {
            Assert.assertThrows(AssertionError.class, () -> {
                int alpha = secureRandom.nextInt(MAX_NUM);
                byte[] xAlpha = BytesUtils.randomNonZeroByteArray(subfieldByteL - 1, secureRandom);
                byte[][] t = IntStream.range(0, MAX_NUM)
                    .mapToObj(index -> field.createRandom(secureRandom))
                    .toArray(byte[][]::new);
                Gf2kSspVoleSenderOutput.create(field, alpha, xAlpha, t);
            });
        }
        // create a sender output with large length x
        Assert.assertThrows(AssertionError.class, () -> {
            int alpha = secureRandom.nextInt(MAX_NUM);
            byte[] xAlpha = BytesUtils.randomNonZeroByteArray(subfieldByteL + 1, subfieldL, secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleSenderOutput.create(field, alpha, xAlpha, t);
        });
        // create a sender output with small length t
        Assert.assertThrows(AssertionError.class, () -> {
            int alpha = secureRandom.nextInt(MAX_NUM);
            byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kSspVoleSenderOutput.create(field, alpha, xAlpha, t);
        });
        // create a sender output with large length t
        Assert.assertThrows(AssertionError.class, () -> {
            int alpha = secureRandom.nextInt(MAX_NUM);
            byte[] xAlpha = subfield.createNonZeroRandom(secureRandom);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kSspVoleSenderOutput.create(field, alpha, xAlpha, t);
        });
    }

    @Test
    public void testReceiverIllegalInputs() {
        // create a receiver output with length 0
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            Gf2kSspVoleReceiverOutput.create(field, delta, new byte[0][]);
        });
        int fieldByteL = field.getByteL();
        int fieldL = field.getL();
        // create a receiver output with small length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(fieldByteL - 1, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(fieldByteL + 1, fieldL, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kSspVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with small length q
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kSspVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output large length q
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kSspVoleReceiverOutput.create(field, delta, q);
        });
    }

    @Test
    public void testCreateRandom() {
        for (int num = MIN_NUM; num <= MAX_NUM; num++) {
            testCreateRandom(num);
        }
    }

    private void testCreateRandom(int num) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] delta = field.createRangeRandom(secureRandom);
            Gf2kSspVoleReceiverOutput receiverOutput = Gf2kSspVoleReceiverOutput.create(field, num, delta, secureRandom);
            Gf2kSspVoleSenderOutput senderOutput = Gf2kSspVoleSenderOutput.create(receiverOutput, secureRandom);
            VoleTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
        }

    }
}
