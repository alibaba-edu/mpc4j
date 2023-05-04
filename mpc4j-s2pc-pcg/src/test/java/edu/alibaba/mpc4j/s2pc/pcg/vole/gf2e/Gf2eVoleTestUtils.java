package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import org.junit.Assert;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * GK2E-VOLE test utilities.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2eVoleTestUtils {
    /**
     * private constructor.
     */
    private Gf2eVoleTestUtils() {
        // empty
    }

    /**
     * Generates a receiver output.
     *
     * @param gf2e         the GF2E instance.
     * @param num          num.
     * @param delta        Δ.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static Gf2eVoleReceiverOutput genReceiverOutput(Gf2e gf2e, int num, byte[] delta, SecureRandom secureRandom) {
        assert gf2e.validateElement(delta);
        assert num > 0 : "num must be greater than 0: " + num;
        byte[][] q = IntStream.range(0, num)
            .mapToObj(index -> gf2e.createRandom(secureRandom))
            .toArray(byte[][]::new);
        return Gf2eVoleReceiverOutput.create(gf2e, delta, q);
    }

    /**
     * Generates a sender output.
     *
     * @param gf2e           the GF2E instance.
     * @param receiverOutput the receiver output.
     * @param secureRandom   the random state.
     * @return a sender output.
     */
    public static Gf2eVoleSenderOutput genSenderOutput(Gf2e gf2e, Gf2eVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        byte[][] x = IntStream.range(0, num)
            .mapToObj(index -> gf2e.createRandom(secureRandom))
            .toArray(byte[][]::new);
        byte[] delta = receiverOutput.getDelta();
        byte[][] t = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] ti = gf2e.mul(x[index], delta);
                gf2e.addi(ti, receiverOutput.getQ(index));
                return ti;
            })
            .toArray(byte[][]::new);
        return Gf2eVoleSenderOutput.create(gf2e, x, t);
    }

    /**
     * Verifies the output pair.
     *
     * @param gf2e           the GF2E instance.
     * @param num            num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(Gf2e gf2e, int num,
                                    Gf2eVoleSenderOutput senderOutput, Gf2eVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(gf2e, senderOutput.getGf2e());
        Assert.assertEquals(gf2e, receiverOutput.getGf2e());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            IntStream.range(0, num).forEach(index -> {
                byte[] actualT = gf2e.mul(senderOutput.getX(index), receiverOutput.getDelta());
                gf2e.addi(actualT, receiverOutput.getQ(index));
                Assert.assertArrayEquals(senderOutput.getT(index), actualT);
            });
        }
    }
}
