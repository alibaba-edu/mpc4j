/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

/**
 * Ristretto element test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/RistrettoElementTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoPointTest {
    /**
     * compressed Ristretto generator
     */
    private static final CafeRistrettoCompressedPoint RISTRETTO_GENERATOR_COMPRESSED = new CafeRistrettoCompressedPoint(
        Hex.decode("e2f2ae0a 6abc4e71 a884a961 c500515f 58e30b6a a582dd8d b6a65945 e08d2d76")
    );

    /**
     * generator multiply result table.
     */
    private static final String[] GENERATOR_MULTIPLES = new String[]{
        "00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000",
        "e2f2ae0a 6abc4e71 a884a961 c500515f 58e30b6a a582dd8d b6a65945 e08d2d76",
        "6a493210 f7499cd1 7fecb510 ae0cea23 a110e8d5 b901f8ac add3095c 73a3b919",
        "94741f5d 5d52755e ce4f23f0 44ee27d5 d1ea1e2b d196b462 166b1615 2a9d0259",
        "da808627 73358b46 6ffadfe0 b3293ab3 d9fd53c5 ea6c9553 58f56832 2daf6a57",
        "e882b131 016b52c1 d3337080 187cf768 423efccb b517bb49 5ab812c4 160ff44e",
        "f64746d3 c92b1305 0ed8d802 36a7f000 7c3b3f96 2f5ba793 d19a601e bb1df403",
        "44f53520 926ec81f bd5a3878 45beb7df 85a96a24 ece18738 bdcfa6a7 822a176d",
        "903293d8 f2287ebe 10e2374d c1a53e0b c887e592 699f02d0 77d5263c dd55601c",
        "02622ace 8f7303a3 1cafc63f 8fc48fdc 16e1c8c8 d234b2f0 d6685282 a9076031",
        "20706fd7 88b2720a 1ed2a5da d4952b01 f413bcf0 e7564de8 cdc81668 9e2db95f",
        "bce83f8b a5dd2fa5 72864c24 ba1810f9 522bc600 4afe9587 7ac73241 cafdab42",
        "e4549ee1 6b9aa030 99ca208c 67adafca fa4c3f3e 4e5303de 6026e3ca 8ff84460",
        "aa52e000 df2e16f5 5fb1032f c33bc427 42dad6bd 5a8fc0be 0167436c 5948501f",
        "46376b80 f409b29d c2b5f6f0 c5259199 0896e571 6f41477c d30085ab 7f10301e",
        "e0c418f7 c8d9c4cd d7395b93 ea124f3a d99021bb 681dfc33 02a9d99a 2e53e64e"};

    /**
     * invalid encoding strings.
     */
    private static final String[] INVALID_ENCODINGS = new String[]{
        // Non-canonical field encodings.
        "00ffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff",
        "ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffff7f",
        "f3ffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffff7f",
        "edffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffff7f",

        // Negative field elements.
        "01000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000",
        "01ffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffff7f",
        "ed57ffd8 c914fb20 1471d1c3 d245ce3c 746fcbe6 3a3679d5 1b6a516e bebe0e20",
        "c34c4e18 26e5d403 b78e246e 88aa051c 36ccf0aa febffe13 7d148a2b f9104562",
        "c940e5a4 404157cf b1628b10 8db051a8 d439e1a4 21394ec4 ebccb9ec 92a8ac78",
        "47cfc549 7c53dc8e 61c91d17 fd626ffb 1c49e2bc a94eed05 2281b510 b1117a24",
        "f1c6165d 33367351 b0da8f6e 4511010c 68174a03 b6581212 c71c0e1d 026c3c72",
        "87260f7a 2f124951 18360f02 c26a470f 450dadf3 4a413d21 042b43b9 d93e1309",

        // Non-square x^2.
        "26948d35 ca62e643 e26a8317 7332e6b6 afeb9d08 e4268b65 0f1f5bbd 8d81d371",
        "4eac077a 713c57b4 f4397629 a4145982 c661f480 44dd3f96 427d40b1 47d9742f",
        "de6a7b00 deadc788 eb6b6c8d 20c0ae96 c2f20190 78fa604f ee5b87d6 e989ad7b",
        "bcab477b e20861e0 1e4a0e29 5284146a 510150d9 817763ca f1a6f4b4 22d67042",
        "2a292df7 e32cabab bd9de088 d1d1abec 9fc0440f 637ed2fb a145094d c14bea08",
        "f4a9e534 fc0d216c 44b218fa 0c42d996 35a0127e e2e53c71 2f706096 49fdff22",
        "8268436f 8c412619 6cf64b3c 7ddbda90 746a3786 25f9813d d9b84570 77256731",
        "2810e5cb c2cc4d4e ece54f61 c6f69758 e289aa7a b440b3cb eaa21995 c2f4232b",

        // Negative xy value.
        "3eb858e7 8f5a7254 d8c97311 74a94f76 755fd394 1c0ac937 35c07ba1 4579630e",
        "a45fdc55 c76448c0 49a1ab33 f17023ed fb2be358 1e9c7aad e8a61252 15e04220",
        "d483fe81 3c6ba647 ebbfd3ec 41adca1c 6130c2be eee9d9bf 065c8d15 1c5f396e",
        "8a2e1d30 050198c6 5a544831 23960ccc 38aef684 8e1ec8f5 f780e852 3769ba32",
        "32888462 f8b486c6 8ad7dd96 10be5192 bbeaf3b4 43951ac1 a8118419 d9fa097b",
        "22714250 1b9d4355 ccba2904 04bde415 75b03769 3cef1f43 8c47f8fb f35d1165",
        "5c37cc49 1da847cf eb9281d4 07efc41e 15144c87 6e0170b4 99a96a22 ed31e01e",
        "44542511 7cb8c90e dcbc7c1c c0e74f74 7f2c1efa 5630a967 c64f2877 92a48a4b",

        // s = -1, which causes y = 0.
        "ecffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffffff ffffff7f"};

    /**
     * uniform bytes inputs
     */
    private static final String[] FROM_UNIFORM_BYTES_INPUTS = new String[]{
        "5d1be09e3d0c82fc538112490e35701979d99e06ca3e2b5b54bffe8b4dc772c1" +
            "4d98b696a1bbfb5ca32c436cc61c16563790306c79eaca7705668b47dffe5bb6",
        "f116b34b8f17ceb56e8732a60d913dd10cce47a6d53bee9204be8b44f6678b27" +
            "0102a56902e2488c46120e9276cfe54638286b9e4b3cdb470b542d46c2068d38",
        "8422e1bbdaab52938b81fd602effb6f89110e1e57208ad12d9ad767e2e25510c" +
            "27140775f9337088b982d83d7fcf0b2fa1edffe51952cbe7365e95c86eaf325c",
        "ac22415129b61427bf464e17baee8db65940c233b98afce8d17c57beeb7876c2" +
            "150d15af1cb1fb824bbd14955f2b57d08d388aab431a391cfc33d5bafb5dbbaf",
        "165d697a1ef3d5cf3c38565beefcf88c0f282b8e7dbd28544c483432f1cec767" +
            "5debea8ebb4e5fe7d6f6e5db15f15587ac4d4d4a1de7191e0c1ca6664abcc413",
        "a836e6c9a9ca9f1e8d486273ad56a78c70cf18f0ce10abb1c7172ddd605d7fd2" +
            "979854f47ae1ccf204a33102095b4200e5befc0465accc263175485f0e17ea5c",
        "2cdc11eaeb95daf01189417cdddbf95952993aa9cb9c640eb5058d09702c7462" +
            "2c9965a697a3b345ec24ee56335b556e677b30e6f90ac77d781064f866a3c982",
    };

    /**
     * uniform bytes outputs
     */
    private static final String[] FROM_UNIFORM_BYTES_OUTPUTS = new String[]{
        "3066f82a 1a747d45 120d1740 f1435853 1a8f04bb ffe6a819 f86dfe50 f44a0a46",
        "f26e5b6f 7d362d2d 2a94c5d0 e7602cb4 773c95a2 e5c31a64 f133189f a76ed61b",
        "006ccd2a 9e6867e6 a2c5cea8 3d3302cc 9de128dd 2a9a57dd 8ee7b9d7 ffe02826",
        "f8f0c87c f237953c 5890aec3 99816900 5dae3eca 1fbb0454 8c635953 c817f92a",
        "ae81e7de df20a497 e10c304a 765c1767 a42d6e06 029758d2 d7e8ef7c c4c41179",
        "e2705652 ff9f5e44 d3e841bf 1c251cf7 dddb77d1 40870d1a b2ed64f1 a9ce8628",
        "80bd0726 2511cdde 4863f8a7 434cef69 6750681c b9510eea 557088f7 6d9e5065"};

    @Test
    public void testDecompressCompress() {
        CafeRistrettoPoint basePoint = RISTRETTO_GENERATOR_COMPRESSED.decompress();
        Assert.assertEquals(CafeConstants.RISTRETTO_GENERATOR, basePoint);
        Assert.assertEquals(RISTRETTO_GENERATOR_COMPRESSED, basePoint.compress());
    }

    @Test
    public void testAdd() {
        CafeRistrettoPoint point = CafeRistrettoPoint.IDENTITY;
        for (String generatorMultiple : GENERATOR_MULTIPLES) {
            CafeRistrettoCompressedPoint compressedRistrettoPoint = new CafeRistrettoCompressedPoint(Hex.decode(generatorMultiple));
            Assert.assertEquals(compressedRistrettoPoint, point.compress());
            Assert.assertEquals(point, compressedRistrettoPoint.decompress());
            point = point.add(CafeConstants.RISTRETTO_GENERATOR);
        }
    }

    @Test
    public void testSub() {
        CafeRistrettoPoint point = CafeConstants.RISTRETTO_GENERATOR.dbl().dbl().dbl().dbl();
        for (int i = GENERATOR_MULTIPLES.length - 1; i >= 0; i--) {
            point = point.sub(CafeConstants.RISTRETTO_GENERATOR);
            CafeRistrettoCompressedPoint compressedRistrettoPoint = new CafeRistrettoCompressedPoint(Hex.decode(GENERATOR_MULTIPLES[i]));
            Assert.assertEquals(compressedRistrettoPoint, point.compress());
            Assert.assertEquals(point, compressedRistrettoPoint.decompress());
        }
    }

    @Test
    public void testNeg() {
        Assert.assertEquals(
            CafeConstants.RISTRETTO_GENERATOR.neg(),
            CafeRistrettoPoint.IDENTITY.sub(CafeConstants.RISTRETTO_GENERATOR)
        );
    }

    @Test
    public void testDbl() {
        CafeRistrettoPoint expected = new CafeRistrettoCompressedPoint(Hex.decode(GENERATOR_MULTIPLES[2])).decompress();
        Assert.assertEquals(expected, CafeConstants.RISTRETTO_GENERATOR.dbl());
    }

    @Test
    public void testMul() {
        byte[] s = new byte[CafeRistrettoCompressedPoint.BYTE_SIZE];
        s[0] = 12;
        CafeScalar twelve = new CafeScalar(s);
        CafeRistrettoPoint expected = new CafeRistrettoCompressedPoint(Hex.decode(GENERATOR_MULTIPLES[12])).decompress();
        Assert.assertEquals(expected, CafeConstants.RISTRETTO_GENERATOR.mul(twelve));
    }

    @Test
    public void testInvalidEncoding() {
        for (String invalidEncoding : INVALID_ENCODINGS) {
            CafeRistrettoCompressedPoint s = new CafeRistrettoCompressedPoint(Hex.decode(invalidEncoding));
            try {
                s.decompress();
                Assert.fail("Invalid encoding should have been rejected");
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    @Test
    public void testFromUniformBytes() {
        Assert.assertEquals(FROM_UNIFORM_BYTES_OUTPUTS.length, FROM_UNIFORM_BYTES_INPUTS.length);
        for (int i = 0; i < FROM_UNIFORM_BYTES_INPUTS.length; i++) {
            CafeRistrettoPoint point = CafeRistrettoPoint.fromUniformBytes(Hex.decode(FROM_UNIFORM_BYTES_INPUTS[i]));
            Assert.assertEquals(point.compress(), new CafeRistrettoCompressedPoint(Hex.decode(FROM_UNIFORM_BYTES_OUTPUTS[i])));
        }
    }
}
