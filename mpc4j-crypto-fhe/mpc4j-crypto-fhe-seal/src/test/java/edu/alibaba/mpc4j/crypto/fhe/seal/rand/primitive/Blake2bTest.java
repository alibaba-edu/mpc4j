package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2bState;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Blake2b test. Expect outputs for different keys and inputs are obtained using SEAL library. The official test is from
 * <a href="https://github.com/0xShamil/blake2b/blob/master/src/test/java/com/github/shamil/Blake2bTest.java">Blake2bTest.java</a>,
 * ignoring tests related to <code>salt</code>, <code>personal</code>, and large digest size since our implementation is
 * only used for PRNG thus not supporting <code>salt</code> and <code>personal</code>.
 *
 * @author Weiran Liu
 * @date 2025/2/11
 */
public class Blake2bTest {
    /**
     * default input bytes
     */
    private static final int DEFAULT_INPUT_BYTES = Blake2.BLAKE2B_OUT_BYTES;
    /**
     * default output bytes that must be less than or equal to <code>Blake2.BLAKE2B_OUT_BYTES</code>.
     */
    private static final int DEFAULT_OUTPUT_BYTES = Blake2.BLAKE2B_OUT_BYTES;

    @Test
    public void testNullKeyNullIn() {
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, null, null);
        String expectOutput = "786A02F742015903C6C6FD852552D272912F4740E15847618A86E217F71F5419" +
            "D25E1031AFEE585313896444934EB04B903A685B1448B755D56F701AFE9BE2CE";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyEmptyIn() {
        byte[] in = new byte[0];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, null);
        String expectOutput = "786A02F742015903C6C6FD852552D272912F4740E15847618A86E217F71F5419" +
            "D25E1031AFEE585313896444934EB04B903A685B1448B755D56F701AFE9BE2CE";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyZeroIn() {
        byte[] in = new byte[DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, null);
        String expectOutput = "8715B7B58C747A49E371BA0B02B7DE8F35DA26FF2C8A60B80715D02720212662" +
            "83AF3EEDB537683DD74CB1708601A80C9970376F1226D16AFC242765ECCD592A";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testNullKeyLongIn() {
        byte[] in = new byte[DEFAULT_INPUT_BYTES * DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, null);
        String expectOutput = "F18C1BA69B093034FAA8EF722382F086547FDD178186AE216B5AE2434A318691" +
            "032F3E7F55E8ACC841D07E7519E33C0F850AFBA006EC4EF9E2FC6CD91C7D5DFB";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyNullIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, null, key);
        String expectOutput = "4CA23316A1E03EF625D6E775A450C3B54FE6AC037C0D14B70845D6CF36B4951E" +
            "8945E5286EF6B99379A134AA416A89EF7C730661AAA9B7F35DD58E1B65F91AD6";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyEmptyIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[0];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, key);
        String expectOutput = "4CA23316A1E03EF625D6E775A450C3B54FE6AC037C0D14B70845D6CF36B4951E" +
            "8945E5286EF6B99379A134AA416A89EF7C730661AAA9B7F35DD58E1B65F91AD6";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyZeroIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, key);
        String expectOutput = "60CA9A9020A65836575C7949E0FCD799487A4DE473AEF8873BC9597959FCA44F" +
            "EF47454EC2E0D769139D8DE0D95CA1DC68F7B8C47650B45E24610FAD3935564B";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    @Test
    public void testZeroKeyLongIn() {
        byte[] key = new byte[Blake2.BLAKE2B_KEY_BYTES];
        byte[] in = new byte[DEFAULT_INPUT_BYTES * DEFAULT_INPUT_BYTES];
        byte[] out = new byte[DEFAULT_OUTPUT_BYTES];
        Blake2b.blake2b(out, in, key);
        String expectOutput = "A8379CDCBC6C64E7817F61A15F3CF34DC4F0B89289F6E49810230ABB74AAC2F6" +
            "22FD2C21FCE92673CD82789E2FC737EF9F2EF0D0A532DD9EA7A9CAAA8016F768";
        String actualOutput = Hex.toHexString(out).toUpperCase(Locale.ROOT);
        Assert.assertEquals(expectOutput, actualOutput);
    }

    /**
     * input/message, key, hash, Vectors from BLAKE2 website: <a href="https://blake2.net/blake2b-test.txt">blake2b-test.txt</a>
     */
    private static final String[][] KEYED_TEST_VECTORS = {
        {
            "",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "10ebb67700b1868efb4417987acf4690ae9d972fb7a590c2f02871799aaa4786b5e996e8f0f4eb981fc214b005f42d2ff4233499391653df7aefcbc13fc51568"
        },

        {
            "00",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "961f6dd1e4dd30f63901690c512e78e4b45e4742ed197c3c5e45c549fd25f2e4187b0bc9fe30492b16b0d0bc4ef9b0f34c7003fac09a5ef1532e69430234cebd"
        },

        {
            "0001",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "da2cfbe2d8409a0f38026113884f84b50156371ae304c4430173d08a99d9fb1b983164a3770706d537f49e0c916d9f32b95cc37a95b99d857436f0232c88a965"
        },

        {
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "f1aa2b044f8f0c638a3f362e677b5d891d6fd2ab0765f6ee1e4987de057ead357883d9b405b9d609eea1b869d97fb16d9b51017c553f3b93c0a1e0f1296fedcd"
        },

        {
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "c230f0802679cb33822ef8b3b21bf7a9a28942092901d7dac3760300831026cf354c9232df3e084d9903130c601f63c1f4a4a4b8106e468cd443bbe5a734f45f"
        },

        {
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfe",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",
            "142709d62e28fcccd0af97fad0f8465b971e82201dc51070faa0372aa43e92484be1c1e73ba10906d5d1853db6a4106e0a7bf9800d373d6dee2d46d62ef2a461"
        }
    };

    /**
     * hash, input/message, from: <a href="http://fossies.org/linux/john/src/rawBLAKE2_512_fmt_plug.c">rawBLAKE2_512_fmt_plug.</a>.
     */
    private final static String[][] unkeyedTestVectors = {
        {
            "4245af08b46fbb290222ab8a68613621d92ce78577152d712467742417ebc1153668f1c9e1ec1e152a32a9c242dc686d175e087906377f0c483c5be2cb68953e",
            "blake2"
        },
        {
            "021ced8799296ceca557832ab941a50b4a11f83478cf141f51f933f653ab9fbcc05a037cddbed06e309bf334942c4e58cdf1a46e237911ccd7fcf9787cbc7fd0",
            "hello world"
        },
        {
            "1f7d9b7c9a90f7bfc66e52b69f3b6c3befbd6aee11aac860e99347a495526f30c9e51f6b0db01c24825092a09dd1a15740f0ade8def87e60c15da487571bcef7",
            "verystrongandlongpassword"
        },
        {
            "a8add4bdddfd93e4877d2746e62817b116364a1fa7bc148d95090bc7333b3673f82401cf7aa2e4cb1ecd90296e3f14cb5413f8ed77be73045b13914cdcd6a918",
            "The quick brown fox jumps over the lazy dog"
        },
        {
            "786a02f742015903c6c6fd852552d272912f4740e15847618a86e217f71f5419d25e1031afee585313896444934eb04b903a685b1448b755d56f701afe9be2ce",
            ""
        },
        {
            "ba80a53f981c4d0d6a2797b69f12f6e94c212f14685ac4b74b12bb6fdbffa2d17d87c5392aab792dc252d5de4533cc9518d38aa8dbf1925ab92386edd4009923",
            "abc"
        },
    };

    @Test
    public void testPerform() {
        Blake2bState S = new Blake2bState();
        // test keyed test vectors
        byte[] key = Hex.decode(KEYED_TEST_VECTORS[0][1]);
        for (String[] keyedTestVector : KEYED_TEST_VECTORS) {
            byte[] input = Hex.decode(keyedTestVector[0]);
            byte[] keyedHash = new byte[64];
            Blake2b.blake2b_init_key(S, keyedHash.length, key);
            Blake2b.blake2b_update(S, input);
            Blake2b.blake2b_final(S, keyedHash);
            Assert.assertArrayEquals(Hex.decode(keyedTestVector[2]), keyedHash);
        }

        // test unkeyed test vectors
        for (String[] unkeyedTestVector : unkeyedTestVectors) {
            byte[] unkeyedHash = new byte[64];
            Blake2b.blake2b_init(S, unkeyedHash.length);
            byte[] unkeyedInput = unkeyedTestVector[1].getBytes(StandardCharsets.UTF_8);
            Blake2b.blake2b_update(S, unkeyedInput);
            Blake2b.blake2b_final(S, unkeyedHash);
            S.set_empty_state();
            Assert.assertArrayEquals(Hex.decode(unkeyedTestVector[0]), unkeyedHash);
        }
    }
}
