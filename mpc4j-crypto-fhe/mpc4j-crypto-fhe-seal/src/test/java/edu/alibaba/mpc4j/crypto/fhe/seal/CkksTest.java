package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Arithmetic;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * CKKS test.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/tests/seal/ckks.cpp">ckks.cpp</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/17
 */
public class CkksTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public CkksTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testEncodeVectorDecode() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            int slots = 32;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{40, 40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            for (int i = 0; i < slots; i++) {
                // complex<double> value(0.0, 0.0);
                // values[i] = value;
                Arithmetic.set(values[i], 0, 0);
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 16);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[slots][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            int slots = 32;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{60, 60, 60, 60}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 30);

            for (int i = 0; i < slots; i++) {
                // complex<double> value (static_cast < double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 40);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[slots][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            int slots = 64;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{60, 60, 60}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 30);

            for (int i = 0; i < slots; i++) {
                // complex<double> value(static_cast<double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 40);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[slots][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            int slots = 64;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{30, 30, 30, 30, 30}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 30);

            for (int i = 0; i < slots; i++) {
                // complex<double> value(static_cast<double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 40);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[slots][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            int slots = 32;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30, 30}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 30);

            for (int i = 0; i < slots; i++) {
                // complex<double> value(static_cast<double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 40);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[parms.polyModulusDegree() >> 1][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            // Many primes
            int slots = 32;
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(
                128, new int[]{30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}
            ));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 30);

            for (int i = 0; i < slots; i++) {
                // complex<double> value (static_cast < double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            double delta = (1L << 40);
            Plaintext plain = new Plaintext();
            encoder.encode(values, context.firstParmsId(), delta, plain);
            double[][] result = new double[parms.polyModulusDegree() >> 1][2];
            encoder.decode(plain, result);

            for (int i = 0; i < slots; ++i) {
                double tmp = Math.abs(values[i][0] - result[i][0]);
                Assert.assertTrue(tmp < 0.5);
            }
        }
        {
            int slots = 64;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{40, 40, 40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);

            double[][] values = new double[slots][2];

            int data_bound = (1 << 20);

            for (int i = 0; i < slots; i++) {
                // complex<double> value (static_cast < double>(rand() % data_bound), 0);
                // values[i] = value;
                Arithmetic.set(values[i], secureRandom.nextInt(data_bound));
            }

            CkksEncoder encoder = new CkksEncoder(context);
            {
                // Use a very large scale
                double delta = Math.pow(2.0, 110);
                Plaintext plain = new Plaintext();
                encoder.encode(values, context.firstParmsId(), delta, plain);
                double[][] result = new double[slots][2];
                encoder.decode(plain, result);

                for (int i = 0; i < slots; ++i) {
                    double tmp = Math.abs(values[i][0] - result[i][0]);
                    Assert.assertTrue(tmp < 0.5);
                }
            }
            {
                // Use a scale over 128 bits
                double delta = Math.pow(2.0, 130);
                Plaintext plain = new Plaintext();
                encoder.encode(values, context.firstParmsId(), delta, plain);
                double[][] result = new double[slots][2];
                encoder.decode(plain, result);

                for (int i = 0; i < slots; ++i) {
                    double tmp = Math.abs(values[i][0] - result[i][0]);
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }

    }

    @Test
    public void testEncodeSingleDecode() {
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        {
            int slots = 16;
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            CkksEncoder encoder = new CkksEncoder(context);

            int data_bound = 1 << 30;
            double delta = 1L << 16;
            Plaintext plain = new Plaintext();
            double[][] result = new double[parms.polyModulusDegree() >> 1][2];

            for (int iRun = 0; iRun < 50; iRun++) {
                double value = secureRandom.nextInt(data_bound);
                encoder.encode(value, context.firstParmsId(), delta, plain);
                encoder.decode(plain, result);

                for (int i = 0; i < slots * 2; ++i) {
                    double tmp = Math.abs(value - result[i][0]);
                    Assert.assertTrue(tmp < 0.5);
                }
            }
        }
        {
            int slots = 32;
            parms.setPolyModulusDegree(slots << 1);
            parms.setCoeffModulus(CoeffModulus.create(slots << 1, new int[]{40, 40, 40, 40}));
            SealContext context = new SealContext(parms, false, SecLevelType.NONE);
            CkksEncoder encoder = new CkksEncoder(context);

            {
                int data_bound = (1 << 30);
                Plaintext plain = new Plaintext();
                double[][] result = new double[slots][2];

                for (int iRun = 0; iRun < 50; iRun++) {
                    int value = secureRandom.nextInt(data_bound);
                    encoder.encode(value, context.firstParmsId(), plain);
                    encoder.decode(plain, result);

                    for (int i = 0; i < slots; ++i) {
                        double tmp = Math.abs(value - result[i][0]);
                        Assert.assertTrue(tmp < 0.5);
                    }
                }
            }
            {
                // Use a very large scale
                int data_bound = (1 << 20);
                Plaintext plain = new Plaintext();
                double[][] result = new double[slots][2];

                for (int iRun = 0; iRun < 50; iRun++) {
                    int value = secureRandom.nextInt(data_bound);
                    encoder.encode(value, context.firstParmsId(), plain);
                    encoder.decode(plain, result);

                    for (int i = 0; i < slots; ++i) {
                        double tmp = Math.abs(value - result[i][0]);
                        Assert.assertTrue(tmp < 0.5);
                    }
                }
            }
            {
                // Use a scale over 128 bits
                int data_bound = (1 << 20);
                Plaintext plain = new Plaintext();
                double[][] result = new double[slots][2];

                for (int iRun = 0; iRun < 50; iRun++) {
                    int value = secureRandom.nextInt(data_bound);
                    encoder.encode(value, context.firstParmsId(), plain);
                    encoder.decode(plain, result);

                    for (int i = 0; i < slots; ++i) {
                        double tmp = Math.abs(value - result[i][0]);
                        Assert.assertTrue(tmp < 0.5);
                    }
                }
            }
        }
    }

    @Test
    public void testFmod() {
        // we find some cases for fmod where Java behaves differently from C++.
        double coeffd = 1.7042430230528E19;
        double mod = Math.pow(2.0, 64);
        long coeffl = (long) CkksEncoder.fmod(coeffd, mod);
        Assert.assertEquals(Long.parseUnsignedLong("17042430230528000000"), coeffl);

        coeffd = 3.3141844316507238E+19;
        coeffl = (long) CkksEncoder.fmod(coeffd, mod);
        Assert.assertEquals(Long.parseUnsignedLong("14695100242797686784"), coeffl);
    }

    @Test
    public void testRound() {
        // we find some cases for round where Java behaves differently from C++.
        double coeffd = 1.7042430230528E20;
        coeffd = CkksEncoder.round(coeffd);
        Assert.assertEquals(1.7042430230528E+20, coeffd, 1e-20);

        coeffd = -1.966908539188776E19;
        coeffd = CkksEncoder.round(coeffd);
        Assert.assertEquals(-1.9669085391887761E+19, coeffd, 1e-20);
    }
}
