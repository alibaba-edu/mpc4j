package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE测试工具类。
 *
 * @author Hanwen Feng
 * @date 2022/6/15
 */
public class Zp64VoleTestUtils {
    /**
     * 私有构造函数
     */
    private Zp64VoleTestUtils() {
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
    public static Zp64VoleReceiverOutput genReceiverOutput(long prime, int num, long delta,
                                                           SecureRandom secureRandom) {
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert delta >= 0 && LongUtils.ceilLog2(delta) <= LongUtils.ceilLog2(prime) - 1
                : "Δ must be in range [0, " + (1L << (LongUtils.ceilLog2(prime) - 1)) + "): " + delta;
        assert num >= 0 : "num must be greater or equal than 0";
        if (num == 0) {
            return Zp64VoleReceiverOutput.createEmpty(prime, delta);
        }
        long[] q = IntStream.range(0, num)
                .mapToLong(index -> LongUtils.randomNonNegative(prime, secureRandom))
                .toArray();
        return Zp64VoleReceiverOutput.create(prime, delta, q);
    }

    /**
     * 生成发送方输出。
     *
     * @param receiverOutput 接收方输出。
     * @param secureRandom   随机状态。
     * @return 发送方输出。
     */
    public static Zp64VoleSenderOutput genSenderOutput(Zp64VoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        long prime = receiverOutput.getPrime();
        if (num == 0) {
            return Zp64VoleSenderOutput.createEmpty(prime);
        }
        long delta = receiverOutput.getDelta();
        IntegersZp64 zp64 = new IntegersZp64(prime);
        long[] x = IntStream.range(0, num)
                .mapToLong(i -> LongUtils.randomNonNegative(prime, secureRandom))
                .toArray();
        long[] t = IntStream.range(0, num)
                .mapToLong(i -> zp64.add(zp64.multiply(x[i], delta), receiverOutput.getQ(i)))
                .toArray();
        return Zp64VoleSenderOutput.create(prime, x, t);
    }

    /**
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
     */
    public static void assertOutput(int num, Zp64VoleSenderOutput senderOutput, Zp64VoleReceiverOutput receiverOutput) {
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
            long prime = senderOutput.getPrime();
            IntegersZp64 zp64 = new IntegersZp64(prime);
            IntStream.range(0, num).forEach(i -> {
                long actualT = zp64.add(
                    zp64.multiply(senderOutput.getX(i), receiverOutput.getDelta()), receiverOutput.getQ(i)
                );
                Assert.assertEquals(senderOutput.getT(i), actualT);
            });
        }
    }
}
