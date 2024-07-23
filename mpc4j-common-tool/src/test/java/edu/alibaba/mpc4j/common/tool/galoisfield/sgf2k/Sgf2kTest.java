package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * tests for Subfield GF2K.
 *
 * @author Weiran Liu
 * @date 2024/6/3
 */
@RunWith(Parameterized.class)
public class Sgf2kTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 40;
    /**
     * parallel num
     */
    private static final int PARALLEL_NUM = 100;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Subfield GF2K with t = 1
        Sgf2k001 sgf2k001 = new Sgf2k001(EnvType.STANDARD);
        configurations.add(new Object[]{sgf2k001.toString(), sgf2k001, 1});

        // Subfield GF2K with t = 128
        Sgf2k128 sgf2k128 = new Sgf2k128(EnvType.STANDARD);
        configurations.add(new Object[]{sgf2k128.toString(), sgf2k128, 128});

        for (int subfieldL : new int[]{2, 4, 8, 16, 32, 64}) {
            // NTL Subfield GF2K
            NtlSubSgf2k ntlSgf2k = new NtlSubSgf2k(EnvType.STANDARD, subfieldL);
            configurations.add(new Object[]{ntlSgf2k.toString(), ntlSgf2k, subfieldL});
            // Rings Subfield GF2K
            RingsSubSgf2k ringsSgf2k = new RingsSubSgf2k(EnvType.STANDARD, subfieldL);
            configurations.add(new Object[]{ringsSgf2k.toString(), ringsSgf2k, subfieldL});
        }

        return configurations;
    }

    /**
     * Subfield GF2K
     */
    private final Sgf2k sgf2k;
    /**
     * subfield L
     */
    private final int subfieldL;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * composite field element
     */
    private final byte[] compositeFieldElement = new byte[]{
        (byte) 0b11111111,

        (byte) 0b10000000,
        (byte) 0b01000000,
        (byte) 0b00100000,
        (byte) 0b00010000,
        (byte) 0b00001000,
        (byte) 0b00000100,
        (byte) 0b00000010,
        (byte) 0b00000001,

        (byte) 0b11000000,
        (byte) 0b01100000,
        (byte) 0b00110000,
        (byte) 0b00011000,
        (byte) 0b00001100,
        (byte) 0b00000110,
        (byte) 0b00000011,
    };

    public Sgf2kTest(String name, Sgf2k sgf2k, int subfieldL) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.sgf2k = sgf2k;
        secureRandom = new SecureRandom();
        this.subfieldL = subfieldL;
        Assert.assertEquals(128, sgf2k.getL());
        Assert.assertEquals(16, sgf2k.getByteL());
        Assert.assertEquals(subfieldL, sgf2k.getSubfieldL());
        Assert.assertEquals(CommonUtils.getByteLength(subfieldL), sgf2k.getSubfieldByteL());
        Assert.assertEquals(128 / subfieldL, sgf2k.getR());
    }

    @Test
    public void testComposition() {
        switch (subfieldL) {
            case 1:
                testComposition001();
                break;
            case 2:
                testComposition002();
                break;
            case 4:
                testComposition004();
                break;
            case 8:
                testComposition008();
                break;
            case 16:
                testComposition016();
                break;
            case 32:
                testComposition032();
                break;
            case 64:
                testComposition064();
                break;
            case 128:
                testComposition128();
                break;
            default:
                throw new IllegalStateException("Invalid subfield L, must be ∈ {1, 2, 4, 8, 16, 32, 64, 128}: " + subfieldL);

        }
    }

    private void testComposition001() {
        byte[][] subfieldElements = new byte[][]{
            // 0b00000011
            new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00000110
            new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00001100
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00011000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001},
            new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00110000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b01100000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000000},
            // 0b11000000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000001},
            // 0b00000001
            new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00000010
            new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00000100
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00001000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00010000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b00100000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            // 0b01000000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000},
            // 0b10000000
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001},
            // 0b11111111
            new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000001},
            new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000001},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition002() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000010}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000001}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000011}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000011},
            new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000010}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000010},
            new byte[]{0b00000011}, new byte[]{0b00000011}, new byte[]{0b00000011}, new byte[]{0b00000011},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition004() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{0b00000011}, new byte[]{0b00000000},
            new byte[]{0b00000110}, new byte[]{0b00000000},
            new byte[]{0b00001100}, new byte[]{0b00000000},
            new byte[]{0b00001000}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000011},
            new byte[]{0b00000000}, new byte[]{0b00000110},
            new byte[]{0b00000000}, new byte[]{0b00001100},

            new byte[]{0b00000001}, new byte[]{0b00000000},
            new byte[]{0b00000010}, new byte[]{0b00000000},
            new byte[]{0b00000100}, new byte[]{0b00000000},
            new byte[]{0b00001000}, new byte[]{0b00000000},
            new byte[]{0b00000000}, new byte[]{0b00000001},
            new byte[]{0b00000000}, new byte[]{0b00000010},
            new byte[]{0b00000000}, new byte[]{0b00000100},
            new byte[]{0b00000000}, new byte[]{0b00001000},

            new byte[]{0b00001111}, new byte[]{0b00001111},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition008() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{(byte) 0b00000011},
            new byte[]{(byte) 0b00000110},
            new byte[]{(byte) 0b00001100},
            new byte[]{(byte) 0b00011000},
            new byte[]{(byte) 0b00110000},
            new byte[]{(byte) 0b01100000},
            new byte[]{(byte) 0b11000000},

            new byte[]{(byte) 0b00000001},
            new byte[]{(byte) 0b00000010},
            new byte[]{(byte) 0b00000100},
            new byte[]{(byte) 0b00001000},
            new byte[]{(byte) 0b00010000},
            new byte[]{(byte) 0b00100000},
            new byte[]{(byte) 0b01000000},
            new byte[]{(byte) 0b10000000},

            new byte[]{(byte) 0b11111111},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition016() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{(byte) 0b00000110, (byte) 0b00000011},
            new byte[]{(byte) 0b00011000, (byte) 0b00001100},
            new byte[]{(byte) 0b01100000, (byte) 0b00110000},
            new byte[]{(byte) 0b00000001, (byte) 0b11000000},

            new byte[]{(byte) 0b00000100, (byte) 0b00000010},
            new byte[]{(byte) 0b00010000, (byte) 0b00001000},
            new byte[]{(byte) 0b01000000, (byte) 0b00100000},
            new byte[]{(byte) 0b11111111, (byte) 0b10000000},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition032() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{(byte) 0b00011000, (byte) 0b00001100, (byte) 0b00000110, (byte) 0b00000011},
            new byte[]{(byte) 0b00000001, (byte) 0b11000000, (byte) 0b01100000, (byte) 0b00110000},

            new byte[]{(byte) 0b00010000, (byte) 0b00001000, (byte) 0b00000100, (byte) 0b00000010},
            new byte[]{(byte) 0b11111111, (byte) 0b10000000, (byte) 0b01000000, (byte) 0b00100000},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition064() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{
                (byte) 0b00000001, (byte) 0b11000000, (byte) 0b01100000, (byte) 0b00110000,
                (byte) 0b00011000, (byte) 0b00001100, (byte) 0b00000110, (byte) 0b00000011},

            new byte[]{
                (byte) 0b11111111, (byte) 0b10000000, (byte) 0b01000000, (byte) 0b00100000,
                (byte) 0b00010000, (byte) 0b00001000, (byte) 0b00000100, (byte) 0b00000010},
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    private void testComposition128() {
        byte[][] subfieldElements = new byte[][]{
            new byte[]{
                (byte) 0b11111111, (byte) 0b10000000, (byte) 0b01000000, (byte) 0b00100000,
                (byte) 0b00010000, (byte) 0b00001000, (byte) 0b00000100, (byte) 0b00000010,
                (byte) 0b00000001, (byte) 0b11000000, (byte) 0b01100000, (byte) 0b00110000,
                (byte) 0b00011000, (byte) 0b00001100, (byte) 0b00000110, (byte) 0b00000011
            },
        };
        Assert.assertArrayEquals(subfieldElements, sgf2k.decomposite(compositeFieldElement));
        Assert.assertArrayEquals(compositeFieldElement, sgf2k.composite(subfieldElements));
    }

    @Test
    public void testAddSub() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] p = sgf2k.createRandom(secureRandom);
            byte[] q = sgf2k.createRandom(secureRandom);
            // p + p = 0
            Assert.assertTrue(sgf2k.isZero(sgf2k.add(p, p)));
            // -p = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.neg(p)));
            // p + (-p) = 0
            Assert.assertTrue(sgf2k.isZero(sgf2k.add(p, sgf2k.neg(p))));
            // p - p = 0
            Assert.assertTrue(sgf2k.isZero(sgf2k.sub(p, p)));
            // p + q - q = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.sub(sgf2k.add(p, q), q)));
            // p - q + q = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.add(sgf2k.sub(p, q), q)));
        }
    }

    @Test
    public void testAddiSubi() {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] p = sgf2k.createRandom(secureRandom);
            byte[] q = sgf2k.createRandom(secureRandom);
            // p + p = 0
            byte[] copyP = BytesUtils.clone(p);
            sgf2k.addi(copyP, p);
            Assert.assertTrue(sgf2k.isZero(copyP));
            // -p = p
            copyP = BytesUtils.clone(p);
            sgf2k.negi(p);
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
            // p + (-p) = 0
            copyP = BytesUtils.clone(p);
            sgf2k.negi(copyP);
            sgf2k.addi(copyP, p);
            Assert.assertTrue(sgf2k.isZero(copyP));
            // p - p = 0
            copyP = BytesUtils.clone(p);
            sgf2k.subi(copyP, p);
            Assert.assertTrue(sgf2k.isZero(copyP));
            // p + q - q = p
            copyP = BytesUtils.clone(p);
            sgf2k.addi(copyP, q);
            sgf2k.subi(copyP, q);
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
            // p - q + q = p
            copyP = BytesUtils.clone(p);
            sgf2k.subi(copyP, q);
            sgf2k.addi(copyP, q);
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
        }
    }

    @Test
    public void testMixMulDiv() {
        Gf2e subfield = sgf2k.getSubfield();
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] p = sgf2k.createNonZeroRandom(secureRandom);
            byte[] q = sgf2k.createNonZeroRandom(secureRandom);
            // p * 0 = 0
            byte[] r = sgf2k.mul(p, sgf2k.createZero());
            Assert.assertTrue(sgf2k.isZero(r));
            // p * 1 = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.mul(p, sgf2k.createOne())));
            // p * p^-1 = 1
            Assert.assertTrue(sgf2k.isOne(sgf2k.mul(p, sgf2k.inv(p))));
            // p / p = 1
            Assert.assertTrue(sgf2k.isOne(sgf2k.div(p, p)));
            // p * q / q = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.div(sgf2k.mul(p, q), q)));
            // p / q * q = p
            Assert.assertTrue(sgf2k.isEqual(p, sgf2k.mul(sgf2k.div(p, q), q)));
            // mix multiplication is equivalent to field multiplication with constant coefficient
            byte[] subfieldElement = subfield.createNonZeroRandom(secureRandom);
            byte[] inverseSubfieldElement = subfield.inv(subfieldElement);
            byte[] mixMulResult = sgf2k.mixMul(subfieldElement, p);
            byte[] mulResult = sgf2k.mul(p, sgf2k.extend(subfieldElement));
            Assert.assertArrayEquals(mulResult, mixMulResult);
            // mix multiplication with inverse is equivalent to field multiplication with constant inverse coefficient
            byte[] mixDivResult = sgf2k.mixMul(inverseSubfieldElement, p);
            byte[] divResult = sgf2k.div(p, sgf2k.extend(subfieldElement));
            Assert.assertArrayEquals(divResult, mixDivResult);
        }
    }

    @Test
    public void testMixMuliDivi() {
        Gf2e subfield = sgf2k.getSubfield();
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] p = sgf2k.createNonZeroRandom(secureRandom);
            byte[] q = sgf2k.createNonZeroRandom(secureRandom);
            // p * 0 = 0
            byte[] copyP = BytesUtils.clone(p);
            sgf2k.muli(copyP, sgf2k.createZero());
            Assert.assertTrue(sgf2k.isZero(copyP));
            // p * 1 = p
            copyP = BytesUtils.clone(p);
            sgf2k.muli(copyP, sgf2k.createOne());
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
            // p * p^-1 = 1
            copyP = BytesUtils.clone(p);
            sgf2k.invi(copyP);
            sgf2k.muli(copyP, p);
            Assert.assertTrue(sgf2k.isOne(copyP));
            // p / p = 1
            copyP = BytesUtils.clone(p);
            sgf2k.divi(copyP, p);
            Assert.assertTrue(sgf2k.isOne(copyP));
            // p * q / q = p
            copyP = BytesUtils.clone(p);
            sgf2k.muli(copyP, q);
            sgf2k.divi(copyP, q);
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
            // p / q * q = p
            copyP = BytesUtils.clone(p);
            sgf2k.divi(copyP, q);
            sgf2k.muli(copyP, q);
            Assert.assertTrue(sgf2k.isEqual(p, copyP));
            // mix multiplication is equivalent to field multiplication with constant coefficient
            copyP = BytesUtils.clone(p);
            byte[] subfieldElement = subfield.createNonZeroRandom(secureRandom);
            byte[] inverseSubfieldElement = subfield.inv(subfieldElement);
            byte[] mixMulResult = sgf2k.mixMul(subfieldElement, p);
            sgf2k.muli(copyP, sgf2k.extend(subfieldElement));
            Assert.assertArrayEquals(mixMulResult, copyP);
            // mix multiplication with inverse is equivalent to field multiplication with constant inverse coefficient
            copyP = BytesUtils.clone(p);
            byte[] mixDivResult = sgf2k.mixMul(inverseSubfieldElement, p);
            sgf2k.divi(copyP, sgf2k.extend(subfieldElement));
            Assert.assertArrayEquals(mixDivResult, copyP);
        }
    }

    @Test
    public void testPow() {
        Gf2e subfield = sgf2k.getSubfield();
        for (int i = 0; i < RANDOM_ROUND; i++) {
            for (int h = 0; h < sgf2k.getR(); h++) {
                // mixPow = extend + fieldPow
                byte[] p = subfield.createRandom(secureRandom);
                byte[] mixPow = sgf2k.mixPow(p, h);
                byte[] fieldPow = sgf2k.fieldPow(sgf2k.extend(p), h);
                Assert.assertArrayEquals(mixPow, fieldPow);
            }
        }
    }

    @Test
    public void testParallelMulDiv() {
        byte[] p = sgf2k.createNonZeroRandom(secureRandom);
        byte[] q = sgf2k.createNonZeroRandom(secureRandom);
        Set<ByteBuffer> set = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(i -> sgf2k.mul(p, q))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, set.size());
    }

    @Test
    public void testParallelMuliDivi() {
        byte[] p = sgf2k.createNonZeroRandom(secureRandom);
        byte[] q = sgf2k.createNonZeroRandom(secureRandom);
        Set<ByteBuffer> set = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(i -> {
                byte[] copyP = BytesUtils.clone(p);
                return sgf2k.mul(copyP, q);
            })
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, set.size());
    }
}
