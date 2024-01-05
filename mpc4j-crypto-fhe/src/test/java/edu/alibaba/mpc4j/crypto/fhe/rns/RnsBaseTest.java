package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * RnsBase unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/rns.cpp
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/17
 */
public class RnsBaseTest {

    @Test
    public void testCreate() {
        // throw exception
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{0}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{0, 3}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{2, 2}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{2, 3, 4}));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RnsBase(new long[]{3, 4, 5, 6}));
        // create new instance successfully
        new RnsBase(new long[]{3, 4, 5, 7});
        new RnsBase(new long[]{2});
        new RnsBase(new long[]{3});
        new RnsBase(new long[]{4});
    }

    @Test
    public void testArrayAccess() {
        RnsBase rnsBase = new RnsBase(new long[]{2});
        Assert.assertEquals(1, rnsBase.size());
        Assert.assertEquals(new Modulus(2), rnsBase.getBase(0));
        RnsBase finalRnsBase = rnsBase;
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> finalRnsBase.getBase(1));

        rnsBase = new RnsBase(new long[]{2, 3, 5});
        Assert.assertEquals(3, rnsBase.size());
        Assert.assertEquals(new Modulus(2), rnsBase.getBase(0));
        Assert.assertEquals(new Modulus(3), rnsBase.getBase(1));
        Assert.assertEquals(new Modulus(5), rnsBase.getBase(2));
        RnsBase finalRnsBase1 = rnsBase;
        Assert.assertThrows(ArrayIndexOutOfBoundsException.class, () -> finalRnsBase1.getBase(3));
    }

    @Test
    public void testCopy() {
        RnsBase rnsBase = new RnsBase(new long[]{3, 4});
        RnsBase rnsBase1 = new RnsBase(rnsBase);
        Assert.assertEquals(rnsBase.size(), rnsBase1.size());
        Assert.assertEquals(rnsBase.getBase(0), rnsBase1.getBase(0));
        Assert.assertEquals(rnsBase.getBase(1), rnsBase1.getBase(1));

        Assert.assertArrayEquals(rnsBase.getBaseProd(), rnsBase1.getBaseProd());
        Assert.assertArrayEquals(rnsBase.getInvPuncturedProdModBaseArray(), rnsBase1.getInvPuncturedProdModBaseArray());
        Assert.assertTrue(Arrays.deepEquals(rnsBase.getPuncturedProdArray(), rnsBase1.getPuncturedProdArray()));
    }

    @Test
    public void testContains() {
        RnsBase rnsBase = new RnsBase(new long[]{2, 3, 5, 13});
        Assert.assertTrue(rnsBase.contains(2));
        Assert.assertTrue(rnsBase.contains(3));
        Assert.assertTrue(rnsBase.contains(5));
        Assert.assertTrue(rnsBase.contains(13));

        Assert.assertFalse(rnsBase.contains(7));
        Assert.assertFalse(rnsBase.contains(4));
        Assert.assertFalse(rnsBase.contains(0));
    }

    @Test
    public void testIsSubBaseOf() {
        RnsBase base = new RnsBase(new long[]{2});
        RnsBase base2 = new RnsBase(new long[]{2});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSubBaseOf(base));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertTrue(base.isSuperBaseOf(base2));

        base = new RnsBase(new long[]{2});
        base2 = new RnsBase(new long[]{2, 3});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));

        base = new RnsBase(new long[]{3, 13, 7});
        base2 = new RnsBase(new long[]{2, 3, 5, 7, 13, 19});
        Assert.assertTrue(base.isSubBaseOf(base2));
        Assert.assertTrue(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));

        base = new RnsBase(new long[]{3, 13, 7, 23});
        base2 = new RnsBase(new long[]{2, 3, 5, 7, 13, 19});
        Assert.assertFalse(base.isSubBaseOf(base2));
        Assert.assertFalse(base2.isSuperBaseOf(base));
        Assert.assertFalse(base.isSuperBaseOf(base2));
        Assert.assertFalse(base2.isSubBaseOf(base));
    }


    @Test
    public void testExtend() {
        RnsBase base = new RnsBase(new long[]{3});
        RnsBase base2 = base.extend(5);
        Assert.assertEquals(2, base2.size());
        Assert.assertEquals(base.getBase(0), base2.getBase(0));
        Assert.assertEquals(new Modulus(5), base2.getBase(1));

        RnsBase base3 = base2.extend(7);
        Assert.assertEquals(3, base3.size());
        Assert.assertEquals(base2.getBase(0), base3.getBase(0));
        Assert.assertEquals(base2.getBase(1), base3.getBase(1));
        Assert.assertEquals(new Modulus(7), base3.getBase(2));

        Assert.assertThrows(AssertionError.class, () -> base3.extend(0));
        Assert.assertThrows(IllegalArgumentException.class, () -> base3.extend(14));

        RnsBase base4 = new RnsBase(new long[]{3, 4, 5});
        RnsBase base5 = new RnsBase(new long[]{7, 11, 13, 17});
        RnsBase base6 = base4.extend(base5);

        Assert.assertEquals(7, base6.size());
        Assert.assertEquals(new Modulus(3), base6.getBase(0));
        Assert.assertEquals(new Modulus(4), base6.getBase(1));
        Assert.assertEquals(new Modulus(5), base6.getBase(2));
        Assert.assertEquals(new Modulus(7), base6.getBase(3));
        Assert.assertEquals(new Modulus(11), base6.getBase(4));
        Assert.assertEquals(new Modulus(13), base6.getBase(5));
        Assert.assertEquals(new Modulus(17), base6.getBase(6));

        Assert.assertThrows(IllegalArgumentException.class, () -> base4.extend(new RnsBase(new long[]{7, 10, 11})));
    }

    @Test
    public void testDrop() {
        RnsBase base = new RnsBase(new long[]{3, 5, 7, 11});

        RnsBase base2 = base.drop();
        Assert.assertEquals(3, base2.size());
        for (int i = 0; i < base2.size(); i++) {
            Assert.assertEquals(base.getBase(i), base2.getBase(i));
        }

        RnsBase base3 = base2.drop().drop();
        Assert.assertEquals(1, base3.size());
        Assert.assertEquals(base.getBase(0), base3.getBase(0));
        // cannot drop size = 1 's RnsBase
        Assert.assertThrows(RuntimeException.class, base3::drop);
        Assert.assertThrows(RuntimeException.class, () -> base3.drop(3));
        Assert.assertThrows(RuntimeException.class, () -> base3.drop(5));

        RnsBase base4 = base.drop(5);
        Assert.assertEquals(3, base4.size());
        Assert.assertEquals(base.getBase(0), base4.getBase(0));
        Assert.assertEquals(base.getBase(2), base4.getBase(1));
        Assert.assertEquals(base.getBase(3), base4.getBase(2));

        Assert.assertThrows(IllegalArgumentException.class, () -> base4.drop(13));
        Assert.assertThrows(IllegalArgumentException.class, () -> base4.drop(0));
        base4.drop(7).drop(11);
        Assert.assertThrows(RuntimeException.class, () -> base4.drop(7).drop(11).drop(3));
    }

    @Test
    public void testComposeDecompose() {
        RnsBase base = new RnsBase(new long[]{2});
        testRns1(base, new long[]{0}, new long[]{0});
        testRns1(base, new long[]{1}, new long[]{1});

        base = new RnsBase(new long[]{5});
        testRns1(base, new long[]{0}, new long[]{0});
        testRns1(base, new long[]{1}, new long[]{1});
        testRns1(base, new long[]{2}, new long[]{2});
        testRns1(base, new long[]{3}, new long[]{3});
        testRns1(base, new long[]{4}, new long[]{4});

        base = new RnsBase(new long[]{3, 5});
        testRns1(base, new long[]{0, 0}, new long[]{0, 0});
        testRns1(base, new long[]{1, 0}, new long[]{1, 1});
        testRns1(base, new long[]{2, 0}, new long[]{2, 2});
        testRns1(base, new long[]{3, 0}, new long[]{0, 3});
        testRns1(base, new long[]{4, 0}, new long[]{1, 4});
        testRns1(base, new long[]{5, 0}, new long[]{2, 0});
        testRns1(base, new long[]{8, 0}, new long[]{2, 3});
        testRns1(base, new long[]{12, 0}, new long[]{0, 2});
        testRns1(base, new long[]{14, 0}, new long[]{2, 4});

        base = new RnsBase(new long[]{2, 3, 5});
        testRns1(base, new long[]{0, 0, 0}, new long[]{0, 0, 0});
        testRns1(base, new long[]{1, 0, 0}, new long[]{1, 1, 1});
        testRns1(base, new long[]{2, 0, 0}, new long[]{0, 2, 2});
        testRns1(base, new long[]{3, 0, 0}, new long[]{1, 0, 3});
        testRns1(base, new long[]{4, 0, 0}, new long[]{0, 1, 4});
        testRns1(base, new long[]{5, 0, 0}, new long[]{1, 2, 0});
        testRns1(base, new long[]{10, 0, 0}, new long[]{0, 1, 0});
        testRns1(base, new long[]{11, 0, 0}, new long[]{1, 2, 1});
        testRns1(base, new long[]{16, 0, 0}, new long[]{0, 1, 1});
        testRns1(base, new long[]{27, 0, 0}, new long[]{1, 0, 2});
        testRns1(base, new long[]{29, 0, 0}, new long[]{1, 2, 4});

        base = new RnsBase(new long[]{13, 37, 53, 97});
        testRns1(base, new long[]{0, 0, 0, 0}, new long[]{0, 0, 0, 0});
        testRns1(base, new long[]{1, 0, 0, 0}, new long[]{1, 1, 1, 1});
        testRns1(base, new long[]{2, 0, 0, 0}, new long[]{2, 2, 2, 2});
        testRns1(base, new long[]{12, 0, 0, 0}, new long[]{12, 12, 12, 12});
        testRns1(base, new long[]{321, 0, 0, 0}, new long[]{9, 25, 3, 30});

        // large number
        Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 4);
        long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL, 0xCCCCCCCCCCL, 0xDDDDDDDDDDL};
        RnsBase base1 = new RnsBase(primes);

        testRns1(base1, inValues, new long[]{
            UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[0]),
            UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[1]),
            UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[2]),
            UintArithmeticSmallMod.moduloUint(inValues, inValues.length, primes[3])
        });
    }

    @Test
    public void testComposeDecomposeArray() {
        {
            RnsBase base = new RnsBase(new long[]{2});
            testRns2(base, 1, new long[]{0}, new long[]{0});
            testRns2(base, 1, new long[]{1}, new long[]{1});
        }

        {
            RnsBase base = new RnsBase(new long[]{5});
            testRns2(base, 3, new long[]{0, 1, 2}, new long[]{0, 1, 2});
        }

        {
            RnsBase base = new RnsBase(new long[]{3, 5});
            testRns2(base, 1, new long[]{0, 0}, new long[]{0, 0});
            testRns2(base, 1, new long[]{2, 0}, new long[]{2, 2});
            testRns2(base, 1, new long[]{7, 0}, new long[]{1, 2});

            testRns2(base, 2, new long[]{0, 0, 0, 0}, new long[]{0, 0, 0, 0});
            testRns2(base, 2, new long[]{1, 0, 2, 0}, new long[]{1, 2, 1, 2});
            testRns2(base, 2, new long[]{7, 0, 8, 0}, new long[]{1, 2, 2, 3});
        }

        {
            RnsBase base = new RnsBase(new long[]{3, 5, 7});
            testRns2(base, 1, new long[]{0, 0, 0}, new long[]{0, 0, 0});
            testRns2(base, 1, new long[]{2, 0, 0}, new long[]{2, 2, 2});
            testRns2(base, 1, new long[]{7, 0, 0}, new long[]{1, 2, 0});
            testRns2(base, 2, new long[]{0, 0, 0, 0, 0, 0}, new long[]{0, 0, 0, 0, 0, 0});
            testRns2(base, 2, new long[]{1, 0, 0, 2, 0, 0}, new long[]{1, 2, 1, 2, 1, 2});
            testRns2(base, 2, new long[]{7, 0, 0, 8, 0, 0}, new long[]{1, 2, 2, 3, 0, 1});
            testRns2(base, 3, new long[]{7, 0, 0, 8, 0, 0, 9, 0, 0}, new long[]{1, 2, 0, 2, 3, 4, 0, 1, 2});
        }

        {
            // large number
            Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 2);
            long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL,
                0xCCCCCCCCCCL, 0xDDDDDDDDDDL,
                0xEEEEEEEEEEL, 0xFFFFFFFFFFL
            };
            long[][] inValuesT = new long[][]{
                {0xAAAAAAAAAAAL, 0xBBBBBBBBBBL},
                {0xCCCCCCCCCCL, 0xDDDDDDDDDDL},
                {0xEEEEEEEEEEL, 0xFFFFFFFFFFL},
            };
            RnsBase base = new RnsBase(primes);
            testRns2(base, 3, inValues, new long[]{
                UintArithmeticSmallMod.moduloUint(inValuesT[0], 2, primes[0]),
                UintArithmeticSmallMod.moduloUint(inValuesT[1], 2, primes[0]),
                UintArithmeticSmallMod.moduloUint(inValuesT[2], 2, primes[0]),

                UintArithmeticSmallMod.moduloUint(inValuesT[0], 2, primes[1]),
                UintArithmeticSmallMod.moduloUint(inValuesT[1], 2, primes[1]),
                UintArithmeticSmallMod.moduloUint(inValuesT[2], 2, primes[1])
            });
        }

        {
            // large number2
            Modulus[] primes = Numth.getPrimes(1024 * 2, 60, 2);
            long[] inValues = new long[]{0xAAAAAAAAAAAL, 0xBBBBBBBBBBL,
                0xCCCCCCCCCCL, 0xDDDDDDDDDDL,
                0xEEEEEEEEEEL, 0xFFFFFFFFFFL};
            RnsBase base = new RnsBase(primes);
            testRns2(base, 3, inValues, new long[]{
                UintArithmeticSmallMod.moduloUint(inValues, 0, 2, primes[0]),
                UintArithmeticSmallMod.moduloUint(inValues, 2, 2, primes[0]),
                UintArithmeticSmallMod.moduloUint(inValues, 4, 2, primes[0]),

                UintArithmeticSmallMod.moduloUint(inValues, 0, 2, primes[1]),
                UintArithmeticSmallMod.moduloUint(inValues, 2, 2, primes[1]),
                UintArithmeticSmallMod.moduloUint(inValues, 4, 2, primes[1])
            });
        }
    }

    private void testRns1(RnsBase base, long[] in, long[] out) {
        long[] inCopy = Arrays.copyOf(in, in.length);
        base.decompose(inCopy);
        Assert.assertArrayEquals(inCopy, out);

        base.compose(inCopy);
        Assert.assertArrayEquals(inCopy, in);
    }

    private void testRns2(RnsBase base, int count, long[] in, long[] out) {
        long[] inCopy = Arrays.copyOf(in, in.length);
        base.decomposeArray(inCopy, count);
        Assert.assertArrayEquals(inCopy, out);

        base.composeArray(inCopy, count);
        Assert.assertArrayEquals(inCopy, in);
    }
}