package edu.alibaba.mpc4j.common.structure.okve.tool;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * binary linear independent row finder test.
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
public class BinaryMaxLisFinderTest {
    /**
     * 3-by-3 system, singular
     */
    private static final byte[][] SINGULAR_3_3 = new byte[][] {
        new byte[] { 0b00000010 },
        new byte[] { 0b00000110 },
        new byte[] { 0b00000001 },
    };
    private static final TIntSet SINGULAR_3_3_RESULT = new TIntHashSet(new int[] {0, 1, 2});

    /**
     * 3-by-3 system, non-singular
     */
    private static final byte[][] NON_SINGULAR_3_3 = new byte[][] {
        new byte[] { 0b00000010 },
        new byte[] { 0b00000101 },
        new byte[] { 0b00000111 },
    };
    private static final TIntSet NON_SINGULAR_3_3_RESULT = new TIntHashSet(new int[] {0, 1});

    /**
     * 4-by-3 system, singular (for 3)
     */
    private static final byte[][] SINGULAR_4_3 = new byte[][] {
        new byte[] { 0b00000010 },
        new byte[] { 0b00000110 },
        new byte[] { 0b00000001 },
        new byte[] { 0b00000101 },
    };
    private static final TIntSet SINGULAR_4_3_RESULT = new TIntHashSet(new int[] {0, 1, 2});

    /**
     * 4-by-3 system, non-singular (for 3)
     */
    private static final byte[][] NON_SINGULAR_4_3 = new byte[][] {
        new byte[] { 0b00000110 },
        new byte[] { 0b00000110 },
        new byte[] { 0b00000001 },
        new byte[] { 0b00000101 },
    };
    private static final TIntSet NON_SINGULAR_4_3_RESULT = new TIntHashSet(new int[] {0, 2, 3});

    /**
     * 8-by-8 system, singular
     */
    private static final byte[][] SINGULAR_8_8 = new byte[][] {
        new byte[] {(byte)0b11011110, },
        new byte[] {(byte)0b00110101, },
        new byte[] {(byte)0b01000111, },
        new byte[] {(byte)0b11010011, },
        new byte[] {(byte)0b01011101, },
        new byte[] {(byte)0b01110111, },
        new byte[] {(byte)0b01100011, },
        new byte[] {(byte)0b01110011, },
    };
    private static final TIntSet SINGULAR_8_8_RESULT = new TIntHashSet(new int[] {0, 1, 2, 3, 4, 5, 6, 7});

    /**
     * 8-by-8 system, non-singular
     */
    private static final byte[][] NON_SINGULAR_8_8 = new byte[][] {
        new byte[] {(byte)0b10000000, },
        new byte[] {(byte)0b11000000, },
        new byte[] {(byte)0b01000000, },
        new byte[] {(byte)0b00100000, },
        new byte[] {(byte)0b00010000, },
        new byte[] {(byte)0b00000100, },
        new byte[] {(byte)0b00000010, },
        new byte[] {(byte)0b00000001, },
    };
    private static final TIntSet NON_SINGULAR_8_8_RESULT = new TIntHashSet(new int[] {0, 1, 3, 4, 5, 6, 7});

