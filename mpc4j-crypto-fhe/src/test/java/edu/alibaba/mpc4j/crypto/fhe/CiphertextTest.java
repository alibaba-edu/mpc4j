package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Ciphertext Test.
 * <p></p>
 * The implementation comes from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/ciphertext.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/13
 */
public class CiphertextTest {

    @Test
    public void testBfvCiphertextBasics() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);

        parms.setPolyModulusDegree(2);
        parms.setCoeffModulus(CoeffModulus.create(2, new int[]{30}));
        parms.setPlainModulus(2);
        SealContext context = new SealContext(parms, false, SecLevelType.NONE);

        Ciphertext ctxt = new Ciphertext(context);
        ctxt.reserve(10);
        Assert.assertEquals(0, ctxt.size());
        Assert.assertEquals(0, ctxt.dynArray().size());
        Assert.assertEquals(10L * 2, ctxt.dynArray().capacity());
        Assert.assertEquals(2, ctxt.polyModulusDegree());
        Assert.assertSame(ctxt.parmsId(), context.firstParmsId());
        Assert.assertFalse(ctxt.isNttForm());
        long[] ptr = ctxt.data();

        ctxt.reserve(5);
        Assert.assertEquals(0, ctxt.size());
        Assert.assertEquals(0, ctxt.dynArray().size());
        Assert.assertEquals(5L * 2, ctxt.dynArray().capacity());
        Assert.assertEquals(2, ctxt.polyModulusDegree());
        Assert.assertNotSame(ptr, ctxt.data());
        Assert.assertSame(ctxt.parmsId(), context.firstParmsId());
        ptr = ctxt.data();

        ctxt.reserve(10);
        Assert.assertEquals(0, ctxt.size());
        Assert.assertEquals(0, ctxt.dynArray().size());
        Assert.assertEquals(10L * 2, ctxt.dynArray().capacity());
        Assert.assertEquals(2, ctxt.polyModulusDegree());
        Assert.assertSame(ctxt.parmsId(), context.firstParmsId());
        Assert.assertNotSame(ptr, ctxt.data());
        ptr = ctxt.data();

        ctxt.reserve(2);
        Assert.assertEquals(0, ctxt.size());
        Assert.assertEquals(0, ctxt.dynArray().size());
        Assert.assertEquals(2L * 2, ctxt.dynArray().capacity());
        Assert.assertEquals(2, ctxt.polyModulusDegree());
        Assert.assertSame(ctxt.parmsId(), context.firstParmsId());
        Assert.assertNotSame(ptr, ctxt.data());
        ptr = ctxt.data();

        ctxt.reserve(5);
        Assert.assertEquals(0, ctxt.size());
        Assert.assertEquals(0, ctxt.dynArray().size());
        Assert.assertEquals(5L * 2, ctxt.dynArray().capacity());
        Assert.assertEquals(2, ctxt.polyModulusDegree());
        Assert.assertSame(ctxt.parmsId(), context.firstParmsId());
        Assert.assertNotSame(ptr, ctxt.data());

        Ciphertext ctxt2 = new Ciphertext();
        ctxt2.copyFrom(ctxt);
        Assert.assertEquals(ctxt.getCoeffModulusSize(), ctxt2.getCoeffModulusSize());
        Assert.assertEquals(ctxt.isNttForm(), ctxt2.isNttForm());
        Assert.assertEquals(ctxt.polyModulusDegree(), ctxt2.polyModulusDegree());
        Assert.assertSame(ctxt.parmsId(), ctxt2.parmsId());
        Assert.assertEquals(ctxt.size(), ctxt2.size());
    }

    @Test
    public void testBfvSaveLoadCiphertext() throws IOException {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(2);
        parms.setCoeffModulus(CoeffModulus.create(2, new int[]{30}));
        parms.setPlainModulus(2);

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);

        Ciphertext ctxt = new Ciphertext(context);
        Ciphertext ctxt2 = new Ciphertext();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int outSize = ctxt.save(outputStream);
        outputStream.close();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        int inSize = ctxt2.load(context, inputStream);
        inputStream.close();
        Assert.assertEquals(outSize, inSize);
        Assert.assertEquals(ctxt.parmsId(), ctxt2.parmsId());
        Assert.assertFalse(ctxt.isNttForm());

        parms.setPolyModulusDegree(1024);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(1024));
        parms.setPlainModulus(0xF0F0);
        context = new SealContext(parms, false);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);
        Encryptor encryptor = new Encryptor(context, pk);
        encryptor.encrypt(new Plaintext("Ax^10 + 9x^9 + 8x^8 + 7x^7 + 6x^6 + 5x^5 + 4x^4 + 3x^3 + 2x^2 + 1"), ctxt);
        outputStream = new ByteArrayOutputStream();
        outSize = ctxt.save(outputStream);
        outputStream.close();
        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        inSize = ctxt2.load(context, inputStream);
        inputStream.close();
        Assert.assertEquals(outSize, inSize);
        Assert.assertEquals(ctxt.parmsId(), ctxt2.parmsId());
        Assert.assertFalse(ctxt.isNttForm());
        Assert.assertTrue(UintCore.isEqualUint(
            ctxt.data(), ctxt2.data(), parms.polyModulusDegree() * parms.coeffModulus().length * 2
        ));
        Assert.assertNotSame(ctxt.data(), ctxt2.data());
    }

    // TODO: BGVCiphertextBasics

    // TODO: BGVSaveLoadCiphertext
}
