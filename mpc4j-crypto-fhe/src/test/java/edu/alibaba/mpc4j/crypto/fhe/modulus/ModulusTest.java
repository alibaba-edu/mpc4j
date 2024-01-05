package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import org.junit.Test;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Modulus unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/modulus.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/3
 */
public class ModulusTest {

    @Test
    public void testCreateModulus() {
        Modulus mod = new Modulus();
        Assert.assertTrue(mod.isZero());
        Assert.assertEquals(0, mod.value());
        Assert.assertEquals(0, mod.bitCount());
        Assert.assertEquals(1, mod.uint64Count());
        Assert.assertEquals(0, mod.constRatio()[0]);
        Assert.assertEquals(0, mod.constRatio()[1]);
        Assert.assertEquals(0, mod.constRatio()[2]);
        Assert.assertFalse(mod.isPrime());

        long value = 3;
        mod = new Modulus(value);
        Assert.assertFalse(mod.isZero());
        Assert.assertEquals(3, mod.value());
        Assert.assertEquals(2, mod.bitCount());
        Assert.assertEquals(1, mod.uint64Count());
        Assert.assertEquals(0X5555555555555555L, mod.constRatio()[0]);
        Assert.assertEquals(0X5555555555555555L, mod.constRatio()[1]);
        Assert.assertEquals(1, mod.constRatio()[2]);

        Modulus mod2 = new Modulus(2);
        Modulus mod3 = new Modulus(3);
        Assert.assertNotSame(mod2, mod3);
        Assert.assertEquals(mod, mod3);

        value = 0;
        mod.setValue(value);
        Assert.assertTrue(mod.isZero());
        Assert.assertEquals(0, mod.value());
        Assert.assertEquals(0, mod.bitCount());
        Assert.assertEquals(1, mod.uint64Count());
        Assert.assertEquals(0, mod.constRatio()[0]);
        Assert.assertEquals(0, mod.constRatio()[1]);
        Assert.assertEquals(0, mod.constRatio()[2]);

        value = 0xF00000F00000FL;
        mod.setValue(value);
        Assert.assertFalse(mod.isZero());
        Assert.assertEquals(value, mod.value());
        Assert.assertEquals(52, mod.bitCount());
        Assert.assertEquals(1, mod.uint64Count());
        Assert.assertEquals(0x1100000000000011L, mod.constRatio()[0]);
        Assert.assertEquals(4369, mod.constRatio()[1]);
        Assert.assertEquals(281470698520321L, mod.constRatio()[2]);
        Assert.assertFalse(mod.isPrime());

        value = 0xF00000F000079L;
        mod.setValue(value);
        Assert.assertFalse(mod.isZero());
        Assert.assertEquals(value, mod.value());
        Assert.assertEquals(52, mod.bitCount());
        Assert.assertEquals(1, mod.uint64Count());
        Assert.assertEquals(1224979096621368355L, mod.constRatio()[0]);
        Assert.assertEquals(4369, mod.constRatio()[1]);
        Assert.assertEquals(1144844808538997L, mod.constRatio()[2]);
        Assert.assertTrue(mod.isPrime());
    }

    @Test
    public void testSaveLoadModulus() throws IOException {
        testSaveLoadModulus(ComprModeType.NONE);
        testSaveLoadModulus(ComprModeType.ZLIB);
        testSaveLoadModulus(ComprModeType.ZSTD);
    }

    private void testSaveLoadModulus(ComprModeType comprMode) throws IOException {
        Modulus mod = new Modulus();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mod.save(outputStream, comprMode);
        outputStream.close();

        Modulus mod2 = new Modulus();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        mod2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(mod2.value(), mod.value());
        Assert.assertEquals(mod2.bitCount(), mod.bitCount());
        Assert.assertEquals(mod2.uint64Count(), mod.uint64Count());
        Assert.assertEquals(mod2.constRatio()[0], mod.constRatio()[0]);
        Assert.assertEquals(mod2.constRatio()[1], mod.constRatio()[1]);
        Assert.assertEquals(mod2.constRatio()[2], mod.constRatio()[2]);
        Assert.assertEquals(mod2.isPrime(), mod.isPrime());

        mod.setValue(3);
        outputStream = new ByteArrayOutputStream();
        mod.save(outputStream, comprMode);
        outputStream.close();
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        mod2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(mod2.value(), mod.value());
        Assert.assertEquals(mod2.bitCount(), mod.bitCount());
        Assert.assertEquals(mod2.uint64Count(), mod.uint64Count());
        Assert.assertEquals(mod2.constRatio()[0], mod.constRatio()[0]);
        Assert.assertEquals(mod2.constRatio()[1], mod.constRatio()[1]);
        Assert.assertEquals(mod2.constRatio()[2], mod.constRatio()[2]);
        Assert.assertEquals(mod2.isPrime(), mod.isPrime());

        mod.setValue(0xF00000F00000FL);
        outputStream = new ByteArrayOutputStream();
        mod.save(outputStream, comprMode);
        outputStream.close();
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        mod2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(mod2.value(), mod.value());
        Assert.assertEquals(mod2.bitCount(), mod.bitCount());
        Assert.assertEquals(mod2.uint64Count(), mod.uint64Count());
        Assert.assertEquals(mod2.constRatio()[0], mod.constRatio()[0]);
        Assert.assertEquals(mod2.constRatio()[1], mod.constRatio()[1]);
        Assert.assertEquals(mod2.constRatio()[2], mod.constRatio()[2]);
        Assert.assertEquals(mod2.isPrime(), mod.isPrime());

        mod.setValue(0xF00000F000079L);
        outputStream = new ByteArrayOutputStream();
        mod.save(outputStream, comprMode);
        outputStream.close();
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        mod2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(mod2.value(), mod.value());
        Assert.assertEquals(mod2.bitCount(), mod.bitCount());
        Assert.assertEquals(mod2.uint64Count(), mod.uint64Count());
        Assert.assertEquals(mod2.constRatio()[0], mod.constRatio()[0]);
        Assert.assertEquals(mod2.constRatio()[1], mod.constRatio()[1]);
        Assert.assertEquals(mod2.constRatio()[2], mod.constRatio()[2]);
        Assert.assertEquals(mod2.isPrime(), mod.isPrime());
    }

    @Test
    public void testReduce() {
        Modulus mod = new Modulus();
        Assert.assertThrows(IllegalArgumentException.class, () -> mod.reduce(10));

        mod.setValue(2);
        Assert.assertEquals(0, mod.reduce(0));
        Assert.assertEquals(1, mod.reduce(1));
        Assert.assertEquals(0, mod.reduce(2));
        Assert.assertEquals(0, mod.reduce(0xF0F0F0L));

        mod.setValue(10);
        Assert.assertEquals(0, mod.reduce(0));
        Assert.assertEquals(1, mod.reduce(1));
        Assert.assertEquals(8, mod.reduce(8));
        Assert.assertEquals(7, mod.reduce(1234567));
        Assert.assertEquals(0, mod.reduce(12345670));
    }
}
