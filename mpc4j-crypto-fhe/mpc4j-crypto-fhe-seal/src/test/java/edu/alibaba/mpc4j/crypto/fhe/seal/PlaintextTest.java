package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.Serialization;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Arithmetic;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Plaintext unit test.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/tests/seal/plaintext.cpp">plaintext.cpp</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/10
 */
public class PlaintextTest {

    @Test
    public void testPlaintextBasics() {
        Plaintext plain = new Plaintext(2);
        Assert.assertEquals(2, plain.capacity());
        Assert.assertEquals(2, plain.coeffCount());
        Assert.assertEquals(0, plain.significantCoeffCount());
        Assert.assertEquals(0, plain.nonZeroCoeffCount());
        Assert.assertFalse(plain.isNttForm());

        plain.set(0, 1);
        plain.set(1, 2);
        plain.reserve(10);
        Assert.assertEquals(10, plain.capacity());
        Assert.assertEquals(2, plain.coeffCount());
        Assert.assertEquals(2, plain.significantCoeffCount());
        Assert.assertEquals(2, plain.nonZeroCoeffCount());
        Assert.assertEquals(1, plain.get(0));
        Assert.assertEquals(2, plain.get(1));
        Assert.assertFalse(plain.isNttForm());

        plain.resize(5);
        Assert.assertEquals(10, plain.capacity());
        Assert.assertEquals(5, plain.coeffCount());
        Assert.assertEquals(2, plain.significantCoeffCount());
        Assert.assertEquals(2, plain.nonZeroCoeffCount());
        Assert.assertEquals(1, plain.get(0));
        Assert.assertEquals(2, plain.get(1));
        Assert.assertEquals(0, plain.get(2));
        Assert.assertEquals(0, plain.get(3));
        Assert.assertEquals(0, plain.get(4));
        Assert.assertFalse(plain.isNttForm());

        Plaintext plain2 = new Plaintext();
        plain2.resize(15);
        Assert.assertEquals(15, plain2.capacity());
        Assert.assertEquals(15, plain2.coeffCount());
        Assert.assertEquals(0, plain2.significantCoeffCount());
        Assert.assertEquals(0, plain2.significantCoeffCount());
        Assert.assertFalse(plain.isNttForm());

        plain2 = plain;
        Assert.assertEquals(10, plain2.capacity());
        Assert.assertEquals(5, plain2.coeffCount());
        Assert.assertEquals(2, plain2.significantCoeffCount());
        Assert.assertEquals(2, plain2.nonZeroCoeffCount());
        Assert.assertEquals(1, plain2.get(0));
        Assert.assertEquals(2, plain2.get(1));
        Assert.assertEquals(0, plain2.get(2));
        Assert.assertEquals(0, plain2.get(3));
        Assert.assertEquals(0, plain2.get(4));
        Assert.assertSame(plain2, plain);

        Plaintext plain3 = new Plaintext();
        plain3.copyFrom(plain2);
        Assert.assertEquals(10, plain3.capacity());
        Assert.assertEquals(5, plain3.coeffCount());
        Assert.assertEquals(2, plain3.significantCoeffCount());
        Assert.assertEquals(2, plain3.nonZeroCoeffCount());
        Assert.assertEquals(1, plain3.get(0));
        Assert.assertEquals(2, plain3.get(1));
        Assert.assertEquals(0, plain3.get(2));
        Assert.assertEquals(0, plain3.get(3));
        Assert.assertEquals(0, plain3.get(4));
        Assert.assertNotSame(plain2, plain3);

        plain.setParmsId(new ParmsId(new long[]{1, 2, 3, 4}));
        Assert.assertTrue(plain.isNttForm());

        plain2.setParmsId(ParmsId.parmsIdZero());
        Assert.assertFalse(plain2.isNttForm());
        plain2.setParmsId(new ParmsId(new long[]{1, 2, 3, 5}));
        Assert.assertTrue(plain2.isNttForm());
    }

