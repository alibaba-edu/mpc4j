package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ValCheck;
import org.junit.Assert;
import org.junit.Test;

/**
 * Key Generator unit tests.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/tests/seal/">keygenerator.cpp</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/22
 */
public class KeyGeneratorTest {

    @Test
    public void testBfvKeyGeneration() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        {
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);

            Assert.assertThrows(IllegalArgumentException.class, keyGenerator::createRelinKeys);
            Assert.assertThrows(IllegalArgumentException.class, keyGenerator::createGaloisKeys);
        }
        {
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);

            RelinKeys evk = new RelinKeys();
            keyGenerator.createRelinKeys(evk);
            Assert.assertEquals(evk.parmsId(), context.keyParmsId());
            Assert.assertEquals(1, evk.key(2).length);
            for (PublicKey[] a : evk.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(evk, context));

            GaloisKeys galoisKeys = new GaloisKeys();
            keyGenerator.createGaloisKeys(galoisKeys);
            for (PublicKey[] a : galoisKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(galoisKeys, context));

            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertEquals(1, galoisKeys.key(3).length);
            Assert.assertEquals(10, galoisKeys.size());

            // new galoisKeys
            keyGenerator.createGaloisKeys(new int[]{1, 3, 5, 7}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(3));
            Assert.assertTrue(galoisKeys.hasKey(5));
            Assert.assertTrue(galoisKeys.hasKey(7));
            Assert.assertFalse(galoisKeys.hasKey(9));
            Assert.assertFalse(galoisKeys.hasKey(127));

            keyGenerator.createGaloisKeys(new int[]{1}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertFalse(galoisKeys.hasKey(3));
            Assert.assertFalse(galoisKeys.hasKey(127));
            Assert.assertEquals(1, galoisKeys.key(1).length);
            Assert.assertEquals(1, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{127}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertFalse(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(127));
            Assert.assertEquals(1, galoisKeys.key(127).length);
            Assert.assertEquals(1, galoisKeys.size());
        }
        {
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);

            RelinKeys relinKeys = new RelinKeys();
            keyGenerator.createRelinKeys(relinKeys);

            Assert.assertEquals(relinKeys.parmsId(), context.keyParmsId());
            Assert.assertEquals(2, relinKeys.key(2).length);
            for (PublicKey[] a : relinKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(relinKeys, context));

            GaloisKeys galoisKeys = new GaloisKeys();
            keyGenerator.createGaloisKeys(galoisKeys);
            for (PublicKey[] a : galoisKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(galoisKeys, context));
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertEquals(2, galoisKeys.key(3).length);
            Assert.assertEquals(14, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{1, 3, 5, 7}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(3));
            Assert.assertTrue(galoisKeys.hasKey(5));
            Assert.assertTrue(galoisKeys.hasKey(7));
            Assert.assertFalse(galoisKeys.hasKey(9));
            Assert.assertFalse(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(1).length);
            Assert.assertEquals(2, galoisKeys.key(3).length);
            Assert.assertEquals(2, galoisKeys.key(5).length);
            Assert.assertEquals(2, galoisKeys.key(7).length);
            Assert.assertEquals(4, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{1}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertFalse(galoisKeys.hasKey(3));
            Assert.assertFalse(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(1).length);
            Assert.assertEquals(1, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{511}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.keyParmsId());
            Assert.assertFalse(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(511).length);
            Assert.assertEquals(1, galoisKeys.size());
        }
    }

    @Test
    public void testCkksKeyGeneration() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            // ASSERT_THROW(auto evk = keygen.create_relin_keys(), logic_error);
            Assert.assertThrows(IllegalArgumentException.class, keygen::createRelinKeys);
            // ASSERT_THROW(auto galk = keygen.create_galois_keys(), logic_error);
            Assert.assertThrows(IllegalArgumentException.class, keygen::createGaloisKeys);
        }
        {
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            RelinKeys evk = new RelinKeys();
            keygen.createRelinKeys(evk);
            Assert.assertEquals(evk.parmsId(), context.keyParmsId());
            Assert.assertEquals(1L, evk.key(2).length);
            for (PublicKey[] a : evk.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(evk, context));

            GaloisKeys galks = new GaloisKeys();
            keygen.createGaloisKeys(galks);
            for (PublicKey[] a : galks.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(galks, context));

            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertEquals(1L, galks.key(3).length);
            Assert.assertEquals(10L, galks.size());

            keygen.createGaloisKeys(new int[]{1, 3, 5, 7}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertTrue(galks.hasKey(1));
            Assert.assertTrue(galks.hasKey(3));
            Assert.assertTrue(galks.hasKey(5));
            Assert.assertTrue(galks.hasKey(7));
            Assert.assertFalse(galks.hasKey(9));
            Assert.assertFalse(galks.hasKey(127));
            Assert.assertEquals(1L, galks.key(1).length);
            Assert.assertEquals(1L, galks.key(3).length);
            Assert.assertEquals(1L, galks.key(5).length);
            Assert.assertEquals(1L, galks.key(7).length);
            Assert.assertEquals(4L, galks.size());

            keygen.createGaloisKeys(new int[]{1}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertTrue(galks.hasKey(1));
            Assert.assertFalse(galks.hasKey(3));
            Assert.assertFalse(galks.hasKey(127));
            Assert.assertEquals(1L, galks.key(1).length);
            Assert.assertEquals(1L, galks.size());

            keygen.createGaloisKeys(new int[]{127}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertFalse(galks.hasKey(1));
            Assert.assertTrue(galks.hasKey(127));
            Assert.assertEquals(1L, galks.key(127).length);
            Assert.assertEquals(1L, galks.size());
        }
        {
            parms.setPolyModulusDegree(256);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            RelinKeys evk = new RelinKeys();
            keygen.createRelinKeys(evk);
            Assert.assertEquals(evk.parmsId(), context.keyParmsId());
            Assert.assertEquals(2L, evk.key(2).length);
            for (PublicKey[] a : evk.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(evk, context));

            GaloisKeys galks = new GaloisKeys();
            keygen.createGaloisKeys(galks);
            for (PublicKey[] a : galks.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValCheck.isValidFor(galks, context));

            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertEquals(2L, galks.key(3).length);
            Assert.assertEquals(14L, galks.size());

            keygen.createGaloisKeys(new int[]{1, 3, 5, 7}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertTrue(galks.hasKey(1));
            Assert.assertTrue(galks.hasKey(3));
            Assert.assertTrue(galks.hasKey(5));
            Assert.assertTrue(galks.hasKey(7));
            Assert.assertFalse(galks.hasKey(9));
            Assert.assertFalse(galks.hasKey(511));
            Assert.assertEquals(2L, galks.key(1).length);
            Assert.assertEquals(2L, galks.key(3).length);
            Assert.assertEquals(2L, galks.key(5).length);
            Assert.assertEquals(2L, galks.key(7).length);
            Assert.assertEquals(4L, galks.size());

            keygen.createGaloisKeys(new int[]{1}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertTrue(galks.hasKey(1));
            Assert.assertFalse(galks.hasKey(3));
            Assert.assertFalse(galks.hasKey(511));
            Assert.assertEquals(2L, galks.key(1).length);
            Assert.assertEquals(1L, galks.size());

            keygen.createGaloisKeys(new int[]{511}, galks);
            Assert.assertEquals(galks.parmsId(), context.keyParmsId());
            Assert.assertFalse(galks.hasKey(1));
            Assert.assertTrue(galks.hasKey(511));
            Assert.assertEquals(2L, galks.key(511).length);
            Assert.assertEquals(1L, galks.size());
        }
    }

    @Test
    public void testConstructor() {
        testConstructor(SchemeType.BFV);
        // TODO: test BGV
    }

    @SuppressWarnings("SameParameterValue")
    private void testConstructor(SchemeType scheme) {
        EncryptionParameters parms = new EncryptionParameters(scheme);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(65537);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{60, 50, 40}));
        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        Evaluator evaluator = new Evaluator(context);

        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);
        SecretKey sk = keygen.secretKey();
        RelinKeys rlk = new RelinKeys();
        keygen.createRelinKeys(rlk);
        GaloisKeys galk = new GaloisKeys();
        keygen.createGaloisKeys(galk);

        Assert.assertTrue(ValCheck.isValidFor(rlk, context));
        Assert.assertTrue(ValCheck.isValidFor(galk, context));

        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, sk);
        Plaintext pt = new Plaintext("1x^2 + 2");
        Plaintext ptres = new Plaintext();
        Ciphertext ct = new Ciphertext();
        encryptor.encrypt(pt, ct);
        evaluator.squareInplace(ct);
        evaluator.relinearizeInplace(ct, rlk);
        decryptor.decrypt(ct, ptres);
        Assert.assertEquals("1x^4 + 4x^2 + 4", ptres.toString());

        KeyGenerator keygen2 = new KeyGenerator(context, sk);
        SecretKey sk2 = keygen.secretKey();
        PublicKey pk2 = new PublicKey();
        keygen2.createPublicKey(pk2);
        Assert.assertEquals(sk2.data(), sk.data());

        RelinKeys rlk2 = new RelinKeys();
        keygen2.createRelinKeys(rlk2);
        GaloisKeys galk2 = new GaloisKeys();
        keygen2.createGaloisKeys(galk2);

        Assert.assertTrue(ValCheck.isValidFor(rlk2, context));
        Assert.assertTrue(ValCheck.isValidFor(galk2, context));

        Encryptor encryptor2 = new Encryptor(context, pk2);
        Decryptor decryptor2 = new Decryptor(context, sk2);
        pt = new Plaintext("1x^2 + 2");
        ptres.setZero();
        encryptor2.encrypt(pt, ct);
        evaluator.squareInplace(ct);
        evaluator.relinearizeInplace(ct, rlk2);
        decryptor2.decrypt(ct, ptres);
        Assert.assertEquals("1x^4 + 4x^2 + 4", ptres.toString());

        PublicKey pk3 = new PublicKey();
        keygen2.createPublicKey(pk3);

        // There is a small random chance for this to fail
        for (int i = 0; i < pk3.data().dynArray().size(); i++) {
            Assert.assertNotEquals(pk3.data().data()[i], pk2.data().data()[i]);
        }
    }
}
