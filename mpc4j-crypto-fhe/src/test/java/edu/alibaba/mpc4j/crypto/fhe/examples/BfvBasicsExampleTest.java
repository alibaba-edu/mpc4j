package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * BFV Basics Example.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/1_bfv_basics.cpp
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/10/11
 */
public class BfvBasicsExampleTest {

    @Test
    public void exampleBfvBasics() {
        printExampleBanner("Example: BFV Basics");

        /*
        In this example, we demonstrate performing simple computations (a polynomial
        evaluation) on encrypted integers using the BFV encryption scheme.

        The first task is to set up an instance of the EncryptionParameters class.
        It is critical to understand how the different parameters behave, how they
        affect the encryption scheme, performance, and the security level. There are
        three encryption parameters that are necessary to set:

        - poly_modulus_degree (degree of polynomial modulus);
        - coeff_modulus ([ciphertext] coefficient modulus);
        - plain_modulus (plaintext modulus; only for the BFV scheme).

        The BFV scheme cannot perform arbitrary computations on encrypted data.
        Instead, each ciphertext has a specific quantity called the `invariant noise
        budget' -- or `noise budget' for short -- measured in bits. The noise budget
        in a freshly encrypted ciphertext (initial noise budget) is determined by
        the encryption parameters. Homomorphic operations consume the noise budget
        at a rate also determined by the encryption parameters. In BFV the two basic
        operations allowed on encrypted data are additions and multiplications, of
        which additions can generally be thought of as being nearly free in terms of
        noise budget consumption compared to multiplications. Since noise budget
        consumption compounds in sequential multiplications, the most significant
        factor in choosing appropriate encryption parameters is the multiplicative
        depth of the arithmetic circuit that the user wants to evaluate on encrypted
        data. Once the noise budget of a ciphertext reaches zero it becomes too
        corrupted to be decrypted. Thus, it is essential to choose the parameters to
        be large enough to support the desired computation; otherwise the result is
        impossible to make sense of even with the secret key.
        */
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);

        /*
        The first parameter we set is the degree of the `polynomial modulus'. This
        must be a positive power of 2, representing the degree of a power-of-two
        cyclotomic polynomial; it is not necessary to understand what this means.

        Larger poly_modulus_degree makes ciphertext sizes larger and all operations
        slower, but enables more complicated encrypted computations. Recommended
        values are 1024, 2048, 4096, 8192, 16384, 32768, but it is also possible
        to go beyond this range.

        In this example we use a relatively small polynomial modulus. Anything
        smaller than this will enable only very restricted encrypted computations.
        */
        int polyModulusDegree = 4096;
        parms.setPolyModulusDegree(polyModulusDegree);

