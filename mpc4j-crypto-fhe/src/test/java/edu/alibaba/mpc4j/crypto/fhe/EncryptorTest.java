package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Encrypt and Decrypt unit tests.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/tests/seal/encryptor.cpp.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/27
 */
public class EncryptorTest {

    @Test
    public void testBfvEncryptDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        {
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain;
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }
        {
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain;
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }
        {
            parms.setPolyModulusDegree(256);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain;
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly =
                "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                    + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                    + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                    + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                    + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain = new Plaintext(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }
        {
            parms.setPolyModulusDegree(256);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, keygen.secretKey());
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";
            Plaintext plain = new Plaintext(hexPoly);

            encryptor.encryptSymmetric(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }
    }

    @Test
    public void testBfvEncryptZeroDecrypt() throws IOException {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(64);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40}));
        SealContext context = new SealContext(parms, true, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk, keyGenerator.secretKey());
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext ct = new Ciphertext();
        Plaintext pt = new Plaintext();
        ParmsId nextParms = context.firstContextData().nextContextData().parmsId();
        {
            encryptor.encryptZero(ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), 1e-7);
            Assert.assertEquals(1, ct.correctionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZero(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), 1e-7);
            Assert.assertEquals(1, ct.correctionFactor());
            Assert.assertSame(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = encryptor.encryptZero().save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = ct.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(ct.scale(), 1.0, 1e-7);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            outputStream = new ByteArrayOutputStream();
            outSize = encryptor.encryptZero(nextParms).save(outputStream);
            outputStream.close();
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            inSize = ct.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(ct.scale(), 1.0, 1e-7);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            Assert.assertEquals(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
        {
            encryptor.encryptZeroSymmetric(ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), 1e-7);
            Assert.assertEquals(1, ct.correctionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZeroSymmetric(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), 1e-7);
            Assert.assertEquals(1, ct.correctionFactor());
            // when assigning parms_id, the value is same but the instance is different
            Assert.assertSame(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int outSize = encryptor.encryptZeroSymmetric().save(outputStream);
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int inSize = ct.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(ct.scale(), 1.0, 1e-7);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            outputStream = new ByteArrayOutputStream();
            outSize = encryptor.encryptZeroSymmetric(nextParms).save(outputStream);
            outputStream.close();
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            inSize = ct.load(context, inputStream);
            inputStream.close();
            Assert.assertEquals(outSize, inSize);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(ct.scale(), 1.0, 1e-7);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            Assert.assertEquals(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
    }

    // TODO: CKKSEncryptZeroDecrypt

    // TODO: CKKSEncryptDecrypt

    // TODO: BGVEncryptDecrypt

    // TODO: BGVEncryptZeroDecrypt
}
