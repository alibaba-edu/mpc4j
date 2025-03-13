package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Arithmetic;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Evaluator Test.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/tests/seal/evaluator.cpp">evaluator.cpp</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/10/5
 */
public class EvaluatorTest {

    @Test
    public void testBfvEncryptNegateDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keygen.secretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain;

        plain = new Plaintext(
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
        );
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
            plain.toString(),
            "3Fx^28 + 3Fx^25 + 3Fx^21 + 3Fx^20 + 3Fx^18 + 3Fx^14 + 3Fx^12 + 3Fx^10 + 3Fx^9 + 3Fx^6 + 3Fx^5 + 3Fx^4 + 3Fx^3"
        );
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("0");
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1");
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "3F");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("3F");
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^1");
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "3Fx^1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("3Fx^2 + 3F");
        encryptor.encrypt(plain, encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
    }

    @Test
    public void testBfvEncryptAddDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keygen.secretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2;

        plain1 = new Plaintext(
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
        );
        plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + 1x^6 + 2x^5 + 1x^4 + 1x^3 + 1"
        );
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("0");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals("0", plain.toString());
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("1x^2 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("3Fx^1 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 3Fx^1");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
        plain2 = new Plaintext("1x^1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3Fx^2 + 3F");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("2x^2 + 1x^1 + 3");
        plain2 = new Plaintext("3x^3 + 4x^2 + 5x^1 + 6");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3x^3 + 6x^2 + 6x^1 + 9");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3x^5 + 1x^4 + 4x^3 + 1");
        plain2 = new Plaintext("5x^2 + 9x^1 + 2");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.addInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3x^5 + 1x^4 + 4x^3 + 5x^2 + 9x^1 + 3");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
    }

    // TODO: BGVEncryptNegateDecrypt

    @Test
    public void testCkksEncryptAddDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Adding two zero vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;
            encoder.encode(input, context.firstParmsId(), delta, plain);

            encryptor.encrypt(plain, encrypted);
            evaluator.addInplace(encrypted, encrypted);

            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Adding two random vectors 100 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int data_bound = (1 << 30);
            final double delta = 1 << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 100; expCount++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.add(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                evaluator.addInplace(encrypted1, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Adding two random vectors 100 times
            int slot_size = 8;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            int data_bound = (1 << 30);
            final double delta = 1 << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 100; expCount++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.add(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                evaluator.addInplace(encrypted1, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptAddPlainDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Adding two zero vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;
            encoder.encode(input, context.firstParmsId(), delta, plain);

            encryptor.encrypt(plain, encrypted);
            evaluator.addPlainInplace(encrypted, plain);

            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Adding two random vectors 50 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int data_bound = (1 << 8);
            final double delta = 1L << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 50; expCount++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.add(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.addPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Adding two random vectors 50 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            double input2;
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int data_bound = (1 << 8);
            final double delta = 1L << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 50; expCount++) {
                // input2 = static_cast<double>(rand() % (data_bound * data_bound)) / data_bound;
                input2 = rand.nextLong((long) data_bound * data_bound) / (double) data_bound;
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    // expected[i] = input1[i] + input2;
                    Arithmetic.add(expected[i], input1[i], input2);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.addPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Adding two random vectors 50 times
            int slot_size = 8;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            double input2;
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            int data_bound = (1 << 8);
            final double delta = 1L << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 50; expCount++) {
                // input2 = static_cast<double>(rand() % (data_bound * data_bound)) / data_bound;
                input2 = rand.nextLong((long) data_bound * data_bound) / (double) data_bound;
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.add(expected[i], input1[i], input2);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.addPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptSubPlainDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Subtracting two zero vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 16;
            encoder.encode(input, context.firstParmsId(), delta, plain);

            encryptor.encrypt(plain, encrypted);
            evaluator.addPlainInplace(encrypted, plain);

            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Subtracting two random vectors 100 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int data_bound = (1 << 8);
            final double delta = 1L << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 100; expCount++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.sub(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.subPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Subtracting two random vectors 100 times
            int slot_size = 8;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            int data_bound = (1 << 8);
            final double delta = 1L << 16;

            SecureRandom rand = new SecureRandom();

            for (int expCount = 0; expCount < 100; expCount++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.sub(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.subPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testBfvEncryptSubDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keygen.secretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2;

        plain1 = new Plaintext(
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
        );
        plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.subInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F"
        );
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("0");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.subInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("1x^2 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.subInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3Fx^2 + 3F");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("3Fx^1 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.subInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1x^1 + 2");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
        plain2 = new Plaintext("1x^1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        evaluator.subInplace(encrypted1, encrypted2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3Fx^2 + 3Ex^1 + 3F");
        Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
    }

    @Test
    public void testBfvEncryptAddPlainDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keygen.secretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2;

        plain1 = new Plaintext(
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
        );
        plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.addPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + 1x^6 + 2x^5 + 1x^4 + 1x^3 + 1");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("0");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.addPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("1x^2 + 1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.addPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("3Fx^1 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.addPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 3Fx^1");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
        plain2 = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.addPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
    }

    @Test
    public void bfvEncryptSubPlainDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted1 = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2;

        plain1 = new Plaintext(
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
        );
        plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.subPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F"
        );
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("0");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.subPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("0");
        plain2 = new Plaintext("1x^2 + 1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.subPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3Fx^2 + 3F");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("3Fx^1 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.subPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1x^1 + 2");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
        plain2 = new Plaintext("1x^1");
        encryptor.encrypt(plain1, encrypted1);
        evaluator.subPlainInplace(encrypted1, plain2);
        decryptor.decrypt(encrypted1, plain);
        Assert.assertEquals(plain.toString(), "3Fx^2 + 3Ex^1 + 3F");
        Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
    }

    @Test
    public void bfvEncryptMultiplyPlainDecrypt() {
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1, plain2;

            plain1 = new Plaintext(
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                    + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                    + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                    + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1x^1 + 1");
            plain2 = new Plaintext("1x^2");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(plain.toString(), "1x^4 + 1x^3 + 1x^2");
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1x^1 + 1");
            plain2 = new Plaintext("1x^1");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(plain.toString(), "1x^3 + 1x^2 + 1x^1");
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1");
            plain2 = new Plaintext("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(plain.toString(), "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
            plain2 = new Plaintext("1x^1");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(plain.toString(), "3Fx^3 + 3Fx^2 + 3Fx^1");
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus((1L << 20) - 1);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1, plain2;

            plain1 = new Plaintext(
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2 = new Plaintext("1");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            plain2 = new Plaintext("5");
            encryptor.encrypt(plain1, encrypted);
            evaluator.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                plain.toString(),
                "5x^28 + 5x^25 + 5x^21 + 5x^20 + 5x^18 + 5x^14 + 5x^12 + 5x^10 + 5x^9 + 5x^6 + 5x^5 + 5x^4 + 5x^3"
            );
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 20);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            long[] truth = new long[batchEncoder.slotCount()];
            Arrays.fill(truth, 7);
            batchEncoder.encodeInt64(truth, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(truth, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            Arrays.fill(truth, -7);
            batchEncoder.encodeInt64(truth, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(truth, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 40);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            // First test with constant plaintext
            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            long[] truth = new long[batchEncoder.slotCount()];
            Arrays.fill(truth, 7);
            batchEncoder.encodeInt64(truth, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(truth, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            Arrays.fill(truth, -7);
            batchEncoder.encodeInt64(truth, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(truth, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            // Now test a non-constant plaintext
            long[] input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, 7);
            input[input.length - 1] = 1;
            long[] truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;
            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(truthResult, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

            input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, -7);
            input[input.length - 1] = 1;
            truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;
            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(truthResult, result);
            Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
        }
    }

    @Test
    public void bfvEncryptMultiplyDecrypt() {
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Evaluator evaluator = new Evaluator(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1, plain2;

            plain1 = new Plaintext(
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2 = new Plaintext(
                "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                    + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                    + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                    + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("0");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1x^1 + 1");
            plain2 = new Plaintext("1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^2 + 1x^1 + 1");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1");
            plain2 = new Plaintext("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^16");
            plain2 = new Plaintext("1x^8");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^24");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus((1L << 60) - 1L);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Evaluator evaluator = new Evaluator(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1, plain2;

            plain1 = new Plaintext(
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2 = new Plaintext(
                "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                    + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                    + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                    + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("0");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1x^1 + 1");
            plain2 = new Plaintext("1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^2 + 1x^1 + 1");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1");
            plain2 = new Plaintext("FFFFFFFFFFFFFFEx^1 + FFFFFFFFFFFFFFE");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                plain.toString(), "FFFFFFFFFFFFFFEx^3 + FFFFFFFFFFFFFFEx^2 + FFFFFFFFFFFFFFEx^1 + FFFFFFFFFFFFFFE");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^16");
            plain2 = new Plaintext("1x^8");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^24");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1L << 6);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Evaluator evaluator = new Evaluator(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1, plain2;

            plain1 = new Plaintext(
                "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2 = new Plaintext("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                    + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                    + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                    + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("0");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("0");
            plain2 = new Plaintext("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "0");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1x^1 + 1");
            plain2 = new Plaintext("1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^2 + 1x^1 + 1");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^2 + 1");
            plain2 = new Plaintext("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());

            plain1 = new Plaintext("1x^16");
            plain2 = new Plaintext("1x^8");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluator.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(plain.toString(), "1x^24");
            Assert.assertSame(encrypted2.parmsId(), encrypted1.parmsId());
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
        }
        {
            EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1L << 8);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Evaluator evaluator = new Evaluator(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1;

            plain1 = new Plaintext("1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1");
            encryptor.encrypt(plain1, encrypted1);
            evaluator.multiply(encrypted1, encrypted1, encrypted1);
            evaluator.multiply(encrypted1, encrypted1, encrypted1);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                plain.toString(),
                "1x^24 + 4x^23 + Ax^22 + 14x^21 + 1Fx^20 + 2Cx^19 + 3Cx^18 + 4Cx^17 + 5Fx^16 + "
                    + "6Cx^15 + 70x^14 + 74x^13 + 71x^12 + 6Cx^11 + 64x^10 + 50x^9 + 40x^8 + 34x^7 + "
                    + "26x^6 + 1Cx^5 + 11x^4 + 8x^3 + 6x^2 + 4x^1 + 1"
            );
            Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
        }
    }


    // TODO: BGVEncryptSubDecrypt

    // TODO: BGVEncryptAddPlainDecrypt

    // TODO: BGVEncryptSubPlainDecrypt

    // TODO: BGVEncryptMultiplyPlainDecrypt

    // TODO: BGVEncryptMultiplyDecrypt

    @Test
    public void testBfvReLinearize() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40, 40}));

        SealContext context = new SealContext(parms, true, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain;
        Plaintext plain2 = new Plaintext();

        plain = new Plaintext("0");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain, plain2);

        plain = new Plaintext("1x^10 + 2");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain2.toString(), "1x^20 + 4x^10 + 4");

        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain2.toString(), "1x^40 + 8x^30 + 18x^20 + 20x^10 + 10");

        // Relinearization with modulus switching
        plain = new Plaintext("1x^10 + 2");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        evaluator.modSwitchToNextInplace(encrypted);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain2.toString(), "1x^20 + 4x^10 + 4");

        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        evaluator.modSwitchToNextInplace(encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        evaluator.modSwitchToNextInplace(encrypted);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain2.toString(), "1x^40 + 8x^30 + 18x^20 + 20x^10 + 10");
    }

    // TODO: BGVRelinearize

    @Test
    public void testCkksEncryptNaiveMultiplyDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Multiplying two zero vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{30, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1 << 30;
            encoder.encode(input, context.firstParmsId(), delta, plain);

            encryptor.encrypt(plain, encrypted);
            evaluator.multiplyInplace(encrypted, encrypted);

            // Check correctness of encryption
            Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

            decryptor.decrypt(encrypted, plainRes);
            encoder.decode(plainRes, output);
            for (int i = 0; i < slot_size; i++) {
                double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Multiplying two random vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            final double delta = 1L << 40;

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    // input1[i] = static_cast<double>(rand() % data_bound);
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    // input2[i] = static_cast<double>(rand() % data_bound);
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    // expected[i] = input1[i] * input2[i];
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                evaluator.multiplyInplace(encrypted1, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors
            int slot_size = 16;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            double[][] input1 = new double[slot_size][2];
            double[][] input2 = new double[slot_size][2];
            double[][] expected = new double[slot_size][2];
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];
            final double delta = 1L << 40;

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    // input1[i] = static_cast < double>(rand() % data_bound);
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    // input2[i] = static_cast < double>(rand() % data_bound);
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    // expected[i] = input1[i] * input2[i];
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                evaluator.multiplyInplace(encrypted1, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptMultiplyByNumberDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Multiplying two random vectors by an integer
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            long input2;
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int iExp = 0; iExp < 50; iExp++) {
                input2 = Math.max(rand.nextInt(data_bound), 1);
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.multiplyPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors by an integer
            int slot_size = 8;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            long input2;
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int iExp = 0; iExp < 50; iExp++) {
                input2 = Math.max(rand.nextInt(data_bound), 1);
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[parms.polyModulusDegree() >> 1][2];
                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.multiplyPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors by a double
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            double input2;
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int iExp = 0; iExp < 50; iExp++) {
                input2 = rand.nextLong((long) data_bound * data_bound) / (double) (data_bound);
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2);
                }

                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.multiplyPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors by a double
            int slot_size = 16;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 2.1);
            double[][] input1 = new double[slot_size][2];
            Arrays.stream(input1).forEach(v -> Arithmetic.set(v, 2.1));
            double input2;
            // vector<complex<double>> expected(slot_size, 2.1);
            double[][] expected = new double[slot_size][2];
            Arrays.stream(expected).forEach(v -> Arithmetic.set(v, 2.1));
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            int data_bound = (1 << 10);
            SecureRandom rand = new SecureRandom();

            for (int iExp = 0; iExp < 50; iExp++) {
                input2 = rand.nextLong((long) data_bound * data_bound) / (double) (data_bound);
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2);
                }

                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                evaluator.multiplyPlainInplace(encrypted1, plain2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptMultiplyRelinDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Multiplying two random vectors 50 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            int data_bound = 1 << 10;

            SecureRandom rand = new SecureRandom();
            for (int round = 0; round < 50; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors 50 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            int data_bound = 1 << 10;

            SecureRandom rand = new SecureRandom();
            for (int round = 0; round < 50; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                final double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors 50 times
            int slot_size = 2;
            parms.setPolyModulusDegree(8);
            parms.setCoeffModulus(CoeffModulus.create(8, new int[]{60, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];
            int data_bound = 1 << 10;
            final double delta = 1L << 40;

            SecureRandom rand = new SecureRandom();
            for (int round = 0; round < 50; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }

                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                // Evaluator.relinearize_inplace(encrypted1, rlk);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptSquareRelinDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Squaring two random vectors 100 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            int data_bound = 1 << 7;
            SecureRandom rand = new SecureRandom();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input[i], input[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                final double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                // Evaluator.square_inplace(encrypted);
                evaluator.multiplyInplace(encrypted, encrypted);
                evaluator.relinearizeInplace(encrypted, rlk);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Squaring two random vectors 100 times
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            int data_bound = 1 << 7;
            SecureRandom rand = new SecureRandom();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input[i], input[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                final double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

                // Evaluator.square_inplace(encrypted);
                evaluator.multiplyInplace(encrypted, encrypted);
                evaluator.relinearizeInplace(encrypted, rlk);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Squaring two random vectors 100 times
            int slot_size = 16;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 30, 30, 30}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            int data_bound = 1 << 7;
            SecureRandom rand = new SecureRandom();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input[i], input[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[parms.polyModulusDegree() >> 1][2];
                final double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

                // Evaluator.square_inplace(encrypted);
                evaluator.multiplyInplace(encrypted, encrypted);
                evaluator.relinearizeInplace(encrypted, rlk);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptMultiplyRelinRescaleDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Multiplying two random vectors 100 times
            int slot_size = 64;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{30, 30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 7;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertSame(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertSame(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.rescaleToNextInplace(encrypted1);

                // Check correctness of modulus switching
                Assert.assertSame(encrypted1.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors 100 times
            int slot_size = 16;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30, 30}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 7;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[parms.polyModulusDegree() >> 1][2];
                double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.rescaleToNextInplace(encrypted1);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted1.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplying two random vectors 100 times
            int slot_size = 16;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{60, 60, 60, 60, 60}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            // Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 7;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                    Arithmetic.muli(expected[i], input2[i]);
                }

                double[][] output = new double[parms.polyModulusDegree() >> 1][2];
                double delta = 1L << 60;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);

                // Scale down by two levels
                ParmsId target_parms = context.firstContextData().nextContextData().nextContextData().parmsId();
                evaluator.rescaleToInplace(encrypted1, target_parms);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted1.parmsId(), target_parms);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }

            // Test with inverted order: rescale then relin
            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 7;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                    Arithmetic.muli(expected[i], input2[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[parms.polyModulusDegree() >> 1][2];
                double delta = 1L << 50;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());

                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.multiplyInplace(encrypted1, encrypted2);

                // Scale down by two levels
                ParmsId target_parms = context.firstContextData().nextContextData().nextContextData().parmsId();
                evaluator.rescaleToInplace(encrypted1, target_parms);

                // Relinearize now
                evaluator.relinearizeInplace(encrypted1, rlk);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted1.parmsId(), target_parms);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptSquareRelinRescaleDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Squaring two random vectors 100 times
            int slot_size = 64;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{50, 50, 50}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            int data_bound = 1 << 8;

            for (int round = 0; round < 100; round++) {
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input[i], input[i]);
                }

                double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                evaluator.squareInplace(encrypted);
                evaluator.relinearizeInplace(encrypted, rlk);
                evaluator.rescaleToNextInplace(encrypted);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Squaring two random vectors 100 times
            int slot_size = 16;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{50, 50, 50}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input (slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output (slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];
            // vector<complex<double>> expected (slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            int data_bound = 1 << 8;

            for (int round = 0; round < 100; round++) {
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input[i], input[i]);
                }

                double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                evaluator.squareInplace(encrypted);
                evaluator.relinearizeInplace(encrypted, rlk);
                evaluator.rescaleToNextInplace(encrypted);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptModSwitchDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Modulus switching without rescaling for random vectors
            int slot_size = 64;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{60, 60, 60, 60, 60}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            int data_bound = 1 << 30;
            SecureRandom rand = new SecureRandom();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                }

                double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                // Not inplace
                Ciphertext destination = new Ciphertext();
                evaluator.modSwitchToNext(encrypted, destination);

                // Check correctness of modulus switching
                Assert.assertEquals(destination.parmsId(), next_parms_id);

                decryptor.decrypt(destination, plainRes);
                encoder.decode(plainRes, output);

                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }

                // Inplace
                evaluator.modSwitchToNextInplace(encrypted);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Modulus switching without rescaling for random vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{40, 40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            int data_bound = 1 << 30;
            SecureRandom rand = new SecureRandom();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[slot_size][2];

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                }

                double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                // Not inplace
                Ciphertext destination = new Ciphertext();
                evaluator.modSwitchToNext(encrypted, destination);

                // Check correctness of modulus switching
                Assert.assertEquals(destination.parmsId(), next_parms_id);

                decryptor.decrypt(destination, plainRes);
                encoder.decode(plainRes, output);

                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }

                // Inplace
                evaluator.modSwitchToNextInplace(encrypted);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Modulus switching without rescaling for random vectors
            int slot_size = 32;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            int data_bound = 1 << 30;
            SecureRandom rand = new SecureRandom();

            // vector<complex<double>> input(slot_size, 0.0);
            double[][] input = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plainRes = new Plaintext();

            for (int round = 0; round < 100; round++) {
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input[i], rand.nextInt(data_bound));
                }

                double delta = 1L << 40;
                encoder.encode(input, context.firstParmsId(), delta, plain);

                encryptor.encrypt(plain, encrypted);

                // Check correctness of encryption
                Assert.assertEquals(encrypted.parmsId(), context.firstParmsId());

                // Not inplace
                Ciphertext destination = new Ciphertext();
                evaluator.modSwitchToNext(encrypted, destination);

                // Check correctness of modulus switching
                Assert.assertEquals(destination.parmsId(), next_parms_id);

                decryptor.decrypt(destination, plainRes);
                encoder.decode(plainRes, output);

                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }

                // Inplace
                evaluator.modSwitchToNextInplace(encrypted);

                // Check correctness of modulus switching
                Assert.assertEquals(encrypted.parmsId(), next_parms_id);

                decryptor.decrypt(encrypted, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(input[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    @Test
    public void testCkksEncryptMultiplyRelinRescaleModSwitchAddDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Multiplication and addition without rescaling for random vectors
            int slot_size = 64;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{50, 50, 50}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext encrypted3 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plain3 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> input3(slot_size, 0.0);
            double[][] input3 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];

            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 8;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                    Arithmetic.addi(expected[i], input3[i]);
                }

                // vector<complex<double>> output(slot_size);
                double[][] output = new double[slot_size][2];
                double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);
                encoder.encode(input3, context.firstParmsId(), delta * delta, plain3);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                encryptor.encrypt(plain3, encrypted3);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted3.parmsId(), context.firstParmsId());

                // Enc1*enc2
                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.rescaleToNextInplace(encrypted1);

                // Check correctness of modulus switching with rescaling
                Assert.assertEquals(encrypted1.parmsId(), next_parms_id);

                // Move enc3 to the level of enc1 * enc2
                evaluator.rescaleToInplace(encrypted3, next_parms_id);

                // Enc1*enc2 + enc3
                evaluator.addInplace(encrypted1, encrypted3);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            // Multiplication and addition without rescaling for random vectors
            int slot_size = 16;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{50, 50, 50}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            ParmsId next_parms_id = context.firstContextData().nextContextData().parmsId();
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            RelinKeys rlk = new RelinKeys();
            keygen.createRelinKeys(rlk);

            CkksEncoder encoder = new CkksEncoder(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            Evaluator evaluator = new Evaluator(context);

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext encrypted3 = new Ciphertext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            Plaintext plain3 = new Plaintext();
            Plaintext plainRes = new Plaintext();

            // vector<complex<double>> input1(slot_size, 0.0);
            double[][] input1 = new double[slot_size][2];
            // vector<complex<double>> input2(slot_size, 0.0);
            double[][] input2 = new double[slot_size][2];
            // vector<complex<double>> input3(slot_size, 0.0);
            double[][] input3 = new double[slot_size][2];
            // vector<complex<double>> expected(slot_size, 0.0);
            double[][] expected = new double[slot_size][2];
            // vector<complex<double>> output(slot_size);
            double[][] output = new double[parms.polyModulusDegree() >> 1][2];

            for (int round = 0; round < 100; round++) {
                int data_bound = 1 << 8;
                SecureRandom rand = new SecureRandom();
                for (int i = 0; i < slot_size; i++) {
                    Arithmetic.set(input1[i], rand.nextInt(data_bound));
                    Arithmetic.set(input2[i], rand.nextInt(data_bound));
                    Arithmetic.mul(expected[i], input1[i], input2[i]);
                    Arithmetic.addi(expected[i], input3[i]);
                }

                double delta = 1L << 40;
                encoder.encode(input1, context.firstParmsId(), delta, plain1);
                encoder.encode(input2, context.firstParmsId(), delta, plain2);
                encoder.encode(input3, context.firstParmsId(), delta * delta, plain3);

                encryptor.encrypt(plain1, encrypted1);
                encryptor.encrypt(plain2, encrypted2);
                encryptor.encrypt(plain3, encrypted3);

                // Check correctness of encryption
                Assert.assertEquals(encrypted1.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted2.parmsId(), context.firstParmsId());
                // Check correctness of encryption
                Assert.assertEquals(encrypted3.parmsId(), context.firstParmsId());

                // Enc1*enc2
                evaluator.multiplyInplace(encrypted1, encrypted2);
                evaluator.relinearizeInplace(encrypted1, rlk);
                evaluator.rescaleToNextInplace(encrypted1);

                // Check correctness of modulus switching with rescaling
                Assert.assertEquals(encrypted1.parmsId(), next_parms_id);

                // Move enc3 to the level of enc1 * enc2
                evaluator.rescaleToInplace(encrypted3, next_parms_id);

                // Enc1*enc2 + enc3
                evaluator.addInplace(encrypted1, encrypted3);

                decryptor.decrypt(encrypted1, plainRes);
                encoder.decode(plainRes, output);
                for (int i = 0; i < slot_size; i++) {
                    double tmp = Math.abs(Arithmetic.real(expected[i]) - Arithmetic.real(output[i]));
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
    }

    /**
     * rotate precision
     */
    private static final double ROTATE_PRECISION = 1e-9;

    @Test
    public void testCkksEncryptRotateDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Maximal number of slots
            int slot_size = 4;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            GaloisKeys glk = new GaloisKeys();
            keygen.createGaloisKeys(glk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            CkksEncoder encoder = new CkksEncoder(context);
            final double delta = 1L << 30;

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            // vector<complex<double>> input{ complex<double>(1, 1), complex<double>(2, 2), complex<double>(3, 3),
            //     complex<double>(4, 4) };
            // input.resize(slot_size);
            double[][] input = new double[][]{
                new double[]{1, 1}, new double[]{2, 2}, new double[]{3, 3}, new double[]{4, 4},
            };

            // vector<complex<double>> output(slot_size, 0);
            double[][] output = new double[slot_size][2];

            encoder.encode(input, context.firstParmsId(), delta, plain);
            int shift = 1;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 2;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 3;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.complexConjugateInplace(encrypted, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[i].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[i]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(-input[i].imag(), round(output[i].imag()));
                Assert.assertEquals(-Arithmetic.imag(input[i]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }
        }
        {
            int slot_size = 32;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            GaloisKeys glk = new GaloisKeys();
            keygen.createGaloisKeys(glk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            CkksEncoder encoder = new CkksEncoder(context);
            final double delta = 1L << 30;

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            // vector<complex<double>> input{ complex<double>(1, 1), complex<double>(2, 2), complex<double>(3, 3),
            //     complex<double>(4, 4) };
            // input.resize(slot_size);
            double[][] input = new double[slot_size][2];
            input[0] = new double[]{1, 1};
            input[1] = new double[]{2, 2};
            input[2] = new double[]{3, 3};
            input[3] = new double[]{4, 4};

            double[][] output = new double[slot_size][2];

            encoder.encode(input, context.firstParmsId(), delta, plain);
            int shift = 1;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].real()), round(output[i].real()));
                Assert.assertEquals(Math.round(Arithmetic.real(input[(i + shift) % slot_size])), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].imag()), round(output[i].imag()));
                Assert.assertEquals(Math.round(Arithmetic.imag(input[(i + shift) % slot_size])), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 2;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].real()), round(output[i].real()));
                Assert.assertEquals(Math.round(Arithmetic.real(input[(i + shift) % slot_size])), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].imag()), round(output[i].imag()));
                Assert.assertEquals(Math.round(Arithmetic.imag(input[(i + shift) % slot_size])), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 3;
            encryptor.encrypt(plain, encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].real()), round(output[i].real()));
                Assert.assertEquals(Math.round(Arithmetic.real(input[(i + shift) % slot_size])), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(round(input[(i + static_cast<size_t>(shift)) % slot_size].imag()), round(output[i].imag()));
                Assert.assertEquals(Math.round(Arithmetic.imag(input[(i + shift) % slot_size])), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.complexConjugateInplace(encrypted, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[i].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[i]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(-input[i].imag(), round(output[i].imag()));
                Assert.assertEquals(-Arithmetic.imag(input[i]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }
        }
    }

    @Test
    public void testCkksEncryptRescaleRotateDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            // Maximal number of slots
            int slot_size = 4;
            parms.setPolyModulusDegree(slot_size * 2);
            parms.setCoeffModulus(CoeffModulus.create(slot_size * 2, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            GaloisKeys glk = new GaloisKeys();
            keygen.createGaloisKeys(glk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            CkksEncoder encoder = new CkksEncoder(context);
            final double delta = Math.pow(2.0, 70);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            // vector<complex<double>> input{ complex<double>(1, 1), complex<double>(2, 2), complex<double>(3, 3),
            //     complex<double>(4, 4) };
            // input.resize(slot_size);
            double[][] input = new double[][]{
                new double[]{1, 1}, new double[]{2, 2}, new double[]{3, 3}, new double[]{4, 4},
            };

            double[][] output = new double[slot_size][2];

            encoder.encode(input, context.firstParmsId(), delta, plain);
            int shift = 1;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 2;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 3;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.complexConjugateInplace(encrypted, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[i].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[i]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(-input[i].imag(), round(output[i].imag()));
                Assert.assertEquals(-Arithmetic.imag(input[i]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }
        }
        {
            int slot_size = 32;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40, 40}));

            SealContext context = new SealContext(parms, true, SecLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            GaloisKeys glk = new GaloisKeys();
            keygen.createGaloisKeys(glk);

            Encryptor encryptor = new Encryptor(context, pk);
            Evaluator evaluator = new Evaluator(context);
            Decryptor decryptor = new Decryptor(context, keygen.secretKey());
            CkksEncoder encoder = new CkksEncoder(context);
            final double delta = Math.pow(2, 70);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            // vector<complex<double>> input{ complex<double>(1, 1), complex<double>(2, 2), complex<double>(3, 3),
            //     complex<double>(4, 4) };
            // input.resize(slot_size);
            double[][] input = new double[slot_size][2];
            input[0] = new double[]{1, 1};
            input[1] = new double[]{2, 2};
            input[2] = new double[]{3, 3};
            input[3] = new double[]{4, 4};

            double[][] output = new double[slot_size][2];

            encoder.encode(input, context.firstParmsId(), delta, plain);
            int shift = 1;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 2;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            shift = 3;
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.rotateVectorInplace(encrypted, shift, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[(i + shift) % slot_size]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(input[(i + static_cast<size_t>(shift)) % slot_size].imag(), round(output[i].imag()));
                Assert.assertEquals(Arithmetic.imag(input[(i + shift) % slot_size]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }

            encoder.encode(input, context.firstParmsId(), delta, plain);
            encryptor.encrypt(plain, encrypted);
            evaluator.rescaleToNextInplace(encrypted);
            evaluator.complexConjugateInplace(encrypted, glk);
            decryptor.decrypt(encrypted, plain);
            encoder.decode(plain, output);
            for (int i = 0; i < slot_size; i++) {
                // ASSERT_EQ(input[i].real(), round(output[i].real()));
                Assert.assertEquals(Arithmetic.real(input[i]), Math.round(Arithmetic.real(output[i])), ROTATE_PRECISION);
                // ASSERT_EQ(-input[i].imag(), round(output[i].imag()));
                Assert.assertEquals(-Arithmetic.imag(input[i]), Math.round(Arithmetic.imag(output[i])), ROTATE_PRECISION);
            }
        }
    }

    @Test
    public void testBfvEncryptSquareDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 8);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain;

        plain = new Plaintext("1");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("0");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("FFx^2 + FF");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^4 + 2x^2 + 1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("FF");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^12 + 2x^11 + 3x^10 + 4x^9 + 3x^8 + 4x^7 + 5x^6 + 4x^5 + 4x^4 + 2x^3 + 1x^2 + 2x^1 + 1"
        );
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^16");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^32");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluator.squareInplace(encrypted);
        evaluator.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^24 + 4x^23 + Ax^22 + 14x^21 + 1Fx^20 + 2Cx^19 + 3Cx^18 + 4Cx^17 + 5Fx^16 + 6Cx^15 + 70x^14 + 74x^13 + "
                + "71x^12 + 6Cx^11 + 64x^10 + 50x^9 + 40x^8 + 34x^7 + 26x^6 + 1Cx^5 + 11x^4 + 8x^3 + 6x^2 + 4x^1 + 1"
        );
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
    }

    @Test
    public void bfvEncryptMultiplyManyDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Ciphertext encrypted3 = new Ciphertext();
        Ciphertext encrypted4 = new Ciphertext();
        Ciphertext product = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2, plain3, plain4;

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("1x^2 + 1x^1");
        plain3 = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        Ciphertext[] encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluator.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(3, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(plain.toString(), "1x^6 + 2x^5 + 3x^4 + 3x^3 + 2x^2 + 1x^1");
        Assert.assertSame(encrypted1.parmsId(), product.parmsId());
        Assert.assertSame(encrypted2.parmsId(), product.parmsId());
        Assert.assertSame(encrypted3.parmsId(), product.parmsId());
        Assert.assertSame(product.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^3 + 3F");
        plain2 = new Plaintext("3Fx^4 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2};
        evaluator.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(2, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(plain.toString(), "1x^7 + 1x^4 + 1x^3 + 1");
        Assert.assertSame(encrypted1.parmsId(), product.parmsId());
        Assert.assertSame(encrypted2.parmsId(), product.parmsId());
        Assert.assertSame(product.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^1");
        plain2 = new Plaintext("3Fx^4 + 3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
        plain3 = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluator.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(3, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(plain.toString(), "3Fx^7 + 3Ex^6 + 3Dx^5 + 3Dx^4 + 3Dx^3 + 3Ex^2 + 3Fx^1");
        Assert.assertSame(encrypted1.parmsId(), product.parmsId());
        Assert.assertSame(encrypted2.parmsId(), product.parmsId());
        Assert.assertSame(encrypted3.parmsId(), product.parmsId());
        Assert.assertSame(product.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1");
        plain2 = new Plaintext("3F");
        plain3 = new Plaintext("1");
        plain4 = new Plaintext("3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluator.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(4, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(plain.toString(), "1");
        Assert.assertSame(encrypted1.parmsId(), product.parmsId());
        Assert.assertSame(encrypted2.parmsId(), product.parmsId());
        Assert.assertSame(encrypted3.parmsId(), product.parmsId());
        Assert.assertSame(encrypted4.parmsId(), product.parmsId());
        Assert.assertSame(product.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^16 + 1x^15 + 1x^8 + 1x^7 + 1x^6 + 1x^3 + 1x^2 + 1");
        plain2 = new Plaintext("0");
        plain3 = new Plaintext("1x^13 + 1x^12 + 1x^5 + 1x^4 + 1x^3 + 1");
        plain4 = new Plaintext("1x^15 + 1x^10 + 1x^9 + 1x^8 + 1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluator.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(4, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted1.parmsId(), product.parmsId());
        Assert.assertSame(encrypted2.parmsId(), product.parmsId());
        Assert.assertSame(encrypted3.parmsId(), product.parmsId());
        Assert.assertSame(encrypted4.parmsId(), product.parmsId());
        Assert.assertSame(product.parmsId(), context.firstParmsId());
    }

    @Test
    public void bfvEncryptExponentiateDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain;

        plain = new Plaintext("1x^2 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluator.exponentiateInplace(encrypted, 1, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^2 + 1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluator.exponentiateInplace(encrypted, 2, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^4 + 2x^3 + 3x^2 + 2x^1 + 1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("3Fx^2 + 3Fx^1 + 3F");
        encryptor.encrypt(plain, encrypted);
        evaluator.exponentiateInplace(encrypted, 3, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "3Fx^6 + 3Dx^5 + 3Ax^4 + 39x^3 + 3Ax^2 + 3Dx^1 + 3F");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^8");
        encryptor.encrypt(plain, encrypted);
        evaluator.exponentiateInplace(encrypted, 4, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1x^32");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
    }

    @Test
    public void bfvEncryptAddManyDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Ciphertext encrypted3 = new Ciphertext();
        Ciphertext encrypted4 = new Ciphertext();
        Ciphertext sum = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1, plain2, plain3, plain4;

        plain1 = new Plaintext("1x^2 + 1");
        plain2 = new Plaintext("1x^2 + 1x^1");
        plain3 = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        Ciphertext[] encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluator.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(plain.toString(), "3x^2 + 2x^1 + 2");
        Assert.assertSame(encrypted1.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted2.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted3.parmsId(), sum.parmsId());
        Assert.assertSame(sum.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("3Fx^3 + 3F");
        plain2 = new Plaintext("3Fx^4 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2};
        evaluator.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(plain.toString(), "3Fx^4 + 3Fx^3 + 3E");
        Assert.assertSame(encrypted1.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted2.parmsId(), sum.parmsId());
        Assert.assertSame(sum.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^1");
        plain2 = new Plaintext("3Fx^4 + 3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
        plain3 = new Plaintext("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluator.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(plain.toString(), "3Fx^4 + 3Fx^3 + 1x^1");
        Assert.assertSame(encrypted1.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted2.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted3.parmsId(), sum.parmsId());
        Assert.assertSame(sum.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1");
        plain2 = new Plaintext("3F");
        plain3 = new Plaintext("1");
        plain4 = new Plaintext("3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);

        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluator.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted1.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted2.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted3.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted4.parmsId(), sum.parmsId());
        Assert.assertSame(sum.parmsId(), context.firstParmsId());

        plain1 = new Plaintext("1x^16 + 1x^15 + 1x^8 + 1x^7 + 1x^6 + 1x^3 + 1x^2 + 1");
        plain2 = new Plaintext("0");
        plain3 = new Plaintext("1x^13 + 1x^12 + 1x^5 + 1x^4 + 1x^3 + 1");
        plain4 = new Plaintext("1x^15 + 1x^10 + 1x^9 + 1x^8 + 1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);
        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluator.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
            plain.toString(),
            "1x^16 + 2x^15 + 1x^13 + 1x^12 + 1x^10 + 1x^9 + 2x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 2x^3 + 2x^2 + 1x^1 + 3"
        );
        Assert.assertSame(encrypted1.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted2.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted3.parmsId(), sum.parmsId());
        Assert.assertSame(encrypted4.parmsId(), sum.parmsId());
        Assert.assertSame(sum.parmsId(), context.firstParmsId());
    }

    // TODO: BGVEncryptSquareDecrypt

    // TODO: BGVEncryptMultiplyManyDecrypt

    // TODO: BGVEncryptExponentiateDecrypt

    // TODO: BGVEncryptAddManyDecrypt

    @Test
    public void transformPlainToNtt() {
        transformPlainToNtt(SchemeType.BFV);
        // TODO: test BGV
    }

    @SuppressWarnings("SameParameterValue")
    private void transformPlainToNtt(SchemeType scheme) {
        EncryptionParameters parms = new EncryptionParameters(scheme);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        SealContext context = new SealContext(parms, true, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        Evaluator evaluator = new Evaluator(context);
        Plaintext plain = new Plaintext("0");
        Assert.assertFalse(plain.isNttForm());
        evaluator.transformToNttInplace(plain, context.firstParmsId());
        Assert.assertTrue(plain.isZero());
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), context.firstParmsId());

        plain = new Plaintext("0");
        Assert.assertFalse(plain.isNttForm());
        ParmsId nextParmsId = context.firstContextData().nextContextData().parmsId();
        evaluator.transformToNttInplace(plain, nextParmsId);
        Assert.assertTrue(plain.isZero());
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), nextParmsId);

        plain = new Plaintext("1");
        Assert.assertFalse(plain.isNttForm());
        evaluator.transformToNttInplace(plain, context.firstParmsId());
        Assert.assertEquals(256, plain.coeffCount());
        for (int i = 0; i < 256; i++) {
            Assert.assertEquals(plain.at(i), 1);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), context.firstParmsId());

        plain = new Plaintext("1");
        Assert.assertFalse(plain.isNttForm());
        evaluator.transformToNttInplace(plain, nextParmsId);
        Assert.assertEquals(128, plain.coeffCount());
        for (int i = 0; i < 128; i++) {
            Assert.assertEquals(plain.at(i), 1);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), nextParmsId);

        plain = new Plaintext("2");
        Assert.assertFalse(plain.isNttForm());
        evaluator.transformToNttInplace(plain, context.firstParmsId());
        Assert.assertEquals(256, plain.coeffCount());
        for (int i = 0; i < 256; i++) {
            Assert.assertEquals(plain.at(i), 2);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), context.firstParmsId());

        plain = new Plaintext("2");
        Assert.assertFalse(plain.isNttForm());
        evaluator.transformToNttInplace(plain, nextParmsId);
        Assert.assertEquals(128, plain.coeffCount());
        for (int i = 0; i < 128; i++) {
            Assert.assertEquals(plain.at(i), 2);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertSame(plain.parmsId(), nextParmsId);
    }

    @Test
    public void transformEncryptedToFromNTT() {
        transformEncryptedToFromNTT(SchemeType.BFV);
        // TODO: test BGV
    }

    @SuppressWarnings("SameParameterValue")
    private void transformEncryptedToFromNTT(SchemeType scheme) {
        EncryptionParameters parms = new EncryptionParameters(scheme);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain;

        plain = new Plaintext("0");
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1");
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "1");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
            plain.toString(),
            "Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5"
        );
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
    }

    @Test
    public void testBfvEncryptMultiplyPlainNttDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, keygen.secretKey());

        Plaintext plain;
        Plaintext plainMultiplier;
        Ciphertext encrypted = new Ciphertext();

        plain = new Plaintext(new long[]{0});
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        plainMultiplier = new Plaintext("1");
        evaluator.transformToNttInplace(plainMultiplier, context.firstParmsId());
        evaluator.multiplyPlainInplace(encrypted, plainMultiplier);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "0");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext(new long[]{2});
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        plainMultiplier = new Plaintext("3");
        evaluator.transformToNttInplace(plainMultiplier, context.firstParmsId());
        evaluator.multiplyPlainInplace(encrypted, plainMultiplier);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "6");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext(new long[]{1});
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        plainMultiplier = new Plaintext("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
        evaluator.transformToNttInplace(plainMultiplier, context.firstParmsId());
        evaluator.multiplyPlainInplace(encrypted, plainMultiplier);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(plain.toString(), "Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());

        plain = new Plaintext("1x^20");
        encryptor.encrypt(plain, encrypted);
        evaluator.transformToNttInplace(encrypted);
        plainMultiplier = new Plaintext("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
        evaluator.transformToNttInplace(plainMultiplier, context.firstParmsId());
        evaluator.multiplyPlainInplace(encrypted, plainMultiplier);
        evaluator.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
            plain.toString(),
            "Fx^30 + Ex^29 + Dx^28 + Cx^27 + Bx^26 + Ax^25 + 1x^24 + 2x^23 + 3x^22 + 4x^21 + 5x^20"
        );
        Assert.assertSame(encrypted.parmsId(), context.firstParmsId());
    }


    @Test
    public void bfvEncryptApplyGaloisDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(257);
        parms.setPolyModulusDegree(8);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(8, new int[]{40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        GaloisKeys galoisKeys = new GaloisKeys();
        keyGenerator.createGaloisKeys(new int[]{1, 3, 5, 15}, galoisKeys);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());

        Plaintext plain = new Plaintext("1");
        Ciphertext encrypted = new Ciphertext(context);
        encryptor.encrypt(plain, encrypted);
        evaluator.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1", plain.toString());

        plain = new Plaintext("1x^1");
        encryptor.encrypt(plain, encrypted);
        evaluator.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^3", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("100x^7", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^1", plain.toString());

        plain = new Plaintext("1x^2");
        encryptor.encrypt(plain, encrypted);
        evaluator.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^2", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^6", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("100x^6", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^2", plain.toString());

        plain = new Plaintext("1x^3 + 2x^2 + 1x^1 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluator.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^3 + 2x^2 + 1x^1 + 1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("2x^6 + 1x^3 + 100x^1 + 1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("100x^7 + FFx^6 + 100x^5 + 1", plain.toString());
        evaluator.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals("1x^3 + 2x^2 + 1x^1 + 1", plain.toString());
    }

    @Test
    public void bfvEncryptRotateMatrixDecrypt() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        Modulus plainModulus = new Modulus(257);
        parms.setPolyModulusDegree(8);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(8, new int[]{40, 40}));

        SealContext context = new SealContext(parms, false, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        GaloisKeys galoisKeys = new GaloisKeys();
        keyGenerator.createGaloisKeys(galoisKeys);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());
        BatchEncoder batchEncoder = new BatchEncoder(context);

        Ciphertext encrypted = new Ciphertext(context);
        Plaintext plain = new Plaintext();
        long[] plainVec = new long[]{1, 2, 3, 4, 5, 6, 7, 8};
        batchEncoder.encode(plainVec, plain);
        encryptor.encrypt(plain, encrypted);
        evaluator.rotateColumnsInplace(encrypted, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(plainVec, new long[]{5, 6, 7, 8, 1, 2, 3, 4});

        evaluator.rotateRowsInplace(encrypted, -1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(plainVec, new long[]{8, 5, 6, 7, 4, 1, 2, 3});

        evaluator.rotateRowsInplace(encrypted, 2, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(plainVec, new long[]{6, 7, 8, 5, 2, 3, 4, 1});

        evaluator.rotateColumnsInplace(encrypted, galoisKeys);

        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(plainVec, new long[]{2, 3, 4, 1, 6, 7, 8, 5});

        evaluator.rotateRowsInplace(encrypted, 0, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(plainVec, new long[]{2, 3, 4, 1, 6, 7, 8, 5});
    }

    @Test
    public void bfvEncryptModSwitchToNextDecrypt() {
        // The common parameters: the plaintext and the polynomial moduli
        Modulus plainModulus = new Modulus(1 << 6);

        // The parameters and the context of the higher level
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30}));

        SealContext context = new SealContext(parms, true, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());
        ParmsId parmsId = context.firstParmsId();

        Ciphertext encrypted = new Ciphertext(context);
        Ciphertext encryptedRes = new Ciphertext();
        Plaintext plain;

        plain = new Plaintext(new long[]{0});
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToNext(encrypted, encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        evaluator.modSwitchToNextInplace(encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        parmsId = context.firstParmsId();
        plain = new Plaintext(new long[]{1});
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToNext(encrypted, encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1");

        evaluator.modSwitchToNextInplace(encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1");

        parmsId = context.firstParmsId();
        plain = new Plaintext("1x^127");
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToNext(encrypted, encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1x^127");

        evaluator.modSwitchToNextInplace(encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1x^127");

        parmsId = context.firstParmsId();
        plain = new Plaintext("5x^64 + Ax^5");
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToNext(encrypted, encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");

        evaluator.modSwitchToNextInplace(encryptedRes);
        decryptor.decrypt(encryptedRes, plain);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        Assert.assertSame(encryptedRes.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");
    }

    @Test
    public void bfvEncryptModSwitchToDecrypt() {
        // The common parameters: the plaintext and the polynomial moduli
        Modulus plainModulus = new Modulus(1 << 6);

        // The parameters and the context of the higher level
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30}));

        SealContext context = new SealContext(parms, true, SecLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Evaluator evaluator = new Evaluator(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.secretKey());
        ParmsId parmsId = context.firstParmsId();

        Ciphertext encrypted = new Ciphertext(context);
        Plaintext plain;

        plain = new Plaintext(new long[]{0});
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        parmsId = context.firstParmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "0");

        parmsId = context.firstParmsId();
        plain = new Plaintext(new long[]{1});
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1");

        parmsId = context.firstParmsId();
        plain = new Plaintext("1x^127");
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1x^127");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1x^127");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "1x^127");

        parmsId = context.firstParmsId();
        plain = new Plaintext("5x^64 + Ax^5");
        encryptor.encrypt(plain, encrypted);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");

        parmsId = context.firstParmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");

        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        encryptor.encrypt(plain, encrypted);
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");

        parmsId = context.firstParmsId();
        encryptor.encrypt(plain, encrypted);
        parmsId = context.getContextData(parmsId).nextContextData().parmsId();
        evaluator.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertSame(encrypted.parmsId(), parmsId);
        Assert.assertEquals(plain.toString(), "5x^64 + Ax^5");
    }

    // TODO: BGVEncryptMultiplyPlainNTTDecrypt

    // TODO: BGVEncryptApplyGaloisDecrypt

    // TODO: BGVEncryptRotateMatrixDecrypt

    // TODO: BGVEncryptModSwitchToNextDecrypt

    // TODO: BGVEncryptModSwitchToDecrypt
}
