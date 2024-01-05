package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import org.junit.Test;

import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * Rotation Example.
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/6_rotation.cpp
 *
 * @author Liqiang Peng
 * @date 2023/12/25
 */
public class RotationExampleTest {

    @Test
    public void exampleRotationBfv() {
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
    public void exampleRotationCkks() {
        // TODO: implement CKKS
    }

    @Test
    public void exampleRotation() {
        printExampleBanner("Example: Rotation");

        /*
        Run all rotation examples.
        */
        exampleRotationBfv();
        exampleRotationCkks();
    }
}