    @Test
    public void testSaveLoadPlaintext() throws IOException {
        Plaintext plain = new Plaintext();
        Plaintext plain2 = new Plaintext();

        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
            parms.setPolyModulusDegree(4);
            parms.setCoeffModulus(CoeffModulus.create(4, new int[]{20}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            plain.save(outputStream);
            byte[] data = outputStream.toByteArray();
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            plain2.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertArrayEquals(plain.data(), plain2.data());
            // ASSERT_TRUE(plain2.data() == nullptr);
            Assert.assertArrayEquals(new long[0], plain2.data());
            Assert.assertEquals(0L, plain2.capacity());
            Assert.assertEquals(0L, plain2.coeffCount());
            Assert.assertFalse(plain2.isNttForm());

            plain.reserve(20);
            plain.resize(4);
            // plain[0] = 1;
            plain.set(0, 1);
            // plain[1] = 2;
            plain.set(1, 2);
            // plain[2] = 3;
            plain.set(2, 3);
            outputStream = new ByteArrayOutputStream();
            plain.save(outputStream);
            data = outputStream.toByteArray();
            outputStream.close();
            inputStream = new ByteArrayInputStream(data);
            plain2.unsafeLoad(context, inputStream);
            inputStream.close();

            Assert.assertFalse(Arrays.equals(plain.data(), plain2.data()));
            Assert.assertEquals(4L, plain2.capacity());
            Assert.assertEquals(4L, plain2.coeffCount());
            Assert.assertEquals(1L, plain2.data(0));
            Assert.assertEquals(2L, plain2.data(1));
            Assert.assertEquals(3L, plain2.data(2));
            Assert.assertEquals(0L, plain2.data(3));
            Assert.assertFalse(plain2.isNttForm());

            plain.setParmsId(context.firstParmsId());
            outputStream = new ByteArrayOutputStream();
            plain.save(outputStream);
            data = outputStream.toByteArray();
            outputStream.close();
            inputStream = new ByteArrayInputStream(data);
            plain2.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertTrue(plain2.isNttForm());
            Assert.assertEquals(plain2.parmsId(), plain.parmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30}));
            parms.setPlainModulus(65537);

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            plain = new Plaintext("1x^63 + 2x^62 + Fx^32 + Ax^9 + 1x^1 + 1");
            plain.setParmsId(ParmsId.parmsIdZero());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = plain.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = plain2.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertNotSame(plain.data(), plain2.data());
            Assert.assertEquals(plain, plain2);
            Assert.assertFalse(plain2.isNttForm());

            Evaluator evaluator = new Evaluator(context);
            evaluator.transformToNttInplace(plain, context.firstParmsId());
            Assert.assertSame(plain.parmsId(), context.firstParmsId());
            outputStream = new ByteArrayOutputStream();
            outSize = plain.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
            outputStream.close();
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            inSize = plain2.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertNotSame(plain.data(), plain2.data());
            Assert.assertEquals(plain, plain2);
            Assert.assertTrue(plain2.isNttForm());
        }
        // TODO: test BGV
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            CkksEncoder encoder = new CkksEncoder(context);

            // vector<double>{ 0.1, 2.3, 34.4 }
            double[][] input = new double[3][2];
            Arithmetic.set(input[0], 0.1);
            Arithmetic.set(input[1], 2.3);
            Arithmetic.set(input[2], 34.4);
            encoder.encode(input, Math.pow(2.0, 20), plain);
            Assert.assertTrue(plain.isNttForm());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            plain.save(outputStream);
            byte[] data = outputStream.toByteArray();
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            plain2.load(context, inputStream);
            inputStream.close();
            // ASSERT_TRUE(plain.data() != plain2.data());
            Assert.assertNotSame(plain.data(), plain2.data());
            Assert.assertArrayEquals(plain.data(), plain2.data());
            Assert.assertTrue(plain2.isNttForm());
        }
    }
}
