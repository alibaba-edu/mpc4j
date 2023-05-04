package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import org.junit.Assert;

import java.math.BigInteger;

/**
 * l比特三元组生成协议测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlMtgTestUtils {
    /**
     * 私有构造函数
     */
    private ZlMtgTestUtils() {
        // empty
    }

    public static void assertOutput(Zl zl, int num, ZlTriple senderOutput, ZlTriple receiverOutput) {
        Assert.assertEquals(num, senderOutput.getNum());
        Assert.assertEquals(num, receiverOutput.getNum());
        if (num == 0) {
            Assert.assertEquals(0, senderOutput.getNum());
            Assert.assertArrayEquals(new BigInteger[0], senderOutput.getA());
            Assert.assertArrayEquals(new BigInteger[0], senderOutput.getB());
            Assert.assertArrayEquals(new BigInteger[0], senderOutput.getC());
            Assert.assertEquals(0, receiverOutput.getNum());
            Assert.assertArrayEquals(new BigInteger[0], receiverOutput.getA());
            Assert.assertArrayEquals(new BigInteger[0], receiverOutput.getB());
            Assert.assertArrayEquals(new BigInteger[0], receiverOutput.getC());
        } else {
            for (int index = 0; index < num; index++) {
                BigInteger a0 = senderOutput.getA(index);
                BigInteger b0 = senderOutput.getB(index);
                BigInteger c0 = senderOutput.getC(index);
                BigInteger a1 = receiverOutput.getA(index);
                BigInteger b1 = receiverOutput.getB(index);
                BigInteger c1 = receiverOutput.getC(index);
                // 分别计算a、b、c
                BigInteger a = zl.add(a0, a1);
                BigInteger b = zl.add(b0, b1);
                BigInteger c = zl.add(c0, c1);
                Assert.assertEquals(c, zl.mul(a, b));
            }
        }
    }
}
