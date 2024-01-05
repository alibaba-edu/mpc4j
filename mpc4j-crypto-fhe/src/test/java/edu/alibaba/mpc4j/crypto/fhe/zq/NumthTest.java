package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Number Theory unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/numth.cpp
 * </p>
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/8/9
 */
public class NumthTest {

    @Test
    public void textGcd() {
        Assert.assertEquals(1, Numth.gcd(1, 1));
        Assert.assertEquals(1, Numth.gcd(2, 1));
        Assert.assertEquals(1, Numth.gcd(1, 2));
        Assert.assertEquals(2, Numth.gcd(2, 2));
        Assert.assertEquals(3, Numth.gcd(6, 15));
        Assert.assertEquals(3, Numth.gcd(15, 6));
        Assert.assertEquals(1, Numth.gcd(7, 15));
        Assert.assertEquals(1, Numth.gcd(15, 7));
        Assert.assertEquals(3, Numth.gcd(11112, 44445));
    }

    @Test
    public void textExtendedGcd() {
        long[] result;

        // Corner case behavior
        result = Numth.xgcd(7, 7);
        Assert.assertArrayEquals(result, new long[] {7, 0, 1});
        result = Numth.xgcd(2, 2);
        Assert.assertArrayEquals(result, new long[] {2, 0, 1});

        result = Numth.xgcd(1, 1);
        Assert.assertArrayEquals(result, new long[] {1, 0, 1});
        result = Numth.xgcd(1, 2);
        Assert.assertArrayEquals(result, new long[] {1, 1, 0});
        result = Numth.xgcd(5, 6);
        Assert.assertArrayEquals(result, new long[] {1, -1, 1});
        result = Numth.xgcd(13, 19);
        Assert.assertArrayEquals(result, new long[] {1, 3, -2});
        result = Numth.xgcd(14, 21);
        Assert.assertArrayEquals(result, new long[] {7, -1, 1});

        result = Numth.xgcd(2, 1);
        Assert.assertArrayEquals(result, new long[] {1, 0, 1});
        result = Numth.xgcd(6, 5);
        Assert.assertArrayEquals(result, new long[] {1, 1, -1});
        result = Numth.xgcd(19, 13);
        Assert.assertArrayEquals(result, new long[] {1, -2, 3});
        result = Numth.xgcd(21, 14);
        Assert.assertArrayEquals(result, new long[] {7, 1, -1});
    }

    @Test
    public void testTryInvertUintMod() {
        long input, modulus;
        long[] res = new long[1];

        input = 1;
        modulus = 2;
        Assert.assertTrue(Numth.tryInvertUintMod(input, modulus, res));
        Assert.assertEquals(1, res[0]);

        input = 2;
        Assert.assertFalse(Numth.tryInvertUintMod(input, modulus, res));

        input = 3;
        Assert.assertTrue(Numth.tryInvertUintMod(input, modulus, res));
        Assert.assertEquals(1, res[0]);

        input = 0xFFFFFF;
        Assert.assertTrue(Numth.tryInvertUintMod(input, modulus, res));
        Assert.assertEquals(1, res[0]);

        input = 0xFFFFFE;
        Assert.assertFalse(Numth.tryInvertUintMod(input, modulus, res));

        input = 12345;
        modulus = 3;
        Assert.assertFalse(Numth.tryInvertUintMod(input, modulus, res));

        input = 5;
        modulus = 19;
        Assert.assertTrue(Numth.tryInvertUintMod(input, modulus, res));
        Assert.assertEquals(4, res[0]);

        input = 4;
        Assert.assertTrue(Numth.tryInvertUintMod(input, modulus, res));
        Assert.assertEquals(5, res[0]);
    }

    @Test
    public void testIsPrime() {
        Assert.assertFalse(Numth.isPrime(0));
        Assert.assertTrue(Numth.isPrime(2));
        Assert.assertTrue(Numth.isPrime(3));
        Assert.assertFalse(Numth.isPrime(4));
        Assert.assertTrue(Numth.isPrime(5));
        Assert.assertFalse(Numth.isPrime(221));
        Assert.assertTrue(Numth.isPrime(65537));
        Assert.assertFalse(Numth.isPrime(65536));
        Assert.assertTrue(Numth.isPrime(59399));
        Assert.assertTrue(Numth.isPrime(72307));
        Assert.assertFalse(Numth.isPrime(72307L * 59399L));
        Assert.assertTrue(Numth.isPrime(36893488147419103L));
        Assert.assertFalse(Numth.isPrime(36893488147419107L));
    }

    @Test
    public void testTryPrimitiveRootMod() {
        long[] result = new long[1];
        Modulus mod = new Modulus(11);

        Assert.assertTrue(Numth.tryPrimitiveRoot(2, mod, result));
        Assert.assertEquals(10, result[0]);

        mod.setValue(29);
        Assert.assertTrue(Numth.tryPrimitiveRoot(2, mod, result));
        Assert.assertEquals(28, result[0]);

        long[] tmp = new long[]{ 12, 17 };
        List<Long> corrects = Arrays.stream(tmp).boxed().collect(Collectors.toList());
        Assert.assertTrue(Numth.tryPrimitiveRoot(4, mod, result));
        Assert.assertTrue(corrects.contains(result[0]));

        mod.setValue(1234565441);
        Assert.assertTrue(Numth.tryPrimitiveRoot(2, mod, result));
        Assert.assertEquals(1234565440L, result[0]);
        tmp = new long[]{ 984839708, 273658408, 249725733, 960907033 };
        corrects = Arrays.stream(tmp).boxed().collect(Collectors.toList());
        Assert.assertTrue(Numth.tryPrimitiveRoot(8, mod, result));
        Assert.assertTrue(corrects.contains(result[0]));
    }

    @Test
    public void isPrimitiveRootMod() {
        Modulus mod = new Modulus(11);
        Assert.assertTrue(Numth.isPrimitiveRoot(10, 2, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(9, 2, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(10, 4, mod));

        mod.setValue(29);
        Assert.assertTrue(Numth.isPrimitiveRoot(28, 2, mod));
        Assert.assertTrue(Numth.isPrimitiveRoot(12, 4, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(12, 2, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(12, 8, mod));

        mod.setValue(1234565441L);
        Assert.assertTrue(Numth.isPrimitiveRoot(1234565440L, 2, mod));
        Assert.assertTrue(Numth.isPrimitiveRoot(960907033L, 8, mod));
        Assert.assertTrue(Numth.isPrimitiveRoot(1180581915L, 16, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(1180581915L, 32, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(1180581915L, 8, mod));
        Assert.assertFalse(Numth.isPrimitiveRoot(1180581915L, 2, mod));
    }

    @Test
    public void tryMinimalPrimitiveRootModTest() {
        long[] result = new long[1];
        Modulus mod = new Modulus(11);

        Assert.assertTrue(Numth.tryMinimalPrimitiveRoot(2, mod, result));
        Assert.assertEquals(10, result[0]);

        mod.setValue(29);
        Assert.assertTrue(Numth.tryMinimalPrimitiveRoot(2, mod, result));
        Assert.assertEquals(28, result[0]);
        Assert.assertTrue(Numth.tryMinimalPrimitiveRoot(4, mod, result));
        Assert.assertEquals(12, result[0]);

        mod.setValue(1234565441L);
        Assert.assertTrue(Numth.tryMinimalPrimitiveRoot(2, mod, result));
        Assert.assertEquals(1234565440L, result[0]);
        Assert.assertTrue(Numth.tryMinimalPrimitiveRoot(8, mod, result));
        Assert.assertEquals(249725733L, result[0]);
    }
}