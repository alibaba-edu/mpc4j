package edu.alibaba.mpc4j.s2pc.pcg.vole.z2;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;

import java.security.SecureRandom;

/**
 * Z2-VOLE测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/6/11
 */
public class Z2VoleTestUtils {
    /**
     * 私有构造函数
     */
    private Z2VoleTestUtils() {
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
    public static Z2VoleReceiverOutput genReceiverOutput(int num, boolean delta, SecureRandom secureRandom) {
        assert num >= 0 : "num must be greater or equal than 0";
        if (num == 0) {
            return Z2VoleReceiverOutput.createEmpty(delta);
        }
        int byteNum = CommonUtils.getByteLength(num);
        byte[] q = new byte[byteNum];
        secureRandom.nextBytes(q);
        BytesUtils.reduceByteArray(q, num);
        return Z2VoleReceiverOutput.create(num, delta, q);
    }

    /**
     * 生成发送方输出。
     *
     * @param receiverOutput 接收方输出。
     * @param secureRandom   随机状态。
     * @return 发送方输出。
     */
    public static Z2VoleSenderOutput genSenderOutput(Z2VoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        if (num == 0) {
            return Z2VoleSenderOutput.createEmpty();
        }
        int byteNum = CommonUtils.getByteLength(num);
        byte[] x = new byte[byteNum];
        secureRandom.nextBytes(x);
        BytesUtils.reduceByteArray(x, num);
        byte[] deltaBytes = receiverOutput.getDeltaBytes();
        byte[] t = BytesUtils.and(x, deltaBytes);
        BytesUtils.xori(t, receiverOutput.getQ());
        return Z2VoleSenderOutput.create(num, x, t);
    }

    /**
     * 验证输出结果。
     *
     * @param num            数量。
     * @param senderOutput   发送方输出。
     * @param receiverOutput 接收方输出。
     */
    public static void assertOutput(int num, Z2VoleSenderOutput senderOutput, Z2VoleReceiverOutput receiverOutput) {
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertEquals(0, senderOutput.getX().length);
            Assert.assertEquals(0, senderOutput.getT().length);
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertEquals(0, receiverOutput.getQ().length);
        } else {
            Assert.assertEquals(num, senderOutput.getNum());
            Assert.assertEquals(num, receiverOutput.getNum());
            byte[] qt = BytesUtils.xor(senderOutput.getT(), receiverOutput.getQ());
            byte[] xDelta = BytesUtils.and(senderOutput.getX(), receiverOutput.getDeltaBytes());
            Assert.assertArrayEquals(qt, xDelta);
        }
    }
}
