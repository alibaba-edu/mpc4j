package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameterQualifiers;
import edu.alibaba.mpc4j.crypto.fhe.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;

import org.junit.Test;

import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * Encoders Example.
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/2_encoders.cpp
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/10/11
 */
public class EncodersExampleTest {

    @Test
    public void exampleBatchEncoder() {
        printExampleBanner("Example: Encoders / Batch Encoder");

        /*
        [BatchEncoder] (For BFV or BGV scheme)

        Let N denote the poly_modulus_degree and T denote the plain_modulus. Batching
        allows the BFV plaintext polynomials to be viewed as 2-by-(N/2) matrices, with
        each element an integer modulo T. In the matrix view, encrypted operations act
        element-wise on encrypted matrices, allowing the user to obtain speeds-ups of
        several orders of magnitude in fully vectorizable computations. Thus, in all
        but the simplest computations, batching should be the preferred method to use
        with BFV, and when used properly will result in implementations outperforming
        anything done without batching.

        In a later example, we will demonstrate how to use the BGV scheme. Batching
        works similarly for the BGV scheme to this example for the BFV scheme. For example,
        simply changing `scheme_type::bfv` into `scheme_type::bgv` can make this example
        work for the BGV scheme.
        */
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);
        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));

        /*
        To enable batching, we need to set the plain_modulus to be a prime number
        congruent to 1 modulo 2*poly_modulus_degree. Microsoft SEAL provides a helper
        method for finding such a prime. In this example we create a 20-bit prime
        that supports batching.
        */
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));

        SealContext context = new SealContext(parms);
        ExamplesUtils.printParameters(context);
        System.out.print("\n");

        /*
        We can verify that batching is indeed enabled by looking at the encryption
        parameter qualifiers created by SEALContext.
        */
        EncryptionParameterQualifiers qualifiers = context.firstContextData().qualifiers();
        System.out.print("Batching enabled: " + qualifiers.isUsingBatching() + "\n");

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.secretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);
        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);
        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);

        /*
        Batching is done through an instance of the BatchEncoder class.
        */
        BatchEncoder batchEncoder = new BatchEncoder(context);

        /*
        The total number of batching `slots' equals the poly_modulus_degree, N, and
        these slots are organized into 2-by-(N/2) matrices that can be encrypted and
        computed on. Each slot contains an integer modulo plain_modulus.
        */
        int slotCount = batchEncoder.slotCount();
        int rowSize = slotCount / 2;
        System.out.print("Plaintext matrix row size: " + rowSize + "\n");

        /*
        The matrix plaintext is simply given to BatchEncoder as a flattened vector
        of numbers. The first `row_size' many numbers form the first row, and the
        rest form the second row. Here we create the following matrix:

            [ 0,  1,  2,  3,  0,  0, ...,  0 ]
            [ 4,  5,  6,  7,  0,  0, ...,  0 ]
        */
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
        First we use BatchEncoder to encode the matrix into a plaintext polynomial.
        */
        Plaintext plainMatrix = new Plaintext();
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Encode plaintext matrix:\n");
        batchEncoder.encode(podMatrix, plainMatrix);

        /*
        We can instantly decode to verify correctness of the encoding. Note that no
        encryption or decryption has yet taken place.
        */
        long[] podResult = new long[slotCount];
        System.out.print("    + Decode plaintext matrix ...... Correct.\n");
        batchEncoder.decode(plainMatrix, podResult);
        printMatrix(podResult, rowSize);

        /*
        Next we encrypt the encoded plaintext.
        */
        Ciphertext encryptedMatrix = new Ciphertext();
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Encrypt plain_matrix to encrypted_matrix.\n");
        encryptor.encrypt(plainMatrix, encryptedMatrix);
        System.out.print(
            "    + Noise budget in encrypted_matrix: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n"
        );

        /*
        Operating on the ciphertext results in homomorphic operations being performed
        simultaneously in all 8192 slots (matrix elements). To illustrate this, we
        form another plaintext matrix

            [ 1,  2,  1,  2,  1,  2, ..., 2 ]
            [ 1,  2,  1,  2,  1,  2, ..., 2 ]

        and encode it into a plaintext.
        */
        long[] podMatrix2 = new long[slotCount];
        for (int i = 0; i < slotCount; i++) {
            podMatrix2[i] = (i & 1) + 1;
        }
        Plaintext plainMatrix2 = new Plaintext();
        batchEncoder.encode(podMatrix2, plainMatrix2);
        System.out.print("\n");
        System.out.print("Second input plaintext matrix:\n");
        printMatrix(podMatrix2, rowSize);

        /*
        We now add the second (plaintext) matrix to the encrypted matrix, and square
        the sum.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Sum, square, and relinearize.\n");
        evaluator.addPlainInplace(encryptedMatrix, plainMatrix2);
        evaluator.squareInplace(encryptedMatrix);
        evaluator.relinearizeInplace(encryptedMatrix, relinKeys);

        /*
        How much noise budget do we have left?
        */
        System.out.print("    + Noise budget in result: " + decryptor.invariantNoiseBudget(encryptedMatrix) + " bits\n");

        /*
        We decrypt and decompose the plaintext to recover the result as a matrix.
        */
        Plaintext plainResult = new Plaintext();
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Decrypt and decode result.\n");
        decryptor.decrypt(encryptedMatrix, plainResult);
        batchEncoder.decode(plainResult, podResult);
        System.out.print("    + Result plaintext matrix ...... Correct.\n");
        printMatrix(podResult, rowSize);

        /*
        Batching allows us to efficiently use the full plaintext polynomial when the
        desired encrypted computation is highly parallelizable. However, it has not
        solved the other problem mentioned in the beginning of this file: each slot
        holds only an integer modulo plain_modulus, and unless plain_modulus is very
        large, we can quickly encounter data type overflow and get unexpected results
        when integer computations are desired. Note that overflow cannot be detected
        in encrypted form. The CKKS scheme (and the CKKSEncoder) addresses the data
        type overflow issue, but at the cost of yielding only approximate results.
        */
    }

    @Test
    public void exampleCkksEncoder() {
        // TODO: implement CKKS
    }

    @Test
    public void exampleEncoders() {
        ExamplesUtils.printExampleBanner("Example: Encoders");

        /*
        Run all encoder examples.
        */
        exampleBatchEncoder();
        exampleCkksEncoder();
    }
}
