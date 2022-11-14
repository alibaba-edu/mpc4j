package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import org.junit.Assert;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * GK2K-VOLE测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2kVoleTestUtils {
    /**
     * GF(2^128)计算工具
     */
    private static final Gf2k GF2K = Gf2kFactory.createInstance(EnvType.STANDARD);

    /**
     * 私有构造函数
     */
    private Gf2kVoleTestUtils() {
        // empty
    }

    /**
     * 生成接收方输出。
     *
     * @param num          数量。
     * @param delta        关联值Δ。
     * @param secureRandom 随机状态。
     * @return 接收方输出。
     */
    public static Gf2kVoleReceiverOutput genReceiverOutput(int num, byte[] delta, SecureRandom secureRandom) {
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert num > 0 : "num must be greater than 0";
        byte[][] q = IntStream.range(0, num)
            .mapToObj(index -> GF2K.createRandom(secureRandom))
            .toArray(byte[][]::new);
        return Gf2kVoleReceiverOutput.create(delta, q);
    }

    /**
     * 生成发送方输出。
     *
     * @param receiverOutput 接收方输出。
     * @param secureRandom   随机状态。
     * @return 发送方输出。
     */
    public static Gf2kVoleSenderOutput genSenderOutput(Gf2kVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        assert num > 0 : "num must be greater than 0";
        byte[][] x = IntStream.range(0, num)
            .mapToObj(index -> GF2K.createRandom(secureRandom))
            .toArray(byte[][]::new);
        byte[] delta = receiverOutput.getDelta();
        byte[][] t = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] ti = GF2K.mul(x[index], delta);
                GF2K.addi(ti, receiverOutput.getQ(index));
                return ti;
            })
            .toArray(byte[][]::new);
        return Gf2kVoleSenderOutput.create(x, t);
    }

    /**
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
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
