package edu.alibaba.mpc4j.common.tool.utils;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * 环库（Rings Library）工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class RingsUtilsTest {
    /**
     * 测试有限域GF(2^40)
     */
    private static final int L = CommonConstants.STATS_BIT_LENGTH;
    /**
     * 测试有限域字节长度
     */
    private static final int L_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * GF(2^l)中的0
     */
    private static final UnivariatePolynomialZp64 GF_0 = UnivariatePolynomialZp64.create(2, new long[] {0L,});
    /**
     * GF(2^l)中的1
     */
    private static final UnivariatePolynomialZp64 GF_1 = UnivariatePolynomialZp64.create(2, new long[] {1L,});
    /**
     * GF(2^l)中的2
     */
    private static final UnivariatePolynomialZp64 GF_2 = UnivariatePolynomialZp64.create(2,
        new long[] {0L, 1L,}
    );
    /**
     * GF(2^l)中的33
     */
    private static final UnivariatePolynomialZp64 GF_3 = UnivariatePolynomialZp64.create(2,
        new long[] {1L, 1L,}
    );
    /**
     * GF(2^l)中的2^l - 1
     */
    private static final UnivariatePolynomialZp64 GF_MAX;

    static {
        long[] maxLongArray = new long[L];
        Arrays.fill(maxLongArray, 1L);
        GF_MAX = UnivariatePolynomialZp64.create(2, maxLongArray);
    }

    @Test
    public void testGf2xByteArray() {
        // 验证0的转换结果
        Assert.assertArrayEquals(
            new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00},
            RingsUtils.gf2eToByteArray(GF_0, L_BYTE_LENGTH)
        );
        // 验证1的转换结果
        Assert.assertArrayEquals(
            new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01},
            RingsUtils.gf2eToByteArray(GF_1, L_BYTE_LENGTH)
        );
        // 验证2的转换结果
        Assert.assertArrayEquals(
            new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02},
            RingsUtils.gf2eToByteArray(GF_2, L_BYTE_LENGTH)
        );
        // 验证3的转换结果
        Assert.assertArrayEquals(
            new byte[] {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03},
            RingsUtils.gf2eToByteArray(GF_3, L_BYTE_LENGTH)
        );
        // 验证2^l - 1的转换结果
        Assert.assertArrayEquals(
            new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF},
            RingsUtils.gf2eToByteArray(GF_MAX, L_BYTE_LENGTH)
        );
    }

    @Test
    public void testByteArrayGf2x() {
        // 验证0的转换结果
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x00,}), GF_0);
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x00, (byte)0x00,}), GF_0);
        // 验证1的转换结果
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x01,}), GF_1);
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x00, (byte)0x01,}), GF_1);
        // 验证2的转换结果
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x02,}), GF_2);
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x00, (byte)0x02,}), GF_2);
        // 验证3的转换结果
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x03,}), GF_3);
        Assert.assertEquals(RingsUtils.byteArrayToGf2e(new byte[] {(byte)0x00, (byte)0x03,}), GF_3);
        // 验证2^l - 1的转换结果
        Assert.assertEquals(
            RingsUtils.byteArrayToGf2e(new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF}), GF_MAX
        );
    }
}
