package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

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
 * GF2K-VOLE output test.
 *
 * @author Weiran Liu
 * @date 2024/5/28
 */
@RunWith(Parameterized.class)
public class Gf2kVoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;

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

    public Gf2kVoleOutputTest(String name, int subfieldL) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        field = Sgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
        subfield = field.getSubfield();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with mismatched length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(field, x, t);
        });
        int subfieldByteL = subfield.getByteL();
        int subfieldL = subfield.getL();
        int fieldByteL = field.getByteL();
        int fieldL = field.getL();
        // create a sender output with small length x
        if (subfieldByteL > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] x = BytesUtils.randomNonZeroByteArrayVector(MAX_NUM, subfieldByteL - 1, secureRandom);
                byte[][] t = IntStream.range(0, MAX_NUM)
                    .mapToObj(index -> field.createRandom(secureRandom))
                    .toArray(byte[][]::new);
                Gf2kVoleSenderOutput.create(field, x, t);
            });
        }
        // create a sender output with large length x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = BytesUtils.randomNonZeroByteArrayVector(MAX_NUM, subfieldByteL + 1, subfieldL, secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with small length t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with large length t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kVoleSenderOutput.create(field, x, t);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        int fieldByteL = field.getByteL();
        int fieldL = field.getL();
        // create a receiver output with small length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(fieldByteL - 1, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(fieldByteL + 1, fieldL, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with small length q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output large length q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kVoleReceiverOutput.create(field, delta, q);
        });
    }

    @Test
    public void testIllegalUpdate() {
        byte[] delta = field.createRangeRandom(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(5);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = field.createRangeRandom(secureRandom);
            byte[] delta1 = field.createRangeRandom(secureRandom);
            Gf2kVoleReceiverOutput receiverOutput0 = Gf2kVoleReceiverOutput.createRandom(field, 4, delta0, secureRandom);
            Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleReceiverOutput.createRandom(field, 4, delta1, secureRandom);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        byte[] delta = field.createRangeRandom(secureRandom);
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
        VoleTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        byte[] delta = field.createRangeRandom(secureRandom);
        // reduce to 1
        Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutput1 = Gf2kVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        VoleTestUtils.assertOutput(field, 1, senderOutput1, receiverOutput1);
        // reduce all
        Gf2kVoleReceiverOutput receiverOutputAll = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutputAll = Gf2kVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        VoleTestUtils.assertOutput(field, num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce to num - 1
            Gf2kVoleReceiverOutput receiverOutputNum = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutputNum = Gf2kVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            VoleTestUtils.assertOutput(field, num - 1, senderOutputNum, receiverOutputNum);
            // reduce to half
            Gf2kVoleReceiverOutput receiverOutputHalf = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutputHalf = Gf2kVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            VoleTestUtils.assertOutput(field, num / 2, senderOutputHalf, receiverOutputHalf);
        }
    }

    @Test
    public void testMerge() {
        for (int num1 = 0; num1 < MAX_NUM; num1++) {
            for (int num2 = 0; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        byte[] delta = field.createRangeRandom(secureRandom);
        Gf2kVoleReceiverOutput receiverOutput = Gf2kVoleReceiverOutput.createRandom(field, num1, delta, secureRandom);
        Gf2kVoleReceiverOutput mergeReceiverOutput = Gf2kVoleReceiverOutput.createRandom(field, num2, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.createRandom(receiverOutput, secureRandom);
        Gf2kVoleSenderOutput mergeSenderOutput = Gf2kVoleSenderOutput.createRandom(mergeReceiverOutput, secureRandom);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        VoleTestUtils.assertOutput(field, num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        byte[] delta = field.createRangeRandom(secureRandom);
        // split 1
        Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutput1 = Gf2kVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2kVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Gf2kVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        VoleTestUtils.assertOutput(field, num - 1, senderOutput1, receiverOutput1);
        VoleTestUtils.assertOutput(field, 1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Gf2kVoleReceiverOutput receiverOutputAll = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleSenderOutput senderOutputAll = Gf2kVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2kVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Gf2kVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        VoleTestUtils.assertOutput(field, 0, senderOutputAll, receiverOutputAll);
        VoleTestUtils.assertOutput(field, num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Gf2kVoleReceiverOutput receiverOutputNum = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutputNum = Gf2kVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2kVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Gf2kVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            VoleTestUtils.assertOutput(field, 1, senderOutputNum, receiverOutputNum);
            VoleTestUtils.assertOutput(field, num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Gf2kVoleReceiverOutput receiverOutputHalf = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleSenderOutput senderOutputHalf = Gf2kVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2kVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Gf2kVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            VoleTestUtils.assertOutput(field, num - num / 2, senderOutputHalf, receiverOutputHalf);
            VoleTestUtils.assertOutput(field, num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        byte[] delta = field.createRangeRandom(secureRandom);
        // split and merge 1
        Gf2kVoleReceiverOutput receiverOutput1 = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        Gf2kVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        Gf2kVoleSenderOutput senderOutput1 = Gf2kVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2kVoleSenderOutput copySenderOutput1 = senderOutput1.copy();
        Gf2kVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        // split and merge all
        Gf2kVoleReceiverOutput receiverOutputAll = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVoleReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        Gf2kVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        Gf2kVoleSenderOutput senderOutputAll = Gf2kVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2kVoleSenderOutput copySenderOutputAll = senderOutputAll.copy();
        Gf2kVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        if (num > 1) {
            // split and merge num - 1
            Gf2kVoleReceiverOutput receiverOutputNum = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            Gf2kVoleReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            Gf2kVoleSenderOutput senderOutputNum = Gf2kVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2kVoleSenderOutput copySenderOutputNum = senderOutputNum.copy();
            Gf2kVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            // split half
            Gf2kVoleReceiverOutput receiverOutputHalf = Gf2kVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVoleReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            Gf2kVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
            Gf2kVoleSenderOutput senderOutputHalf = Gf2kVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2kVoleSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            Gf2kVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
        }
    }
}
