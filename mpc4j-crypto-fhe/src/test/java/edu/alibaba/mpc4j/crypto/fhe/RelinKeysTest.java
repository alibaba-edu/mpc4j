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
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * RelinKeys unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/relinkeys.cpp.
 *
 * @author Weiran Liu
 * @date 2023/12/14
 */
public class RelinKeysTest {

    @Test
    public void testRelinKeysSaveLoad() throws IOException {
        testRelinKeysSaveLoad(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testRelinKeysSaveLoad(SchemeType scheme) throws IOException {
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(1 << 6);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60,}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            RelinKeys keys = new RelinKeys();
            RelinKeys testKeys = new RelinKeys();
            keygen.createRelinKeys(keys);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = keys.save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = testKeys.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertEquals(keys.size(), testKeys.size());
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            for (int j = 0; j < testKeys.size(); j++) {
                for (int i = 0; i < testKeys.key(j + 2).length; i++) {
                    Assert.assertEquals(keys.key(j + 2)[i].data().size(), testKeys.key(j + 2)[i].data().size());
                    Assert.assertEquals(
                        keys.key(j + 2)[i].data().dynArray().size(),
                        testKeys.key(j + 2)[i].data().dynArray().size());
                    Assert.assertTrue(UintCore.isEqualUint(
                        keys.key(j + 2)[i].data().data(), testKeys.key(j + 2)[i].data().data(),
                        keys.key(j + 2)[i].data().dynArray().size()));
                }
            }
        }
        {
            EncryptionParameters parms = new EncryptionParameters(scheme);
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(1 << 6);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 50,}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            RelinKeys keys = new RelinKeys();
            RelinKeys testKeys = new RelinKeys();
            keygen.createRelinKeys(keys);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = keys.save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = testKeys.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertEquals(keys.size(), testKeys.size());
            Assert.assertEquals(keys.parmsId(), testKeys.parmsId());
            for (int j = 0; j < testKeys.size(); j++) {
                for (int i = 0; i < testKeys.key(j + 2).length; i++) {
                    Assert.assertEquals(keys.key(j + 2)[i].data().size(), testKeys.key(j + 2)[i].data().size());
                    Assert.assertEquals(
                        keys.key(j + 2)[i].data().dynArray().size(),
                        testKeys.key(j + 2)[i].data().dynArray().size());
                    Assert.assertTrue(UintCore.isEqualUint(
                        keys.key(j + 2)[i].data().data(), testKeys.key(j + 2)[i].data().data(),
                        keys.key(j + 2)[i].data().dynArray().size()));
                }
            }
        }
    }

    @Test
    public void testRelinKeySeededSaveLoad() throws IOException {
        testRelinKeySeededSaveLoad(SchemeType.BFV);
        // TODO: test BGV
    }

    private void testRelinKeySeededSaveLoad(SchemeType scheme) throws IOException {
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

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keygen.createRelinKeys().save(outputStream);
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            RelinKeys testKeys = new RelinKeys();
            testKeys.load(context, inputStream);
            inputStream.close();
            RelinKeys keys = new RelinKeys();
            keygen.createRelinKeys(keys);
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

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keygen.createRelinKeys().save(outputStream);
            outputStream.close();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            RelinKeys testKeys = new RelinKeys();
            testKeys.load(context, inputStream);
            inputStream.close();
            RelinKeys keys = new RelinKeys();
            keygen.createRelinKeys(keys);
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
                PublicKey pkA = iterA[j];
                PublicKey pkB = iterB[j];
                compareError(pkA.data(), pkB.data(), sk, context);
            }
        }
    }

    private void compareError(Ciphertext aCt, Ciphertext bCt, SecretKey sk1, SealContext context1) {
        long[] errorA = getError(aCt, sk1, context1);
        long[] errorB = getError(bCt, sk1, context1);
        Assert.assertEquals(errorA.length, errorB.length);
        Assert.assertTrue(UintCore.isEqualUint(errorA, errorB, errorA.length));
    }

    private long[] getError(Ciphertext encrypted, SecretKey sk2, SealContext context2) {
        ContextData contextData = context2.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
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
