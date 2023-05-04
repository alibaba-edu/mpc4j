package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * ZP-VOLE test utilities.
 *
 * @author Weiran Liu
 * @date 2022/6/14
 */
public class ZpVoleTestUtils {
    /**
     * private constructor.
     */
    private ZpVoleTestUtils() {
        // empty
    }

    /**
     * Generates a receiver output.
     *
     * @param zp           the Zp instance.
     * @param num          num.
     * @param delta        Δ.
     * @param secureRandom the random state.
     * @return a receiver output.
     */
    public static ZpVoleReceiverOutput genReceiverOutput(Zp zp, int num, BigInteger delta, SecureRandom secureRandom) {
        assert zp.validateRangeElement(delta);
        assert num > 0 : "num must be greater than 0: " + num;
        BigInteger[] q = IntStream.range(0, num)
            .mapToObj(index -> zp.createRandom(secureRandom))
            .toArray(BigInteger[]::new);
        return ZpVoleReceiverOutput.create(zp, delta, q);
    }

    /**
     * Generates a sender output.
     *
     * @param receiverOutput the receiver output.
     * @param secureRandom   the random state.
     * @return the sender output.
     */
    public static ZpVoleSenderOutput genSenderOutput(ZpVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        Zp zp = receiverOutput.getZp();
        BigInteger delta = receiverOutput.getDelta();
        BigInteger[] x = IntStream.range(0, num)
            .mapToObj(i -> zp.createRandom(secureRandom))
            .toArray(BigInteger[]::new);
        BigInteger[] t = IntStream.range(0, num)
            .mapToObj(i -> {
                BigInteger ti = zp.mul(x[i], delta);
                ti = zp.add(ti, receiverOutput.getQ(i));
                return ti;
            })
            .toArray(BigInteger[]::new);
        return ZpVoleSenderOutput.create(zp, x, t);
    }

    /**
     * Verifies the output pair.
     *
     * @param num            num.
     * @param senderOutput   the sender output.
     * @param receiverOutput the receiver output.
     */
    public static void assertOutput(int num, ZpVoleSenderOutput senderOutput, ZpVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getZp(), receiverOutput.getZp());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            Zp zp = senderOutput.getZp();
            IntStream.range(0, num).forEach(i -> {
                BigInteger actualT = zp.mul(senderOutput.getX(i), receiverOutput.getDelta());
                actualT = zp.add(actualT, receiverOutput.getQ(i));
                BigInteger expectT = senderOutput.getT(i);
                Assert.assertEquals(expectT, actualT);
            });
        }
    }
}
