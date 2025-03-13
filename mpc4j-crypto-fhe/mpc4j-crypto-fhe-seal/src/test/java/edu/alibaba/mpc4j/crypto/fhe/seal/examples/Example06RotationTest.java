package edu.alibaba.mpc4j.crypto.fhe.seal.examples;

import edu.alibaba.mpc4j.crypto.fhe.seal.*;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.PlainModulus;
import org.junit.Assert;
import org.junit.Test;

import static edu.alibaba.mpc4j.crypto.fhe.seal.examples.ExamplesUtils.*;

/**
 * Rotation Example.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/examples/6_rotation.cpp">6_rotation.cpp</a>.
 *
 * @author Liqiang Peng
 * @date 2023/12/25
 */
public class Example06RotationTest {

    @Test
    public void example_rotation_bfv() {
        printExampleBanner("Example: Rotation / Rotation in BFV");

        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);

        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));

        SealContext context = new SealContext(parms);
        printParameters(context);
        System.out.print("\n");

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.secretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);
        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);
        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);

        BatchEncoder batchEncoder = new BatchEncoder(context);
        int slotCount = batchEncoder.slotCount();
        int rowSize = slotCount / 2;
        System.out.print("Plaintext matrix row size: " + rowSize + "\n");

        long[] podMatrix = new long[slotCount];
        podMatrix[0] = 0L;
        podMatrix[1] = 1L;
        podMatrix[2] = 2L;
        podMatrix[3] = 3L;
        podMatrix[rowSize] = 4L;
        podMatrix[rowSize + 1] = 5L;
        podMatrix[rowSize + 2] = 6L;
        podMatrix[rowSize + 3] = 7L;

        System.out.print("Input plaintext matrix:\n");
        printMatrix(podMatrix, rowSize);

        /*
        First we use BatchEncoder to encode the matrix into a plaintext. We encrypt
        the plaintext as usual.
        */
        Plaintext plainMatrix = new Plaintext();
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Encode and encrypt.\n");
        batchEncoder.encode(podMatrix, plainMatrix);
        Ciphertext encryptedMatrix = new Ciphertext();
        encryptor.encrypt(plainMatrix, encryptedMatrix);
        System.out.print(
            "    + Noise budget in fresh encryption: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n"
        );
        System.out.print("\n");

        /*
        Rotations require yet another type of special key called `Galois keys'. These
        are easily obtained from the KeyGenerator.
        */
        GaloisKeys galoisKeys = new GaloisKeys();
        keygen.createGaloisKeys(galoisKeys);

        /*
        Now rotate both matrix rows 3 steps to the left, decrypt, decode, and print.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Rotate rows 3 steps left.\n");
        evaluator.rotateRowsInplace(encryptedMatrix, 3, galoisKeys);
        Plaintext plainResult = new Plaintext();
        System.out.print(
            "    + Noise budget after rotation: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n"
        );
        System.out.print("    + Decrypt and decode ...... Correct.\n");
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
        printMatrix(podMatrix, rowSize);

        /*
        We can also rotate the columns, i.e., swap the rows.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Rotate columns.\n");
        evaluator.rotateColumnsInplace(encryptedMatrix, galoisKeys);
        System.out.print(
            "    + Noise budget after rotation: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n"
        );
        System.out.print("    + Decrypt and decode ...... Correct.\n");
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
        printMatrix(podMatrix, rowSize);

        /*
        Finally, we rotate the rows 4 steps to the right, decrypt, decode, and print.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Rotate rows 4 steps right.\n");
        evaluator.rotateRowsInplace(encryptedMatrix, -4, galoisKeys);
        System.out.print(
            "    + Noise budget after rotation: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n"
        );
        System.out.print("    + Decrypt and decode ...... Correct.\n");
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podMatrix);
        printMatrix(podMatrix, rowSize);

        /*
        Note that rotations do not consume any noise budget. However, this is only
        the case when the special prime is at least as large as the other primes. The
        same holds for relinearization. Microsoft SEAL does not require that the
        special prime is of any particular size, so ensuring this is the case is left
        for the user to do.
        */
    }

    @Test
    public void example_rotation_ckks() {
        printExampleBanner("Example: Rotation / Rotation in CKKS");

        /*
        Rotations in the CKKS scheme work very similarly to rotations in BFV.
        */
        EncryptionParameters parms = new EncryptionParameters(SchemeType.CKKS);

        int poly_modulus_degree = 8192;
        parms.setPolyModulusDegree(poly_modulus_degree);
        parms.setCoeffModulus(CoeffModulus.create(poly_modulus_degree, new int[]{ 40, 40, 40, 40, 40 }));

        SealContext context = new SealContext(parms);
        printParameters(context);
        System.out.println();

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secret_key = keygen.secretKey();
        PublicKey public_key = new PublicKey();
        keygen.createPublicKey(public_key);
        RelinKeys relin_keys = new RelinKeys();
        keygen.createRelinKeys(relin_keys);
        GaloisKeys galois_keys = new GaloisKeys();
        keygen.createGaloisKeys(galois_keys);
        Encryptor encryptor = new Encryptor(context, public_key);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secret_key);

        CkksEncoder ckks_encoder = new CkksEncoder(context);

        int slot_count = ckks_encoder.slotCount();
        System.out.println("Number of slots: " + slot_count);
        // vector<double> input;
        // input.reserve(slot_count);
        double[] input = new double[slot_count];
        double curr_point = 0;
        double step_size = 1.0 / ((double)(slot_count) - 1);
        for (int i = 0; i < slot_count; i++, curr_point += step_size) {
            input[i] = curr_point;
        }
        System.out.println("Input vector:");
        printVector(input, 3, 7);

        double scale = Math.pow(2.0, 50);

        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.println("Encode and encrypt.");
        Plaintext plain = new Plaintext();
        ckks_encoder.encode(input, scale, plain);
        Ciphertext encrypted = new Ciphertext();
        encryptor.encrypt(plain, encrypted);

        Ciphertext rotated = new Ciphertext();
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.println("Rotate 2 steps left.");
        evaluator.rotateVector(encrypted, 2, galois_keys, rotated);
        System.out.println("    + Decrypt and decode ...... Correct.");
        decryptor.decrypt(rotated, plain);
        // vector<double> result;
        double[] result = new double[slot_count];
        ckks_encoder.decode(plain, result);
        // here the example rotate 2 positions to the left.
        for (int i = 0; i < slot_count; i++) {
            Assert.assertEquals(input[(i + 2) % slot_count], result[i], 1e-5);
        }
        printVector(result, 3, 7);

        /*
        With the CKKS scheme it is also possible to evaluate a complex conjugation on
        a vector of encrypted complex numbers, using Evaluator::complex_conjugate.
        This is in fact a kind of rotation, and requires also Galois keys.
        */
    }
}
