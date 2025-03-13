package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * FIPS-202 test, making sure that the Java version outputs the exact same randomness with C/C++ version.
 *
 * @author Weiran Liu
 * @date 2025/2/10
 */
public class Shake256Test {
    /**
     * buffer size
     */
    private static final int BUFFER_SIZE = 256;

    @Test
    public void testZeroKeyZeroCounter() {
        long[] seed = new long[] {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        long counter = 0;
        byte[] out = new byte[BUFFER_SIZE];
        Shake256.shake256(out, seed, counter);
        String expectOutput = "64FF78306D2EC7B31BEDDB9B444F1D3F712A9F4F3F69E102B8F4190093836964" +
            "3C57AE2E9D4460D641E5DE96BF457242C3F814145399AB706C5EF4E5A636FF62" +
            "E82004DA7C9C0207E66F5E90B72137B9AFD9769FAD72CB06512FBB52C75D4D26" +
            "EC004244183FB39F70792DE08B9F22FEDD39E2B82D0EE8FF756326B11568688E" +
            "6C6A46EAB3F96BBA9011274CCE282F83B9F4EDA1BDA9E5C2D5ED2DB306243BF6" +
            "B633E2371B4BD87F49ED18231328199AE110B2BF13A573582774C7B734BC407E" +
            "6972BE6418389188E46F3BD899C7EF5D4E07291FCA4D198F8CC7E832E4E2C950" +
            "450538492D36F196A4E2B91405556D184D4298F4F0EBE924457713E2E2428A4D";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyOneCounter() {
        long[] seed = new long[] {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        long counter = 1;
        byte[] out = new byte[BUFFER_SIZE];
        Shake256.shake256(out, seed, counter);
        String expectOutput = "0229A5B5CCDA19DBDF5A1B81BB0A1DC59910B7624A781B6C533B241980E3FB71" +
            "5421388953DF3545B03E748787D5EB0178161209349AF9A10183ECAC2E1AB14D" +
            "B0205A50A593FB0464918F2AAE28B245CCB02070ED0C74658126BA2F255E13F0" +
            "D240D2EA4B53B0A5A62B172F2C5024B5AFDB44BFE472606B593FD21566E30D94" +
            "7D5B0445B479CCB226EA68E414DE28E7C4DF9446B56FB4A5D6E77368297AF90C" +
            "F212BC763048DC9A4E1FA1286C32DF6184F01E381978C9F9E4F82D248EC49131" +
            "B8992312DE2646CEEE816399F4F521B7B3260259B548FB93B6B363B0C6D4F2AC" +
            "ECD9BE720E2BA983385FAFD80A4B54F87BAFC5E1EB5C5C7B8E280A496E22C7B2";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNonZeroKeyNonZeroCounter() {
        long[] seed = new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L};
        long counter = 9L;
        byte[] out = new byte[BUFFER_SIZE];
        Shake256.shake256(out, seed, counter);
        String expectOutput = "760D6680A582F5C6AE6EE1E29EE1EE1ADE3960EE0AA2E8FC995317473F380FD3" +
            "C2740204496C95A6101DDA8CC0698BEFF1791A82B1C727062D99FCEC1C953215" +
            "E9DA06888A05297040181CC5F737EB7747AF42296C527B57EC2EAE3D56242E64" +
            "C27BAFC5C57EBCADDC2D8D6B1879FE957FD7AB1823001CC71BC093BA18B1D1F7" +
            "47CED38A01E54FD26EC3102C612BCFC255A09D959E606C33A79276608E597A95" +
            "4849AE38A07C45A718BAE5C9CF1A1A0C511082598E300685186466CC706BB682" +
            "3245BB038E0EC58FBA50AC4ECA4A377D42586FD2D0ABA218C62FD88246EB4547" +
            "FD7907E08CDA570706AA06F8A8AF88F50CF1EBCC420FBA3EEDF79A844E068778";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }
}
