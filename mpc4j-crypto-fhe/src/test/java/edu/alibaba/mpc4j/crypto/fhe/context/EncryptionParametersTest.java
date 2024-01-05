package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * EncryptionParameters unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/encryptionparams.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/30
 */
@SuppressWarnings("DuplicateExpressions")
public class EncryptionParametersTest {

    @Test
    public void testEncryptionParametersSet() {
        testEncryptionParametersSet(SchemeType.BFV);
        // TODO: test BGV
        // TODO: test CKKS
    }

    private void testEncryptionParametersSet(SchemeType scheme) {
        EncryptionParameters parms = new EncryptionParameters(scheme);
        parms.setCoeffModulus(new long[]{2, 3});
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms.setPlainModulus(2);
        }
        parms.setPolyModulusDegree(2);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());

        Assert.assertEquals(scheme, parms.scheme());
        Assert.assertEquals(parms.coeffModulus()[0].value(), 2);
        Assert.assertEquals(parms.coeffModulus()[1].value(), 3);
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            Assert.assertEquals(parms.plainModulus().value(), 2);
        } else if (scheme.equals(SchemeType.CKKS)) {
            Assert.assertEquals(parms.plainModulus().value(), 0);
        }
        Assert.assertEquals(parms.polyModulusDegree(), 2);
        Assert.assertEquals(parms.randomGeneratorFactory(), UniformRandomGeneratorFactory.defaultFactory());

        parms.setCoeffModulus(CoeffModulus.create(2, new int[]{30, 40, 50}));
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms.setPlainModulus(2);
        }
        parms.setPolyModulusDegree(128);
        parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());

        Assert.assertTrue(Numth.isPrime(parms.coeffModulus()[0].value()));
        Assert.assertTrue(Numth.isPrime(parms.coeffModulus()[1].value()));
        Assert.assertTrue(Numth.isPrime(parms.coeffModulus()[2].value()));

        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            Assert.assertEquals(parms.plainModulus().value(), 2);
        } else if (scheme.equals(SchemeType.CKKS)) {
            Assert.assertEquals(parms.plainModulus().value(), 0);
        }
        Assert.assertEquals(128, parms.polyModulusDegree());
        Assert.assertEquals(parms.randomGeneratorFactory(), UniformRandomGeneratorFactory.defaultFactory());
    }

    @Test
    public void testEncryptionParametersCompare() {
        testEncryptionParametersCompare(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testEncryptionParametersCompare(SchemeType scheme) {
        EncryptionParameters parms1 = new EncryptionParameters(scheme);
        parms1.setCoeffModulus(CoeffModulus.create(64, new int[]{30}));
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms1.setPlainModulus(1 << 6);
        }
        parms1.setPolyModulusDegree(64);
        parms1.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());

        EncryptionParameters parms2 = new EncryptionParameters(parms1);
        Assert.assertEquals(parms1, parms2);

        EncryptionParameters parms3 = new EncryptionParameters(parms2);
        Assert.assertEquals(parms3, parms2);
        parms3.setCoeffModulus(CoeffModulus.create(64, new int[]{ 32 }));
        Assert.assertNotEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        Assert.assertEquals(parms3, parms2);
        parms3.setCoeffModulus(CoeffModulus.create(64, new int[]{ 30, 30 }));
        Assert.assertNotEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        parms3.setPolyModulusDegree(128);
        Assert.assertNotEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms3.setPlainModulus((1 << 6) + 1);
        }
        Assert.assertNotEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        parms3.setRandomGeneratorFactory(null);
        Assert.assertEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        parms3.setPolyModulusDegree(128);
        parms3.setPolyModulusDegree(64);
        Assert.assertEquals(parms3, parms2);

        parms3 = new EncryptionParameters(parms2);
        parms3.setCoeffModulus(new long[]{ 2 });
        parms3.setCoeffModulus(CoeffModulus.create(64, new int[]{ 50 }));
        parms3.setCoeffModulus(parms2.coeffModulus());
        Assert.assertEquals(parms3, parms2);
    }

    @Test
    public void testEncryptionParametersSaveLoad() throws IOException {
        testEncryptionParametersSaveLoad(SchemeType.BFV);
        // TODO: test BGV
        // TODO: test CKKS
    }

    private void testEncryptionParametersSaveLoad(SchemeType scheme) throws IOException {
        EncryptionParameters parms = new EncryptionParameters(scheme);
        EncryptionParameters parms2 = new EncryptionParameters(scheme);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{ 30 }));
        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms.setPlainModulus(1 << 6);
        }
        parms.setPolyModulusDegree(64);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        parms.save(outputStream);
        outputStream.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        parms2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(parms.scheme(), parms2.scheme());
        Assert.assertArrayEquals(parms.coeffModulus(), parms2.coeffModulus());
        Assert.assertEquals(parms.plainModulus(), parms2.plainModulus());
        Assert.assertEquals(parms.polyModulusDegree(), parms2.polyModulusDegree());
        Assert.assertEquals(parms, parms2);

        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{ 30, 60, 60 }));

        if (scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)) {
            parms.setPlainModulus(1 << 30);
        }
        parms.setPolyModulusDegree(256);

        outputStream = new ByteArrayOutputStream();
        parms.save(outputStream);
        outputStream.close();
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        parms2.load(null, inputStream);
        inputStream.close();
        Assert.assertEquals(parms.scheme(), parms2.scheme());
        Assert.assertArrayEquals(parms.coeffModulus(), parms2.coeffModulus());
        Assert.assertEquals(parms.plainModulus(), parms2.plainModulus());
        Assert.assertEquals(parms.polyModulusDegree(), parms2.polyModulusDegree());
        Assert.assertEquals(parms, parms2);
    }
}
