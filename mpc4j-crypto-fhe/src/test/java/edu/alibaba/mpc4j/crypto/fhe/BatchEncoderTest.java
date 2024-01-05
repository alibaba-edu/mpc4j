package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Ciphertext Test.
 * <p></p>
 * The implementation comes from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/batchencoder.cpp
 *
 * @author Anony_Trent
 * @date 2023/10/5
 */
public class BatchEncoderTest {

    @Test
    public void testBatchUnbatchUIntVector() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(64);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));
        // t must be a prime number and t mod 2n = 1, then we can us batch encode
        parms.setPlainModulus(257);

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        Assert.assertTrue(context.firstContextData().qualifiers().isUsingBatching());

        BatchEncoder batchEncoder = new BatchEncoder(context);
        Assert.assertEquals(64, batchEncoder.slotCount());
        long[] plainVec = new long[batchEncoder.slotCount()];
        for (int i = 0; i < batchEncoder.slotCount(); i++) {
            plainVec[i] = i;
        }

        Plaintext plain = new Plaintext();
        batchEncoder.encode(plainVec, plain);
        long[] plainVec2 = new long[batchEncoder.slotCount()];
        batchEncoder.decode(plain, plainVec2);
        Assert.assertArrayEquals(plainVec, plainVec2);

        for (int i = 0; i < batchEncoder.slotCount(); i++) {
            plainVec[i] = 5;
        }
        batchEncoder.encode(plainVec, plain);
        Assert.assertEquals("5", plain.toString());
        batchEncoder.decode(plain, plainVec2);
        Assert.assertArrayEquals(plainVec, plainVec2);

        long[] shortPlainVec = new long[20];
        for (int i = 0; i < 20; i++) {
            shortPlainVec[i] = i;
        }
        batchEncoder.encode(shortPlainVec, plain);
        long[] shortPlainVec2 = new long[64];
        batchEncoder.decode(plain, shortPlainVec2);
        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(shortPlainVec[i], shortPlainVec2[i]);
        }
        for (int i = 20; i < batchEncoder.slotCount(); i++) {
            Assert.assertEquals(0, shortPlainVec2[i]);
        }
    }

    @Test
    public void testBatchUnbatchIntVector() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(64);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));
        // t must be a prime number and t mod 2n = 1, then we can us batch encode
        parms.setPlainModulus(257);

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        Assert.assertTrue(context.firstContextData().qualifiers().isUsingBatching());

        BatchEncoder batchEncoder = new BatchEncoder(context);
        Assert.assertEquals(64, batchEncoder.slotCount());
        long[] plainVec = new long[batchEncoder.slotCount()];
        for (int i = 0; i < batchEncoder.slotCount(); i++) {
            plainVec[i] = (i * (1 - (i & 1) * 2));
        }

        Plaintext plain = new Plaintext();
        batchEncoder.encodeInt64(plainVec, plain);
        long[] plainVec2 = new long[batchEncoder.slotCount()];
        batchEncoder.decodeInt64(plain, plainVec2);
        Assert.assertArrayEquals(plainVec, plainVec2);

        for (int i = 0; i < batchEncoder.slotCount(); i++) {
            plainVec[i] = -5;
        }
        batchEncoder.encodeInt64(plainVec, plain);
        Assert.assertEquals("FC", plain.toString());
        batchEncoder.decodeInt64(plain, plainVec2);
        Assert.assertArrayEquals(plainVec, plainVec2);

        long[] shortPlainVec = new long[20];
        for (int i = 0; i < 20; i++) {
            shortPlainVec[i] = i * (1 - (i & 1) * 2);
        }
        batchEncoder.encodeInt64(shortPlainVec, plain);
        long[] shortPlainVec2 = new long[64];
        batchEncoder.decodeInt64(plain, shortPlainVec2);
        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(shortPlainVec[i], shortPlainVec2[i]);
        }
        for (int i = 20; i < batchEncoder.slotCount(); i++) {
            Assert.assertEquals(0, shortPlainVec2[i]);
        }
    }
}