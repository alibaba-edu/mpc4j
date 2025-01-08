package edu.alibaba.mpc4j.common.tool.galoisfield;

import org.junit.Assert;
import org.junit.Test;

/**
 * Z3 utilities tests.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class Z3UtilsTest {

    @Test
    public void testCompressToByteArray() {
        // empty input
        Assert.assertArrayEquals(new byte[0], Z3Utils.compressToByteArray(new byte[0]));
        // 4 elements into 1 byte
        Assert.assertArrayEquals(
            new byte[]{(byte) 0b00_01_10_00,},
            Z3Utils.compressToByteArray(new byte[]{0b00, 0b01, 0b10, 0b00,})
        );
        // 3 elements into 1 byte
        Assert.assertArrayEquals(
            new byte[]{(byte) 0b00_01_10_00,},
            Z3Utils.compressToByteArray(new byte[]{0b00, 0b01, 0b10,})
        );
        // 5 elements into 2 bytes
        Assert.assertArrayEquals(
            new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_00_00_00,},
            Z3Utils.compressToByteArray(new byte[]{0b00, 0b01, 0b10, 0b00, 0b10,})
        );
        // 8 elements into 2 bytes
        Assert.assertArrayEquals(
            new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_00_00,},
            Z3Utils.compressToByteArray(new byte[]{0b00, 0b01, 0b10, 0b00, 0b10, 0b01, 0b00, 0b00,})
        );
        // 9 elements into 3 bytes
        Assert.assertArrayEquals(
            new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_00_00, (byte) 0b01_00_00_00,},
            Z3Utils.compressToByteArray(new byte[]{0b00, 0b01, 0b10, 0b00, 0b10, 0b01, 0b00, 0b00, 0b01,})
        );
    }

    @Test
    public void testDecompressFromByteArray() {
        // empty input
        Assert.assertArrayEquals(new byte[0], Z3Utils.decompressFromByteArray(new byte[0], 0));
        // 0 byte into 1 element
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromByteArray(new byte[0], 1)
        );
        // 1 byte into 0 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 0),
            new byte[0]
        );
        // 1 byte into 1 element
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 1),
            new byte[]{0b00}
        );
        // 1 byte into 2 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 2),
            new byte[]{0b00, 0b01,}
        );
        // 1 byte into 3 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 3),
            new byte[]{0b00, 0b01, 0b10,}
        );
        // 1 byte into 4 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 4),
            new byte[]{0b00, 0b01, 0b10, 0b00,}
        );
        // 1 byte into 5 elements
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00,}, 5)
        );
        // 2 bytes into 0 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 0),
            new byte[0]
        );
        // 2 bytes into 1 element
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 1),
            new byte[]{0b00}
        );
        // 2 bytes into 4 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 4),
            new byte[]{0b00, 0b01, 0b10, 0b00,}
        );
        // 2 bytes into 5 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 5),
            new byte[]{0b00, 0b01, 0b10, 0b00, 0b10}
        );
        // 2 bytes into 8 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 8),
            new byte[]{0b00, 0b01, 0b10, 0b00, 0b10, 0b01, 0b01, 0b10}
        );
        // 2 bytes into 9 elements
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromByteArray(new byte[]{(byte) 0b00_01_10_00, (byte) 0b10_01_01_10}, 9)
        );
    }

    @Test
    public void testCompressToLongArray() {
        // empty input
        Assert.assertArrayEquals(new long[0], Z3Utils.compressToLongArray(new byte[0]));
        // 32 elements into 1 long
        Assert.assertArrayEquals(
            new long[]{0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,},
            Z3Utils.compressToLongArray(new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
            })
        );
        // 31 elements into 1 long
        Assert.assertArrayEquals(
            new long[]{0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,},
            Z3Utils.compressToLongArray(new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10,
            })
        );
        // 30 elements into 1 long
        Assert.assertArrayEquals(
            new long[]{0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_00_00L,},
            Z3Utils.compressToLongArray(new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01,
            })
        );
        // 33 elements into 2 longs
        Assert.assertArrayEquals(
            new long[]{
                0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                0b00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00L,
            },
            Z3Utils.compressToLongArray(new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00,
            })
        );
        // 34 elements into 2 longs
        Assert.assertArrayEquals(
            new long[]{
                0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                0b00_01_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00_00L,
            },
            Z3Utils.compressToLongArray(new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01,
            })
        );
    }

    @Test
    public void testDecompressFromLongArray() {
        // empty input
        Assert.assertArrayEquals(new byte[0], Z3Utils.decompressFromLongArray(new long[0], 0));
        // 0 long into 1 element
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromLongArray(new long[0], 1)
        );
        // 1 long into 0 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                0
            ),
            new byte[0]
        );
        // 1 long into 1 element
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                1
            ),
            new byte[]{0b00,}
        );
        // 1 long into 2 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                2
            ),
            new byte[]{0b00, 0b01}
        );
        // 1 long into 3 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                3
            ),
            new byte[]{0b00, 0b01, 0b10}
        );
        // 1 long into 32 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                32
            ),
            new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
            }
        );
        // 1 long into 33 elements
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                33
            )
        );
        // 2 longs into 0 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                0
            ),
            new byte[0]
        );
        // 2 longs into 1 element
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                1
            ),
            new byte[]{
                0b00,
            }
        );
        // 2 longs into 4 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                4
            ),
            new byte[]{
                0b00, 0b01, 0b10, 0b00,
            }
        );
        // 2 longs into 63 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                63
            ),
            new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10,
            }
        );
        // 2 longs into 31 elements
        Assert.assertArrayEquals(
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                64
            ),
            new byte[]{
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
                0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00, 0b01, 0b10, 0b00,
            }
        );
        // 2 longs into 65 elements
        Assert.assertThrows(AssertionError.class, () ->
            Z3Utils.decompressFromLongArray(
                new long[]{
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                    0b00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00_00_01_10_00_01_10_00_01_10_00_01_10_00_01_10_00L,
                },
                65
            )
        );
    }

    @Test
    public void testUncheckByteAdd() {
        // check for each 2 bits
        for (int shift = 0; shift < Byte.SIZE; shift += 2) {
            for (byte a = 0; a < 0b11; a++) {
                for (byte b = 0; b < 0b11; b++) {
                    byte expect = (byte) (a + b);
                    expect = (expect >= 0b11) ? (byte) (expect - 0b11) : expect;
                    expect <<= shift;
                    Assert.assertEquals(
                        a + " + " + b + " is not correct",
                        expect, Z3Utils.uncheckCompressByteAdd((byte) (a << shift), (byte) (b << shift))
                    );
                }
            }
        }
    }

    @Test
    public void testUncheckByteNeg() {
        // check for each 2 bits
        for (int shift = 0; shift < Byte.SIZE; shift += 2) {
            for (byte a = 0; a < 0b11; a++) {
                byte expect = (byte) (3 - a);
                expect = (expect == 0b11) ? 0 : expect;
                expect <<= shift;
                Assert.assertEquals(
                    "-" + a + " is not correct",
                    expect, Z3Utils.uncheckCompressByteNeg((byte) (a << shift))
                );
            }
        }
    }

    @Test
    public void testUncheckLongAdd() {
        // check for each two bits
        for (int shift = 0; shift < Long.SIZE - 2; shift += 2) {
            for (long a = 0; a < 0b11; a++) {
                for (long b = 0; b < 0b11; b++) {
                    long expect = a + b;
                    expect = (expect >= 0b11) ? (expect - 0b11) : expect;
                    expect <<= shift;
                    Assert.assertEquals(a + " + " + b + " is not correct", expect, Z3Utils.uncheckCompressLongAdd(a << shift, b << shift));
                }
            }
        }
    }

    @Test
    public void testUncheckLongNeg() {
        // check for each two bits
        for (int shift = 0; shift < Long.SIZE - 2; shift += 2) {
            for (long a = 0; a < 0b11; a++) {
                long expect = 3 - a;
                expect = (expect == 0b11) ? 0 : expect;
                expect <<= shift;
                Assert.assertEquals("-" + a + " is not correct", expect, Z3Utils.uncheckCompressLongNeg(a << shift));
            }
        }
    }
}
