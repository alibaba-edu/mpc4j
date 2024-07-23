package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE tests.
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/6/15
 */
public class Zp64VoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;
    /**
     * Zp64
     */
    private final Zp64 field;

    public Zp64VoleOutputTest() {
        field = Zp64Factory.createInstance(EnvType.STANDARD, 62);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with mismatched length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleSenderOutput.create(field, x, t);
        });
        // create a sender output with negative x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] x = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -field.createNonZeroRandom(secureRandom))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleSenderOutput.create(field, x, t);
        });
        // create a sender output with large x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> field.getPrime() + 1L)
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleSenderOutput.create(field, x, t);
        });
        // create a sender output with negative t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] x = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -field.createNonZeroRandom(secureRandom))
                .toArray();
            Zp64VoleSenderOutput.create(field, x, t);
        });
        // create a sender output with large t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] x = IntStream.range(0, MIN_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            long[] t = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.getPrime() + 1L)
                .toArray();
            Zp64VoleSenderOutput.create(field, x, t);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        // create a receiver output with a negative Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta = -1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with invalid Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta = field.getPrime() - 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta = field.getPrime() + 1L;
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.createRandom(secureRandom))
                .toArray();
            Zp64VoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with negative q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta = field.createRangeRandom(secureRandom);
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> -field.createNonZeroRandom(secureRandom))
                .toArray();
            Zp64VoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta = field.createRangeRandom(secureRandom);
            long[] q = IntStream.range(0, MAX_NUM)
                .mapToLong(index -> field.getPrime() + 1L)
                .toArray();
            Zp64VoleReceiverOutput.create(field, delta, q);
        });
    }

    @Test
    public void testIllegalUpdate() {
        long delta = field.createRangeRandom(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(5);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long delta0, delta1;
            do {
                delta0 = field.createRangeRandom(secureRandom);
                delta1 = field.createRangeRandom(secureRandom);
            } while (delta0 == delta1);
            Zp64VoleReceiverOutput receiverOutput0 = Zp64VoleReceiverOutput.createRandom(field, 4, delta0, secureRandom);
            Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.createRandom(field, 4, delta1, secureRandom);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        long delta = field.createRangeRandom(secureRandom);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
        VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        long delta = field.createRangeRandom(secureRandom);
        // reduce 1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        VoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        VoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            VoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            senderOutputHalf.reduce(num / 2);
            receiverOutputHalf.reduce(num / 2);
            VoleTestUtils.assertOutput(num / 2, senderOutputHalf, receiverOutputHalf);
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
        long delta = field.createRangeRandom(secureRandom);
        Zp64VoleReceiverOutput receiverOutput = Zp64VoleReceiverOutput.createRandom(field, num1, delta, secureRandom);
        Zp64VoleReceiverOutput mergeReceiverOutput = Zp64VoleReceiverOutput.createRandom(field, num2, delta, secureRandom);
        Zp64VoleSenderOutput senderOutput = Zp64VoleSenderOutput.createRandom(receiverOutput, secureRandom);
        Zp64VoleSenderOutput mergeSenderOutput = Zp64VoleSenderOutput.createRandom(mergeReceiverOutput, secureRandom);
        // merge
        senderOutput.merge(mergeSenderOutput);
        receiverOutput.merge(mergeReceiverOutput);
        // verify
        VoleTestUtils.assertOutput(num1 + num2, senderOutput, receiverOutput);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        long delta = field.createRangeRandom(secureRandom);
        // split 1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Zp64VoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        Zp64VoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        VoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        VoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Zp64VoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        Zp64VoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        VoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        VoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Zp64VoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            Zp64VoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            VoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            VoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Zp64VoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            Zp64VoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            VoleTestUtils.assertOutput(num - num / 2, senderOutputHalf, receiverOutputHalf);
            VoleTestUtils.assertOutput(num / 2, splitSenderOutputHalf, splitReceiverOutputHalf);
        }
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        long delta = field.createRangeRandom(secureRandom);
        // split and merge 1
        Zp64VoleReceiverOutput receiverOutput1 = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        Zp64VoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        Zp64VoleSenderOutput senderOutput1 = Zp64VoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        Zp64VoleSenderOutput copySenderOutput1 = senderOutput1.copy();
        Zp64VoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        // split and merge all
        Zp64VoleReceiverOutput receiverOutputAll = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        Zp64VoleReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        Zp64VoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        Zp64VoleSenderOutput senderOutputAll = Zp64VoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        Zp64VoleSenderOutput copySenderOutputAll = senderOutputAll.copy();
        Zp64VoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        if (num > 1) {
            // split and merge num - 1
            Zp64VoleReceiverOutput receiverOutputNum = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            Zp64VoleReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            Zp64VoleSenderOutput senderOutputNum = Zp64VoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            Zp64VoleSenderOutput copySenderOutputNum = senderOutputNum.copy();
            Zp64VoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            // split half
            Zp64VoleReceiverOutput receiverOutputHalf = Zp64VoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            Zp64VoleReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            Zp64VoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
            Zp64VoleSenderOutput senderOutputHalf = Zp64VoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            Zp64VoleSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            Zp64VoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
        }
    }
}
