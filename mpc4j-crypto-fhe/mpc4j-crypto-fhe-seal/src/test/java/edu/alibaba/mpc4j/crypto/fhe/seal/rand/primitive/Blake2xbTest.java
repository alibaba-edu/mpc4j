package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * Blake2xb test. Expect outputs for different keys and inputs are obtained using SEAL library.
 *
 * @author Weiran Liu
 * @date 2025/2/13
 */
public class Blake2xbTest {
    /**
     * default input bytes
     */
    private static final int DEFAULT_INPUT_BYTES = Blake2.BLAKE2B_OUT_BYTES;
    /**
     * default output bytes, can be greater than <code>Blake2.BLAKE2B_OUT_BYTES</code>.
     */
    private static final int DEFAULT_OUTPUT_BYTES = 128;

    @Test
    public void testNullKeyNullIn() {
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, null, null);
        String expectOutput = "C4A522632C214104372ED2A05D7A9C0423153094F569E06C75A9989F530CE51E" +
            "FB837F468CB95AD94755E4CED2093C7EDFFA59DC89331F17A87FE01C22FF4DDC" +
            "68E4D158946ACF99A7590F142E0B6B08667FCB735DDCF7997693B11647E03DA5" +
            "9781606CE4C7E38E98F290319FD093EFC3ED3C68DB238080F5D6C9565B6F6998";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyEmptyIn() {
        byte[] in = new byte[0];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, null);
        String expectOutput = "C4A522632C214104372ED2A05D7A9C0423153094F569E06C75A9989F530CE51E" +
            "FB837F468CB95AD94755E4CED2093C7EDFFA59DC89331F17A87FE01C22FF4DDC" +
            "68E4D158946ACF99A7590F142E0B6B08667FCB735DDCF7997693B11647E03DA5" +
            "9781606CE4C7E38E98F290319FD093EFC3ED3C68DB238080F5D6C9565B6F6998";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyZeroIn() {
        byte[] in = new byte[DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, null);
        String expectOutput = "3FD61611200FC5D5C3F08796A345415A59B6A0F4156C8ADC3CF69119856155CA" +
            "8331400E98B5C4ACD1AA257F54A5B2D1DB5E563D78C4FAAD97B7B1934528BB8D" +
            "7F6BAB7A103AEBDB0D8A7186A5E4F6698113A7A94F7C8598F4A7DAAEA82C145B" +
            "8B48F448BA21818656AFD8C4A6E7632D563126F340E38568A7702838F5B644FE";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyLongIn() {
        byte[] in = new byte[DEFAULT_INPUT_BYTES * DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, null);
        String expectOutput = "1C4F11CCC5FB2FCB3F3B5AED2F26F67F8CE6ED660569BD074F3362FE1211492D" +
            "3709A8D7654C5D5CCD5763A8874C5E8127EAB62F5D7A28327798E5BA3E3D3B83" +
            "A9FB37CAD220C9259ABA8D1795FC50B95290D2727E748ADE88B12A1797A75B76" +
            "245D340DCAA2323DB1ED848DCFD74ACC9FAD1C74196394B1D47A943D9598E27F";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyNullIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, null, key);
        String expectOutput = "43B35CA9CBBF7EED1B93B859EE83B759CE606C97222821602732F578605C6123" +
            "1C3396497125F49B2C43F8166D0434382DF36C357A76A5BAD8CB331BE7F44454" +
            "A66B302D7FD639FC6C04CD4F1792D455E707468A5D27B487B4D409633265F62C" +
            "1A7117F9336258FC6F793D3E8B789C92F47B3C2AD65C90E34E2ABDFE8EA8A3A7";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyEmptyIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[0];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, key);
        String expectOutput = "43B35CA9CBBF7EED1B93B859EE83B759CE606C97222821602732F578605C6123" +
            "1C3396497125F49B2C43F8166D0434382DF36C357A76A5BAD8CB331BE7F44454" +
            "A66B302D7FD639FC6C04CD4F1792D455E707468A5D27B487B4D409633265F62C" +
            "1A7117F9336258FC6F793D3E8B789C92F47B3C2AD65C90E34E2ABDFE8EA8A3A7";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyZeroIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, key);
        String expectOutput = "C4DDC9AF6039F2AB43E805A272CA3DBA54DAB7AB4434ABD68B1CE9DD750FCF68" +
            "939CE09913DB75CBFD30387D3A21F9063CD0A45254BAB8CAA84B4D5DEBD99DB4" +
            "685E1E9BAC8BBEE177383087E77942CF14301243B8D43E38B417C1A91579004F" +
            "E7224D2CB2E1ABEFF8B26EED09B493060A4B994FEBF3EF5AEF5011757B16DE38";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyLongIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[DEFAULT_INPUT_BYTES * DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2xb.blake2xb(out, in, key);
        String expectOutput = "AE7661A4799CAE77979048A76493B9713315C37956F94A83A6BF4E47D81DA532" +
            "1C31AFA72AB7BB23480F7A644D4C48189C1E01CFFEEE3F11FAF2D9E14D7A6DDE" +
            "F83F34A0A141E3A404D67E54B87E16C39496BB366516FAC7E1D93F42D66B5FC3" +
            "FB2A3ED0AD792130031FA00E95E68946FA303E68327E00E4D0B933403716C44C";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    /**
     * buffer size
     */
    private static final int BUFFER_SIZE = 256;

    @Test
    public void testZeroKeyZeroCounter() {
        long[] seed = new long[] {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        long counter = 0;
        byte[] out = new byte[BUFFER_SIZE];
        Blake2xb.blake2xb(out, seed, counter);
        String expectOutput = "BB53A26BA73CFFA467BBF7D30B1B6768375D8D89D67A8490BCE4D9A35948BE66" +
            "9E48D2250A0CC36817C6F69ECC8F1A0B69270910390C1722B7F46D2AA5CCDD57" +
            "FF9526AF084B3F4E15A30E7A203F2C10336A3E955FAF3403139628BE65C9DD26" +
            "631BC195AF4B3E7ED81DDD84F1AE1227C3762FD08E80CDD15303AE45AAB4D79D" +
            "B07A57DD0905A93CA66639764414BFD01B230D5BE4F389EAA9563A64C3C2D227" +
            "7C871E7FF04A3ABEB05DF56AD7E00950D1B2E48374B3906DDAE0899C46516639" +
            "A838AEA592B36E880B7AAAB169CBDD416F35B146EA6685BBD9D94D70286C0076" +
            "9C4478074CAAC89B1F66268509637D94D4E1F24CB22389A2D64F6F79FB9CEE99";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyOneCounter() {
        long[] seed = new long[] {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
        long counter = 1L;
        byte[] out = new byte[BUFFER_SIZE];
        Blake2xb.blake2xb(out, seed, counter);
        String expectOutput = "921A222E9B300BC12BC3CC7C0ED8DDE7CC43AB9D9C8B1C94C6DBFD2586C122F7" +
            "D2949046AF13207D9FDF90E0FAEB21172BCDF8BEED54565D1193A3FF205131B8" +
            "1B327752B3A323FCC5655E3EB9C3288B705BBE8365A6ED4AFEA95FED39C48B5A" +
            "A7D60C7535787121B629C5F65479AFBE2BE42FE460219C038E582F504332480C" +
            "DBE35FA52D3307415C1978892F5E9A76172A0C2746F8428889AF5EDB47B9885D" +
            "11FFA62965A2C76C80FB8F0404609AA762D25FABE75114D08F22E58FE7E57EDA" +
            "6762011B839AC53069ACE0676E0288B6BB4FE2E15E1C7B08E3C21312613315FE" +
            "B00EE3BED16D00BC00903EC9A056ED97CE6A35DA0FA1C2C16C5E025CC21853E8";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNonZeroKeyNonZeroCounter() {
        long[] seed = new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L};
        long counter = 9L;
        byte[] out = new byte[BUFFER_SIZE];
        Blake2xb.blake2xb(out, seed, counter);
        String expectOutput = "B2E43BB535794650DA468E6A304F16BC4001A094DA6CA6EF3FDE79631B569AE1" +
            "C2413EADDC2DFA33A97FDCB6CC0770AE17C5A53FF635366DAF9A188F34BACEF6" +
            "36D83153C436B5DD43040521632354A6455431C08BAF979F93D893E8A208AB58" +
            "006153C8399E5AC09E499BD0DD085418B2892D7684F0BC02E65FBAA76EF58932" +
            "5F71BE72ABA09392BBD33B004624A49602AC3876836E5C485574DD65E2439F9E" +
            "461FEB56B8C2E9BE603191F64DC5C6BE8238A985DCEE5BE6AAC161FE7BC0AEC8" +
            "068CA67775EDDDB94564962BF38862A7DB5C80A468E3032ECF23D93AE2FC7400" +
            "27C2B5B13FD243C2193599552D4FF64A76595B5194275BEA12E774AFEC37BF6B";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }
}
