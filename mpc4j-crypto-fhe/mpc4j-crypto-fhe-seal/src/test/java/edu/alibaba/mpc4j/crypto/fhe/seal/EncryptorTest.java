package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Arithmetic;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Encrypt and Decrypt unit tests.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/tests/seal/encryptor.cpp">encryptor.cpp</a>.
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
            Assert.assertEquals(1.0, ct.scale(), DOUBLE_EQUAL_PRECISION);
            Assert.assertEquals(1, ct.correctionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZero(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), DOUBLE_EQUAL_PRECISION);
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
            Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
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
            Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            Assert.assertEquals(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
        {
            encryptor.encryptZeroSymmetric(ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), DOUBLE_EQUAL_PRECISION);
            Assert.assertEquals(1, ct.correctionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZeroSymmetric(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertEquals(1.0, ct.scale(), DOUBLE_EQUAL_PRECISION);
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
            Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
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
            Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
            Assert.assertEquals(ct.correctionFactor(), 1L);
            Assert.assertEquals(ct.parmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
    }

    /**
     * double equal precision
     */
    private static final double DOUBLE_EQUAL_PRECISION = 1e-7;

    @Test
    public void testCkksEncryptZeroDecrypt() throws IOException {
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk, keygen.secretKey());
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            CkksEncoder encoder = new CkksEncoder(context);

            Ciphertext ct = new Ciphertext();
            Plaintext pt = new Plaintext();
            double[][] res = new double[parms.polyModulusDegree() >> 1][2];
            ParmsId next_parms = context.firstContextData().nextContextData().parmsId();
            {
                encryptor.encryptZero(ct);
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                decryptor.decrypt(ct, pt);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }

                encryptor.encryptZero(next_parms, ct);
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                Assert.assertEquals(ct.parmsId(), next_parms);
                decryptor.decrypt(ct, pt);
                Assert.assertEquals(pt.parmsId(), next_parms);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }
            }
            {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                encryptor.encryptZero().save(outputStream);
                byte[] data = outputStream.toByteArray();
                outputStream.close();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                ct.load(context, inputStream);
                inputStream.close();
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                decryptor.decrypt(ct, pt);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }

                outputStream = new ByteArrayOutputStream();
                encryptor.encryptZero(next_parms).save(outputStream);
                data = outputStream.toByteArray();
                outputStream.close();
                inputStream = new ByteArrayInputStream(data);
                ct.load(context, inputStream);
                inputStream.close();
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                Assert.assertEquals(ct.parmsId(), next_parms);
                decryptor.decrypt(ct, pt);
                Assert.assertEquals(pt.parmsId(), next_parms);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }
            }
            {
                encryptor.encryptZeroSymmetric(ct);
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                decryptor.decrypt(ct, pt);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }

                encryptor.encryptZeroSymmetric(next_parms, ct);
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                Assert.assertEquals(ct.parmsId(), next_parms);
                decryptor.decrypt(ct, pt);
                Assert.assertEquals(pt.parmsId(), next_parms);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }
            }
            {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                encryptor.encryptZeroSymmetric().save(outputStream);
                byte[] data = outputStream.toByteArray();
                outputStream.close();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                ct.load(context, inputStream);
                inputStream.close();
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                decryptor.decrypt(ct, pt);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }

                outputStream = new ByteArrayOutputStream();
                encryptor.encryptZeroSymmetric(next_parms).save(outputStream);
                data = outputStream.toByteArray();
                outputStream.close();
                inputStream = new ByteArrayInputStream(data);
                ct.load(context, inputStream);
                inputStream.close();
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(ct.isNttForm());
                Assert.assertEquals(ct.scale(), 1.0, DOUBLE_EQUAL_PRECISION);
                Assert.assertEquals(ct.correctionFactor(), 1L);
                ct.setScale(Math.pow(2.0, 20));
                Assert.assertEquals(ct.parmsId(), next_parms);
                decryptor.decrypt(ct, pt);
                Assert.assertEquals(pt.parmsId(), next_parms);
                encoder.decode(pt, res);
                for (double[] val : res) {
                    Assert.assertEquals(Arithmetic.real(val), 0.0, 0.01);
                    Assert.assertEquals(Arithmetic.imag(val), 0.0, 0.01);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptDecrypt() throws IOException {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // input consists of ones
            int slot_size = 32;
            parms.setPolyModulusDegree(2 * slot_size);
            parms.setCoeffModulus(CoeffModulus.create(2 * slot_size, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            double[][] input = new double[slot_size][2];
            // vector<complex<double>> input(slot_size, 1.0);
            Arrays.stream(input).forEach(v -> Arithmetic.set(v, 1.0));
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);

            // check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);

            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // input consists of zeros
            int slot_size = 32;
            parms.setPolyModulusDegree(2 * slot_size);
            parms.setCoeffModulus(CoeffModulus.create(2 * slot_size, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            double[][] input = new double[slot_size][2];
            // vector<complex<double>> input(slot_size, 0.0);
            Arrays.stream(input).forEach(v -> Arithmetic.set(v, 0.0));
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);

            // check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);

            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Input is a random mix of positive and negative integers
            int slot_size = 64;
            parms.setPolyModulusDegree(2 * slot_size);
            parms.setCoeffModulus(CoeffModulus.create(2 * slot_size, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int input_bound = 1 << 30;
            final double delta = 1L << 50;

            SecureRandom rand = new SecureRandom();
            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    // input[i] = pow(-1.0, rand() % 2) * static_cast<double>(rand() % input_bound);
                    Arithmetic.set(input[i], Math.pow(-1.0, rand.nextInt(2)) * (double) (rand.nextInt(input_bound)));
                }

                encoder.encode(input, context.firstParmsId(), delta, plain);
                encryptor.encrypt(plain, encrypted);

                // check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);

                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Input is a random mix of positive and negative integers
            int slot_size = 32;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            SecureRandom rand = new SecureRandom();
            int input_bound = 1 << 30;
            final double delta = 1L << 60;

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    // input[i] = pow(-1.0, rand() % 2) * static_cast<double>(rand() % input_bound);
                    Arithmetic.set(input[i], Math.pow(-1.0, rand.nextInt(2)) * (double) rand.nextInt(input_bound));
                }

                encoder.encode(input, context.firstParmsId(), delta, plain);
                encryptor.encrypt(plain, encrypted);

                // check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plain, output);

                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Encrypt at lower level
            int slot_size = 32;
            parms.setPolyModulusDegree(2 * slot_size);
            parms.setCoeffModulus(CoeffModulus.create(2 * slot_size, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 1.0);
            double[][] input = new double[slot_size][2];
            Arrays.stream(input).forEach(v -> Arithmetic.set(v, 1.0));
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;

            ContextData first_context_data = context.firstContextData();
            Assert.assertNotNull(first_context_data);
            ContextData second_context_data = first_context_data.nextContextData();
            Assert.assertNotNull(second_context_data);
            ParmsId second_parms_id = second_context_data.parmsId();

            encoder.encode(input, second_parms_id, delta, plain);
            encryptor.encrypt(plain, encrypted);

            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), second_parms_id);

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);

            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            encoder.encode(input, second_parms_id, delta, plain);
            encryptor.encrypt(plain).save(outputStream);
            byte[] data = outputStream.toByteArray();
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            encrypted.load(context, inputStream);
            inputStream.close();
            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), second_parms_id);
            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Encrypt at lower level
            int slot_size = 32;
            parms.setPolyModulusDegree(2 * slot_size);
            parms.setCoeffModulus(CoeffModulus.create(2 * slot_size, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, keygen.secretKey());
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            double[][] input = new double[slot_size][2];
            // vector<complex<double>> input (slot_size, 1.0);
            Arrays.stream(input).forEach(v -> Arithmetic.set(v, 1.0));
            // vector<complex<double>> output (slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;

            ContextData first_context_data = context.firstContextData();
            Assert.assertNotNull(first_context_data);
            ContextData second_context_data = first_context_data.nextContextData();
            Assert.assertNotNull(second_context_data);
            ParmsId second_parms_id = second_context_data.parmsId();

            encoder.encode(input, second_parms_id, delta, plain);
            encryptor.encryptSymmetric(plain, encrypted);
            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), second_parms_id);
            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            encoder.encode(input, second_parms_id, delta, plain);
            encryptor.encryptSymmetric(plain).save(outputStream);
            byte[] data = outputStream.toByteArray();
            outputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            encrypted.load(context, inputStream);
            inputStream.close();
            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), second_parms_id);
            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
    }

    // TODO: BGVEncryptDecrypt

    // TODO: BGVEncryptZeroDecrypt
}
