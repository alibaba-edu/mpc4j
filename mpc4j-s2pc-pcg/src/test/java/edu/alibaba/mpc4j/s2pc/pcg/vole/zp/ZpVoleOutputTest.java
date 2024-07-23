package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.s2pc.pcg.vole.VoleTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * ZP-VOLE output tests.
 *
 * @author Weiran Liu
 * @date 2022/6/14
 */
public class ZpVoleOutputTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;
    /**
     * Zp
     */
    private final Zp field;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public ZpVoleOutputTest() {
        field = ZpFactory.createInstance(EnvType.STANDARD, ZpManager.getPrime(64));
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalSenderInputs() {
        // create a sender output with mismatched length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] x = IntStream.range(0, MIN_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with negative x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createNonZeroRandom(secureRandom).negate())
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with large x
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with negative t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createNonZeroRandom(secureRandom).negate())
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(field, x, t);
        });
        // create a sender output with large t
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] x = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            BigInteger[] t = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleSenderOutput.create(field, x, t);
        });
    }

    @Test
    public void testIllegalReceiverInputs() {
        // create a receiver output with a negative Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta = BigInteger.ONE.negate();
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with invalid Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta = field.getPrime().subtract(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta = field.getPrime().add(BigInteger.ONE);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createRandom(secureRandom))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with negative q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta = field.createRangeRandom(secureRandom);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.createNonZeroRandom(secureRandom).negate())
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(field, delta, q);
        });
        // create a receiver output with large q
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta = field.createRangeRandom(secureRandom);
            BigInteger[] q = IntStream.range(0, MAX_NUM)
                .mapToObj(index -> field.getPrime().add(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZpVoleReceiverOutput.create(field, delta, q);
        });
    }

    @Test
    public void testIllegalUpdate() {
        BigInteger delta = field.createRangeRandom(secureRandom);
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.split(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(0);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            receiverOutput.reduce(5);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, 4, delta, secureRandom);
            ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
            senderOutput.reduce(5);
        });
        // merge two receiver outputs with different Δ
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger delta0 = field.createRangeRandom(secureRandom);
            BigInteger delta1 = field.createRangeRandom(secureRandom);
            ZpVoleReceiverOutput receiverOutput0 = ZpVoleReceiverOutput.createRandom(field, 4, delta0, secureRandom);
            ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.createRandom(field, 4, delta1, secureRandom);
            receiverOutput0.merge(receiverOutput1);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        BigInteger delta = field.createRangeRandom(secureRandom);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
        VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        BigInteger delta = field.createRangeRandom(secureRandom);
        // reduce 1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleSenderOutput senderOutput1 = ZpVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        senderOutput1.reduce(1);
        receiverOutput1.reduce(1);
        VoleTestUtils.assertOutput(1, senderOutput1, receiverOutput1);
        // reduce all
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleSenderOutput senderOutputAll = ZpVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        senderOutputAll.reduce(num);
        receiverOutputAll.reduce(num);
        VoleTestUtils.assertOutput(num, senderOutputAll, receiverOutputAll);
        if (num > 1) {
            // reduce num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleSenderOutput senderOutputNum = ZpVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            senderOutputNum.reduce(num - 1);
            receiverOutputNum.reduce(num - 1);
            VoleTestUtils.assertOutput(num - 1, senderOutputNum, receiverOutputNum);
            // reduce half
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
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
        BigInteger delta = field.createRangeRandom(secureRandom);
        ZpVoleReceiverOutput receiverOutput = ZpVoleReceiverOutput.createRandom(field, num1, delta, secureRandom);
        ZpVoleReceiverOutput mergeReceiverOutput = ZpVoleReceiverOutput.createRandom(field, num2, delta, secureRandom);
        ZpVoleSenderOutput senderOutput = ZpVoleSenderOutput.createRandom(receiverOutput, secureRandom);
        ZpVoleSenderOutput mergeSenderOutput = ZpVoleSenderOutput.createRandom(mergeReceiverOutput, secureRandom);
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
        BigInteger delta = field.createRangeRandom(secureRandom);
        // split 1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleSenderOutput senderOutput1 = ZpVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        ZpVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        ZpVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        VoleTestUtils.assertOutput(num - 1, senderOutput1, receiverOutput1);
        VoleTestUtils.assertOutput(1, splitSenderOutput1, splitReceiverOutput1);
        // split all
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleSenderOutput senderOutputAll = ZpVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        ZpVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        ZpVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        VoleTestUtils.assertOutput(0, senderOutputAll, receiverOutputAll);
        VoleTestUtils.assertOutput(num, splitSenderOutputAll, splitReceiverOutputAll);
        if (num > 1) {
            // split num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleSenderOutput senderOutputNum = ZpVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            ZpVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            ZpVoleReceiverOutput splitReceiverOutputN = receiverOutputNum.split(num - 1);
            VoleTestUtils.assertOutput(1, senderOutputNum, receiverOutputNum);
            VoleTestUtils.assertOutput(num - 1, splitSenderOutputNum, splitReceiverOutputN);
            // split half
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            ZpVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            ZpVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
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
        BigInteger delta = field.createRangeRandom(secureRandom);
        // split and merge 1
        ZpVoleReceiverOutput receiverOutput1 = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleReceiverOutput copyReceiverOutput1 = receiverOutput1.copy();
        ZpVoleReceiverOutput splitReceiverOutput1 = receiverOutput1.split(1);
        receiverOutput1.merge(splitReceiverOutput1);
        Assert.assertEquals(copyReceiverOutput1, receiverOutput1);
        ZpVoleSenderOutput senderOutput1 = ZpVoleSenderOutput.createRandom(receiverOutput1, secureRandom);
        ZpVoleSenderOutput copySenderOutput1 = senderOutput1.copy();
        ZpVoleSenderOutput splitSenderOutput1 = senderOutput1.split(1);
        senderOutput1.merge(splitSenderOutput1);
        Assert.assertEquals(copySenderOutput1, senderOutput1);
        // split and merge all
        ZpVoleReceiverOutput receiverOutputAll = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
        ZpVoleReceiverOutput copyReceiverOutputAll = receiverOutputAll.copy();
        ZpVoleReceiverOutput splitReceiverOutputAll = receiverOutputAll.split(num);
        receiverOutputAll.merge(splitReceiverOutputAll);
        Assert.assertEquals(copyReceiverOutputAll, receiverOutputAll);
        ZpVoleSenderOutput senderOutputAll = ZpVoleSenderOutput.createRandom(receiverOutputAll, secureRandom);
        ZpVoleSenderOutput copySenderOutputAll = senderOutputAll.copy();
        ZpVoleSenderOutput splitSenderOutputAll = senderOutputAll.split(num);
        senderOutputAll.merge(splitSenderOutputAll);
        Assert.assertEquals(copySenderOutputAll, senderOutputAll);
        if (num > 1) {
            // split and merge num - 1
            ZpVoleReceiverOutput receiverOutputNum = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleReceiverOutput copyReceiverOutputNum = receiverOutputNum.copy();
            ZpVoleReceiverOutput splitReceiverOutputNum = receiverOutputNum.split(num - 1);
            receiverOutputNum.merge(splitReceiverOutputNum);
            Assert.assertEquals(copyReceiverOutputNum, receiverOutputNum);
            ZpVoleSenderOutput senderOutputNum = ZpVoleSenderOutput.createRandom(receiverOutputNum, secureRandom);
            ZpVoleSenderOutput copySenderOutputNum = senderOutputNum.copy();
            ZpVoleSenderOutput splitSenderOutputNum = senderOutputNum.split(num - 1);
            senderOutputNum.merge(splitSenderOutputNum);
            Assert.assertEquals(copySenderOutputNum, senderOutputNum);
            // split half
            ZpVoleReceiverOutput receiverOutputHalf = ZpVoleReceiverOutput.createRandom(field, num, delta, secureRandom);
            ZpVoleReceiverOutput copyReceiverOutputHalf = receiverOutputHalf.copy();
            ZpVoleReceiverOutput splitReceiverOutputHalf = receiverOutputHalf.split(num / 2);
            receiverOutputHalf.merge(splitReceiverOutputHalf);
            Assert.assertEquals(copyReceiverOutputHalf, receiverOutputHalf);
            ZpVoleSenderOutput senderOutputHalf = ZpVoleSenderOutput.createRandom(receiverOutputHalf, secureRandom);
            ZpVoleSenderOutput copySenderOutputHalf = senderOutputHalf.copy();
            ZpVoleSenderOutput splitSenderOutputHalf = senderOutputHalf.split(num / 2);
            senderOutputHalf.merge(splitSenderOutputHalf);
            Assert.assertEquals(copySenderOutputHalf, senderOutputHalf);
        }
    }
}
