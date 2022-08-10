package edu.alibaba.mpc4j.common.tool.coder;

import edu.alibaba.mpc4j.common.tool.coder.linear.Bch076By511Coder;
import org.junit.Assert;
import org.junit.Test;

/**
 * 数据字（dataword）为076比特，码字（codeword）为511比特的BCH编码器测试。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class Bch076By511CoderTest {

    @Test
    public void testEncode() {
        // 样例数据字，76比特（10字节）
        byte[] dataword = new byte[] {
            (byte)0x0F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFD, (byte)0xFF,
            (byte)0xDF, (byte)0xFF,
        };
        // 样例码字，511比特（64字节）长
        byte[] correctCodeword = new byte[] {
            (byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xEF, (byte)0xFE,
            (byte)0xFF, (byte)0xFD, (byte)0x08, (byte)0xE8, (byte)0xE2, (byte)0xCF, (byte)0xE4, (byte)0xD1,
            (byte)0x43, (byte)0xF3, (byte)0x6E, (byte)0xB0, (byte)0x44, (byte)0xA8, (byte)0xCC, (byte)0xFB,
            (byte)0xE4, (byte)0xE2, (byte)0xE1, (byte)0x40, (byte)0x62, (byte)0x3C, (byte)0xE1, (byte)0xB3,
            (byte)0xF2, (byte)0x5A, (byte)0x89, (byte)0x62, (byte)0x98, (byte)0x7D, (byte)0x54, (byte)0x93,
            (byte)0xA4, (byte)0xC3, (byte)0x14, (byte)0xA6, (byte)0x70, (byte)0xB3, (byte)0xCF, (byte)0x1C,
            (byte)0x5F, (byte)0xA8, (byte)0xD1, (byte)0x56, (byte)0x8C, (byte)0x06, (byte)0x9E, (byte)0xAC,
            (byte)0xC7, (byte)0xFD, (byte)0x37, (byte)0x9F, (byte)0xE8, (byte)0x41, (byte)0xAF, (byte)0x1C,
        };
        Bch076By511Coder bchCoder = Bch076By511Coder.getInstance();
        byte[] codeword = bchCoder.encode(dataword);
        Assert.assertArrayEquals(correctCodeword, codeword);
    }
}
