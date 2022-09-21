package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * ZP-VOLE测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/6/14
 */
public class ZpVoleTestUtils {
    /**
     * 私有构造函数
     */
    private ZpVoleTestUtils() {
        // empty
    }

    /**
     * 生成接收方输出。
     *
     * @param prime        素数域。
     * @param num          数量。
     * @param delta        关联值Δ。
     * @param secureRandom 随机状态。
     * @return 接收方输出。
     */
    public static ZpVoleReceiverOutput genReceiverOutput(BigInteger prime, int num, BigInteger delta,
                                                         SecureRandom secureRandom) {
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert BigIntegerUtils.greaterOrEqual(delta, BigInteger.ZERO) && delta.bitLength() <= prime.bitLength() - 1
            : "Δ must be in range [0, " + BigInteger.ONE.shiftLeft(prime.bitLength() - 1) + "): " + delta;
        assert num > 0 : "num must be greater than 0";
        BigInteger[] q = IntStream.range(0, num)
            .mapToObj(index -> BigIntegerUtils.randomPositive(prime, secureRandom))
            .toArray(BigInteger[]::new);
        return ZpVoleReceiverOutput.create(prime, delta, q);
    }

    /**
     * 生成发送方输出。
     *
     * @param receiverOutput 接收方输出。
     * @param secureRandom   随机状态。
     * @return 发送方输出。
     */
    public static ZpVoleSenderOutput genSenderOutput(ZpVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        BigInteger prime = receiverOutput.getPrime();
        BigInteger delta = receiverOutput.getDelta();
        BigInteger[] x = IntStream.range(0, num)
            .mapToObj(i -> BigIntegerUtils.randomPositive(prime, secureRandom))
            .toArray(BigInteger[]::new);
        BigInteger[] t = IntStream.range(0, num)
            .mapToObj(i -> x[i].multiply(delta).mod(prime).add(receiverOutput.getQ(i)).mod(prime))
            .toArray(BigInteger[]::new);
        return ZpVoleSenderOutput.create(prime, x, t);
    }

    /**
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
     */
    public static void assertOutput(int num, ZpVoleSenderOutput senderOutput, ZpVoleReceiverOutput receiverOutput) {
        Assert.assertEquals(senderOutput.getPrime(), receiverOutput.getPrime());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            BigInteger prime = senderOutput.getPrime();
            IntStream.range(0, num).forEach(i -> {
                BigInteger actualT = senderOutput.getX(i).multiply(receiverOutput.getDelta()).mod(prime)
                    .add(receiverOutput.getQ(i)).mod(prime);
                BigInteger expectT = senderOutput.getT(i);
                Assert.assertEquals(expectT, actualT);
            });
        }
    }
}
