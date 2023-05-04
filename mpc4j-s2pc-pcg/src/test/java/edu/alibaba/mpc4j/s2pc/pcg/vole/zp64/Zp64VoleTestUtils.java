package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import org.junit.Assert;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE test utilities.
 *
 * @author Hanwen Feng
 * @date 2022/6/15
 */
public class Zp64VoleTestUtils {
    /**
     * private constructor.
     */
    private Zp64VoleTestUtils() {
        // empty
    }

    /**
     * Generates a receiver output.
     *
     * @param zp64 the Zp64 instance.
     * @param num          num.
     * @param delta        Δ.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static Zp64VoleReceiverOutput genReceiverOutput(Zp64 zp64, int num, long delta, SecureRandom secureRandom) {
        assert zp64.validateRangeElement(delta);
        assert num > 0 : "num must be greater than 0: " + num;
        long[] q = IntStream.range(0, num)
                .mapToLong(index -> zp64.createRandom(secureRandom))
                .toArray();
        return Zp64VoleReceiverOutput.create(zp64, delta, q);
    }

    /**
     * Generates a sender output.
     *
     * @param receiverOutput the receiver output.
     * @param secureRandom   the random state.
     * @return a sender output.
     */
    public static Zp64VoleSenderOutput genSenderOutput(Zp64VoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        Zp64 zp64 = receiverOutput.getZp64();
        long delta = receiverOutput.getDelta();
        long[] x = IntStream.range(0, num)
                .mapToLong(i -> zp64.createRandom(secureRandom))
                .toArray();
        long[] t = IntStream.range(0, num)
                .mapToLong(i -> {
                    long ti = zp64.mul(x[i], delta);
                    ti = zp64.add(ti, receiverOutput.getQ(i));
                    return ti;
                })
                .toArray();
        return Zp64VoleSenderOutput.create(zp64, x, t);
    }

    /**
     * Verifies the output pair.
     *
     * @param num            num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(int num, Zp64VoleSenderOutput senderOutput, Zp64VoleReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getZp64(), receiverOutput.getZp64());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            Zp64 zp64 = senderOutput.getZp64();
            IntStream.range(0, num).forEach(i -> {
                long actualT = zp64.mul(senderOutput.getX(i), receiverOutput.getDelta());
                actualT = zp64.add(actualT, receiverOutput.getQ(i));
                Assert.assertEquals(senderOutput.getT(i), actualT);
            });
        }
    }
}
