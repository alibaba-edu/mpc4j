package edu.alibaba.mpc4j.crypto.fhe.seal.examples;

import edu.alibaba.mpc4j.crypto.fhe.seal.*;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.ComprModeType;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.crypto.fhe.seal.examples.ExamplesUtils.printExampleBanner;
import static edu.alibaba.mpc4j.crypto.fhe.seal.examples.ExamplesUtils.printParameters;

/**
 * Performance Example.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/examples/8_performance.cpp">8_performance.cpp</a>.
 *
 * @author Liqiang Peng
 * @date 2023/12/25
 */
public class Example08PerformanceTest {

    private void bfvPerformanceTest(SealContext context) throws Exception {
        StopWatch stopWatch = new StopWatch();

        printParameters(context);
        System.out.print("\n");

        EncryptionParameters parms = context.firstContextData().parms();
        Modulus plain_modulus = parms.plainModulus();
        int poly_modulus_degree = parms.polyModulusDegree();

        System.out.print("Generating secret/public keys: ");
        KeyGenerator keygen = new KeyGenerator(context);
        System.out.print("Done\n");

        SecretKey secret_key = keygen.secretKey();
        PublicKey public_key = new PublicKey();
        keygen.createPublicKey(public_key);

        RelinKeys relin_keys = new RelinKeys();
        GaloisKeys gal_keys = new GaloisKeys();
        double time_diff;
        if (context.usingKeySwitching())
        {
            /*
            Generate relinearization keys.
            */
            System.out.print("Generating relinearization keys: ");
            stopWatch.start();
            keygen.createRelinKeys(relin_keys);
            stopWatch.stop();
            time_diff = (double) stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            System.out.print("Done [" + time_diff + " microseconds]\n");

            if (!context.keyContextData().qualifiers().isUsingBatching()) {
                System.out.print("Given encryption parameters do not support batching.\n");
                return;
            }

            /*
            Generate Galois keys. In larger examples the Galois keys can use a lot of
            memory, which can be a problem in constrained systems. The user should
            try some of the larger runs of the test and observe their effect on the
            memory pool allocation size. The key generation can also take a long time,
            as can be observed from the print-out.
            */
            System.out.print("Generating Galois keys: ");
            stopWatch.start();
            keygen.createGaloisKeys(gal_keys);
            stopWatch.stop();
            time_diff = (double) stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            System.out.print("Done [" + time_diff + " microseconds]\n");
        }

        Encryptor encryptor = new Encryptor(context, public_key);
        Decryptor decryptor = new Decryptor(context, secret_key);
        Evaluator evaluator = new Evaluator(context);
        BatchEncoder batch_encoder = new BatchEncoder(context);

        /*
        These will hold the total times used by each operation.
        */
        double time_batch_sum = 0.0d;
        double time_unbatch_sum = 0.0d;
        double time_encrypt_sum = 0.0d;
        double time_decrypt_sum = 0.0d;
        double time_add_sum = 0.0d;
        double time_multiply_sum = 0.0d;
        double time_multiply_plain_sum = 0.0d;
        double time_square_sum = 0.0d;
        double time_relinearize_sum = 0.0d;
        double time_rotate_rows_one_step_sum = 0.0d;
        double time_rotate_rows_random_sum = 0.0d;
        double time_rotate_columns_sum = 0.0d;
        double time_serialize_sum = 0.0d;
        double time_serialize_zlib_sum = 0.0d;
        double time_serialize_zstd_sum = 0.0d;

        /*
        How many times to run the test?
        */
        long count = 10;

        /*
        Populate a vector of values to batch.
        */
        int slot_count = batch_encoder.slotCount();
        long[] pod_vector = new long[slot_count];
        SecureRandom rd = new SecureRandom();
        for (int i = 0; i < slot_count; i++) {
            pod_vector[i] = plain_modulus.reduce(rd.nextLong());
        }

        System.out.print("Running tests ");
        for (int i = 0; i < count; i++) {
            /*
            [Batching]
            There is nothing unusual here. We batch our random plaintext matrix
            into the polynomial. Note how the plaintext we create is of the exactly
            right size so unnecessary reallocations are avoided.
            */
            Plaintext plain = new Plaintext(poly_modulus_degree, 0);
            Plaintext plain1 = new Plaintext(poly_modulus_degree, 0);
            Plaintext plain2 = new Plaintext(poly_modulus_degree, 0);
            stopWatch.start();
            batch_encoder.encode(pod_vector, plain);
            stopWatch.stop();
            time_batch_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Unbatching]
            We unbatch what we just batched.
            */
            long[] pod_vector2 = new long[slot_count];
            stopWatch.start();
            batch_encoder.decode(plain, pod_vector2);
            stopWatch.stop();
            time_unbatch_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            if (!Arrays.equals(pod_vector2, pod_vector)) {
                throw new RuntimeException("Batch/unbatch failed. Something is wrong.");
            }

            /*
            [Encryption]
            We make sure our ciphertext is already allocated and large enough
            to hold the encryption with these encryption parameters. We encrypt
            our random batched matrix here.
            */
            Ciphertext encrypted = new Ciphertext(context);
            stopWatch.start();
            encryptor.encrypt(plain, encrypted);
            stopWatch.stop();
            time_encrypt_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Decryption]
            We decrypt what we just encrypted.
            */
            stopWatch.start();
            decryptor.decrypt(encrypted, plain2);
            stopWatch.stop();
            time_decrypt_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            if (!plain2.equals(plain)) {
                throw new RuntimeException("Encrypt/decrypt failed. Something is wrong.");
            }

