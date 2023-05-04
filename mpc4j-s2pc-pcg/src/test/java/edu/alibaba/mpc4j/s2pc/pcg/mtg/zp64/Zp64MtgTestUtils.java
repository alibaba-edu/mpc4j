package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import org.junit.Assert;

import java.util.stream.IntStream;

/**
 * zp64三元组生成协议测试工具类。
 *
 * @author Liqiang Peng
 * @date 2022/9/7
 */
public class Zp64MtgTestUtils {
    /**
     * 私有构造函数
     */
    private Zp64MtgTestUtils() {
        // empty
    }

    public static void assertOutput(int num, Zp64Triple senderOutput, Zp64Triple receiverOutput) {
        Assert.assertTrue(num > 0);
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        Assert.assertEquals(senderOutput.getZp64(), receiverOutput.getZp64());
        Zp64 zp64 = senderOutput.getZp64();
        // 分别计算a、b、c
        IntStream.range(0, num).forEach(index -> {
            long a0 = senderOutput.getA(index);
            long b0 = senderOutput.getB(index);
            long c0 = senderOutput.getC(index);
            long a1 = receiverOutput.getA(index);
            long b1 = receiverOutput.getB(index);
            long c1 = receiverOutput.getC(index);
            long a = zp64.add(a0, a1);
            long b = zp64.add(b0, b1);
            long c = zp64.add(c0, c1);
            Assert.assertEquals(c, zp64.mul(a, b));
        });
    }
}
