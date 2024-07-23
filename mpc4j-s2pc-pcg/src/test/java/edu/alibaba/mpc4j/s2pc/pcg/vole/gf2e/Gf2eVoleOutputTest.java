package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * GF2E-VOLE output tests.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
@RunWith(Parameterized.class)
public class Gf2eVoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        int[] ls = new int[]{1, 63, 64, 65, 127, 128, 129};
        for (int l : ls) {
            configurations.add(new Object[]{"l = " + l, l});
        }

        return configurations;
    }

    /**
     * field
     */
    private final Gf2e field;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Gf2eVoleOutputTest(String name, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        field = Gf2eFactory.createInstance(EnvType.STANDARD, l);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        int byteL = field.getByteL();
        // create a sender output with mismatched length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with small length x
        if (byteL > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] x = BytesUtils.randomByteArrayVector(MAX_NUM, byteL - 1, secureRandom);
                byte[][] t = IntStream.range(0, MAX_NUM)
                    .mapToObj(index -> field.createRandom(secureRandom))
                    .toArray(byte[][]::new);
                Gf2eVoleSenderOutput.create(field, x, t);
            });
        }
        // create a sender output with large length x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = BytesUtils.randomByteArrayVector(MAX_NUM, byteL + 1, secureRandom);
            byte[][] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with small length t
        if (byteL > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] x = IntStream.range(0, MAX_NUM)
                    .mapToObj(index -> field.createRandom(secureRandom))
                    .toArray(byte[][]::new);
                byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, byteL - 1, secureRandom);
                Gf2eVoleSenderOutput.create(field, x, t);
            });
        }
        // create a sender output with large length t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t = BytesUtils.randomByteArrayVector(MAX_NUM, byteL + 1, secureRandom);
            Gf2eVoleSenderOutput.create(field, x, t);
        });
        // merge two sender outputs with different l
        Gf2e otherField = Gf2eFactory.createInstance(EnvType.STANDARD, field.getL() + 1);
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] x0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t0 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput senderOutput0 = Gf2eVoleSenderOutput.create(field, x0, t0);
            byte[][] x1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> otherField.createRandom(secureRandom))
                .toArray(byte[][]::new);
            byte[][] t1 = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> otherField.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleSenderOutput.create(otherField, x1, t1);
            senderOutput0.merge(senderOutput1);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        int byteL = field.getByteL();
        // create a receiver output with small length Δ
        if (byteL > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[] delta = BytesUtils.randomByteArray(byteL - 1, secureRandom);
                byte[][] q = IntStream.range(0, MAX_NUM)
                    .mapToObj(index -> field.createRandom(secureRandom))
                    .toArray(byte[][]::new);
                Gf2eVoleReceiverOutput.create(field, delta, q);
            });
        }
        // create a receiver output with large length Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = BytesUtils.randomByteArray(byteL + 1, secureRandom);
            byte[][] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(byte[][]::new);
            Gf2eVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with small length q
        if (byteL > 1) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[] delta = field.createRangeRandom(secureRandom);
                byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, byteL - 1, secureRandom);
                Gf2eVoleReceiverOutput.create(field, delta, q);
            });
        }
        // create a receiver output large length q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta = field.createRangeRandom(secureRandom);
            byte[][] q = BytesUtils.randomByteArrayVector(MAX_NUM, byteL + 1, secureRandom);
            Gf2eVoleReceiverOutput.create(field, delta, q);
        });
    }

    @Test
    public void testIllegalUpdate() {
        byte[] delta = field.createRangeRandom(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(5);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] delta0;
            byte[] delta1;
            do {
                delta0 = field.createRangeRandom(secureRandom);
                delta1 = field.createRangeRandom(secureRandom);
            } while (Arrays.equals(delta0, delta1));
            Gf2eVoleReceiverOutput receiverOutput0 = Gf2eVoleReceiverOutput.createRandom(field, 4, delta0, secureRandom);
            Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.createRandom(field, 4, delta1, secureRandom);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        byte[] delta = field.createRangeRandom(secureRandom);
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
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
        Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        VoleTestUtils.assertOutput(field, 1, senderOutput1, receiverOutput1);
        // reduce all
        Gf2eVoleReceiverOutput receiverOutputAll = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutputAll = Gf2eVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        VoleTestUtils.assertOutput(field, num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce to num - 1
            Gf2eVoleReceiverOutput receiverOutputNum = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutputNum = Gf2eVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            VoleTestUtils.assertOutput(field, num - 1, senderOutputNum, receiverOutputNum);
            // reduce to half
            Gf2eVoleReceiverOutput receiverOutputHalf = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutputHalf = Gf2eVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
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
        Gf2eVoleReceiverOutput receiverOutput = Gf2eVoleReceiverOutput.createRandom(field, num1, delta, secureRandom);
        Gf2eVoleReceiverOutput mergeReceiverOutput = Gf2eVoleReceiverOutput.createRandom(field, num2, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutput = Gf2eVoleSenderOutput.createRandom(receiverOutput, secureRandom);
        Gf2eVoleSenderOutput mergeSenderOutput = Gf2eVoleSenderOutput.createRandom(mergeReceiverOutput, secureRandom);
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
        Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2eVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Gf2eVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        VoleTestUtils.assertOutput(field, num - 1, senderOutput1, receiverOutput1);
        VoleTestUtils.assertOutput(field, 1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Gf2eVoleReceiverOutput receiverOutputAll = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleSenderOutput senderOutputAll = Gf2eVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2eVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Gf2eVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        VoleTestUtils.assertOutput(field, 0, senderOutputAll, receiverOutputAll);
        VoleTestUtils.assertOutput(field, num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Gf2eVoleReceiverOutput receiverOutputNum = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutputNum = Gf2eVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2eVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Gf2eVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            VoleTestUtils.assertOutput(field, 1, senderOutputNum, receiverOutputNum);
            VoleTestUtils.assertOutput(field, num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Gf2eVoleReceiverOutput receiverOutputHalf = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleSenderOutput senderOutputHalf = Gf2eVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2eVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Gf2eVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
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
        Gf2eVoleReceiverOutput receiverOutput1 = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        Gf2eVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        Gf2eVoleSenderOutput senderOutput1 = Gf2eVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Gf2eVoleSenderOutput copySenderOutput1 = senderOutput1.copy();
        Gf2eVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        // split and merge all
        Gf2eVoleReceiverOutput receiverOutputAll = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Gf2eVoleReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        Gf2eVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        Gf2eVoleSenderOutput senderOutputAll = Gf2eVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Gf2eVoleSenderOutput copySenderOutputAll = senderOutputAll.copy();
        Gf2eVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        if (num > 1) {
            // split and merge num - 1
            Gf2eVoleReceiverOutput receiverOutputNum = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            Gf2eVoleReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            Gf2eVoleSenderOutput senderOutputNum = Gf2eVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Gf2eVoleSenderOutput copySenderOutputNum = senderOutputNum.copy();
            Gf2eVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            // split half
            Gf2eVoleReceiverOutput receiverOutputHalf = Gf2eVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Gf2eVoleReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            Gf2eVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
            Gf2eVoleSenderOutput senderOutputHalf = Gf2eVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Gf2eVoleSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            Gf2eVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
        }
    }
}