        /*
        Next we set the [ciphertext] `coefficient modulus' (coeff_modulus). This
        parameter is a large integer, which is a product of distinct prime numbers,
        each up to 60 bits in size. It is represented as a vector of these prime
        numbers, each represented by an instance of the Modulus class. The
        bit-length of coeff_modulus means the sum of the bit-lengths of its prime
        factors.

        A larger coeff_modulus implies a larger noise budget, hence more encrypted
        computation capabilities. However, an upper bound for the total bit-length
        of the coeff_modulus is determined by the poly_modulus_degree, as follows:

            +----------------------------------------------------+
            | poly_modulus_degree | max coeff_modulus bit-length |
            +---------------------+------------------------------+
            | 1024                | 27                           |
            | 2048                | 54                           |
            | 4096                | 109                          |
            | 8192                | 218                          |
            | 16384               | 438                          |
            | 32768               | 881                          |
            +---------------------+------------------------------+

        These numbers can also be found in native/src/seal/util/hestdparms.h encoded
        in the function SEAL_HE_STD_PARMS_128_TC, and can also be obtained from the
        function

            CoeffModulus::MaxBitCount(poly_modulus_degree).

        For example, if poly_modulus_degree is 4096, the coeff_modulus could consist
        of three 36-bit primes (108 bits).

        Microsoft SEAL comes with helper functions for selecting the coeff_modulus.
        For new users the easiest way is to simply use

            CoeffModulus::BFVDefault(poly_modulus_degree),

        which returns std::vector<Modulus> consisting of a generally good choice
        for the given poly_modulus_degree.
        */
        parms.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree));

        /*
        The plaintext modulus can be any positive integer, even though here we take
        it to be a power of two. In fact, in many cases one might instead want it
        to be a prime number; we will see this in later examples. The plaintext
        modulus determines the size of the plaintext data type and the consumption
        of noise budget in multiplications. Thus, it is essential to try to keep the
        plaintext data type as small as possible for best performance. The noise
        budget in a freshly encrypted ciphertext is

            ~ log2(coeff_modulus/plain_modulus) (bits)

        and the noise budget consumption in a homomorphic multiplication is of the
        form log2(plain_modulus) + (other terms).

        The plaintext modulus is specific to the BFV scheme, and cannot be set when
        using the CKKS scheme.
        */
        parms.setPlainModulus(1024);

        /*
        Now that all parameters are set, we are ready to construct a SEALContext
        object. This is a heavy class that checks the validity and properties of the
        parameters we just set.
        */
        SealContext context = new SealContext(parms);

        /*
        Print the parameters that we have chosen.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Set encryption parameters and print\n");
        printParameters(context);

        /*
        When parameters are used to create SEALContext, Microsoft SEAL will first
        validate those parameters. The parameters chosen here are valid.
        */
        System.out.print("Parameter validation (success): " + context.parametersErrorMessage() + "\n");

        System.out.print("\n");
        System.out.print("~~~~~~ A naive way to calculate 4(x^2+1)(x+1)^2. ~~~~~~\n");

        /*
        The encryption schemes in Microsoft SEAL are public key encryption schemes.
        For users unfamiliar with this terminology, a public key encryption scheme
        has a separate public key for encrypting data, and a separate secret key for
        decrypting data. This way multiple parties can encrypt data using the same
        shared public key, but only the proper recipient of the data can decrypt it
        with the secret key.

        We are now ready to generate the secret and public keys. For this purpose
        we need an instance of the KeyGenerator class. Constructing a KeyGenerator
        automatically generates a secret key. We can then create as many public
        keys for it as we want using KeyGenerator::create_public_key.

        Note that KeyGenerator::create_public_key has another overload that takes
        no parameters and returns a Serializable<PublicKey> object. We will discuss
        this in `6_serialization.cpp'.
        */
        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.secretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);

        /*
        To be able to encrypt we need to construct an instance of Encryptor. Note
        that the Encryptor only requires the public key, as expected. It is also
        possible to use Microsoft SEAL in secret-key mode by providing the Encryptor
        the secret key instead. We will discuss this in `6_serialization.cpp'.
        */
        Encryptor encryptor = new Encryptor(context, publicKey);

        /*
        Computations on the ciphertexts are performed with the Evaluator class. In
        a real use-case the Evaluator would not be constructed by the same party
        that holds the secret key.
        */
        Evaluator evaluator = new Evaluator(context);

        /*
        We will of course want to decrypt our results to verify that everything worked,
        so we need to also construct an instance of Decryptor. Note that the Decryptor
        requires the secret key.
        */
        Decryptor decryptor = new Decryptor(context, secretKey);

        /*
        As an example, we evaluate the degree 4 polynomial

            4x^4 + 8x^3 + 8x^2 + 8x + 4

        over an encrypted x = 6. The coefficients of the polynomial can be considered
        as plaintext inputs, as we will see below. The computation is done modulo the
        plain_modulus 1024.

        While this examples is simple and easy to understand, it does not have much
        practical value. In later examples we will demonstrate how to compute more
        efficiently on encrypted integers and real or complex numbers.

        Plaintexts in the BFV scheme are polynomials of degree less than the degree
        of the polynomial modulus, and coefficients integers modulo the plaintext
        modulus. For readers with background in ring theory, the plaintext space is
        the polynomial quotient ring Z_T[X]/(X^N + 1), where N is poly_modulus_degree
        and T is plain_modulus.

        To get started, we create a plaintext containing the constant 6. For the
        plaintext element we use a constructor that takes the desired polynomial as
        a string with coefficients represented as hexadecimal numbers.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        long x = 6;
        Plaintext xPlain = new Plaintext(uint64ToHexString(x));
        System.out.print("Express x = " + x + " as a plaintext polynomial 0x" + xPlain + ".\n");

        /*
        We then encrypt the plaintext, producing a ciphertext. We note that the
        Encryptor::encrypt function has another overload that takes as input only
        a plaintext and returns a Serializable<Ciphertext> object. We will discuss
        this in `6_serialization.cpp'.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        Ciphertext xEncrypted = new Ciphertext();
        System.out.print("Encrypt x_plain to x_encrypted.\n");
        encryptor.encrypt(xPlain, xEncrypted);

        /*
        In Microsoft SEAL, a valid ciphertext consists of two or more polynomials
        whose coefficients are integers modulo the product of the primes in the
        coeff_modulus. The number of polynomials in a ciphertext is called its `size'
        and is given by Ciphertext::size(). A freshly encrypted ciphertext always
        has size 2.
        */
        System.out.print("    + size of freshly encrypted x: " + xEncrypted.size() + "\n");

        /*
        There is plenty of noise budget left in this freshly encrypted ciphertext.
        */
        System.out.print("    + noise budget in freshly encrypted x: " + decryptor.invariantNoiseBudget(xEncrypted) + "\n");

        /*
        We decrypt the ciphertext and print the resulting plaintext in order to
        demonstrate correctness of the encryption.
        */
        Plaintext xDecrypted = new Plaintext();
        System.out.print("    + decryption of x_encrypted: ");
        decryptor.decrypt(xEncrypted, xDecrypted);
        System.out.print("0x" + xDecrypted + " ...... Correct.\n");

        /*
        When using Microsoft SEAL, it is typically advantageous to compute in a way
        that minimizes the longest chain of sequential multiplications. In other
        words, encrypted computations are best evaluated in a way that minimizes
        the multiplicative depth of the computation, because the total noise budget
        consumption is proportional to the multiplicative depth. For example, for
        our example computation it is advantageous to factorize the polynomial as

            4x^4 + 8x^3 + 8x^2 + 8x + 4 = 4(x + 1)^2 * (x^2 + 1)

        to obtain a simple depth 2 representation. Thus, we compute (x + 1)^2 and
        (x^2 + 1) separately, before multiplying them, and multiplying by 4.

        First, we compute x^2 and add a plaintext "1". We can clearly see from the
        print-out that multiplication has consumed a lot of noise budget. The user
        can vary the plain_modulus parameter to see its effect on the rate of noise
        budget consumption.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute x_sq_plus_one (x^2+1).\n");
        Ciphertext xSqPlusOne = new Ciphertext();
        evaluator.square(xEncrypted, xSqPlusOne);
        Plaintext plainOne = new Plaintext("1");
        evaluator.addPlainInplace(xSqPlusOne, plainOne);

        /*
        Encrypted multiplication results in the output ciphertext growing in size.
        More precisely, if the input ciphertexts have size M and N, then the output
        ciphertext after homomorphic multiplication will have size M+N-1. In this
        case we perform a squaring, and observe both size growth and noise budget
        consumption.
        */
        System.out.print("    + size of x_sq_plus_one: " + xSqPlusOne.size() + "\n");
        System.out.print("    + noise budget in x_sq_plus_one: " + decryptor.invariantNoiseBudget(xSqPlusOne) + " bits\n");

        /*
        Even though the size has grown, decryption works as usual as long as noise
        budget has not reached 0.
        */
        Plaintext decryptedResult = new Plaintext();
        System.out.print("    + decryption of x_sq_plus_one: ");
        decryptor.decrypt(xSqPlusOne, decryptedResult);
        System.out.print("0x" + decryptedResult + "...... Correct\n");

        /*
        Next, we compute (x + 1)^2.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute x_plus_one_sq ((x+1)^2)\n");
        Ciphertext xPlusOneSq = new Ciphertext();
        evaluator.addPlain(xEncrypted, plainOne, xPlusOneSq);
        evaluator.squareInplace(xPlusOneSq);
        System.out.print("    + size of x_plus_one_sq: " + xPlusOneSq.size() + "\n");
        System.out.print("    + noise budget in x_plus_one_sq: " + decryptor.invariantNoiseBudget(xPlusOneSq) + " bits\n");
        System.out.print("    + decryption of x_plus_one_sq: ");
        decryptor.decrypt(xPlusOneSq, decryptedResult);
        System.out.print("0x" + decryptedResult + " ...... Correct.\n");

        /*
        Finally, we multiply (x^2 + 1) * (x + 1)^2 * 4.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute encrypted_result (4(x^2+1)(x+1)^2).\n");
        Ciphertext encryptedResult = new Ciphertext();
        Plaintext plainFour = new Plaintext("4");
        evaluator.multiplyPlainInplace(xSqPlusOne, plainFour);
        evaluator.multiply(xSqPlusOne, xPlusOneSq, encryptedResult);
        System.out.print("    + size of encrypted_result: " + encryptedResult.size() + "\n");
        System.out.print("    + noise budget in encrypted_result: " + decryptor.invariantNoiseBudget(encryptedResult) + " bits\n");
        System.out.print("NOTE: Decryption can be incorrect if noise budget is zero.\n");

        System.out.print("\n");
        System.out.print("~~~~~~ A better way to calculate 4(x^2+1)(x+1)^2. ~~~~~~\n");

        /*
        Noise budget has reached 0, which means that decryption cannot be expected
        to give the correct result. This is because both ciphertexts x_sq_plus_one
        and x_plus_one_sq consist of 3 polynomials due to the previous squaring
        operations, and homomorphic operations on large ciphertexts consume much more
        noise budget than computations on small ciphertexts. Computing on smaller
        ciphertexts is also computationally significantly cheaper.

        `Relinearization' is an operation that reduces the size of a ciphertext after
        multiplication back to the initial size, 2. Thus, relinearizing one or both
        input ciphertexts before the next multiplication can have a huge positive
        impact on both noise growth and performance, even though relinearization has
        a significant computational cost itself. It is only possible to relinearize
        size 3 ciphertexts down to size 2, so often the user would want to relinearize
        after each multiplication to keep the ciphertext sizes at 2.

        Relinearization requires special `relinearization keys', which can be thought
        of as a kind of public key. Relinearization keys can easily be created with
        the KeyGenerator.

        Relinearization is used similarly in both the BFV and the CKKS schemes, but
        in this example we continue using BFV. We repeat our computation from before,
        but this time relinearize after every multiplication.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Generate relinearization keys.\n");
        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);

        /*
        We now repeat the computation relinearizing after each multiplication.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute and relinearize x_squared (x^2),\n");
        System.out.print(StringUtils.repeat(' ', 13) + "then compute x_sq_plus_one (x^2+1)\n");
        Ciphertext xSquared = new Ciphertext();
        evaluator.square(xEncrypted, xSquared);
        System.out.print("    + size of x_squared: " + xSquared.size() + "\n");
        evaluator.relinearizeInplace(xSquared, relinKeys);
        System.out.print("    + size of x_squared (after relinearization): " + xSquared.size() + "\n");
        evaluator.addPlain(xSquared, plainOne, xSqPlusOne);
        System.out.print("    + noise budget in x_sq_plus_one: " + decryptor.invariantNoiseBudget(xSqPlusOne) + " bits\n");
        System.out.print("    + decryption of x_sq_plus_one: ");
        decryptor.decrypt(xSqPlusOne, decryptedResult);
        System.out.print("0x" + decryptedResult + " ...... Correct.\n");

        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        Ciphertext xPlusOne = new Ciphertext();
        System.out.print("Compute x_plus_one (x+1),\n");
        System.out.print("then compute and relinearize x_plus_one_sq ((x+1)^2).\n");
        evaluator.addPlain(xEncrypted, plainOne, xPlusOne);
        evaluator.square(xPlusOne, xPlusOneSq);
        System.out.print("    + size of x_plus_one_sq: " + xPlusOneSq.size() + "\n");
        evaluator.relinearizeInplace(xPlusOneSq, relinKeys);
        System.out.print("    + size of x_plus_one_sq: " + xPlusOneSq.size() + "\n");
        System.out.print("    + noise budget in x_plus_one_sq: " + decryptor.invariantNoiseBudget(xPlusOneSq) + " bits\n");
        System.out.print("    + decryption of x_plus_one_sq: ");
        decryptor.decrypt(xPlusOneSq, decryptedResult);
        System.out.print("0x" + decryptedResult + " ...... Correct.\n");

        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute and relinearize encrypted_result (4(x^2+1)(x+1)^2).\n");
        evaluator.multiplyPlainInplace(xSqPlusOne, plainFour);
        evaluator.multiply(xSqPlusOne, xPlusOneSq, encryptedResult);
        System.out.print("    + size of encrypted_result: " + encryptedResult.size() + "\n");
        evaluator.relinearizeInplace(encryptedResult, relinKeys);
        System.out.print("    + size of encrypted_result (after relinearization): " + encryptedResult.size() + "\n");
        System.out.print("    + noise budget in encrypted_result: " + decryptor.invariantNoiseBudget(encryptedResult) + " bits\n");

        System.out.print("\n");
        System.out.print("NOTE: Notice the increase in remaining noise budget.\n");

        /*
        Relinearization clearly improved our noise consumption. We have still plenty
        of noise budget left, so we can expect the correct answer when decrypting.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Decrypt encrypted_result (4(x^2+1)(x+1)^2).\n");
        decryptor.decrypt(encryptedResult, decryptedResult);
        System.out.print("    + decryption of 4(x^2+1)(x+1)^2 = 0x" + decryptedResult + " ...... Correct.\n");

        /*
        For x=6, 4(x^2+1)(x+1)^2 = 7252. Since the plaintext modulus is set to 1024,
        this result is computed in integers modulo 1024. Therefore the expected output
        should be 7252 % 1024 == 84, or 0x54 in hexadecimal.
        */

        /*
        Sometimes we create customized encryption parameters which turn out to be invalid.
        Microsoft SEAL can interpret the reason why parameters are considered invalid.
        Here we simply reduce the polynomial modulus degree to make the parameters not
        compliant with the HomomorphicEncryption.org security standard.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("An example of invalid parameters\n");
        parms.setPolyModulusDegree(2048);
        context = new SealContext(parms);
        printParameters(context);
        System.out.print("Parameter validation (failed): " + context.parametersErrorMessage() + "\n");

        /*
        This information is helpful to fix invalid encryption parameters.
        */
    }
}