            /*
            [Add]
            We create two ciphertexts and perform a few additions with them.
            */
            Ciphertext encrypted1 = new Ciphertext(context);
            long[] temp = new long[slot_count];
            Arrays.fill(temp, i);
            batch_encoder.encode(temp, plain1);
            encryptor.encrypt(plain1, encrypted1);
            Ciphertext encrypted2 = new Ciphertext(context);
            Arrays.fill(temp, i + 1);
            batch_encoder.encode(temp, plain2);
            encryptor.encrypt(plain2, encrypted2);
            stopWatch.start();
            evaluator.addInplace(encrypted1, encrypted1);
            evaluator.addInplace(encrypted2, encrypted2);
            evaluator.addInplace(encrypted1, encrypted2);
            stopWatch.stop();
            time_add_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Multiply]
            We multiply two ciphertexts. Since the size of the result will be 3,
            and will overwrite the first argument, we reserve first enough memory
            to avoid reallocating during multiplication.
            */
            encrypted1.reserve(3);
            stopWatch.start();
            evaluator.multiplyInplace(encrypted1, encrypted2);
            stopWatch.stop();
            time_multiply_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Multiply Plain]
            We multiply a ciphertext with a random plaintext. Recall that
            multiply_plain does not change the size of the ciphertext so we use
            encrypted2 here.
            */
            stopWatch.start();
            evaluator.multiplyPlainInplace(encrypted2, plain);
            stopWatch.stop();
            time_multiply_plain_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Square]
            We continue to use encrypted2. Now we square it; this should be
            faster than generic homomorphic multiplication.
            */
            stopWatch.start();
            evaluator.squareInplace(encrypted2);
            stopWatch.stop();
            time_square_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            if (context.usingKeySwitching()) {
                /*
                [Relinearize]
                Time to get back to encrypted1. We now relinearize it back
                to size 2. Since the allocation is currently big enough to
                contain a ciphertext of size 3, no costly reallocations are
                needed in the process.
                */
                stopWatch.start();
                evaluator.relinearizeInplace(encrypted1, relin_keys);
                stopWatch.stop();
                time_relinearize_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rotate Rows One Step]
                We rotate matrix rows by one step left and measure the time.
                */
                stopWatch.start();
                evaluator.rotateRowsInplace(encrypted, 1, gal_keys);
                evaluator.rotateRowsInplace(encrypted, -1, gal_keys);
                stopWatch.stop();
                time_rotate_rows_one_step_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rotate Rows Random]
                We rotate matrix rows by a random number of steps. This is much more
                expensive than rotating by just one step.
                */
                int row_size = batch_encoder.slotCount() / 2;
                // row_size is always a power of 2
                int random_rotation = (int) (rd.nextLong() & row_size - 1);
                stopWatch.start();
                evaluator.rotateRowsInplace(encrypted, random_rotation, gal_keys);
                stopWatch.stop();
                time_rotate_rows_random_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rotate Columns]
                Nothing surprising here.
                */
                stopWatch.start();
                evaluator.rotateColumnsInplace(encrypted, gal_keys);
                stopWatch.stop();
                time_rotate_columns_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();
            }

            /*
            [Serialize Ciphertext]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.NONE);
            stopWatch.stop();
            time_serialize_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Serialize Ciphertext (ZLIB)]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.ZLIB);
            stopWatch.stop();
            time_serialize_zlib_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Serialize Ciphertext (Zstandard)]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.ZSTD);
            stopWatch.stop();
            time_serialize_zstd_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            Print a dot to indicate progress.
            */
            System.out.print(".");
        }

        System.out.println(" Done");
        System.out.println();

        double avg_batch = time_batch_sum / count;
        double avg_unbatch = time_unbatch_sum / count;
        double avg_encrypt = time_encrypt_sum / count;
        double avg_decrypt = time_decrypt_sum / count;
        double avg_add = time_add_sum / (3 * count);
        double avg_multiply = time_multiply_sum / count;
        double avg_multiply_plain = time_multiply_plain_sum / count;
        double avg_square = time_square_sum / count;
        double avg_relinearize = time_relinearize_sum / count;
        double avg_rotate_rows_one_step = time_rotate_rows_one_step_sum / (2 * count);
        double avg_rotate_rows_random = time_rotate_rows_random_sum / count;
        double avg_rotate_columns = time_rotate_columns_sum / count;
        double avg_serialize = time_serialize_sum / count;
        double avg_serialize_zlib = time_serialize_zlib_sum / count;
        double avg_serialize_zstd = time_serialize_zstd_sum / count;

        System.out.print("Average batch: " + avg_batch + " microseconds\n");
        System.out.print("Average unbatch: " + avg_unbatch + " microseconds\n");
        System.out.print("Average encrypt: " + avg_encrypt + " microseconds\n");
        System.out.print("Average decrypt: " + avg_decrypt + " microseconds\n");
        System.out.print("Average add: " + avg_add + " microseconds\n");
        System.out.print("Average multiply: " + avg_multiply + " microseconds\n");
        System.out.print("Average multiply plain: " + avg_multiply_plain + " microseconds\n");
        System.out.print("Average square: " + avg_square + " microseconds\n");
        if (context.usingKeySwitching()) {
            System.out.print("Average relinearize: " + avg_relinearize + " microseconds\n");
            System.out.print("Average rotate rows one step: " + avg_rotate_rows_one_step + " microseconds\n");
            System.out.print("Average rotate rows random: " + avg_rotate_rows_random + " microseconds\n");
            System.out.print("Average rotate columns: " + avg_rotate_columns + " microseconds\n");
        }
        System.out.print("Average serialize ciphertext: " + avg_serialize + " microseconds\n");
        System.out.print("Average compressed (ZLIB) serialize ciphertext: " + avg_serialize_zlib + " microseconds\n");
        System.out.print("Average compressed (Zstandard) serialize ciphertext: " + avg_serialize_zstd + " microseconds\n");
    }

    @Test
    public void example_bfv_performance_default() throws Exception {
        printExampleBanner("BFV Performance Test with Degrees: 4096, 8192, and 16384");

        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        int polyModulusDegree = 4096;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(786433);
        SealContext context = new SealContext(parms);
        bfvPerformanceTest(context);

        System.out.print("\n");
        polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(786433);
        context = new SealContext(parms);
        bfvPerformanceTest(context);

        /*
        Comment out the following to run the biggest example.
        */
        // System.out.print("\n");
        // polyModulusDegree = 16384;
        // parms.setPolyModulusDegree(polyModulusDegree);
        // parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        // parms.setPlainModulus(786433);
        // context = new SealContext(parms);
        // bfvPerformanceTest(context);

        // System.out.print("\n");
        // polyModulusDegree = 32768;
        // parms.setPolyModulusDegree(polyModulusDegree);
        // parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        // parms.setPlainModulus(786433);
        // context = new SealContext(parms);
        // bfvPerformanceTest(context);
    }

    private void ckks_performance_test(SealContext context) throws IOException {
        StopWatch stopWatch = new StopWatch();

        printParameters(context);
        System.out.println();

        EncryptionParameters parms = context.firstContextData().parms();
        int poly_modulus_degree = parms.polyModulusDegree();

        System.out.println("Generating secret/public keys: ");
        KeyGenerator keygen = new KeyGenerator(context);
        System.out.println("Done");

        SecretKey secret_key = keygen.secretKey();
        PublicKey public_key = new PublicKey();
        keygen.createPublicKey(public_key);

        RelinKeys relin_keys = new RelinKeys();
        GaloisKeys gal_keys = new GaloisKeys();
        long time_diff;
        if (context.usingKeySwitching()) {
            System.out.println("Generating relinearization keys: ");
            stopWatch.start();
            keygen.createRelinKeys(relin_keys);
            stopWatch.stop();
            time_diff = stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            System.out.println("Done [" + time_diff + " milliseconds]");

            if (!context.firstContextData().qualifiers().isUsingBatching()) {
                System.out.println("Given encryption parameters do not support batching.");
                return;
            }

            System.out.println("Generating Galois keys: ");
            stopWatch.start();
            keygen.createGaloisKeys(gal_keys);
            stopWatch.stop();
            time_diff = stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            System.out.println("Done [" + time_diff + " milliseconds]");
        }

        Encryptor encryptor = new Encryptor(context, public_key);
        Decryptor decryptor = new Decryptor(context, secret_key);
        Evaluator evaluator = new Evaluator(context);
        CkksEncoder ckks_encoder = new CkksEncoder(context);

        double time_encode_sum = 0.0d;
        double time_decode_sum = 0.0d;
        double time_encrypt_sum = 0.0d;
        double time_decrypt_sum = 0.0d;
        double time_add_sum = 0.0d;
        double time_multiply_sum = 0.0d;
        double time_multiply_plain_sum = 0.0d;
        double time_square_sum = 0.0d;
        double time_relinearize_sum = 0.0d;
        double time_rescale_sum = 0.0d;
        double time_rotate_one_step_sum = 0.0d;
        double time_rotate_random_sum = 0.0d;
        double time_conjugate_sum = 0.0d;
        double time_serialize_sum = 0.0d;
        double time_serialize_zlib_sum = 0.0d;
        double time_serialize_zstd_sum = 0.0d;
        /*
        How many times to run the test?
        */
        long count = 10;

        /*
        Populate a vector of floating-point values to batch.
        */
        double[] pod_vector = new double[ckks_encoder.slotCount()];
        SecureRandom rd = new SecureRandom();
        for (int i = 0; i < ckks_encoder.slotCount(); i++) {
            pod_vector[i] = 1.001 * i;
        }

        System.out.print("Running tests ");
        for (long i = 0; i < count; i++) {
            /*
            [Encoding]
            For scale we use the square root of the last coeff_modulus prime
            from parms.
            */
            Plaintext plain = new Plaintext(parms.polyModulusDegree() * parms.coeffModulus().length, 0);
            /*

             */
            double scale = Math.sqrt((double) (parms.coeffModulus()[parms.coeffModulus().length - 1].value()));
            stopWatch.start();
            ckks_encoder.encode(pod_vector, scale, plain);
            stopWatch.stop();
            time_encode_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Decoding]
            */
            double[] pod_vector2 = new double[ckks_encoder.slotCount()];
            stopWatch.start();
            ckks_encoder.decode(plain, pod_vector2);
            stopWatch.stop();
            time_decode_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Encryption]
            */
            Ciphertext encrypted = new Ciphertext(context);
            stopWatch.start();
            encryptor.encrypt(plain, encrypted);
            stopWatch.stop();
            time_encrypt_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Decryption]
            */
            Plaintext plain2 = new Plaintext(poly_modulus_degree, 0);
            stopWatch.start();
            decryptor.decrypt(encrypted, plain2);
            stopWatch.stop();
            time_decrypt_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Add]
            */
            Ciphertext encrypted1 = new Ciphertext(context);
            ckks_encoder.encode(i + 1, plain);
            encryptor.encrypt(plain, encrypted1);
            Ciphertext encrypted2 = new Ciphertext(context);
            ckks_encoder.encode(i + 1, plain2);
            encryptor.encrypt(plain2, encrypted2);
            stopWatch.start();
            evaluator.addInplace(encrypted1, encrypted1);
            evaluator.addInplace(encrypted2, encrypted2);
            evaluator.addInplace(encrypted1, encrypted2);
            stopWatch.stop();
            time_add_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Multiply]
            */
            encrypted1.reserve(3);
            stopWatch.start();
            evaluator.multiplyInplace(encrypted1, encrypted2);
            stopWatch.stop();
            time_multiply_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Multiply Plain]
            */
            stopWatch.start();
            evaluator.multiplyPlainInplace(encrypted2, plain);
            stopWatch.stop();
            time_multiply_plain_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            /*
            [Square]
            */
            stopWatch.start();
            evaluator.squareInplace(encrypted2);
            stopWatch.stop();
            time_square_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();

            if (context.usingKeySwitching()) {
                /*
                [Relinearize]
                */
                stopWatch.start();
                evaluator.relinearizeInplace(encrypted1, relin_keys);
                stopWatch.stop();
                time_relinearize_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rescale]
                */
                stopWatch.start();
                evaluator.rescaleToNextInplace(encrypted1);
                stopWatch.stop();
                time_rescale_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rotate Vector]
                */
                stopWatch.start();
                evaluator.rotateVectorInplace(encrypted, 1, gal_keys);
                evaluator.rotateVectorInplace(encrypted, -1, gal_keys);
                stopWatch.stop();
                time_rotate_one_step_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Rotate Vector Random]
                */
                // ckks_encoder.slot_count() is always a power of 2.
                int random_rotation = (rd.nextInt() & (ckks_encoder.slotCount() - 1));
                stopWatch.start();
                evaluator.rotateVectorInplace(encrypted, random_rotation, gal_keys);
                stopWatch.stop();
                time_rotate_random_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();

                /*
                [Complex Conjugate]
                */
                stopWatch.start();
                evaluator.complexConjugateInplace(encrypted, gal_keys);
                stopWatch.stop();
                time_conjugate_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
                stopWatch.reset();
            }

            /*
            [Serialize Ciphertext]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.NONE);
            stopWatch.stop();
            time_serialize_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            /*
            [Serialize Ciphertext (ZLIB)]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.ZLIB);
            stopWatch.stop();
            time_serialize_zlib_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            /*
            [Serialize Ciphertext (Zstandard)]
            */
            stopWatch.start();
            encrypted.save(ComprModeType.ZSTD);
            stopWatch.stop();
            time_serialize_zstd_sum += stopWatch.getTime(TimeUnit.MICROSECONDS);
            stopWatch.reset();
            /*
            Print a dot to indicate progress.
            */
            System.out.print(".");
        }

        System.out.println(" Done");
        System.out.println();

        double avg_encode = time_encode_sum / count;
        double avg_decode = time_decode_sum / count;
        double avg_encrypt = time_encrypt_sum / count;
        double avg_decrypt = time_decrypt_sum / count;
        double avg_add = time_add_sum / (3 * count);
        double avg_multiply = time_multiply_sum / count;
        double avg_multiply_plain = time_multiply_plain_sum / count;
        double avg_square = time_square_sum / count;
        double avg_relinearize = time_relinearize_sum / count;
        double avg_rescale = time_rescale_sum / count;
        double avg_rotate_one_step = time_rotate_one_step_sum / (2 * count);
        double avg_rotate_random = time_rotate_random_sum / count;
        double avg_conjugate = time_conjugate_sum / count;
        double avg_serialize = time_serialize_sum / count;
        double avg_serialize_zlib = time_serialize_zlib_sum / count;
        double avg_serialize_zstd = time_serialize_zstd_sum / count;
        System.out.println("Average encode: " + avg_encode + " microseconds");
        System.out.println("Average decode: " + avg_decode + " microseconds");
        System.out.println("Average encrypt: " + avg_encrypt + " microseconds");
        System.out.println("Average decrypt: " + avg_decrypt + " microseconds");
        System.out.println("Average add: " + avg_add + " microseconds");
        System.out.println("Average multiply: " + avg_multiply + " microseconds");
        System.out.println("Average multiply plain: " + avg_multiply_plain + " microseconds");
        System.out.println("Average square: " + avg_square + " microseconds");
        if (context.usingKeySwitching()) {
            System.out.println("Average relinearize: " + avg_relinearize + " microseconds");
            System.out.println("Average rescale: " + avg_rescale + " microseconds");
            System.out.println("Average rotate vector one step: " + avg_rotate_one_step + " microseconds");
            System.out.println("Average rotate vector random: " + avg_rotate_random + " microseconds");
            System.out.println("Average complex conjugate: " + avg_conjugate + " microseconds");
        }
        System.out.println("Average serialize ciphertext: " + avg_serialize + " microseconds");
        System.out.println("Average compressed (ZLIB) serialize ciphertext: " + avg_serialize_zlib + " microseconds");
        System.out.println("Average compressed (Zstandard) serialize ciphertext: " + avg_serialize_zstd + " microseconds");
    }

    @Test
    public void example_ckks_performance_default() throws IOException {
        printExampleBanner("CKKS Performance Test with Degrees: 4096, 8192, and 16384");

        // It is not recommended to use BFVDefault primes in CKKS. However, for performance
        // test, BFVDefault primes are good enough.
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);
        int poly_modulus_degree = 4096;
        parms.setPolyModulusDegree(poly_modulus_degree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(poly_modulus_degree));
        SealContext context = new SealContext(parms);
        ckks_performance_test(context);

        System.out.println();
        poly_modulus_degree = 8192;
        parms.setPolyModulusDegree(poly_modulus_degree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(poly_modulus_degree));
        context = new SealContext(parms);
        ckks_performance_test(context);

        /*
        Comment out the following to run the biggest example.
        */
        // System.out.println();
        // poly_modulus_degree = 16384;
        // parms.setPolyModulusDegree(poly_modulus_degree);
        // parms.setCoeffModulus(CoeffModulus.bfvDefault(poly_modulus_degree));
        // context = new SealContext(parms);
        // ckks_performance_test(context);

        // System.out.println();
        // poly_modulus_degree = 32768;
        // parms.setPolyModulusDegree(poly_modulus_degree);
        // parms.setCoeffModulus(CoeffModulus.bfvDefault(poly_modulus_degree));
        // context = new SealContext(parms);
        // ckks_performance_test(context);
    }

    // TODO: implement bgv_performance_test and example_bgv_performance_default
}
