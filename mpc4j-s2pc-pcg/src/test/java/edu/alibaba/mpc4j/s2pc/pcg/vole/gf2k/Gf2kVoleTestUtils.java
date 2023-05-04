package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import org.junit.Assert;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * GK2K-VOLE test utilities.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2kVoleTestUtils {
    /**
     * the GF2K instance
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);
    /**
     * private constructor.
     */
    private Gf2kVoleTestUtils() {
        // empty
    }

    /**
     * Generates a receiver output.
     *
     * @param num          num.
     * @param delta        Δ.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static Gf2kVoleReceiverOutput genReceiverOutput(int num, byte[] delta, SecureRandom secureRandom) {
        assert GF2K.validateElement(delta);
        assert num > 0 : "num must be greater than 0: " + num;
        byte[][] q = IntStream.range(0, num)
            .mapToObj(index -> GF2K.createRandom(secureRandom))
            .toArray(byte[][]::new);
        return Gf2kVoleReceiverOutput.create(delta, q);
    }

    /**
     * Generates a sender output.
     *
     * @param receiverOutput the receiver output.
     * @param secureRandom   the random state.
     * @return a sender output.
     */
    public static Gf2kVoleSenderOutput genSenderOutput(Gf2kVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        byte[][] xs = IntStream.range(0, num)
            .mapToObj(index -> GF2K.createRandom(secureRandom))
            .toArray(byte[][]::new);
        byte[] delta = receiverOutput.getDelta();
        byte[][] ts = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] ti = GF2K.mul(xs[index], delta);
                GF2K.addi(ti, receiverOutput.getQ(index));
                return ti;
            })
            .toArray(byte[][]::new);
        return Gf2kVoleSenderOutput.create(xs, ts);
    }

    /**
     * Verifies the output pair.
     *
     * @param num            num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(int num, Gf2kVoleSenderOutput senderOutput, Gf2kVoleReceiverOutput receiverOutput) {
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
                byte[] actualT = GF2K.mul(senderOutput.getX(index), receiverOutput.getDelta());
                GF2K.addi(actualT, receiverOutput.getQ(index));
                Assert.assertArrayEquals(senderOutput.getT(index), actualT);
            });
        }
    }
}
