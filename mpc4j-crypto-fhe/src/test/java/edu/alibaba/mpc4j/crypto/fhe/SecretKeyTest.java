package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SecretKey unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/secretkey.cpp.
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public class SecretKeyTest {

    @Test
    public void testSaveLoadSecretKey() throws IOException {
        testSaveLoadSecretKey(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testSaveLoadSecretKey(SchemeType scheme) throws IOException {
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(1 << 6);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            SecretKey sk = keygen.secretKey();
            Assert.assertSame(sk.parmsId(), context.keyParmsId());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = sk.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
            outputStream.close();

            SecretKey sk2 = new SecretKey();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = sk2.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);

            Assert.assertNotSame(sk.data(), sk2.data());
            Assert.assertEquals(sk.data(), sk2.data());
            Assert.assertEquals(sk.parmsId(), sk2.parmsId());
        }

        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(1 << 20);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{30, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            SecretKey sk = keygen.secretKey();
            Assert.assertSame(sk.parmsId(), context.keyParmsId());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = sk.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
            outputStream.close();

            SecretKey sk2 = new SecretKey();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = sk2.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);

            Assert.assertNotSame(sk.data(), sk2.data());
            Assert.assertEquals(sk.data(), sk2.data());
            Assert.assertEquals(sk.parmsId(), sk2.parmsId());
        }
    }
}
