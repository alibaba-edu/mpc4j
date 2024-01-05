package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import org.junit.Test;

import java.util.Arrays;

import static edu.alibaba.mpc4j.crypto.fhe.context.SealContext.*;
import static edu.alibaba.mpc4j.crypto.fhe.examples.ExamplesUtils.*;

/**
 * Levels Example.
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/examples/3_levels.cpp
 *
 * @author Liqiang Peng
 * @date 2023/12/22
 */
public class LevelsExampleTest {

    @Test
    public void exampleLevels() {
        printExampleBanner("Example: Levels");

        /*
        In this examples we describe the concept of `levels' in BFV and CKKS and the
        related objects that represent them in Microsoft SEAL.

        In Microsoft SEAL a set of encryption parameters (excluding the random number
        generator) is identified uniquely by a 256-bit hash of the parameters. This
        hash is called the `parms_id' and can be easily accessed and printed at any
        time. The hash will change as soon as any of the parameters is changed.

        When a SEALContext is created from a given EncryptionParameters instance,
        Microsoft SEAL automatically creates a so-called `modulus switching chain',
        which is a chain of other encryption parameters derived from the original set.
        The parameters in the modulus switching chain are the same as the original
        parameters with the exception that size of the coefficient modulus is
        decreasing going down the chain. More precisely, each parameter set in the
        chain attempts to remove the last coefficient modulus prime from the
        previous set; this continues until the parameter set is no longer valid
        (e.g., plain_modulus is larger than the remaining coeff_modulus). It is easy
        to walk through the chain and access all the parameter sets. Additionally,
        each parameter set in the chain has a `chain index' that indicates its
        position in the chain so that the last set has index 0. We say that a set
        of encryption parameters, or an object carrying those encryption parameters,
        is at a higher level in the chain than another set of parameters if its the
        chain index is bigger, i.e., it is earlier in the chain.

        Each set of parameters in the chain involves unique pre-computations performed
        when the SEALContext is created, and stored in a SEALContext::ContextData
        object. The chain is basically a linked list of SEALContext::ContextData
        objects, and can easily be accessed through the SEALContext at any time. Each
        node can be identified by the parms_id of its specific encryption parameters
        (poly_modulus_degree remains the same but coeff_modulus varies).
        */
        EncryptionParameters parms = new EncryptionParameters(SchemeType.BFV);

        int polyModulusDegree = 8192;
        parms.setPolyModulusDegree(polyModulusDegree);

        /*
        In this example we use a custom coeff_modulus, consisting of 5 primes of
        sizes 50, 30, 30, 50, and 50 bits. Note that this is still OK according to
        the explanation in `1_bfv_basics.cpp'. Indeed,

            CoeffModulus::MaxBitCount(poly_modulus_degree)

        returns 218 (greater than 50+30+30+50+50=210).

        Due to the modulus switching chain, the order of the 5 primes is significant.
        The last prime has a special meaning and we call it the `special prime'. Thus,
        the first parameter set in the modulus switching chain is the only one that
        involves the special prime. All key objects, such as SecretKey, are created
        at this highest level. All data objects, such as Ciphertext, can be only at
        lower levels. The special prime should be as large as the largest of the
        other primes in the coeff_modulus, although this is not a strict requirement.

                  special prime +---------+
                                          |
                                          v
        coeff_modulus: { 50, 30, 30, 50, 50 }  +---+  Level 4 (all keys; `key level')
                                                   |
                                                   |
            coeff_modulus: { 50, 30, 30, 50 }  +---+  Level 3 (highest `data level')
                                                   |
                                                   |
                coeff_modulus: { 50, 30, 30 }  +---+  Level 2
                                                   |
                                                   |
                    coeff_modulus: { 50, 30 }  +---+  Level 1
                                                   |
                                                   |
                        coeff_modulus: { 50 }  +---+  Level 0 (lowest level)
        */
        parms.setCoeffModulus(CoeffModulus.create(polyModulusDegree, new int[]{ 50, 30, 30, 50, 50 }));

        /*
        In this example the plain_modulus does not play much of a role; we choose
        some reasonable value.
        */
        parms.setPlainModulus(PlainModulus.batching(polyModulusDegree, 20));

        SealContext context = new SealContext(parms);
        printParameters(context);
        System.out.print("\n");

        /*
        There are convenience method for accessing the SEALContext::ContextData for
        some of the most important levels:

            SEALContext::key_context_data(): access to key level ContextData
            SEALContext::first_context_data(): access to highest data level ContextData
            SEALContext::last_context_data(): access to lowest level ContextData

        We iterate over the chain and print the parms_id for each set of parameters.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Print the modulus switching chain.\n");

        /*
        First print the key level parameter information.
        */
        ContextData contextData = context.keyContextData();
        System.out.print("----> Level (chain index): " + contextData.chainIndex());
        System.out.print(" ...... key_context_data()\n");
        System.out.print("      parms_id: " + Arrays.toString(contextData.parmsId().value) + "\n");
        System.out.print("      coeff_modulus primes: ");
        for (int i = 0; i < parms.coeffModulus().length; i++) {
            System.out.printf("%x ", parms.coeffModulus()[i].value());
        }
        System.out.print("\n");
        System.out.print("\\\n");
        System.out.print(" \\-->");

        /*
        Next iterate over the remaining (data) levels.
        */
        contextData = context.firstContextData();
        while (contextData != null) {
            System.out.print(" Level (chain index): " + contextData.chainIndex());
            if (contextData.parmsId().equals(context.firstParmsId())) {
                System.out.print(" ...... first_context_data()\n");
            } else if (contextData.parmsId().equals(context.lastParmsId())) {
                System.out.print(" ...... last_context_data()\n");
            } else {
                System.out.print("\n");
            }
            System.out.print("      parms_id: " + Arrays.toString(contextData.parmsId().value) + "\n");
            System.out.print("      coeff_modulus primes: ");
            for (int i = 0; i < contextData.parms().coeffModulus().length; i++) {
                System.out.printf("%x ", contextData.parms().coeffModulus()[i].value());
            }
            System.out.print("\\\n");
            System.out.print(" \\-->");

            /*
            Step forward in the chain.
            */
            contextData = contextData.nextContextData();
        }
        System.out.print(" End of chain reached\n\n");

        /*
        We create some keys and check that indeed they appear at the highest level.
        */
        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey secretKey = keygen.secretKey();
        PublicKey publicKey = new PublicKey();
        keygen.createPublicKey(publicKey);
        RelinKeys relinKeys = new RelinKeys();
        keygen.createRelinKeys(relinKeys);

        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Print the parameter IDs of generated elements.\n");
        System.out.print("    + public_key:  " + Arrays.toString(publicKey.parmsId().value) + "\n");
        System.out.print("    + secret_key:  " + Arrays.toString(secretKey.parmsId().value) + "\n");
        System.out.print("    + relin_keys:  " + Arrays.toString(relinKeys.parmsId().value) + "\n");

        Encryptor encryptor = new Encryptor(context, publicKey);
        Evaluator evaluator = new Evaluator(context);
        Decryptor decryptor = new Decryptor(context, secretKey);

        /*
        In the BFV scheme plaintexts do not carry a parms_id, but ciphertexts do. Note
        how the freshly encrypted ciphertext is at the highest data level.
        */
        Plaintext plain = new Plaintext("1x^3 + 2x^2 + 3x^1 + 4");
        Ciphertext encrypted = new Ciphertext();
        encryptor.encrypt(plain, encrypted);
        System.out.print("    + plain:       " + Arrays.toString(plain.parmsId().value) + " (not set in BFV)\n");
        System.out.print("    + encrypted:   " + Arrays.toString(encrypted.parmsId().value) + "\n\n");

        /*
        `Modulus switching' is a technique of changing the ciphertext parameters down
        in the chain. The function Evaluator::mod_switch_to_next always switches to
        the next level down the chain, whereas Evaluator::mod_switch_to switches to
        a parameter set down the chain corresponding to a given parms_id. However, it
        is impossible to switch up in the chain.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Perform modulus switching on encrypted and print.\n");
        contextData = context.firstContextData();
        System.out.print("---->");
        while (contextData.nextContextData() != null) {
            System.out.print(" Level (chain index): " + contextData.chainIndex() + "\n");
            System.out.print("      parms_id of encrypted: " + Arrays.toString(encrypted.parmsId().value) + "\n");
            System.out.print("      Noise budget at this level: " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
            System.out.print("\\\n");
            System.out.print(" \\-->");
            evaluator.modSwitchToNextInplace(encrypted);
            contextData = contextData.nextContextData();
        }
        System.out.print(" Level (chain index): " + contextData.chainIndex() + "\n");
        System.out.print("      parms_id of encrypted: " + Arrays.toString(encrypted.parmsId().value) + "\n");
        System.out.print("      Noise budget at this level: " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
        System.out.print("\\\n");
        System.out.print(" \\-->");
        System.out.print(" End of chain reached\n\n");

        /*
        At this point it is hard to see any benefit in doing this: we lost a huge
        amount of noise budget (i.e., computational power) at each switch and seemed
        to get nothing in return. Decryption still works.
        */
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Decrypt still works after modulus switching.\n");
        decryptor.decrypt(encrypted, plain);
        System.out.print("    + Decryption of encrypted: " + plain);
        System.out.print(" ...... Correct.\n\n");

        /*
        However, there is a hidden benefit: the size of the ciphertext depends
        linearly on the number of primes in the coefficient modulus. Thus, if there
        is no need or intention to perform any further computations on a given
        ciphertext, we might as well switch it down to the smallest (last) set of
        parameters in the chain before sending it back to the secret key holder for
        decryption.

        Also the lost noise budget is actually not an issue at all, if we do things
        right, as we will see below.

        First we recreate the original ciphertext and perform some computations.
        */
        System.out.print("Computation is more efficient with modulus switching.\n");
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Compute the 8th power.\n");
        encryptor.encrypt(plain, encrypted);
        System.out.print("    + Noise budget fresh:                   " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        System.out.print("    + Noise budget of the 2nd power:         " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        System.out.print("    + Noise budget of the 4th power:         " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");

        /*
        Surprisingly, in this case modulus switching has no effect at all on the
        noise budget.
        */
        evaluator.modSwitchToNextInplace(encrypted);
        System.out.print("    + Noise budget after modulus switching:  " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
        /*
        This means that there is no harm at all in dropping some of the coefficient
        modulus after doing enough computations. In some cases one might want to
        switch to a lower level slightly earlier, actually sacrificing some of the
        noise budget in the process, to gain computational performance from having
        smaller parameters. We see from the print-out that the next modulus switch
        should be done ideally when the noise budget is down to around 25 bits.
        */
        evaluator.squareInplace(encrypted);
        evaluator.relinearizeInplace(encrypted, relinKeys);
        System.out.print("    + Noise budget of the 8th power:         " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");
        evaluator.modSwitchToNextInplace(encrypted);
        System.out.print("    + Noise budget after modulus switching:  " + decryptor.invariantNoiseBudget(encrypted) + " bits\n");

        /*
        At this point the ciphertext still decrypts correctly, has very small size,
        and the computation was as efficient as possible. Note that the decryptor
        can be used to decrypt a ciphertext at any level in the modulus switching
        chain.
        */
        decryptor.decrypt(encrypted, plain);
        System.out.print("    + Decryption of the 8th power (hexadecimal) ...... Correct.\n");
        System.out.print("    " + plain + "\n\n");

        /*
        In BFV modulus switching is not necessary and in some cases the user might
        not want to create the modulus switching chain, except for the highest two
        levels. This can be done by passing a bool `false' to SEALContext constructor.
        */
        context = new SealContext(parms, false);

        /*
        We can check that indeed the modulus switching chain has been created only
        for the highest two levels (key level and highest data level). The following
        loop should execute only once.
        */
        System.out.print("Optionally disable modulus switching chain expansion.\n");
        printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());
        System.out.print("Print the modulus switching chain.\n");
        System.out.print("---->");
        for (contextData = context.keyContextData(); contextData != null; contextData = contextData.nextContextData()) {
            System.out.print(" Level (chain index): " + contextData.chainIndex() + "\n");
            System.out.print("      parms_id: " + Arrays.toString(contextData.parmsId().value) + "\n");
            System.out.print("      coeff_modulus primes: ");
            for (int i = 0; i < contextData.parms().coeffModulus().length; i++) {
                System.out.printf("%x ", contextData.parms().coeffModulus()[i].value());
            }
            System.out.print("\\\n");
            System.out.print(" \\-->");
        }
        System.out.print(" End of chain reached\n\n");

        /*
        It is very important to understand how this example works since in the BGV
        scheme modulus switching has a much more fundamental purpose and the next
        examples will be difficult to understand unless these basic properties are
        totally clear.
        */
    }
}
