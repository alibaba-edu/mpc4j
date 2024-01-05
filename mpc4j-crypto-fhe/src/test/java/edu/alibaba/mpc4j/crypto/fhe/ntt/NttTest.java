package edu.alibaba.mpc4j.crypto.fhe.ntt;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * NTT unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/ntt.cpp
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
public class NttTest {

    @Test
    public void testNttBasics() {
        NttTables table;

        int coeffCountPower = 1;
        Modulus modulus = Numth.getPrime(2 << coeffCountPower, 60);
        table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(2, table.getCoeffCount());
        Assert.assertEquals(1, table.getCoeffCountPower());

        coeffCountPower = 2;
        modulus = Numth.getPrime(2 << coeffCountPower, 50);
        table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(4, table.getCoeffCount());
        Assert.assertEquals(2, table.getCoeffCountPower());

        coeffCountPower = 10;
        modulus = Numth.getPrime(2 << coeffCountPower, 40);
        table = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1024, table.getCoeffCount());
        Assert.assertEquals(10, table.getCoeffCountPower());

        NttTables[] tables = new NttTables[5];
        NttTables.createNttTables(
            coeffCountPower, CoeffModulus.create(1 << coeffCountPower, new int[]{20, 20, 20, 20, 20}), tables
        );
        for (int j = 0; j < 5; j++) {
            Assert.assertEquals(1024, tables[j].getCoeffCount());
            Assert.assertEquals(10, tables[j].getCoeffCountPower());
        }
    }

    @Test
    public void testNttPrimitiveRoots() {
        int coeffCountPower = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1, tables.getRootPowers(0).operand);
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        long[] inv = new long[1];
        Numth.tryInvertUintMod(tables.getRootPowers(1).operand, modulus.value(), inv);
        Assert.assertEquals(tables.getInvRootPowers(1).operand, inv[0]);

        coeffCountPower = 2;
        tables = new NttTables(coeffCountPower, modulus);
        Assert.assertEquals(1, tables.getRootPowers(0).operand);
        Assert.assertEquals(288794978602139552L, tables.getRootPowers(1).operand);
        Assert.assertEquals(178930308976060547L, tables.getRootPowers(2).operand);
        Assert.assertEquals(748001537669050592L, tables.getRootPowers(3).operand);
    }

    @Test
    public void testNegacyclicNtt() {
        int coeffCountPower = 1;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(coeffCountPower, modulus);
        long[] poly = new long[]{0L, 0L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{0L, 0L}, poly);

        poly = new long[]{1L, 0L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{1, 1}, poly);

        poly = new long[]{1L, 1L};
        NttTool.nttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(new long[]{288794978602139553L, 864126526004445282L}, poly);
    }

    @Test
    public void testInverseNegacyclicNtt() {
        int coeffCountPower = 3;
        Modulus modulus = new Modulus(0xffffffffffc0001L);
        NttTables tables = new NttTables(coeffCountPower, modulus);
        long[] poly = new long[800];
        long[] temp = new long[800];

        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        for (int i = 0; i < 800; i++) {
            Assert.assertEquals(0, poly[i]);
        }

        Random random = new Random();
        for (int i = 0; i < 800; i++) {
            poly[i] = Math.abs(random.nextLong()) % modulus.value();
        }
        System.arraycopy(poly, 0, temp, 0, 800);

        NttTool.nttNegacyclicHarvey(poly, tables);
        NttTool.inverseNttNegacyclicHarvey(poly, tables);
        Assert.assertArrayEquals(temp, poly);
    }
}
