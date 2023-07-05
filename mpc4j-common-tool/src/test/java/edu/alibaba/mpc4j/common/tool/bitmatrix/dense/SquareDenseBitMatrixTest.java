package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrixFactory.DenseBitMatrixType;

/**
 * square dense matrix test.
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
@RunWith(Parameterized.class)
public class SquareDenseBitMatrixTest {
    /**
     * invertible 3 × 3 matrix
     */
    private static final byte[][] INVERTIBLE_SQUARE_3_MATRIX = new byte[][]{
        new byte[]{(byte) 0x04},
        new byte[]{(byte) 0x05},
        new byte[]{(byte) 0x07},
    };
    /**
     * irreversible 3 × 3 matrix
     */
    private static final byte[][] IRREVERSIBLE_SQUARE_3_MATRIX = new byte[][]{
        new byte[]{(byte) 0x01},
        new byte[]{(byte) 0x05},
        new byte[]{(byte) 0x04},
    };
    /**
     * invertible 9 × 9 matrix
     */
    private static final byte[][] INVERTIBLE_SQUARE_9_MATRIX = new byte[][]{
        new byte[]{(byte) 0x01, (byte) 0xe9},
        new byte[]{(byte) 0x01, (byte) 0xC0},
        new byte[]{(byte) 0x00, (byte) 0x70},
        new byte[]{(byte) 0x01, (byte) 0x04},
        new byte[]{(byte) 0x01, (byte) 0x96},
        new byte[]{(byte) 0x01, (byte) 0xF1},
        new byte[]{(byte) 0x00, (byte) 0x73},
        new byte[]{(byte) 0x01, (byte) 0x94},
        new byte[]{(byte) 0x00, (byte) 0x4B},
    };
    /**
     * irreversible 9 × 9 matrix
     */
    private static final byte[][] IRREVERSIBLE_SQUARE_9_MATRIX = new byte[][]{
        new byte[]{(byte) 0x01, (byte) 0xe9},
        new byte[]{(byte) 0x01, (byte) 0xC0},
        new byte[]{(byte) 0x00, (byte) 0x70},
        new byte[]{(byte) 0x01, (byte) 0x04},
        new byte[]{(byte) 0x01, (byte) 0x96},
        new byte[]{(byte) 0x01, (byte) 0xF1},
        new byte[]{(byte) 0x00, (byte) 0x73},
        new byte[]{(byte) 0x01, (byte) 0x94},
        new byte[]{(byte) 0x00, (byte) 0x4C},
    };
    /**
     * invertible 128 × 128 matrix
     */
    private static final byte[][] INVERTIBLE_SQUARE_128_MATRIX = new byte[][]{
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
    /**
     * irreversible 128 × 128 matrix
     */
    private static final byte[][] IRREVERSIBLE_SQUARE_128_MATRIX = new byte[][]{
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
        Hex.decode("7677e3f99bd8b7eabc873bc23c662509"), Hex.decode("f43a4253f3c3fd597cdacbe067e296d1"),
    };
    /**
     * invertible sparse matrix
     */
    private static final int[][] SPARSE_SQUARE_64_MATRIX = new int[][]{
        new int[]{0,}, new int[]{0, 1,}, new int[]{2,}, new int[]{2, 3,},
        new int[]{1, 3, 4,}, new int[]{2, 4, 5,}, new int[]{3, 5, 6,}, new int[]{4, 6, 7,},
        new int[]{5, 7, 8,}, new int[]{6, 8, 9,}, new int[]{7, 9, 10,}, new int[]{8, 10, 11,},
        new int[]{9, 11, 12,}, new int[]{10, 12, 13,}, new int[]{11, 13, 14,}, new int[]{12, 14, 15,},

        new int[]{13, 15, 16,}, new int[]{14, 16, 17,}, new int[]{15, 17, 18,}, new int[]{16, 18, 19,},
        new int[]{17, 19, 20,}, new int[]{18, 20, 21,}, new int[]{19, 21, 22,}, new int[]{20, 22, 23,},
        new int[]{21, 23, 24,}, new int[]{22, 24, 25,}, new int[]{23, 25, 26,}, new int[]{24, 26, 27,},
        new int[]{25, 27, 28,}, new int[]{26, 28, 29,}, new int[]{27, 29, 30,}, new int[]{28, 30, 31,},

        new int[]{29, 31, 32,}, new int[]{30, 32, 33,}, new int[]{31, 33, 34,}, new int[]{32, 34, 35,},
        new int[]{33, 35, 36,}, new int[]{34, 36, 37,}, new int[]{35, 37, 38,}, new int[]{36, 38, 39,},
        new int[]{37, 39, 40,}, new int[]{38, 40, 41,}, new int[]{39, 41, 42,}, new int[]{40, 42, 43,},
        new int[]{41, 43, 44,}, new int[]{42, 44, 45,}, new int[]{43, 45, 46,}, new int[]{44, 46, 47,},

        new int[]{45, 47, 48,}, new int[]{46, 48, 49,}, new int[]{47, 49, 50,}, new int[]{48, 50, 51,},
        new int[]{49, 51, 52,}, new int[]{50, 52, 53,}, new int[]{51, 53, 54,}, new int[]{52, 54, 55,},
        new int[]{53, 55, 56,}, new int[]{54, 56, 57,}, new int[]{55, 57, 58,}, new int[]{56, 58, 59,},
        new int[]{57, 59, 60,}, new int[]{58, 60, 61,}, new int[]{59, 61, 62,}, new int[]{60, 62, 63,},
    };
    /**
     * sizes
     */
    private static final int[] SIZES = new int[]{1, 7, 8, 9, 127, 128, 129};
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * round
     */
    private static final int ROUND = 40;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // LONG_MATRIX
        configurations.add(new Object[]{DenseBitMatrixType.LONG_MATRIX.name(), DenseBitMatrixType.LONG_MATRIX});
        // BYTE_MATRIX
        configurations.add(new Object[]{DenseBitMatrixType.BYTE_MATRIX.name(), DenseBitMatrixType.BYTE_MATRIX});

        return configurations;
    }

    /**
     * type
     */
    private final DenseBitMatrixType type;

    public SquareDenseBitMatrixTest(String name, DenseBitMatrixType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testConstantAdd() {
        for (int size : SIZES) {
            testConstantAdd(size);
        }
    }

    private void testConstantAdd(int size) {
        // 0 + 0 = 0
        DenseBitMatrix zero = DenseBitMatrixFactory.createAllZero(type, size, size);
        Assert.assertEquals(zero, zero.xor(zero));
        DenseBitMatrix inner = DenseBitMatrixFactory.createAllZero(type, size, size);
        inner.xori(zero);
        Assert.assertEquals(zero, inner);
        // 0 + 1 = 1
        DenseBitMatrix one = DenseBitMatrixFactory.createAllOne(type, size, size);
        Assert.assertEquals(one, one.xor(zero));
        Assert.assertEquals(one, zero.xor(one));
        inner = DenseBitMatrixFactory.createAllZero(type, size, size);
        inner.xori(one);
        Assert.assertEquals(one, inner);
        inner = DenseBitMatrixFactory.createAllOne(type, size, size);
        inner.xori(zero);
        Assert.assertEquals(one, inner);
        // 1 + 1 = 0
        Assert.assertEquals(zero, one.xor(one));
        inner = DenseBitMatrixFactory.createAllOne(type, size, size);
        inner.xori(one);
        Assert.assertEquals(zero, inner);
    }

    @Test
    public void testRandomAdd() {
        for (int size : SIZES) {
            testRandomAdd(size);
        }
    }

    private void testRandomAdd(int size) {
        DenseBitMatrix zero = DenseBitMatrixFactory.createAllZero(type, size, size);
        DenseBitMatrix inner;
        // random + random = 0
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix addRandom = random.xor(random);
            Assert.assertEquals(zero, addRandom);
            inner = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            inner.xori(inner);
            Assert.assertEquals(zero, inner);
        }
        // random + 0 = random
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix random = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            Assert.assertEquals(random, random.xor(zero));
        }
    }

    @Test
    public void testRandomSquareMultiply() {
        for (int size : SIZES) {
            testRandomSquareMultiply(size);
        }
    }

    private void testRandomSquareMultiply(int size) {
        // 相同类型相乘
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            // 测试方法：(A * B)^T = B^T * A^T
            byte[][] mulTrans = a.multiply(b).transpose(EnvType.STANDARD, false).getByteArrayData();
            byte[][] transMul = b.transpose(EnvType.STANDARD, false)
                .multiply(a.transpose(EnvType.STANDARD, false))
                .getByteArrayData();
            Assert.assertArrayEquals(mulTrans, transMul);
        }
        // 乘以字节矩阵
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(
                DenseBitMatrixType.BYTE_MATRIX, size, size, SECURE_RANDOM
            );
            // 测试方法：(A * B)^T = B^T * A^T，注意要把(A * B)^T转换为字节矩阵
            byte[][] mulTrans = a.multiply(b).transpose(EnvType.STANDARD, false).getByteArrayData();
            byte[][] transMul = b.transpose(EnvType.STANDARD, false)
                .multiply(a.transpose(EnvType.STANDARD, false))
                .getByteArrayData();
            Assert.assertArrayEquals(mulTrans, transMul);
        }
    }

    @Test
    public void testRandomMultiply() {
        for (int size : SIZES) {
            testRandomMultiply(size);
        }
    }

    private void testRandomMultiply(int size) {
        for (int rightColumns : SIZES) {
            for (int round = 0; round < ROUND; round++) {
                DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
                // 右侧矩阵的行数必须等于左侧矩阵的列数
                DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, rightColumns, SECURE_RANDOM);
                // 测试方法：(A * B)^T = B^T * A^T
                byte[][] mulTrans = a.multiply(b).transpose(EnvType.STANDARD, false).getByteArrayData();
                byte[][] transMul = b.transpose(EnvType.STANDARD, false)
                    .multiply(a.transpose(EnvType.STANDARD, false))
                    .getByteArrayData();
                Assert.assertArrayEquals(mulTrans, transMul);
            }
        }
    }

    @Test
    public void testLmul() {
        for (int size : SIZES) {
            testLmul(size);
        }
    }

    private void testLmul(int size) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).getByteArrayData();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, size)
                .mapToObj(a::getByteArrayRow)
                .map(b::leftMultiply)
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
            // 将a矩阵转换为布尔矩阵，分别与b矩阵相乘
            byte[][] binaryVectorActualArray = IntStream.range(0, size)
                .mapToObj(rowIndex -> BinaryUtils.byteArrayToBinary(a.getByteArrayRow(rowIndex), size))
                .map(b::leftMultiply)
                .map(BinaryUtils::binaryToRoundByteArray)
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, binaryVectorActualArray);
        }
    }

    @Test
    public void testLmulAddi() {
        for (int size : SIZES) {
            testLmulAddi(size);
        }
    }

    private void testLmulAddi(int size) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).xor(c).getByteArrayData();
            // 将a矩阵分别转换成byte[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, size)
                .mapToObj(rowIndex -> {
                    byte[] t = BytesUtils.clone(c.getByteArrayRow(rowIndex));
                    b.leftMultiplyXori(a.getByteArrayRow(rowIndex), t);
                    return t;
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            byte[][] expectArray = a.multiply(b).xor(c).getByteArrayData();
            // 将a矩阵分别转换成boolean[]分别与b矩阵左乘
            byte[][] byteVectorActualArray = IntStream.range(0, size)
                .mapToObj(rowIndex -> {
                    boolean[] v = BinaryUtils.byteArrayToBinary(a.getByteArrayRow(rowIndex), size);
                    boolean[] t = BinaryUtils.byteArrayToBinary(c.getByteArrayRow(rowIndex), size);
                    b.leftMultiplyXori(v, t);
                    return BinaryUtils.binaryToRoundByteArray(t);
                })
                .toArray(byte[][]::new);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testLextMulAddi() {
        for (int size : SIZES) {
            testLextMulAddi(size);
        }
    }

    private void testLextMulAddi(int size) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix c = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            // 测试方法： ((A^T*B) + C)^T = (A.toArrays())*B + C^T.toArray()
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).xor(c).transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            byte[][] byteVectorActualArray = c.transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            b.leftGf2lMultiplyXori(a.getByteArrayData(), byteVectorActualArray);
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testLextMul() {
        for (int size : SIZES) {
           testLextMul(size);
        }
    }

    private void testLextMul(int size) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix a = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix b = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            // 测试方法： (A^T*B)^T = (A.toArrays())*B
            DenseBitMatrix aTranspose = a.transpose(EnvType.STANDARD_JDK, false);
            byte[][] expectArray = aTranspose.multiply(b).transpose(EnvType.STANDARD_JDK, false).getByteArrayData();
            byte[][] byteVectorActualArray = b.leftGf2lMultiply(a.getByteArrayData());
            Assert.assertArrayEquals(expectArray, byteVectorActualArray);
        }
    }

    @Test
    public void testTranspose() {
        for (int size : SIZES) {
            testTranspose(size);
        }
    }

    private void testTranspose(int size) {
        for (int round = 0; round < ROUND; round++) {
            DenseBitMatrix origin = DenseBitMatrixFactory.createRandom(type, size, size, SECURE_RANDOM);
            DenseBitMatrix transpose = origin.transpose(EnvType.STANDARD, false);
            DenseBitMatrix recover = transpose.transpose(EnvType.STANDARD, false);
            Assert.assertEquals(origin, recover);
        }
    }

    @Test
    public void testIrreversible() {
        Assert.assertThrows(ArithmeticException.class, () -> {
            DenseBitMatrix bitMatrix = DenseBitMatrixFactory.createFromDense(type, 3, IRREVERSIBLE_SQUARE_3_MATRIX);
            bitMatrix.inverse();
        });
        Assert.assertThrows(ArithmeticException.class, () -> {
            DenseBitMatrix bitMatrix = DenseBitMatrixFactory.createFromDense(type, 9, IRREVERSIBLE_SQUARE_9_MATRIX);
            bitMatrix.inverse();
        });
        Assert.assertThrows(ArithmeticException.class, () -> {
            DenseBitMatrix bitMatrix = DenseBitMatrixFactory.createFromDense(type, 128, IRREVERSIBLE_SQUARE_128_MATRIX);
            bitMatrix.inverse();
        });
    }

    @Test
    public void testInverse() {
        DenseBitMatrix square3BitMatrix = DenseBitMatrixFactory.createFromDense(type, 3, INVERTIBLE_SQUARE_3_MATRIX);
        testInverse(square3BitMatrix);
        DenseBitMatrix square9BitMatrix = DenseBitMatrixFactory.createFromDense(type, 9, INVERTIBLE_SQUARE_9_MATRIX);
        testInverse(square9BitMatrix);
        DenseBitMatrix square128BitMatrix = DenseBitMatrixFactory.createFromDense(type, 128, INVERTIBLE_SQUARE_128_MATRIX);
        testInverse(square128BitMatrix);
        DenseBitMatrix squareSparseBitMatrix = DenseBitMatrixFactory.createFromSparse(type, 64, SPARSE_SQUARE_64_MATRIX);
        testInverse(squareSparseBitMatrix);
    }

    private void testInverse(DenseBitMatrix bitMatrix) {
        // 验证求逆
        DenseBitMatrix invertBitMatrix = bitMatrix.inverse();
        int size = bitMatrix.getSize();
        int byteSize = bitMatrix.getByteSize();
        IntStream.range(0, ROUND).forEach(round -> {
            byte[] input = new byte[byteSize];
            SECURE_RANDOM.nextBytes(input);
            BytesUtils.reduceByteArray(input, size);
            byte[] output = bitMatrix.leftMultiply(input);
            byte[] recoveredInput = invertBitMatrix.leftMultiply(output);
            Assert.assertArrayEquals(input, recoveredInput);
        });
    }
}
