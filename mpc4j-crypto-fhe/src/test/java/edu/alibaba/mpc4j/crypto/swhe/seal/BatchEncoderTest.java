package edu.alibaba.mpc4j.crypto.swhe.seal;

import edu.alibaba.mpc4j.crypto.swhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.swhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.swhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.swhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.swhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.swhe.seal.modulus.PlainModulus;
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


    public void evalMultiPoly(long[] queryValue) {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));
        SealContext context = new SealContext(parms);


        BatchEncoder batchEncoder = new BatchEncoder(context);

        int queryCount = queryValue.length;

        // 每一个多项的常数项
        long[] a0 = new long[]{2, 12, 30};
        // 一次项
        long[] a1 = new long[]{-3, -7, -11};
        // 二次项
        long[] a2 = new long[]{1, 1, 1};


        // 查询项目
        long[] query = queryValue;


        Plaintext a0Encode = new Plaintext();
        Plaintext a1Encode = new Plaintext();
        Plaintext a2Encode = new Plaintext();
        Plaintext queryEncode = new Plaintext();

        batchEncoder.encodeInt64(a0, a0Encode);
        batchEncoder.encodeInt64(a1, a1Encode);
        batchEncoder.encodeInt64(a2, a2Encode);

        batchEncoder.encodeInt64(query, queryEncode);

        // encrypt
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey publicKey = new PublicKey();
        keyGenerator.createPublicKey(publicKey);
        SecretKey secretKey = keyGenerator.secretKey();

        Encryptor encryptor = new Encryptor(context, publicKey);
        Decryptor decryptor = new Decryptor(context, secretKey);
        Evaluator evaluator = new Evaluator(context);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        Ciphertext queryPower1 = encryptor.encrypt(queryEncode);
        Ciphertext queryPower2 = new Ciphertext();
        evaluator.square(queryPower1, queryPower2);
        evaluator.relinearizeInplace(queryPower2, relinKeys);

        // 开始多项式评估
        Ciphertext result = new Ciphertext();

        Ciphertext temp1 = new Ciphertext();
        Ciphertext temp2 = new Ciphertext();

        evaluator.multiplyPlain(queryPower1, a1Encode, temp1);
        evaluator.multiplyPlain(queryPower2, a2Encode, temp2);

        //
        evaluator.add(temp1, temp2, result);
        evaluator.addPlain(result, a0Encode, result);

        // decrypt and decode
        Plaintext resultPlain = new Plaintext();
        decryptor.decrypt(result, resultPlain);
        long[] decodeResult = new long[polyModulusDegree];
        batchEncoder.decodeInt64(resultPlain, decodeResult);

//        long[] realResult = new long[queryCount];
//        System.arraycopy(decodeResult, 0, realResult, 0, queryCount);

        for (int i = 0; i < queryCount; i++) {
            System.out.printf("Result M%d(%d) = %d\n", i + 1, query[i], decodeResult[i]);
        }
        System.out.println("---------------");

    }

    @Test
    public void evalMultiPolyTest() {
//        evalMultiPoly(new long[]{1, 3, 5});

//        evalMultiPoly(new long[]{1, 5, 7});

        evalMultiPoly(new long[]{3, 6, 9, 10});
    }




    public void evalSinglePoly(int queryValue) {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));
        SealContext context = new SealContext(parms);


        BatchEncoder batchEncoder = new BatchEncoder(context);

        int queryCount = 1;

        // M(x) = 2 - 3x + x^2
        long[] a0 = new long[1];
        a0[0] = 2;

        long[] a1 = new long[1];
        a1[0] = -3;

        long[] a2 = new long[1];
        a2[0] = 1;

        //
        long[] query = new long[1];
        query[0] = queryValue;


        Plaintext a0Encode = new Plaintext();
        Plaintext a1Encode = new Plaintext();
        Plaintext a2Encode = new Plaintext();
        Plaintext queryEncode = new Plaintext();

        batchEncoder.encodeInt64(a0, a0Encode);
        batchEncoder.encodeInt64(a1, a1Encode);
        batchEncoder.encodeInt64(a2, a2Encode);

        batchEncoder.encodeInt64(query, queryEncode);

        // encrypt
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey publicKey = new PublicKey();
        keyGenerator.createPublicKey(publicKey);
        SecretKey secretKey = keyGenerator.secretKey();

        Encryptor encryptor = new Encryptor(context, publicKey);
        Decryptor decryptor = new Decryptor(context, secretKey);
        Evaluator evaluator = new Evaluator(context);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        Ciphertext queryPower1 = encryptor.encrypt(queryEncode);
        Ciphertext queryPower2 = new Ciphertext();
        evaluator.square(queryPower1, queryPower2);
        evaluator.relinearizeInplace(queryPower2, relinKeys);

        // 开始多项式评估
        Ciphertext result = new Ciphertext();

        Ciphertext temp1 = new Ciphertext();
        Ciphertext temp2 = new Ciphertext();

        evaluator.multiplyPlain(queryPower1, a1Encode, temp1);
        evaluator.multiplyPlain(queryPower2, a2Encode, temp2);

        //
        evaluator.add(temp1, temp2, result);
        evaluator.addPlain(result, a0Encode, result);

        // decrypt and decode
        Plaintext resultPlain = new Plaintext();
        decryptor.decrypt(result, resultPlain);
        long[] decodeResult = new long[polyModulusDegree];
        batchEncoder.decodeInt64(resultPlain, decodeResult);

        for (int i = 0; i < queryCount; i++) {
            System.out.printf("Result M(%d) = 2 - 3 * %d + (%d)^2: %d\n", query[i],query[i], query[i], decodeResult[i]);
        }
    }



    @Test
    public void evalPolyTest() {

        evalSinglePoly(1);
        evalSinglePoly(2);
        evalSinglePoly(3);

    }


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