    /**
     * 128-by-128 system, singular
     */
    private static final byte[][] SINGULAR_128_128 = new byte[][] {
        Hex.decode("de3547d35d7763737b6ec5825f32786d"), Hex.decode("a1bf2597d8732f367e52b8560916d23a"),
        Hex.decode("f72e3cc9bdb8a5c0e4aaae0160b2b5e0"), Hex.decode("bd611bd92408e58abd56402baabd035d"),
        Hex.decode("d94fedafaaae5344aa35b034c6861e86"), Hex.decode("130bb264fd142470c1f146023cfd60d2"),
        Hex.decode("b93749b56141c02b49085f82deb76be5"), Hex.decode("4dc1bff17791c1fa6ddf00fd5e5f9d70"),
        Hex.decode("0a7c4d316af58d5eaf2cfc883c8101e2"), Hex.decode("31c1f5999be5fff4dd17e4cd49e5db35"),
        Hex.decode("db04897e856e6c08c8c7d8ace4cd3e39"), Hex.decode("395895c4019c4b2b8c49d9026d7d4a17"),
        Hex.decode("26508955e2b83e3771001c1002d8f5fa"), Hex.decode("83be07712002686fa2f201875ae0a600"),
        Hex.decode("52b9e52c3dde28c99201f98da5d8aa3d"), Hex.decode("929d0082e09ef584e70021ca6af88dcc"),
        Hex.decode("2406a117212465dc6360441c978ecc5c"), Hex.decode("3e7779e13a411c8ca5681c4b1308cf34"),
        Hex.decode("bf8e1f8fbea063f257d41d7958e968f6"), Hex.decode("2d07bb4ac2ed8c2ac671685edf5a3791"),
        Hex.decode("93d0a46e657b841bc75ac99fee6e16b4"), Hex.decode("ba075b4b842ca5c58a98ebe6cb49739c"),
        Hex.decode("ffea50dfcb3b57fa2a4170ad9c27d543"), Hex.decode("6943be0b0ab5184638f6be29c3d1c18b"),
        Hex.decode("35cf23a0da1a9ca3daae223ef02236b7"), Hex.decode("391b93222a952cd000a93d2bbd233db7"),
        Hex.decode("b3068d01c9a27873bdfb5349c0cac8c7"), Hex.decode("70674978ac61fb81ce4574e230ac3bd5"),
        Hex.decode("a7e24dec9677ba73fc12d5f8fc627c3f"), Hex.decode("6cbec6f628412a6a79d449defe293111"),
        Hex.decode("53c4b2e5919486e7b7b5fb794b2ce899"), Hex.decode("795e42d17c018bdbe5d81cb1a1d36d57"),
        Hex.decode("694f75366c82c580a4fd049a2550a686"), Hex.decode("1c68f8a1c9c674faed2fd092038fede5"),
        Hex.decode("be8248ef94534fd4819da3cfa0960842"), Hex.decode("691c2c437029692297aaf93821ca6d61"),
        Hex.decode("c85c33c35301dea615dcad65892924ec"), Hex.decode("d37a219f35c6d449e6e1ff4070d6512d"),
        Hex.decode("5b5fdb364485b8051f6be8ac5aadbf45"), Hex.decode("b40daaeb7d94086114c3bce25d177c8e"),
        Hex.decode("482735744d6b02b590afc9c9f4b7f3a5"), Hex.decode("0399ba00ee38f934d57b1398702a074d"),
        Hex.decode("2565a855f7f1e61f714c7f7dde6643ee"), Hex.decode("1ddc8f722240c8c267ce6f7e08bb9510"),
        Hex.decode("5ba72bea74c1d6e7e73f88afc187cf4f"), Hex.decode("3c6be2b33166f4077cf0e3977af37595"),
        Hex.decode("95ac795885bd57f2878cc87263883bb7"), Hex.decode("99961f68008be3e88ddce2332b7bebd6"),
        Hex.decode("e6595924da9a98d2045d9d95f1bcf4ad"), Hex.decode("9f94997d6075e005951f5157bebc75d3"),
        Hex.decode("1091d9db8f2f39984e41c5840c777285"), Hex.decode("e948a6a7be6bb219e1326af6a0979ccb"),
        Hex.decode("638a0a7ab41703ed40717ce76a89a42c"), Hex.decode("ce9c9797bcc21501dea3e62b2d951ccc"),
        Hex.decode("84105551158d2f50451e06f0eea40433"), Hex.decode("796f96a636876dbe06d384230b79872f"),
        Hex.decode("6ea1eca8c467369c5d15e503f21c85a6"), Hex.decode("d23729c967998285523c00053925ede3"),
        Hex.decode("681221076458e7ef5a5561ab3014b851"), Hex.decode("324429fc65c383aba121bee19c40dfe7"),
        Hex.decode("d8d5b30b88a2946c8324c1acba0713ea"), Hex.decode("329f6ae4aa5cb456c9911cb348ea7833"),
        Hex.decode("6baf970e193b2b69edac7b2b74162feb"), Hex.decode("e9ed10270bb0d0c635ef1a1e88825fab"),
        Hex.decode("7705a10a3f282f4418e525ca0cb9d41d"), Hex.decode("98f7d03fd1b00d19c9fbed783baef41a"),
        Hex.decode("d7e9bc9710dc9735dac9ed73ef3e241f"), Hex.decode("5ae7269b4c7a56f0c3a09408b0af19c0"),
        Hex.decode("a62d09036cad07ffb04925d0d56265fd"), Hex.decode("f9aa86dace570179c09bcfaa1426015b"),
        Hex.decode("1761a3850abca60e27a459d662f25927"), Hex.decode("adcc5c95c3cc7ea02a98c2d3df0e8e0c"),
        Hex.decode("5756b3ccb6c63f4dbf0757666709bcba"), Hex.decode("89b542f8f4628c7138c69203019c24b9"),
        Hex.decode("6717133a514250b0c3705db2ef5f6cf1"), Hex.decode("d4f47655df2fa6af005bf7fcae187608"),
        Hex.decode("a40a7d9894c004be1dc58d7fcce69422"), Hex.decode("5e2a58fd5b8533400a0aab19c3df3c76"),
        Hex.decode("6d524ac227f4a6326c5bd29abe6cf7f8"), Hex.decode("af18686f2c0d6f3f7f9ab29f80bb754b"),
        Hex.decode("fb6038ed5118d586e7a3eb836757da21"), Hex.decode("e38dabfbbb019ba5895ab7907646115d"),
        Hex.decode("3d526ca360aa23474fe0f0cd57dca7cc"), Hex.decode("10c0d74d800c63a6770f9baecb7f1a9c"),
        Hex.decode("7c7c2e1e0374e90446d582083b9f31f6"), Hex.decode("f7ba590b6712aeec0f8396a1306c58ae"),
        Hex.decode("0b720e1bec9738b6b5781905823a1499"), Hex.decode("312e15d01a71aa689245bcaa75245ab4"),
        Hex.decode("36370a61828b2291e142c9eae9ca120b"), Hex.decode("b4acb05cf4f454947f647b6f719d69f6"),
        Hex.decode("1eba4f94bdadf5f2c252106751fe3226"), Hex.decode("32b074edd7898ba56fccd99d271faf62"),
        Hex.decode("6da9e977f3b22f56f5eb74d1e226fd5d"), Hex.decode("b69e491300d4ad49b4c089cb63157e7b"),
        Hex.decode("cab9a3b035ffc228abb5b2e5d50a7058"), Hex.decode("a0a0dbe1644ba1ebdfd0b88bfff2a29e"),
        Hex.decode("eb3907bfb07d1cb5427e50ac024e0183"), Hex.decode("c19045deae0cbce178decb8a2b02ff34"),
        Hex.decode("d0aa1f77eb863f12fca2708fdeaf922d"), Hex.decode("ee713c665f55a1dce55f76b2e8c326d3"),
        Hex.decode("e1482d383316137a227e3f3893d411f3"), Hex.decode("3cac1872f1889df1aa8869040f6b4aa8"),
        Hex.decode("ca734c39e86ff4d644335dcb28c90b3b"), Hex.decode("d8fe49e00a09f1cb0880d3652b89bad6"),
        Hex.decode("660886af6bece4ad6aa3de248e26c06c"), Hex.decode("2ac84b877be9a58a72688e85620ff5a0"),
        Hex.decode("d3aa6a6ed6545c541146a496f0125353"), Hex.decode("3a96f3c372de91a8305d9dd6bdf3615e"),
        Hex.decode("77d8b54e2c2a163f187828d7618616bc"), Hex.decode("a0c6f30df3b1e1d245c195d27a040fa5"),
        Hex.decode("a5ba37f02e79df115ae90d44afb7657b"), Hex.decode("c909869537fa2ceed7c12e1f31704fd7"),
        Hex.decode("e3b2c328966d84dc8f93f98c8d86be73"), Hex.decode("c1af03046754191a6e7e85cc33094b6d"),
        Hex.decode("a5b7646fe6514c1e4d1c09d6d9fd4512"), Hex.decode("5d81f8936fd01ceeb78ad918f16b11a4"),
        Hex.decode("3200410bd9e49a14c22b426ce6c960f5"), Hex.decode("f7789808c19daf9a6d02f9a6ca56a838"),
        Hex.decode("4315052835584d650c284378d401fd8d"), Hex.decode("cafbe2e083f76c843e002a19a6efc6c2"),
        Hex.decode("83320fc373f63ddbbd481fe695f2aef7"), Hex.decode("ee4e5c15641f1501a7680aa39fdadc1d"),
        Hex.decode("5081d146d2f3736883caf5cb5572a721"), Hex.decode("2e4b47af9fccc65438403895a2139c97"),
        Hex.decode("a106ec01e3bd4522f22b340ecfd57fc7"), Hex.decode("f137bacfcc2a859943d0019b6bbb655c"),
        Hex.decode("7677e3f99bd8b7eabc873bc23c662509"), Hex.decode("f43a4253f3c3fd597cdacbe067e296da"),
    };
    private static final TIntSet SINGULAR_128_128_RESULT = new TIntHashSet(IntStream.range(0, 128).toArray());

