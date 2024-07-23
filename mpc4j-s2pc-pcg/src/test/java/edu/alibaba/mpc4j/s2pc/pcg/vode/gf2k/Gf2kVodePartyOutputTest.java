package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.VodeTestUtils;
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
 * GF2K-VODE party output test.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
@RunWith(Parameterized.class)
public class Gf2kVodePartyOutputTest {
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
    private final Dgf2k field;
    /**
     * subfield
     */
    private final Gf2e subfield;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2kVodePartyOutputTest(String name, int subfieldL) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        field = Dgf2kFactory.getInstance(EnvType.STANDARD, subfieldL);
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
            Gf2kVodeSenderOutput.create(field, x, t);
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
                Gf2kVodeSenderOutput.create(field, x, t);
            });
        }
        // create a sender output with large length x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = BytesUtils.randomNonZeroByteArrayVector(MAX_NUM, subfieldByteL + 1, subfieldL, secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVodeSenderOutput.create(field, x, t);
        });
        // create a sender output with small length t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kVodeSenderOutput.create(field, x, t);
        });
        // create a sender output with large length t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> subfield.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kVodeSenderOutput.create(field, x, t);
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
            Gf2kVodeReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(fieldByteL + 1, fieldL, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVodeReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with small length q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL - 1, secureRandom);
            Gf2kVodeReceiverOutput.create(field, delta, q);
        });
        // create a receiver output large length q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, fieldByteL + 1, fieldL, secureRandom);
            Gf2kVodeReceiverOutput.create(field, delta, q);
        });
        // merge two receiver output with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = field.createRangeRandom(secureRandom);
            byte[] delta1 = field.createRangeRandom(secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2kVodeReceiverOutput receiverOutput0 = Gf2kVodeReceiverOutput.create(field, delta0, q);
            Gf2kVodeReceiverOutput receiverOutput1 = Gf2kVodeReceiverOutput.create(field, delta1, q);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testIllegalUpdate() {
        byte[] delta = field.createRangeRandom(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(5);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0 = field.createRangeRandom(secureRandom);
            Gf2kVodeReceiverOutput receiverOutput0 = Gf2kVodeReceiverOutput.createRandom(field, 4, delta0, secureRandom);
            byte[] delta1 = field.createRangeRandom(secureRandom);
            Gf2kVodeReceiverOutput receiverOutput1 = Gf2kVodeReceiverOutput.createRandom(field, 4, delta1, secureRandom);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        byte[] delta = field.createRangeRandom(secureRandom);
        Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
        VodeTestUtils.assertOutput(field, num, senderOutput, receiverOutput);
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
        Gf2kVodeReceiverOutput receiverOutput1 = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutput1 = Gf2kVodeSenderOutput.createRandom(receiverOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        VodeTestUtils.assertOutput(field, 1, senderOutput1, receiverOutput1);
        // reduce all
        Gf2kVodeReceiverOutput receiverOutputAll = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutputAll = Gf2kVodeSenderOutput.createRandom(receiverOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        VodeTestUtils.assertOutput(field, num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce to num - 1
            Gf2kVodeReceiverOutput receiverOutputNum = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutputNum = Gf2kVodeSenderOutput.createRandom(receiverOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            VodeTestUtils.assertOutput(field, num - 1, senderOutputNum, receiverOutputNum);
            // reduce to half
            Gf2kVodeReceiverOutput receiverOutputHalf = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutputHalf = Gf2kVodeSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            VodeTestUtils.assertOutput(field, num / 2, senderOutputHalf, receiverOutputHalf);
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
        Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.createRandom(field, num1, delta, secureRandom);
        Gf2kVodeReceiverOutput mergeReceiverOutput = Gf2kVodeReceiverOutput.createRandom(field, num2, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.createRandom(receiverOutput, secureRandom);
        Gf2kVodeSenderOutput mergeSenderOutput = Gf2kVodeSenderOutput.createRandom(mergeReceiverOutput, secureRandom);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        VodeTestUtils.assertOutput(field, num1 + num2, senderOutput, receiverOutput);
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
        Gf2kVodeReceiverOutput receiverOutput1 = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutput1 = Gf2kVodeSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2kVodeSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Gf2kVodeReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        VodeTestUtils.assertOutput(field, num - 1, senderOutput1, receiverOutput1);
        VodeTestUtils.assertOutput(field, 1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Gf2kVodeReceiverOutput receiverOutputAll = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeSenderOutput senderOutputAll = Gf2kVodeSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2kVodeSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Gf2kVodeReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        VodeTestUtils.assertOutput(field, 0, senderOutputAll, receiverOutputAll);
        VodeTestUtils.assertOutput(field, num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Gf2kVodeReceiverOutput receiverOutputNum = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutputNum = Gf2kVodeSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2kVodeSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Gf2kVodeReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            VodeTestUtils.assertOutput(field, 1, senderOutputNum, receiverOutputNum);
            VodeTestUtils.assertOutput(field, num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Gf2kVodeReceiverOutput receiverOutputHalf = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeSenderOutput senderOutputHalf = Gf2kVodeSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2kVodeSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Gf2kVodeReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            VodeTestUtils.assertOutput(field, num - num / 2, senderOutputHalf, receiverOutputHalf);
            VodeTestUtils.assertOutput(field, num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
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
        Gf2kVodeReceiverOutput receiverOutput1 = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        Gf2kVodeReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        Gf2kVodeSenderOutput senderOutput1 = Gf2kVodeSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2kVodeSenderOutput copySenderOutput1 = senderOutput1.copy();
        Gf2kVodeSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        // split and merge all
        Gf2kVodeReceiverOutput receiverOutputAll = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2kVodeReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        Gf2kVodeReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        Gf2kVodeSenderOutput senderOutputAll = Gf2kVodeSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2kVodeSenderOutput copySenderOutputAll = senderOutputAll.copy();
        Gf2kVodeSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        if (num > 1) {
            // split and merge num - 1
            Gf2kVodeReceiverOutput receiverOutputNum = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            Gf2kVodeReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            Gf2kVodeSenderOutput senderOutputNum = Gf2kVodeSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2kVodeSenderOutput copySenderOutputNum = senderOutputNum.copy();
            Gf2kVodeSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            // split half
            Gf2kVodeReceiverOutput receiverOutputHalf = Gf2kVodeReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2kVodeReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            Gf2kVodeReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
            Gf2kVodeSenderOutput senderOutputHalf = Gf2kVodeSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2kVodeSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            Gf2kVodeSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
        }
    }
}
