package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * Performance Example.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/8_performance.cpp
 *
 * @author Liqiang Peng
 * @date 2023/12/25
 */
public class PerformanceExampleTest {

    public void bfvPerformanceTest(SealContext context) throws Exception {
        StopWatch STOP_WATCH = new StopWatch();

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
            STOP_WATCH.start();
            keygen.createRelinKeys(relin_keys);
            STOP_WATCH.stop();
            time_diff = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();
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
            STOP_WATCH.start();
            keygen.createGaloisKeys(gal_keys);
            STOP_WATCH.stop();
            time_diff = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();
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
            STOP_WATCH.start();
            batch_encoder.encode(pod_vector, plain);
            STOP_WATCH.stop();
            time_batch_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Unbatching]
            We unbatch what we just batched.
            */
            long[] pod_vector2 = new long[slot_count];
            STOP_WATCH.start();
            batch_encoder.decode(plain, pod_vector2);
            STOP_WATCH.stop();
            time_unbatch_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();
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
            STOP_WATCH.start();
            encryptor.encrypt(plain, encrypted);
            STOP_WATCH.stop();
            time_encrypt_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Decryption]
            We decrypt what we just encrypted.
            */
            STOP_WATCH.start();
            decryptor.decrypt(encrypted, plain2);
            STOP_WATCH.stop();
            time_decrypt_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();
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
            STOP_WATCH.start();
            evaluator.addInplace(encrypted1, encrypted1);
            evaluator.addInplace(encrypted2, encrypted2);
            evaluator.addInplace(encrypted1, encrypted2);
            STOP_WATCH.stop();
            time_add_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Multiply]
            We multiply two ciphertexts. Since the size of the result will be 3,
            and will overwrite the first argument, we reserve first enough memory
            to avoid reallocating during multiplication.
            */
            encrypted1.reserve(3);
            STOP_WATCH.start();
            evaluator.multiplyInplace(encrypted1, encrypted2);
            STOP_WATCH.stop();
            time_multiply_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Multiply Plain]
            We multiply a ciphertext with a random plaintext. Recall that
            multiply_plain does not change the size of the ciphertext so we use
            encrypted2 here.
            */
            STOP_WATCH.start();
            evaluator.multiplyPlainInplace(encrypted2, plain);
            STOP_WATCH.stop();
            time_multiply_plain_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Square]
            We continue to use encrypted2. Now we square it; this should be
            faster than generic homomorphic multiplication.
            */
            STOP_WATCH.start();
            evaluator.squareInplace(encrypted2);
            STOP_WATCH.stop();
            time_square_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            if (context.usingKeySwitching()) {
                /*
                [Relinearize]
                Time to get back to encrypted1. We now relinearize it back
                to size 2. Since the allocation is currently big enough to
                contain a ciphertext of size 3, no costly reallocations are
                needed in the process.
                */
                STOP_WATCH.start();
                evaluator.relinearizeInplace(encrypted1, relin_keys);
                STOP_WATCH.stop();
                time_relinearize_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
                STOP_WATCH.reset();

                /*
                [Rotate Rows One Step]
                We rotate matrix rows by one step left and measure the time.
                */
                STOP_WATCH.start();
                evaluator.rotateRowsInplace(encrypted, 1, gal_keys);
                evaluator.rotateRowsInplace(encrypted, -1, gal_keys);
                STOP_WATCH.stop();
                time_rotate_rows_one_step_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
                STOP_WATCH.reset();

                /*
                [Rotate Rows Random]
                We rotate matrix rows by a random number of steps. This is much more
                expensive than rotating by just one step.
                */
                int row_size = batch_encoder.slotCount() / 2;
                // row_size is always a power of 2
                int random_rotation = (int) (rd.nextLong() & row_size - 1);
                STOP_WATCH.start();
                evaluator.rotateRowsInplace(encrypted, random_rotation, gal_keys);
                STOP_WATCH.stop();
                time_rotate_rows_random_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
                STOP_WATCH.reset();

                /*
                [Rotate Columns]
                Nothing surprising here.
                */
                STOP_WATCH.start();
                evaluator.rotateColumnsInplace(encrypted, gal_keys);
                STOP_WATCH.stop();
                time_rotate_columns_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
                STOP_WATCH.reset();
            }

            /*
            [Serialize Ciphertext]
            */
            STOP_WATCH.start();
            encrypted.save(ComprModeType.NONE);
            STOP_WATCH.stop();
            time_serialize_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Serialize Ciphertext (ZLIB)]
            */
            STOP_WATCH.start();
            encrypted.save(ComprModeType.ZLIB);
            STOP_WATCH.stop();
            time_serialize_zlib_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            [Serialize Ciphertext (Zstandard)]
            */
            STOP_WATCH.start();
            encrypted.save(ComprModeType.ZSTD);
            STOP_WATCH.stop();
            time_serialize_zstd_sum += STOP_WATCH.getTime(TimeUnit.MICROSECONDS);
            STOP_WATCH.reset();

            /*
            Print a dot to indicate progress.
            */
            System.out.print(".");
            System.out.flush();
        }

        System.out.print(" Done\n\n");
        System.out.flush();

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

        System.out.flush();
    }

    @Test
    public void exampleBfvPerformanceDefault() throws Exception {
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
//        System.out.print("\n");
//        polyModulusDegree = 16384;
//        parms.setPolyModulusDegree(polyModulusDegree);
//        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
//        parms.setPlainModulus(786433);
//        context = new SealContext(parms);
//        bfvPerformanceTest(context);

//         System.out.print("\n");
//         polyModulusDegree = 32768;
//         parms.setPolyModulusDegree(polyModulusDegree);
//         parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
//         parms.setPlainModulus(786433);
//         context = new SealContext(parms);
//         bfvPerformanceTest(context);
    }

    @Test
    public void examplePerformanceTest() throws Exception {
        printExampleBanner("Example: Performance Test");
        // BFV with default degrees
        exampleBfvPerformanceDefault();
        // CKKS with default degrees
        // TODO: implement CKKS
        // BGV with default degrees
        // TODO: implement BGV
    }
}