    /**
     * binary max linear independent row finder
     */
    private final BinaryMaxLisFinder maxLisFinder;

    public BinaryMaxLisFinderTest() {
        maxLisFinder = new BinaryMaxLisFinder();
    }

    @Test
    public void testSingular3x3() {
        test(SINGULAR_3_3, 3, SINGULAR_3_3_RESULT);
    }

    @Test
    public void testNonSingular3x3() {
        test(NON_SINGULAR_3_3, 3, NON_SINGULAR_3_3_RESULT);
    }

    @Test
    public void testSingular4x3() {
        test(SINGULAR_4_3, 3, SINGULAR_4_3_RESULT);
    }

    @Test
    public void testNonSingular4x3() {
        test(NON_SINGULAR_4_3, 3, NON_SINGULAR_4_3_RESULT);
    }

    @Test
    public void testSingular8x8() {
        test(SINGULAR_8_8, 8, SINGULAR_8_8_RESULT);
    }

    @Test
    public void testNonSingular8x8() {
        test(NON_SINGULAR_8_8, 8, NON_SINGULAR_8_8_RESULT);
    }

    @Test
    public void testSingular128x128() {
        test(SINGULAR_128_128, 128, SINGULAR_128_128_RESULT);
    }

    private void test(byte[][] matrix, int m, TIntSet result) {
        TIntSet lisRows = maxLisFinder.getLisRows(matrix, m);
        Assert.assertEquals(result, lisRows);
    }
}
