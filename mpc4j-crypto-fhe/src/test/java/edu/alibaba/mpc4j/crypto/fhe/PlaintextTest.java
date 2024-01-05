package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Plaintext unit test.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/plaintext.cpp.
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
        Plaintext plain;
        Plaintext plain2 = new Plaintext();

        // TODO: test CKKS

        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[] {30, 30}));
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
            Assert.assertNotSame(plain.getData(), plain2.getData());
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
            Assert.assertNotSame(plain.getData(), plain2.getData());
            Assert.assertEquals(plain, plain2);
            Assert.assertTrue(plain2.isNttForm());
        }

        // TODO: test BGV

        // TODO: test another CKKS
    }
}
