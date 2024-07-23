package edu.alibaba.mpc4j.common.tool.utils;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.bouncycastle.crypto.modes.gcm.GCMUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * GF2X utility class tests.
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class Gf2xUtilsTest {
    /**
     * byte L for GF(2^128)
     */
    private static final int BYTE_L = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 0
     */
    private static final byte[] GF128_00000000 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
    };
    /**
     * 1
     */
    private static final byte[] GF128_00000001 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000001,
    };
    /**
     * x
     */
    private static final byte[] GF128_00000010 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000010,
    };
    /**
     * x^2 = x · x
     */
    private static final byte[] GF128_00000100 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000100,
    };
    /**
     * x + 1
     */
    private static final byte[] GF128_00000011 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000011,
    };
    /**
     * x^2 + 1 = (x + 1) · (x + 1)
     */
    private static final byte[] GF128_00000101 = new byte[]{
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000101,
    };

    @Test
    public void testByteArrayRings() {
        // verify 0
        UnivariatePolynomialZp64 gf128_00000000 = UnivariatePolynomialZp64.create(2, new long[]{0L});
        Assert.assertArrayEquals(GF128_00000000, Gf2xUtils.ringsToByteArray(gf128_00000000, BYTE_L));
        Assert.assertEquals(gf128_00000000, Gf2xUtils.byteArrayToRings(GF128_00000000));
        // 0 · 0 = 0
        Assert.assertArrayEquals(
            Gf2xUtils.ringsToByteArray(gf128_00000000.multiply(gf128_00000000), BYTE_L), GF128_00000000
        );
        // verify 1
        UnivariatePolynomialZp64 gf128_00000001 = UnivariatePolynomialZp64.create(2, new long[]{1L});
        Assert.assertArrayEquals(GF128_00000001, Gf2xUtils.ringsToByteArray(gf128_00000001, BYTE_L));
        Assert.assertEquals(gf128_00000001, Gf2xUtils.byteArrayToRings(GF128_00000001));
        // 1 · 1 = 1
        Assert.assertArrayEquals(
            Gf2xUtils.ringsToByteArray(gf128_00000001.multiply(gf128_00000001), BYTE_L), GF128_00000001
        );
        // verify x
        UnivariatePolynomialZp64 gf128_00000010 = UnivariatePolynomialZp64.create(2, new long[]{0L, 1L});
        Assert.assertArrayEquals(GF128_00000010, Gf2xUtils.ringsToByteArray(gf128_00000010, BYTE_L));
        Assert.assertEquals(gf128_00000010, Gf2xUtils.byteArrayToRings(GF128_00000010));
        // x · x = x^2
        Assert.assertArrayEquals(
            Gf2xUtils.ringsToByteArray(gf128_00000010.multiply(gf128_00000010), BYTE_L), GF128_00000100
        );
        // verify x + 1
        UnivariatePolynomialZp64 gf128_00000011 = UnivariatePolynomialZp64.create(2, new long[]{1L, 1L});
        Assert.assertArrayEquals(GF128_00000011, Gf2xUtils.ringsToByteArray(gf128_00000011, BYTE_L));
        Assert.assertEquals(gf128_00000011, Gf2xUtils.byteArrayToRings(GF128_00000011));
        // (x + 1) · (x + 1) = x^2 + 1
        Assert.assertArrayEquals(
            Gf2xUtils.ringsToByteArray(gf128_00000011.multiply(gf128_00000011), BYTE_L), GF128_00000101
        );
    }

    @Test
    public void testByteArrayAesNi() {
        // verify 0
        byte[] gf128_00000000 = new byte[]{
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000000, Gf2xUtils.aesNiToByteArray(gf128_00000000));
        Assert.assertArrayEquals(gf128_00000000, Gf2xUtils.byteArrayToAesNi(GF128_00000000));
        // 0 · 0 = 0
        byte[] gf128_00000000_mul = BytesUtils.clone(gf128_00000000);
        GCMUtil.multiply(gf128_00000000_mul, gf128_00000000);
        Assert.assertArrayEquals(Gf2xUtils.aesNiToByteArray(gf128_00000000_mul), GF128_00000000);
        // verify 1
        byte[] gf128_00000001 = new byte[]{
            (byte) 0b10000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000001, Gf2xUtils.aesNiToByteArray(gf128_00000001));
        Assert.assertArrayEquals(gf128_00000001, Gf2xUtils.byteArrayToAesNi(GF128_00000001));
        // 1 · 1 = 1
        byte[] gf128_00000001_mul = BytesUtils.clone(gf128_00000001);
        GCMUtil.multiply(gf128_00000001_mul, gf128_00000001);
        Assert.assertArrayEquals(Gf2xUtils.aesNiToByteArray(gf128_00000001_mul), GF128_00000001);
        // verify x
        byte[] gf128_00000010 = new byte[]{
            0b01000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000010, Gf2xUtils.aesNiToByteArray(gf128_00000010));
        Assert.assertArrayEquals(gf128_00000010, Gf2xUtils.byteArrayToAesNi(GF128_00000010));
        // x · x = x^2
        byte[] gf128_00000010_mul = BytesUtils.clone(gf128_00000010);
        GCMUtil.multiply(gf128_00000010_mul, gf128_00000010);
        Assert.assertArrayEquals(Gf2xUtils.aesNiToByteArray(gf128_00000010_mul), GF128_00000100);
        // verify x + 1
        byte[] gf128_00000011 = new byte[]{
            (byte) 0b11000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000011, Gf2xUtils.aesNiToByteArray(gf128_00000011));
        Assert.assertArrayEquals(gf128_00000011, Gf2xUtils.byteArrayToAesNi(GF128_00000011));
        // (x + 1) · (x + 1) = x^2 + 1
        byte[] gf128_00000011_mul = BytesUtils.clone(gf128_00000011);
        GCMUtil.multiply(gf128_00000011_mul, gf128_00000011);
        Assert.assertArrayEquals(Gf2xUtils.aesNiToByteArray(gf128_00000011_mul), GF128_00000101);
    }

    @Test
    public void testByteArrayNtl() {
        // we cannot directly invoke NTL, we just copy the correct result in NTL here for verification
        // verify 0
        byte[] gf128_00000000 = new byte[]{
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000000, Gf2xUtils.ntlToByteArray(gf128_00000000));
        Assert.assertArrayEquals(gf128_00000000, Gf2xUtils.byteArrayToNtl(GF128_00000000));
        // verify 1
        byte[] gf128_00000001 = new byte[]{
            0b00000001, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000001, Gf2xUtils.ntlToByteArray(gf128_00000001));
        Assert.assertArrayEquals(gf128_00000001, Gf2xUtils.byteArrayToNtl(GF128_00000001));
        // verify x
        byte[] gf128_00000010 = new byte[]{
            0b00000010, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000010, Gf2xUtils.ntlToByteArray(gf128_00000010));
        Assert.assertArrayEquals(gf128_00000010, Gf2xUtils.byteArrayToNtl(GF128_00000010));
        // verify x + 1
        byte[] gf128_00000011 = new byte[]{
            0b00000011, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
            0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        };
        Assert.assertArrayEquals(GF128_00000011, Gf2xUtils.ntlToByteArray(gf128_00000011));
        Assert.assertArrayEquals(gf128_00000011, Gf2xUtils.byteArrayToNtl(GF128_00000011));
    }
}
