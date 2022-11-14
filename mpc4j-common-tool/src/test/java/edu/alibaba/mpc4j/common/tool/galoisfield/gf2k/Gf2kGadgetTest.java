package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * GF2K小工具测试。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public class Gf2kGadgetTest {
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 40;

    /**
     * GF2K
     */
    private final Gf2k gf2k;
    /**
     * GF2K小工具
     */
    private final Gf2kGadget gf2kGadget;

    public Gf2kGadgetTest() {
        gf2k = Gf2kFactory.createInstance(EnvType.STANDARD);
        gf2kGadget = new Gf2kGadget(gf2k);
    }

    @Test
    public void testBitComposition() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            // 随机生成GF2K内的元素
            byte[] element = gf2k.createRandom(SECURE_RANDOM);
            boolean[] decomposition = gf2kGadget.bitDecomposition(element);
            byte[] compositeElement = gf2kGadget.bitComposition(decomposition);
            Assert.assertArrayEquals(element, compositeElement);
        }
    }

    @Test
    public void testInnerProduct() {
        int l = gf2k.getL();
        int byteL = gf2k.getByteL();
        // 全为0的内积为0
        byte[] zero = gf2k.createZero();
        byte[][] zeroElementVector = new byte[l][byteL];
        byte[] zeroInnerProduct = gf2kGadget.innerProduct(zeroElementVector);
        Assert.assertArrayEquals(zero, zeroInnerProduct);
        // 全为1的内积为-1
        byte[] negOne = gf2k.createZero();
        BytesUtils.noti(negOne, l);
        byte[][] oneElementVector = IntStream.range(0, l)
            .mapToObj(index -> gf2k.createOne())
            .toArray(byte[][]::new);
        byte[] oneInnerProduct = gf2kGadget.innerProduct(oneElementVector);
        Assert.assertArrayEquals(negOne, oneInnerProduct);
    }
}
