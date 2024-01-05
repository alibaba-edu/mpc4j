package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PublicKey unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/publickey.cpp.
 *
 * @author Weiran Liu
 * @date 2023/12/14
 */
public class PublicKeyTest {

    @Test
    public void testSaveLoadPublicKey() throws IOException {
        testSaveLoadPublicKey(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testSaveLoadPublicKey(SchemeType scheme) throws IOException {
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(1 << 6);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[] {60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            Assert.assertSame(pk.parmsId(), context.keyParmsId());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = pk.save(outputStream);
            outputStream.close();
            PublicKey pk2 = new PublicKey();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = pk2.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);

            Assert.assertEquals(pk.data().dynArray().size(), pk2.data().dynArray().size());
            for (int i = 0; i < pk.data().dynArray().size(); i++)
            {
                Assert.assertEquals(pk.data().data()[i], pk2.data().data()[i]);
            }
            Assert.assertEquals(pk.parmsId(), pk2.parmsId());
        }
    }
}
