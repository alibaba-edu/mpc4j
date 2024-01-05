package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * GaloisKeys unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/galoiskeys.cpp
 *
 * @author Weiran Liu
 * @date 2023/12/15
 */
public class GaloisKeysTest {

    @Test
    public void testGaloisKeysSaveLoad() throws IOException {
        testGaloisKeysSaveLoad(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testGaloisKeysSaveLoad(SchemeType scheme) throws IOException {
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60,}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            GaloisKeys keys = new GaloisKeys();
            GaloisKeys testKeys = new GaloisKeys();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keys.save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            testKeys.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertEquals(keys.data().length, testKeys.data().length);
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            Assert.assertEquals(0, keys.data().length);

            keygen.createGaloisKeys(keys);
            outputStream = new ByteArrayOutputStream();
            keys.save(outputStream);
            outputStream.close();
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            testKeys.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertEquals(keys.data().length, testKeys.data().length);
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            for (int j = 0; j < testKeys.data().length; j++) {
                for (int i = 0; i < testKeys.data()[j].length; i++) {
                    Assert.assertEquals(keys.data()[j][i].data().size(), testKeys.data()[j][i].data().size());
                    Assert.assertEquals(
                        keys.data()[j][i].data().dynArray().size(),
                        testKeys.data()[j][i].data().dynArray().size());
                    Assert.assertTrue(UintCore.isEqualUint(
                        keys.data()[j][i].data().data(), testKeys.data()[j][i].data().data(),
                        keys.data()[j][i].data().dynArray().size()));
                }
            }
            Assert.assertEquals(64, keys.data().length);
        }
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 50,}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            GaloisKeys keys = new GaloisKeys();
            GaloisKeys testKeys = new GaloisKeys();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keys.save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            testKeys.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertEquals(keys.data().length, testKeys.data().length);
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            Assert.assertEquals(0, keys.data().length);

            keygen.createGaloisKeys(keys);
            outputStream = new ByteArrayOutputStream();
            keys.save(outputStream);
            outputStream.close();
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            testKeys.unsafeLoad(context, inputStream);
            inputStream.close();
            Assert.assertEquals(keys.data().length, testKeys.data().length);
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            for (int j = 0; j < testKeys.data().length; j++) {
                for (int i = 0; i < testKeys.data()[j].length; i++) {
                    Assert.assertEquals(keys.data()[j][i].data().size(), testKeys.data()[j][i].data().size());
                    Assert.assertEquals(
                        keys.data()[j][i].data().dynArray().size(),
                        testKeys.data()[j][i].data().dynArray().size());
                    Assert.assertTrue(UintCore.isEqualUint(
                        keys.data()[j][i].data().data(), testKeys.data()[j][i].data().data(),
                        keys.data()[j][i].data().dynArray().size()));
                }
            }
            Assert.assertEquals(256, keys.data().length);
        }
    }

    @Test
    public void testGaloisKeysSeededSaveLoad() throws IOException {
        testGaloisKeysSeededSaveLoad(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testGaloisKeysSeededSaveLoad(SchemeType scheme) throws IOException {
        SecureRandom secureRandom = new SecureRandom();
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(8);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(8, new int[]{60, 60}));
            long[] seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
            for (int i = 0; i < seed.length; i++) {
                seed[i] = secureRandom.nextLong();
            }
            UniformRandomGeneratorFactory rng = new UniformRandomGeneratorFactory(seed);
            parms.setRandomGeneratorFactory(rng);
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            SecretKey secretKey = keygen.secretKey();

            SealSerializable<GaloisKeys> serializableGaloisKeys = keygen.createStepGaloisKeys();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializableGaloisKeys.save(outputStream);
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            GaloisKeys testKeys = new GaloisKeys();
            testKeys.load(context, inputStream);
            inputStream.close();
            GaloisKeys keys = new GaloisKeys();
            keygen.createGaloisKeys(keys);
            compareKswitchKeys(keys, testKeys, secretKey, context);
        }
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 50}));
            long[] seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
            for (int i = 0; i < seed.length; i++) {
                seed[i] = secureRandom.nextLong();
            }
            UniformRandomGeneratorFactory rng = new UniformRandomGeneratorFactory(seed);
            parms.setRandomGeneratorFactory(rng);
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            SecretKey secretKey = keygen.secretKey();

            SealSerializable<GaloisKeys> serializableGaloisKeys = keygen.createStepGaloisKeys();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializableGaloisKeys.save(outputStream);
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            GaloisKeys testKeys = new GaloisKeys();
            testKeys.load(context, inputStream);
            inputStream.close();
            GaloisKeys keys = new GaloisKeys();
            keygen.createGaloisKeys(keys);
            compareKswitchKeys(keys, testKeys, secretKey, context);
        }
    }

    private void compareKswitchKeys(KswitchKeys a, KswitchKeys b, SecretKey sk, SealContext context) {
        Assert.assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            PublicKey[] iterA = a.data()[i];
            PublicKey[] iterB = b.data()[i];
            Assert.assertEquals(iterA.length, iterB.length);
            for (int j = 0; j < iterA.length; j++) {
                PublicKey pk_a = iterA[j];
                PublicKey pk_b = iterB[j];
                compareError(pk_a.data(), pk_b.data(), sk, context);
            }
        }
    }

    private void compareError(Ciphertext aCt, Ciphertext bCt, SecretKey sk1, SealContext ctx) {
        long[] errorA = getError(aCt, sk1, ctx);
        long[] errorB = getError(bCt, sk1, ctx);
        Assert.assertEquals(errorA.length, errorB.length);
        Assert.assertTrue(UintCore.isEqualUint(errorA, errorB, errorA.length));
    }

    private long[] getError(Ciphertext encrypted, SecretKey sk2, SealContext ctx2) {
        ContextData ctx2Data = ctx2.getContextData(encrypted.parmsId());
        EncryptionParameters parms = ctx2Data.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int rnsPolyUint64Count = Common.mulSafe(coeffCount, coeffModulusSize, false);

        long[] error = new long[rnsPolyUint64Count];
        int errorOffset = 0;

        long[] copyOperand1 = new long[coeffCount];
        for (int i = 0; i < coeffModulusSize; i++) {
            // Initialize pointers for multiplication
            int encryptedPtr = encrypted.getPolyOffset(1) + (i * coeffCount);
            int secretKeyPtr = i * coeffCount;
            int errorPtr = errorOffset + (i * coeffCount);
            UintCore.setZeroUint(coeffCount, error, errorPtr);
            UintCore.setUint(encrypted.data()[encryptedPtr], coeffCount, copyOperand1);
            // compute c_{j+1} * s^{j+1}
            PolyArithmeticSmallMod.dyadicProductCoeffMod(
                copyOperand1, 0, sk2.data().getData(), secretKeyPtr, coeffCount, coeffModulus[i], copyOperand1, 0
            );
            // add c_{j+1} * s^{j+1} to destination
            PolyArithmeticSmallMod.addPolyCoeffMod(
                error, errorPtr, copyOperand1, 0, coeffCount, coeffModulus[i], error, errorPtr
            );
            // add c_0 into destination
            PolyArithmeticSmallMod.addPolyCoeffMod(
                error, errorPtr, encrypted.data(), (i * coeffCount), coeffCount, coeffModulus[i], error, errorPtr
            );
        }
        return error;
    }
}
