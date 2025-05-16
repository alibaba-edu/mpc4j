package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.StrideIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.AbstractModulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * Provides operations on ciphertexts. Due to the properties of the encryption scheme, the arithmetic operations pass
 * through the encryption layer to the underlying plaintext, changing it according to the type of the operation. Since
 * the plaintext elements are fundamentally polynomials in the polynomial quotient ring Z_T[x]/(X^N+1), where T is the
 * plaintext modulus and X^N+1 is the polynomial modulus, this is the ring where the arithmetic operations will take
 * place. BatchEncoder (batching) provider an alternative possibly more convenient view of the plaintext elements as
 * 2-by-(N/2) matrices of integers modulo the plaintext modulus. In the batching view the arithmetic operations act on
 * the matrices element-wise. Some of the operations only apply in the batching view, such as matrix row and column
 * rotations. Other operations such as relinearization have no semantic meaning but are necessary for performance
 * reasons.
 * <p>
 * Arithmetic Operations
 * <p>
 * The core operations are arithmetic operations, in particular multiplication and addition of ciphertexts. In addition
 * to these, we also provide negation, subtraction, squaring, exponentiation, and multiplication and addition of
 * several ciphertexts for convenience. in many cases some of the inputs to a computation are plaintext elements rather
 * than ciphertexts. For this we provide fast "plain" operations: plain addition, plain subtraction, and plain
 * multiplication.
 * <p>
 * Relinearization
 * <p>
 * One of the most important non-arithmetic operations is relinearization, which takes as input a ciphertext of size
 * K+1 and relinearization keys (at least K-1 keys are needed), and changes the size of the ciphertext down to 2
 * (minimum size). For most use-cases only one relinearization key suffices, in which case relinearization should be
 * performed after every multiplication. Homomorphic multiplication of ciphertexts of size K+1 and L+1 outputs a
 * ciphertext of size K+L+1, and the computational cost of multiplication is proportional to K*L. Plain multiplication
 * and addition operations of any type do not change the size. Relinearization requires relinearization keys to have
 * been generated.
 * <p>
 * Rotations
 * <p>
 * When batching is enabled, we provide operations for rotating the plaintext matrix rows cyclically left or right, and
 * for rotating the columns (swapping the rows). Rotations require Galois keys to have been generated.
 * <p>
 * Other Operations
 * <p>
 * We also provide operations for transforming ciphertexts to NTT form and back, and for transforming plaintext
 * polynomials to NTT form. These can be used in a very fast plain multiplication variant, that assumes the inputs to
 * be in NTT form. Since the NTT has to be done in any case in plain multiplication, this function can be used when
 * e.g. one plaintext input is used in several plain multiplication, and transforming it several times would not make
 * sense.
 * <p>
 * NTT form
 * <p>
 * When using the BFV/BGV scheme (scheme_type::bfv/bgv), all plaintexts and ciphertexts should remain by default in the
 * usual coefficient representation, i.e., not in NTT form. When using the CKKS scheme (scheme_type::ckks), all
 * plaintexts and ciphertexts should remain by default in NTT form. We call these scheme-specific NTT states the
 * "default NTT form". Some functions, such as add, work even if the inputs are not in the default state, but others,
 * such as multiply, will throw an exception. The output of all evaluation functions will be in the same state as the
 * input(s), with the exception of the transform_to_ntt and transform_from_ntt functions, which change the state.
 * Ideally, unless these two functions are called, all other functions should "just work".
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/evaluator.h">evaluator.h</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/25
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Evaluator {
    /**
     * Checks if two ciphertexts have the same scale.
     *
     * @param value1 1st ciphertext.
     * @param value2 2nd ciphertext.
     * @return true if two ciphertext have the same scale; false otherwise.
     */
    private static boolean are_same_scale(Ciphertext value1, Ciphertext value2) {
        return Common.areClose(value1.scale(), value2.scale());
    }

    /**
     * Checks if a ciphertext and a plaintext have the same scale.
     *
     * @param value1 ciphertext.
     * @param value2 plaintext.
     * @return true if the ciphertext and the plaintext have the same scale; false otherwise.
     */
    private static boolean are_same_scale(Ciphertext value1, Plaintext value2) {
        return Common.areClose(value1.scale(), value2.scale());
    }

    /**
     * Checks if the scale is within the bounds defined by the encryption parameters.
     *
     * @param scale       The scale to check.
     * @param contextData context data.
     * @return true if the scale is within the bounds defined by the encryption parameters; false otherwise.
     */
    private static boolean is_scale_within_bounds(double scale, ContextData contextData) {
        int scaleBitCountBound = switch (contextData.parms().scheme()) {
            case BFV, BGV -> contextData.parms().plainModulus().bitCount();
            case CKKS -> contextData.totalCoeffModulusBitCount();
            default ->
                // Unsupported scheme; check will fail
                -1;
        };
        return !(scale <= 0 || (int) (Math.log(scale) / Math.log(2)) >= scaleBitCountBound);
    }

    /**
     * Returns (f, e1, e2) such that
     * <li>(1) e1 * factor1 = e2 * factor2 = f mod p;</li>
     * <li>(2) gcd(e1, p) = 1 and gcd(e2, p) = 1;</li>
     * <li>(3) abs(e1_bal) + abs(e2_bal) is minimal, where e1_bal and e2_bal represent e1 and e2 in (-p/2, p/2].</li>
     *
     * @param factor1      factor1.
     * @param factor2      factor2.
     * @param plainModulus plain modulus.
     * @return (f, e1, e2).
     */
    private static long[] balance_correction_factors(long factor1, long factor2, Modulus plainModulus) {
        long t = plainModulus.value();
        long halfT = t >> 1;

        // ratio = f2 / f1 mod p
        long[] ratio = new long[]{1};
        if (!UintArithmeticSmallMod.tryInvertUintMod(factor1, plainModulus, ratio)) {
            throw new IllegalArgumentException("invalid correction factor1");
        }

        ratio[0] = UintArithmeticSmallMod.multiplyUintMod(ratio[0], factor2, plainModulus);
        long e1 = ratio[0];
        long e2 = 1;
        long sum = sumAbs(e1, e2, t, halfT);

        // Extended Euclidean
        long prevA = plainModulus.value();
        long prevB = 0;
        long a = ratio[0];
        long b = 1;
        while (a != 0) {
            long q = prevA / a;
            long temp = prevA % a;
            prevA = a;
            a = temp;

            temp = Common.subSafe(prevB, Common.mulSafe(b, q, false), false);
            prevB = b;
            b = temp;

            long aMod = UintArithmeticSmallMod.barrettReduce64(Math.abs(a), plainModulus);
            if (a < 0) {
                aMod = UintArithmeticSmallMod.negateUintMod(aMod, plainModulus);
            }
            long bMod = UintArithmeticSmallMod.barrettReduce64(Math.abs(b), plainModulus);
            if (b < 0) {
                bMod = UintArithmeticSmallMod.negateUintMod(aMod, plainModulus);
            }
            // which also implies gcd(b_mod, t) == 1
            if (aMod != 0 && Numth.gcd(aMod, t) == 1) {
                long newSum = sumAbs(aMod, bMod, t, halfT);
                if (newSum < sum) {
                    sum = newSum;
                    e1 = aMod;
                    e2 = bMod;
                }
            }
        }
        long f = UintArithmeticSmallMod.multiplyUintMod(e1, factor1, plainModulus);
        return new long[]{f, e1, e2};
    }

    private static long sumAbs(long x, long y, long t, long halfT) {
        long xBal = Long.compareUnsigned(x, halfT) > 0 ? x - t : x;
        long yBal = Long.compareUnsigned(y, halfT) > 0 ? y - t : y;

        return Math.abs(xBal) + Math.abs(yBal);
    }

    /**
     * SEALContext
     */
    private final SealContext context;

    /**
     * Creates an Evaluator instance initialized with the specified SEALContext.
     *
     * @param context the SEALContext.
     */
    public Evaluator(SealContext context) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
    }

    /**
     * Negates a ciphertext.
     *
     * @param encrypted The ciphertext to negate.
     */
    public void negateInplace(Ciphertext encrypted) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int encrypted_size = encrypted.size();

        PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);
        // Negate each poly in the array
        PolyArithmeticSmallMod.negatePolyCoeffModPoly(encrypted_iter, encrypted_size, coeff_modulus, encrypted_iter);
    }

    /**
     * Negates a ciphertext and stores the result in the destination parameter.
     *
     * @param encrypted   The ciphertext to negate.
     * @param destination The ciphertext to overwrite with the negated result.
     */
    public void negate(Ciphertext encrypted, Ciphertext destination) {
        destination.copyFrom(encrypted);
        negateInplace(destination);
    }

    /**
     * Adds two ciphertexts. This function adds together encrypted1 and encrypted2 and stores the result in encrypted1.
     *
     * @param encrypted1 the first ciphertext to add.
     * @param encrypted2 the second ciphertext to add.
     */
    public void addInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted1, context) || !ValCheck.isBufferValid(encrypted1)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }
        if (!ValCheck.isMetaDataValidFor(encrypted2, context) || !ValCheck.isBufferValid(encrypted2)) {
            throw new IllegalArgumentException("encrypted2 is not valid for encryption parameters");
        }
        if (!encrypted1.parmsId().equals(encrypted2.parmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }
        if (encrypted1.isNttForm() != encrypted2.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }
        if (!are_same_scale(encrypted1, encrypted2)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        // Extract encryption parameters.
        ContextData contextData = context.getContextData(encrypted1.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        Modulus plainModulus = parms.plainModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encrypted1Size = encrypted1.size();
        int encrypted2Size = encrypted2.size();
        // ciphertext sizes may be different
        int maxCount = Math.max(encrypted1Size, encrypted2Size);
        int minCount = Math.min(encrypted1Size, encrypted2Size);

        // Size check
        if (!Common.productFitsIn(false, maxCount, coeffCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        PolyIterator encrypted1Poly = PolyIterator.fromCiphertext(encrypted1);
        PolyIterator encrypted2Poly = PolyIterator.fromCiphertext(encrypted2);

        if (encrypted1.correctionFactor() != encrypted2.correctionFactor()) {
            // Balance correction factors and multiply by scalars before addition in BGV
            long[] factors = balance_correction_factors(
                encrypted1.correctionFactor(), encrypted2.correctionFactor(), plainModulus
            );
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                encrypted1Poly,
                encrypted1Size,
                factors[1],
                coeffModulus,
                encrypted1Poly
            );

            Ciphertext encrypted2Copy = new Ciphertext();
            encrypted2Copy.copyFrom(encrypted2);

            PolyIterator encrypted2CopyPoly = PolyIterator.fromCiphertext(encrypted2Copy);


            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                encrypted2Poly, encrypted2.size(),
                factors[2], coeffModulus, encrypted2CopyPoly
            );

            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2Copy.setCorrectionFactor(factors[0]);

            addInplace(encrypted1, encrypted2Copy);
        } else {
            // prepare destination
            encrypted1.resize(context, contextData.parmsId(), maxCount);
            encrypted1Poly = PolyIterator.fromCiphertext(encrypted1);

            // AddCiphertexts
            PolyArithmeticSmallMod.addPolyCoeffModPoly(
                encrypted1Poly,
                encrypted2Poly,
                minCount, coeffModulus,
                encrypted1Poly
            );

            // Copy the remaindering polys of the array with larger count into encrypted1
            if (encrypted1Size < encrypted2Size) {
                PolyCore.setPolyArray(
                    encrypted2.data(), encrypted2.getPolyOffset(minCount), encrypted2Size - encrypted1Size,
                    coeffCount, coeffModulusSize,
                    encrypted1.data(), encrypted1.getPolyOffset(encrypted1Size)
                );
            }
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted1.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    /**
     * Adds two ciphertexts. This function adds together encrypted1 and encrypted2 and stores the result in the
     * destination parameter.
     *
     * @param encrypted1  the first ciphertext to add.
     * @param encrypted2  the second ciphertext to add.
     * @param destination the ciphertext to overwrite with the addition result.
     */
    public void add(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        if (encrypted2 == destination) {
            addInplace(destination, encrypted1);
        } else {
            destination.copyFrom(encrypted1);
            addInplace(destination, encrypted2);
        }
    }

    /**
     * Adds together a vector of ciphertexts and stores the result in the destination parameter.
     *
     * @param encrypteds  the ciphertexts to add.
     * @param destination the ciphertext to overwrite with the addition result.
     */
    public void addMany(Ciphertext[] encrypteds, Ciphertext destination) {
        if (encrypteds == null || encrypteds.length == 0) {
            throw new IllegalArgumentException("encrypteds cannot be empty");
        }
        for (Ciphertext encrypted : encrypteds) {
            if (encrypted == destination) {
                throw new IllegalArgumentException("encrypteds must be different from destination");
            }
        }
        destination.copyFrom(encrypteds[0]);
        for (int i = 1; i < encrypteds.length; i++) {
            addInplace(destination, encrypteds[i]);
        }
    }

    /**
     * Subtracts two ciphertexts. This function computes the difference of encrypted1 and encrypted2, and stores the
     * result in encrypted1.
     *
     * @param encrypted1 the ciphertext to subtract from.
     * @param encrypted2 the ciphertext to subtract.
     */
    public void subInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted1, context) || !ValCheck.isBufferValid(encrypted1)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }
        if (!ValCheck.isMetaDataValidFor(encrypted2, context) || !ValCheck.isBufferValid(encrypted2)) {
            throw new IllegalArgumentException("encrypted2 is not valid for encryption parameters");
        }
        if (!encrypted1.parmsId().equals(encrypted2.parmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }
        if (encrypted1.isNttForm() != encrypted2.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }
        if (!are_same_scale(encrypted1, encrypted2)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted1.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        Modulus plain_modulus = parms.plainModulus();
        int coeff_count = parms.polyModulusDegree();
        // int coeff_modulus_size = coeff_modulus.length;
        int encrypted1_size = encrypted1.size();
        int encrypted2_size = encrypted2.size();
        // ciphertext sizes may be different
        int max_count = Math.max(encrypted1_size, encrypted2_size);
        int min_count = Math.min(encrypted1_size, encrypted2_size);

        // Size check
        if (!Common.productFitsIn(false, max_count, coeff_count)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        PolyIterator encrypted1_iter = PolyIterator.fromCiphertext(encrypted1);
        PolyIterator encrypted2_iter = PolyIterator.fromCiphertext(encrypted2);

        if (encrypted1.correctionFactor() != encrypted2.correctionFactor()) {
            // Balance correction factors and multiply by scalars before subtraction in BGV
            long[] factors = balance_correction_factors(
                encrypted1.correctionFactor(), encrypted2.correctionFactor(), plain_modulus
            );

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                encrypted1_iter, encrypted1.size(), factors[1], coeff_modulus, encrypted1_iter
            );

            Ciphertext encrypted2_copy = new Ciphertext();
            encrypted2_copy.copyFrom(encrypted2);
            PolyIterator encrypted2_copy_iter = PolyIterator.fromCiphertext(encrypted2_copy);

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                encrypted2_iter, encrypted2.size(), factors[2], coeff_modulus, encrypted2_copy_iter
            );

            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2_copy.setCorrectionFactor(factors[0]);

            subInplace(encrypted1, encrypted2_copy);
        } else {
            // prepare destination
            encrypted1.resize(context, context_data.parmsId(), max_count);
            encrypted1_iter = PolyIterator.fromCiphertext(encrypted1);

            // Subtract ciphertexts
            PolyArithmeticSmallMod.subPolyCoeffModPoly(
                encrypted1_iter, encrypted2_iter, min_count, coeff_modulus, encrypted1_iter
            );

            // If encrypted2 has larger count, negate remaining entries
            if (encrypted1_size < encrypted2_size) {
                encrypted1_iter = PolyIterator.fromCiphertext(encrypted1, min_count);
                encrypted2_iter = PolyIterator.fromCiphertext(encrypted2, min_count);
                PolyArithmeticSmallMod.negatePolyCoeffModPoly(
                    encrypted2_iter, encrypted2_size - min_count, coeff_modulus, encrypted1_iter
                );
            }
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted1.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Subtracts two ciphertexts. This function computes the difference of encrypted1 and encrypted2 and stores the
     * result in the destination parameter.
     *
     * @param encrypted1  the ciphertext to subtract from.
     * @param encrypted2  the ciphertext to subtract.
     * @param destination the ciphertext to overwrite with the subtraction result.
     */
    public void sub(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        if (encrypted2 == destination) {
            subInplace(destination, encrypted1);
            negateInplace(destination);
        } else {
            destination.copyFrom(encrypted1);
            subInplace(destination, encrypted2);
        }
    }

    /**
     * Multiplies two ciphertexts. This functions computes the product of encrypted1 and encrypted2 and stores the
     * result in encrypted1.
     *
     * @param encrypted1 the first ciphertext to multiply.
     * @param encrypted2 the second ciphertext to multiply.
     */
    public void multiplyInplace(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (!ValCheck.isMetaDataValidFor(encrypted1, context) || !ValCheck.isBufferValid(encrypted1)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!ValCheck.isMetaDataValidFor(encrypted2, context) || !ValCheck.isBufferValid(encrypted2)) {
            throw new IllegalArgumentException("encrypted1 is not valid for encryption parameters");
        }

        if (!encrypted1.parmsId().equals(encrypted2.parmsId())) {
            throw new IllegalArgumentException("encrypted1 and encrypted2 parameter mismatch");
        }

        ContextData contextData = context.firstContextData();

        switch (contextData.parms().scheme()) {
            case BFV:
                bfv_multiply(encrypted1, encrypted2);
                break;
            case CKKS:
                ckks_multiply(encrypted1, encrypted2);
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted1.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    private void bfv_multiply(Ciphertext encrypted1, Ciphertext encrypted2) {
        if (encrypted1.isNttForm() || encrypted2.isNttForm()) {
            throw new IllegalArgumentException("encrypted1 or encrypted2 cannot be in NTT form");
        }

        // Extract encryption parameters.
        ContextData contextData = context.getContextData(encrypted1.parmsId());
        EncryptionParameters parms = contextData.parms();
        int coeffCount = parms.polyModulusDegree();
        int baseQSize = parms.coeffModulus().length;
        int encrypted1Size = encrypted1.size();
        int encrypted2Size = encrypted2.size();
        long plainModulus = parms.plainModulus().value();

        RnsTool rnsTool = contextData.rnsTool();
        int baseBskSize = rnsTool.baseBsk().size();
        int baseBskMTildeSize = rnsTool.baseBskMTilde().size();

        // Determine destination.size(), which should be c1.size() * c2.size() - 1
        int destinationSize = Common.subSafe(Common.addSafe(encrypted1Size, encrypted2Size, false), 1, false);

        // Size check
        if (!Common.productFitsIn(false, destinationSize, coeffCount, baseBskMTildeSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Set up iterators for bases
        AbstractModulus[] baseQ = parms.coeffModulus();
        AbstractModulus[] baseBsk = rnsTool.baseBsk().getBase();

        // Set up iterators for NTT tables
        NttTables[] baseQNttTables = contextData.smallNttTables();
        NttTables[] baseBskNttTables = rnsTool.baseBskNttTables();

        // Microsoft SEAL uses BEHZ-style RNS multiplication. This process is somewhat complex and consists of the
        // following steps:
        //
        // (1) Lift encrypted1 and encrypted2 (initially in base q) to an extended base q U Bsk U {m_tilde}
        // (2) Remove extra multiples of q from the results with Montgomery reduction, switching base to q U Bsk
        // (3) Transform the data to NTT form
        // (4) Compute the ciphertext polynomial product using dyadic multiplication
        // (5) Transform the data back from NTT form
        // (6) Multiply the result by t (plain_modulus)
        // (7) Scale the result by q using a divide-and-floor algorithm, switching base to Bsk
        // (8) Use Shenoy-Kumaresan method to convert the result to base q

        // Resize encrypted1 to destination size
        encrypted1.resize(context, contextData.parmsId(), destinationSize);

        // Allocate space for a base q output of behz_extend_base_convert_to_ntt for encrypted1
        PolyIterator encrypted1Q = PolyIterator.allocate(encrypted1Size, coeffCount, baseQSize);
        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        PolyIterator encrypted1Bsk = PolyIterator.allocate(encrypted1Size, coeffCount, baseBskSize);

        // Perform BEHZ steps (1)-(3) for encrypted1
        behzExtendBaseConvertToNtt(encrypted1, encrypted1Size, rnsTool, baseQNttTables, encrypted1Q, encrypted1Bsk);

        // Repeat for encrypted2
        PolyIterator encrypted2Q = PolyIterator.allocate(encrypted2Size, coeffCount, baseQSize);
        PolyIterator encrypted2Bsk = PolyIterator.allocate(encrypted2Size, coeffCount, baseBskSize);
        behzExtendBaseConvertToNtt(encrypted2, encrypted2Size, rnsTool, baseQNttTables, encrypted2Q, encrypted2Bsk);

        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        PolyIterator tempDestinationQ = PolyIterator.allocate(destinationSize, coeffCount, baseQSize);
        PolyIterator tempDestinationBsk = PolyIterator.allocate(destinationSize, coeffCount, baseBskSize);

        // Perform BEHZ step (4): dyadic multiplication on arbitrary size ciphertexts
        for (int i = 0; i < destinationSize; i++) {
            // We iterate over relevant components of encrypted1 and encrypted2 in increasing order for
            // encrypted1 and reversed (decreasing) order for encrypted2. The bounds for the indices of
            // the relevant terms are obtained as follows.
            int currEncrypted1Last = Math.min(i, encrypted1Size - 1);
            int currEncrypted2First = Math.min(i, encrypted2Size - 1);
            int currEncrypted1First = i - currEncrypted2First;

            // The total number of dyadic products is now easy to compute
            int steps = currEncrypted1Last - currEncrypted1First + 1;

            // Perform the BEHZ ciphertext product both for base q and base Bsk
            behzCiphertextProduct(
                encrypted1Q, currEncrypted1First, encrypted2Q, currEncrypted2First,
                coeffCount, steps, baseQ, tempDestinationQ, i
            );
            behzCiphertextProduct(
                encrypted1Bsk, currEncrypted1First, encrypted2Bsk, currEncrypted2First,
                coeffCount, steps, baseBsk, tempDestinationBsk, i
            );
        }

        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationQ, destinationSize, baseQNttTables);
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationBsk, destinationSize, baseBskNttTables);

        PolyIterator encrypted1Poly = PolyIterator.fromCiphertext(encrypted1);
        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < destinationSize; i++) {
            // Bring together the base q and base Bsk components into a single allocation a RnsIter
            RnsIterator tempQBsk = RnsIterator.allocate(coeffCount, baseQSize + baseBskSize);

            // Step (6): multiply base q components by t (plain_modulus), ct_i * t mod q
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                tempDestinationQ.rnsIter[i], baseQSize,
                plainModulus, baseQ,
                tempQBsk
            );
            // multiply base bsk components by t (plain_modulus), ct_i * t mod bsk
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                tempDestinationBsk.rnsIter[i], baseBskSize,
                plainModulus, baseBsk,
                tempQBsk.subRnsIterator(baseQSize, tempQBsk.k() - 1)
            );
            // Allocate yet another temporary for fast divide-and-floor result in base Bsk a RnsIter
            RnsIterator tempBsk = RnsIterator.allocate(coeffCount, baseBskSize);

            // Step (7): divide by q and floor, producing a result in base Bsk
            rnsTool.fastFloorRnsIter(tempQBsk, tempBsk);

            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rnsTool.fastBConvSkRnsIter(tempBsk, encrypted1Poly.rnsIter[i]);
        }
    }

    /**
     * This lambda function takes as input an IterTuple with three components:
     *
     * <p>1. (Const)RNSIter to read an input polynomial from</p>
     * <p>2. RNSIter for the output in base q</p>
     * <p>3. RNSIter for the output in base Bsk</p>
     * <p>
     * It performs steps (1)-(3) of the BEHZ multiplication (see above) on the given input polynomial (given as an
     * RNSIter or ConstRNSIter) and writes the results in base q and base Bsk to the given output
     * iterators.
     *
     * @param encrypt        ciphertext.
     * @param size           ciphertext size.
     * @param rnsTool        RNS tool.
     * @param baseQNttTables NTT tables for the base Q.
     * @param encryptedQ     allocated space for a base q output of behz_extend_base_convert_to_ntt for ciphertext.
     * @param encryptedBsk   allocate space for a base B_sk output of behz_extend_base_convert_to_ntt for ciphertext.
     */
    private void behzExtendBaseConvertToNtt(Ciphertext encrypt, int size,
                                            RnsTool rnsTool, NttTables[] baseQNttTables,
                                            PolyIterator encryptedQ, PolyIterator encryptedBsk) {
        int baseQSize = rnsTool.baseQ().size();
        int baseBskMTildeSize = rnsTool.baseBskMTilde().size();
        int baseBskSize = rnsTool.baseBsk().size();
        NttTables[] baseBskNttTables = rnsTool.baseBskNttTables();

        int n = encrypt.polyModulusDegree();
        for (int i = 0; i < size; i++) {
            // Make copy of input polynomial (in base q) and convert to NTT form
            PolyCore.setPoly(
                encrypt.data(), encrypt.getPolyOffset(i),
                n, baseQSize,
                encryptedQ.coeff(), i * n * baseQSize
            );
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyRns(encryptedQ.rnsIter[i], baseQSize, baseQNttTables);

            // Allocate temporary space for a polynomial in base {B_sk, {m_tilde}}
            RnsIterator temp = RnsIterator.allocate(n, baseBskMTildeSize);

            // 1) Convert from base q to base {B_sk, {m_tilde}}
            PolyIterator encryptedPoly = PolyIterator.fromCiphertext(encrypt);
            rnsTool.fastBConvMTildeRnsIter(encryptedPoly.rnsIter[i], temp);

            // (2) Reduce q-overflows in with Montgomery reduction, switching base to B_sk
            rnsTool.smMrqRnsIter(temp, encryptedBsk.rnsIter[i]);
            // Transform to NTT form in base B_sk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyRns(encryptedBsk.rnsIter[i], baseBskSize, baseBskNttTables);
        }
    }

    /**
     * This lambda function computes the ciphertext product for BFV multiplication. Since we use the BEHZ
     * approach, the multiplication of individual polynomials is done using a dyadic product where the inputs
     * are already in NTT form. The arguments of the lambda function are expected to be as follows:
     *
     * <p>1. a ConstPolyIter pointing to the beginning of the first input ciphertext (in NTT form)</p>
     * <p>2. a ConstPolyIter pointing to the beginning of the second input ciphertext (in NTT form)</p>
     * <p>3. a ConstModulusIter pointing to an array of Modulus elements for the base</p>
     * <p>4. the size of the base</p>
     * <p>5. a PolyIter pointing to the beginning of the output ciphertext</p>
     */
    private void behzCiphertextProduct(PolyIterator in1, int in1Index, PolyIterator in2, int in2Index, int n,
                                       int steps, AbstractModulus[] base,
                                       PolyIterator destination, int desIndex) {
        int baseSize = base.length;
        // Create a shifted iterator for the first input
        PolyIterator shiftedIn1Iter = in1.subPolyIterator(in1Index, in1.m() - 1);

        RnsIterator shiftedOutIter = destination.rnsIter[desIndex];
        for (int j = 0; j < steps; j++) {
            // Create a shifted reverse iterator for the second input
            // 在这里每一次精确获取 in2 对应的 RnsIter
            RnsIterator shiftedReversedIn2Iter = RnsIterator.wrap(in2.coeff(), in2.ptr() + (in2Index - j) * n * baseSize, n, in2.k());

            for (int k = 0; k < baseSize; k++) {
                CoeffIterator temp = CoeffIterator.allocate(n);
                // c_1 mod q_i, c_2 mod q_i in NTT form, compute c_1 * c_2 mod q_i
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    shiftedIn1Iter.rnsIter[j].coeffIter[k], shiftedReversedIn2Iter.coeffIter[k],
                    n, base[k], temp
                );
                PolyArithmeticSmallMod.addPolyCoeffMod(
                    temp, shiftedOutIter.coeffIter[k], n, base[k], shiftedOutIter.coeffIter[k]
                );
            }
        }
    }

    private void ckks_multiply(Ciphertext encrypted1, final Ciphertext encrypted2) {
        if (!(encrypted1.isNttForm() && encrypted2.isNttForm())) {
            throw new IllegalArgumentException("encrypted1 or encrypted2 must be in NTT form");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted1.parmsId());
        EncryptionParameters parms = context_data.parms();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = parms.coeffModulus().length;
        int encrypted1_size = encrypted1.size();
        int encrypted2_size = encrypted2.size();

        // Determine destination.size()
        // Default is 3 (c_0, c_1, c_2)
        int dest_size = Common.subSafe(Common.addSafe(encrypted1_size, encrypted2_size, false), 1, false);

        // Size check
        if (!Common.productFitsIn(false, dest_size, coeff_count, coeff_modulus_size)) {
            throw new IllegalStateException("invalid parameters");
        }

        // Set up iterator for the base
        // auto coeff_modulus = iter(parms.coeff_modulus());
        Modulus[] coeff_modulus = parms.coeffModulus();

        // Prepare destination
        encrypted1.resize(context, context_data.parmsId(), dest_size);

        // Set up iterators for input ciphertexts
        PolyIterator encrypted1_iter = PolyIterator.dynamicWrap(encrypted1.data(), coeff_count, coeff_modulus_size);
        PolyIterator encrypted2_iter = PolyIterator.dynamicWrap(encrypted2.data(), coeff_count, coeff_modulus_size);

        if (dest_size == 3) {
            // We want to keep six polynomials in the L1 cache: x[0], x[1], x[2], y[0], y[1], temp.
            // For a 32KiB cache, which can store 32768 / 8 = 4096 coefficients, = 682.67 coefficients per polynomial,
            // we should keep the tile size at 682 or below. The tile size must divide coeff_count, i.e. be a power of
            // two. Some testing shows similar performance with tile size 256 and 512, and worse performance on smaller
            // tiles. We pick the smaller of the two to prevent L1 cache misses on processors with < 32 KiB L1 cache.
            int tile_size = Math.min(coeff_count, 256);
            int num_tiles = coeff_count / tile_size;
            if (coeff_count % tile_size != 0) {
                throw new IllegalArgumentException("tile_size does not divide coeff_count");
            }

            // Semantic misuse of RNSIter; each is really pointing to the data for each RNS factor in sequence
            // ConstRNSIter encrypted2_0_iter(*encrypted2_iter[0], tile_size);
            RnsIterator encrypted2_0_iter = RnsIterator.wrap(encrypted2_iter.coeff(), encrypted2_iter.ptr(0), tile_size, coeff_modulus_size * num_tiles);
            // ConstRNSIter encrypted2_1_iter(*encrypted2_iter[1], tile_size);
            RnsIterator encrypted2_1_iter = RnsIterator.wrap(encrypted2_iter.coeff(), encrypted2_iter.ptr(1), tile_size, coeff_modulus_size * num_tiles);
            // RNSIter encrypted1_0_iter(*encrypted1_iter[0], tile_size);
            RnsIterator encrypted1_0_iter = RnsIterator.wrap(encrypted1_iter.coeff(), encrypted1_iter.ptr(0), tile_size, coeff_modulus_size * num_tiles);
            // RNSIter encrypted1_1_iter(*encrypted1_iter[1], tile_size);
            RnsIterator encrypted1_1_iter = RnsIterator.wrap(encrypted1_iter.coeff(), encrypted1_iter.ptr(1), tile_size, coeff_modulus_size * num_tiles);
            // RNSIter encrypted1_2_iter(*encrypted1_iter[2], tile_size);
            RnsIterator encrypted1_2_iter = RnsIterator.wrap(encrypted1_iter.coeff(), encrypted1_iter.ptr(2), tile_size, coeff_modulus_size * num_tiles);

            // Temporary buffer to store intermediate results
            // SEAL_ALLOCATE_GET_COEFF_ITER(temp, tile_size, pool);
            CoeffIterator temp = CoeffIterator.allocate(tile_size);

            // Computes the output tile_size coefficients at a time
            // Given input tuples of polynomials x = (x[0], x[1], x[2]), y = (y[0], y[1]), computes
            // x = (x[0] * y[0], x[0] * y[1] + x[1] * y[0], x[1] * y[1])
            // with appropriate modular reduction
            // SEAL_ITERATE(coeff_modulus, coeff_modulus_size, [&](auto I) {
            int ptr = 0;
            for (int I = 0; I < coeff_modulus_size; I++) {
                // SEAL_ITERATE(iter(size_t(0)), num_tiles, [&](SEAL_MAYBE_UNUSED auto J)
                for (int J = 0; J < num_tiles; J++) {
                    // Compute third output polynomial, overwriting input
                    // x[2] = x[1] * y[1]
                    // dyadic_product_coeffmod(
                    //     encrypted1_1_iter[0], encrypted2_1_iter[0], tile_size, I, encrypted1_2_iter[0]);
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encrypted1_1_iter.coeffIter[ptr], encrypted2_1_iter.coeffIter[ptr],
                        tile_size, coeff_modulus[I], encrypted1_2_iter.coeffIter[ptr]
                    );

                    // Compute second output polynomial, overwriting input
                    // temp = x[1] * y[0]
                    // dyadic_product_coeffmod(encrypted1_1_iter[0], encrypted2_0_iter[0], tile_size, I, temp);
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encrypted1_1_iter.coeffIter[ptr], encrypted2_0_iter.coeffIter[ptr],
                        tile_size, coeff_modulus[I], temp
                    );
                    // x[1] = x[0] * y[1]
                    // dyadic_product_coeffmod(
                    //     encrypted1_0_iter[0], encrypted2_1_iter[0], tile_size, I, encrypted1_1_iter[0]);
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encrypted1_0_iter.coeffIter[ptr], encrypted2_1_iter.coeffIter[ptr],
                        tile_size, coeff_modulus[I], encrypted1_1_iter.coeffIter[ptr]
                    );
                    // x[1] += temp
                    // add_poly_coeffmod(encrypted1_1_iter[0], temp, tile_size, I, encrypted1_1_iter[0]);
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        encrypted1_1_iter.coeffIter[ptr], temp,
                        tile_size, coeff_modulus[I], encrypted1_1_iter.coeffIter[ptr]
                    );

                    // Compute first output polynomial, overwriting input
                    // x[0] = x[0] * y[0]
                    // dyadic_product_coeffmod(
                    //    encrypted1_0_iter[0], encrypted2_0_iter[0], tile_size, I, encrypted1_0_iter[0]);
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encrypted1_0_iter.coeffIter[ptr], encrypted2_0_iter.coeffIter[ptr],
                        tile_size, coeff_modulus[I], encrypted1_0_iter.coeffIter[ptr]
                    );

                    // Manually increment iterators
                    ptr++;
                }
            }
        } else {
            // Allocate temporary space for the result
            // SEAL_ALLOCATE_ZERO_GET_POLY_ITER(temp, dest_size, coeff_count, coeff_modulus_size, pool);
            PolyIterator temp = PolyIterator.allocate(dest_size, coeff_count, coeff_modulus_size);

            // SEAL_ITERATE(iter(size_t(0)), dest_size, [&](auto I) {
            for (int I = 0; I < dest_size; I++) {
                // We iterate over relevant components of encrypted1 and encrypted2 in increasing order for
                // encrypted1 and reversed (decreasing) order for encrypted2. The bounds for the indices of
                // the relevant terms are obtained as follows.
                int curr_encrypted1_last = Math.min(I, encrypted1_size - 1);
                int curr_encrypted2_first = Math.min(I, encrypted2_size - 1);
                int curr_encrypted1_first = I - curr_encrypted2_first;
                // size_t curr_encrypted2_last = secret_power_index - curr_encrypted1_last;

                // The total number of dyadic products is now easy to compute
                int steps = curr_encrypted1_last - curr_encrypted1_first + 1;

                // Create a shifted iterator for the first input
                int shifted_encrypted1_iter = curr_encrypted1_first;

                // Create a shifted reverse iterator for the second input
                int shifted_reversed_encrypted2_iter = curr_encrypted2_first;

                // SEAL_ITERATE(iter(shifted_encrypted1_iter, shifted_reversed_encrypted2_iter), steps, [&](auto J)
                for (int J = 0; J < steps; J++) {
                    // SEAL_ITERATE(iter(J, coeff_modulus, temp[I]), coeff_modulus_size, [&](auto K)
                    for (int K = 0; K < coeff_modulus.length; K++) {
                        // SEAL_ALLOCATE_GET_COEFF_ITER(prod, coeff_count, pool);
                        CoeffIterator prod = CoeffIterator.allocate(coeff_count);
                        // dyadic_product_coeffmod(get<0, 0>(K), get<0, 1>(K), coeff_count, get<1>(K), prod);
                        PolyArithmeticSmallMod.dyadicProductCoeffMod(
                            encrypted1_iter.rnsIter[shifted_encrypted1_iter].coeffIter[K],
                            encrypted2_iter.rnsIter[shifted_reversed_encrypted2_iter].coeffIter[K],
                            coeff_count,
                            coeff_modulus[K],
                            prod
                        );
                        // add_poly_coeffmod(prod, get<2>(K), coeff_count, get<1>(K), get<2>(K));
                        PolyArithmeticSmallMod.addPolyCoeffMod(
                            prod, temp.rnsIter[I].coeffIter[K], coeff_count,
                            coeff_modulus[K], temp.rnsIter[I].coeffIter[K]
                        );
                    }
                    shifted_encrypted1_iter++;
                    shifted_reversed_encrypted2_iter--;
                }
            }

            // Set the final result
            PolyCore.setPolyArray(temp.coeff(), dest_size, coeff_count, coeff_modulus_size, encrypted1.data());
        }

        // Set the scale
        // encrypted1.scale() *= encrypted2.scale();
        encrypted1.setScale(encrypted1.scale() * encrypted2.scale());
        if (!is_scale_within_bounds(encrypted1.scale(), context_data)) {
            throw new IllegalArgumentException("scale out of bounds");
        }
    }

    // TODO: bgv_multiply

    /**
     * Multiplies two ciphertexts. This functions computes the product of encrypted1 and encrypted2 and stores the
     * result in the destination parameter.
     *
     * @param encrypted1  the first ciphertext to multiply.
     * @param encrypted2  the second ciphertext to multiply.
     * @param destination the ciphertext to overwrite with the multiplication result.
     */
    public void multiply(Ciphertext encrypted1, Ciphertext encrypted2, Ciphertext destination) {
        if (encrypted2 == destination) {
            multiplyInplace(destination, encrypted1);
        } else {
            destination.copyFrom(encrypted1);
            multiplyInplace(destination, encrypted2);
        }
    }

    /**
     * Squares a ciphertext. This functions computes the square of encrypted.
     *
     * @param encrypted the ciphertext to square.
     */
    public void squareInplace(Ciphertext encrypted) {
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        ContextData contextData = context.firstContextData();
        switch (contextData.parms().scheme()) {
            case BFV:
                bfv_square(encrypted);
                break;
            case CKKS:
                ckks_square(encrypted);
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    private void bfv_square(Ciphertext encrypted) {
        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        int coeff_count = parms.polyModulusDegree();
        int base_q_size = parms.coeffModulus().length;
        int encrypted_size = encrypted.size();
        long plain_modulus = parms.plainModulus().value();

        RnsTool rns_tool = context_data.rnsTool();
        int base_Bsk_size = rns_tool.baseBsk().size();
        int base_Bsk_m_tilde_size = rns_tool.baseBskMTilde().size();

        // Optimization implemented currently only for size 2 ciphertexts
        if (encrypted_size != 2) {
            bfv_multiply(encrypted, encrypted);
            return;
        }

        // Determine destination.size(), which should be c1.size() * c2.size() - 1
        int dest_size = Common.subSafe(Common.addSafe(encrypted_size, encrypted_size, false), 1, false);

        // Size check
        if (!Common.productFitsIn(false, dest_size, coeff_count, base_Bsk_m_tilde_size)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        Modulus[] base_q = parms.coeffModulus();
        AbstractModulus[] base_Bsk = rns_tool.baseBsk().getBase();
        NttTables[] base_q_ntt_tables = context_data.smallNttTables();
        NttTables[] base_Bsk_ntt_tables = rns_tool.baseBskNttTables();

        // Microsoft SEAL uses BEHZ-style RNS multiplication. For details, see Evaluator::bfv_multiply. This function
        // uses additionally Karatsuba multiplication to reduce the complexity of squaring a size-2 ciphertext, but the
        // steps are otherwise the same as in Evaluator::bfv_multiply.

        // Resize encrypted1 to destination size
        encrypted.resize(context, context_data.parmsId(), dest_size);

        // This lambda function takes as input an IterTuple with three components:
        //
        // 1. (Const)RNSIter to read an input polynomial from
        // 2. RNSIter for the output in base q
        // 3. RNSIter for the output in base Bsk
        //
        // It performs steps (1)-(3) of the BEHZ multiplication (see above) on the given input polynomial (given as an
        // RNSIter or ConstRNSIter) and writes the results in base q and base Bsk to the given output
        // iterators.
        // Here we do not use a separate function since this lambda function is invoked only once

        // Allocate space for a base q output of behz_extend_base_convert_to_ntt for encrypted1
        PolyIterator encrypted_q = PolyIterator.allocate(encrypted_size, coeff_count, base_q_size);

        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        PolyIterator encrypted_Bsk = PolyIterator.allocate(encrypted_size, coeff_count, base_Bsk_size);

        PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);

        // Perform BEHZ steps (1)-(3) for encrypted1
        for (int i = 0; i < encrypted_size; i++) {
            // Make copy of input polynomial (in base q) and convert to NTT form
            PolyCore.setPoly(
                encrypted.data(), i * coeff_count * encrypted.getCoeffModulusSize(),
                coeff_count, base_q_size,
                encrypted_q.coeff(), i * coeff_count * base_q_size
            );
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyRns(encrypted_q.rnsIter[i], base_q_size, base_q_ntt_tables);

            // Allocate temporary space for a polynomial in base {B_sk, {m_tilde}}
            RnsIterator temp = RnsIterator.allocate(coeff_count, base_Bsk_m_tilde_size);

            // 1) Convert from base q to base Bsk U {m_tilde}
            rns_tool.fastBConvMTildeRnsIter(encrypted_iter.rnsIter[i], temp);

            // (2) Reduce q-overflows in with Montgomery reduction, switching base to B_sk
            rns_tool.smMrqRnsIter(temp, encrypted_Bsk.rnsIter[i]);

            // Transform to NTT form in base B_sk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyRns(encrypted_Bsk.rnsIter[i], base_Bsk_size, base_Bsk_ntt_tables);
        }

        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        PolyIterator temp_dest_q = PolyIterator.allocate(dest_size, coeff_count, base_q_size);
        PolyIterator temp_dest_Bsk = PolyIterator.allocate(dest_size, coeff_count, base_Bsk_size);

        // Perform BEHZ step (4): dyadic multiplication on arbitrary size ciphertexts
        // behz_ciphertext_square(encrypted_q, base_q, base_q_size, temp_dest_q);
        behzCiphertextSquare(encrypted_q, base_q, temp_dest_q);
        // behz_ciphertext_square(encrypted_Bsk, base_Bsk, base_Bsk_size, temp_dest_Bsk);
        behzCiphertextSquare(encrypted_Bsk, base_Bsk, temp_dest_Bsk);

        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        NttTool.inverseNttNegacyclicHarveyLazyPoly(temp_dest_q, dest_size, base_q_ntt_tables);
        NttTool.inverseNttNegacyclicHarveyLazyPoly(temp_dest_Bsk, dest_size, base_Bsk_ntt_tables);

        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < dest_size; i++) {
            // Bring together the base q and base Bsk components into a single allocation
            RnsIterator temp_q_Bsk = RnsIterator.allocate(coeff_count, base_q_size + base_Bsk_size);

            // Step (6): multiply base q components by t (plain_modulus)
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                temp_dest_q.rnsIter[i], base_q_size,
                plain_modulus, base_q,
                temp_q_Bsk
            );

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                temp_dest_Bsk.rnsIter[i], base_Bsk_size,
                plain_modulus, base_Bsk,
                temp_q_Bsk.subRnsIterator(base_q_size)
            );

            // Allocate yet another temporary for fast divide-and-floor result in base Bsk
            RnsIterator tempBsk = RnsIterator.allocate(coeff_count, base_Bsk_size);

            // Step (7): divide by q and floor, producing a result in base Bsk
            rns_tool.fastFloorRnsIter(temp_q_Bsk, tempBsk);

            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rns_tool.fastBConvSkRnsIter(tempBsk, encrypted_iter.rnsIter[i]);
        }
    }

    /**
     * This lambda function computes the size-2 ciphertext square for BFV multiplication. Since we use the BEHZ
     * approach, the multiplication of individual polynomials is done using a dyadic product where the inputs
     * are already in NTT form. The arguments of the lambda function are expected to be as follows:
     *
     * <p>1. a ConstPolyIter pointing to the beginning of the input ciphertext (in NTT form)</p>
     * <p>3. a ConstModulusIter pointing to an array of Modulus elements for the base</p>
     * <p>4. the size of the base</p>
     * <p>5. a PolyIter pointing to the beginning of the output ciphertext</p>
     */
    private void behzCiphertextSquare(PolyIterator in, AbstractModulus[] baseQ, PolyIterator destination) {
        int baseQSize = baseQ.length;

        // compute c0^2
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            in.rnsIter[0], in.rnsIter[0], baseQSize, baseQ, destination.rnsIter[0]
        );

        // compute 2 * c0 * c1
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            in.rnsIter[0], in.rnsIter[1], baseQSize, baseQ, destination.rnsIter[1]
        );
        PolyArithmeticSmallMod.addPolyCoeffMod(
            destination.rnsIter[1], destination.rnsIter[1], baseQSize, baseQ, destination.rnsIter[1]
        );

        // compute c1^2
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            in.rnsIter[1], in.rnsIter[1], baseQSize, baseQ, destination.rnsIter[2]
        );

    }

    private void ckks_square(Ciphertext encrypted) {
        if (!encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted must be in NTT form");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = parms.coeffModulus().length;
        int encrypted_size = encrypted.size();

        // Optimization implemented currently only for size 2 ciphertexts
        if (encrypted_size != 2) {
            ckks_multiply(encrypted, encrypted);
            return;
        }

        // Determine destination.size()
        // Default is 3 (c_0, c_1, c_2)
        int dest_size = Common.subSafe(Common.addSafe(encrypted_size, encrypted_size, false), 1, false);

        // Size check
        if (!Common.productFitsIn(false, dest_size, coeff_count, coeff_modulus_size)) {
            throw new IllegalStateException("invalid parameters");
        }

        // Set up iterator for the base
        Modulus[] coeff_modulus = parms.coeffModulus();

        // Prepare destination
        encrypted.resize(context, context_data.parmsId(), dest_size);

        // Set up iterators for input ciphertext
        PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);

        // Compute c1^2
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            encrypted_iter.rnsIter[1], encrypted_iter.rnsIter[1], coeff_modulus_size,
            coeff_modulus, encrypted_iter.rnsIter[2]
        );

        // Compute 2*c0*c1
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            encrypted_iter.rnsIter[0], encrypted_iter.rnsIter[1], coeff_modulus_size,
            coeff_modulus, encrypted_iter.rnsIter[1]
        );
        PolyArithmeticSmallMod.addPolyCoeffMod(
            encrypted_iter.rnsIter[1], encrypted_iter.rnsIter[1], coeff_modulus_size,
            coeff_modulus, encrypted_iter.rnsIter[1]
        );

        // Compute c0^2
        PolyArithmeticSmallMod.dyadicProductCoeffMod(
            encrypted_iter.rnsIter[0], encrypted_iter.rnsIter[0], coeff_modulus_size,
            coeff_modulus, encrypted_iter.rnsIter[0]);

        // Set the scale
        encrypted.setScale(encrypted.scale() * encrypted.scale());
        if (!is_scale_within_bounds(encrypted.scale(), context_data)) {
            throw new IllegalArgumentException("scale out of bounds");
        }
    }

    // TODO: bgv_square

    /**
     * Squares a ciphertext. This functions computes the square of encrypted and stores the result in the destination
     * parameter.
     *
     * @param encrypted   the ciphertext to square.
     * @param destination the ciphertext to overwrite with the square.
     */
    public void square(Ciphertext encrypted, Ciphertext destination) {
        destination.copyFrom(encrypted);
        squareInplace(destination);
    }

    /**
     * Relinearizes a ciphertext. This functions relinearizes encrypted, reducing its size down to 2. If the size of
     * encrypted is K+1, the given relinearization keys need to have size at least K-1.
     *
     * @param encrypted the ciphertext to relinearize.
     * @param relinKeys the relinearization keys.
     */
    public void relinearizeInplace(Ciphertext encrypted, RelinKeys relinKeys) {
        relinearize_internal(encrypted, relinKeys, 2);
    }

    /**
     * Relinearizes a ciphertext. This functions relinearizes encrypted, reducing its size down to 2, and stores the
     * result in the destination parameter. If the size of encrypted is K+1, the given relinearization keys need to
     * have size at least K-1.
     *
     * @param encrypted   the ciphertext to relinearize.
     * @param relinKeys   the relinearization keys.
     * @param destination the ciphertext to overwrite with the relinearized result.
     */
    public void relinearize(Ciphertext encrypted, RelinKeys relinKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        relinearize_internal(destination, relinKeys, 2);
    }

    private void relinearize_internal(Ciphertext encrypted, RelinKeys relinKeys, int destination_size) {
        // Verify parameters.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        if (context_data == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!relinKeys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("relin_keys is not valid for encryption parameters");
        }

        int encrypted_size = encrypted.size();

        // Verify parameters.
        if (destination_size < 2 || destination_size > encrypted_size) {
            throw new IllegalArgumentException("destination_size must be at least 2 and less than or equal to current count");
        }
        if (relinKeys.size() < Common.subSafe(encrypted_size, 2, false)) {
            throw new IllegalArgumentException("not enough relinearization keys");
        }

        // If encrypted is already at the desired level, return
        if (destination_size == encrypted_size) {
            return;
        }

        // Calculate number of relinearize_one_step calls needed
        int relins_needed = encrypted_size - destination_size;

        // Iterator pointing to the last component of encrypted
        // auto encrypted_iter = iter(encrypted);
        // encrypted_iter += encrypted_size - 1;
        int encrypted_iter_pos = (encrypted_size - 1) * encrypted.polyModulusDegree() * encrypted.getCoeffModulusSize();
        RnsIterator encrypted_iter = RnsIterator.wrap(
            encrypted.data(), encrypted_iter_pos, encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
        );

        // iter(size_t(0)), relins_needed, [&](auto I)
        for (int i = 0; i < relins_needed; i++) {
            switch_key_inplace(encrypted, encrypted_iter, relinKeys, RelinKeys.getIndex(encrypted_size - 1 - i));
        }

        // Put the output of final relinearization into destination.
        // Prepare destination only at this point because we are resizing down
        encrypted.resize(context, context_data.parmsId(), destination_size);

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down to q_1...q_{k-1} and
     * stores the result in the destination parameter.
     *
     * @param encrypted   the ciphertext to be switched to a smaller modulus.
     * @param destination the ciphertext to overwrite with the modulus switched result.
     */
    public void modSwitchToNext(Ciphertext encrypted, Ciphertext destination) {
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (context.lastParmsId().equals(encrypted.parmsId())) {
            throw new IllegalArgumentException("end of modulus switching chain reached ");
        }

        switch (context.firstContextData().parms().scheme()) {
            case BFV:
                // Modulus switching with scaling
                mod_switch_scale_to_next(encrypted, destination);
                break;
            case CKKS:
                // Modulus switching without scaling
                mod_switch_drop_to_next(encrypted, destination);
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (destination.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    private void mod_switch_scale_to_next(Ciphertext encrypted, Ciphertext destination) {
        // Assuming at this point encrypted is already validated.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        if (context_data.parms().scheme().equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (context_data.parms().scheme().equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (context_data.parms().scheme().equals(SchemeType.BGV) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted must be in NTT form");
        }

        // Extract encryption parameters.
        ContextData next_context_data = context_data.nextContextData();
        EncryptionParameters next_parms = next_context_data.parms();
        RnsTool rns_tool = context_data.rnsTool();

        int encrypted_size = encrypted.size();
        int coeff_count = next_parms.polyModulusDegree();
        int next_coeff_modulus_size = next_parms.coeffModulus().length;

        Ciphertext encrypted_copy = new Ciphertext();
        encrypted_copy.copyFrom(encrypted);
        PolyIterator encrypted_copy_iter = PolyIterator.fromCiphertext(encrypted_copy);

        switch (next_parms.scheme()) {
            case BFV:
                for (int i = 0; i < encrypted_size; i++) {
                    rns_tool.divideAndRoundQLastInplace(encrypted_copy_iter.rnsIter[i]);
                }
                break;
            case CKKS:
                for (int i = 0; i < encrypted_size; i++) {
                    rns_tool.divideAndRoundQLastNttInplace(encrypted_copy_iter.rnsIter[i], context_data.smallNttTables());
                }
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Copy result to destination
        destination.resize(context, next_context_data.parmsId(), encrypted_size);
        // SEAL_ITERATE(iter(encrypted_copy, destination), encrypted_size, [&](auto I)
        for (int i = 0; i < encrypted_size; i++) {
            // set_poly(get<0>(I), coeff_count, next_coeff_modulus_size, get<1>(I));
            PolyCore.setPoly(encrypted_copy.data(), encrypted_copy.getPolyOffset(i),
                coeff_count, next_coeff_modulus_size, destination.data(), destination.getPolyOffset(i)
            );
        }

        // Set other attributes
        destination.setNttForm(encrypted.isNttForm());

        if (next_parms.scheme().equals(SchemeType.CKKS)) {
            // Change the scale when using CKKS
            Modulus[] coeffModulus = context_data.parms().coeffModulus();
            destination.setScale(encrypted.scale() / (double) coeffModulus[coeffModulus.length - 1].value());
        } else if (next_parms.scheme().equals(SchemeType.BGV)) {
            // Change the correction factor when using BGV
            destination.setCorrectionFactor(UintArithmeticSmallMod.multiplyUintMod(
                encrypted.correctionFactor(), rns_tool.invQLastModT(), next_parms.plainModulus()
            ));
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down to q_1...q_{k-1}.
     *
     * @param encrypted the ciphertext to be switched to a smaller modulus.
     */
    public void modSwitchToNextInplace(Ciphertext encrypted) {
        modSwitchToNext(encrypted, encrypted);
    }

    /**
     * Modulus switches an NTT transformed plaintext from modulo q_1...q_k down to modulo q_1...q_{k-1}.
     *
     * @param plain the plaintext to be switched to a smaller modulus.
     */
    public void modSwitchToNextInplace(Plaintext plain) {
        // Verify parameters.
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        mod_switch_drop_to_next(plain);
    }

    private void mod_switch_drop_to_next(final Ciphertext encrypted, Ciphertext destination) {
        // Assuming at this point encrypted is already validated.
        ContextData context_data_ptr = context.getContextData(encrypted.parmsId());
        if (context_data_ptr.parms().scheme().equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }

        // Extract encryption parameters.
        ContextData next_context_data = context_data_ptr.nextContextData();
        EncryptionParameters next_parms = next_context_data.parms();

        if (!is_scale_within_bounds(encrypted.scale(), next_context_data)) {
            throw new IllegalArgumentException("scale out of bounds");
        }

        // q_1,...,q_{k-1}
        int next_coeff_modulus_size = next_parms.coeffModulus().length;
        int coeff_count = next_parms.polyModulusDegree();
        int encrypted_size = encrypted.size();

        // Size check
        if (!Common.productFitsIn(false, encrypted_size, coeff_count, next_coeff_modulus_size)) {
            throw new IllegalStateException("invalid parameters");
        }

        if (encrypted == destination) {
            // Switching in-place so need temporary space
            // SEAL_ALLOCATE_GET_POLY_ITER(temp, encrypted_size, coeff_count, next_coeff_modulus_size, pool);
            PolyIterator temp = PolyIterator.allocate(encrypted_size, coeff_count, next_coeff_modulus_size);

            // Copy data over to temp; only copy the RNS components relevant after modulus drop
            PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);
            drop_modulus_and_copy(encrypted_iter, temp, encrypted_size, next_coeff_modulus_size, coeff_count);

            // Resize destination before writing
            destination.resize(context, next_context_data.parmsId(), encrypted_size);

            // Copy data to destination
            PolyCore.setPolyArray(temp.coeff(), encrypted_size, coeff_count, next_coeff_modulus_size, destination.data());
            // TODO: avoid copying and temporary space allocation
        } else {
            // Resize destination before writing
            destination.resize(context, next_context_data.parmsId(), encrypted_size);

            // Copy data over to destination; only copy the RNS components relevant after modulus drop
            drop_modulus_and_copy(
                PolyIterator.fromCiphertext(encrypted), PolyIterator.fromCiphertext(destination),
                encrypted_size, next_coeff_modulus_size, coeff_count
            );
        }
        destination.setNttForm(true);
        destination.setScale(encrypted.scale());
        destination.setCorrectionFactor(encrypted.correctionFactor());
    }

    private void drop_modulus_and_copy(PolyIterator in_iter, PolyIterator out_iter,
                                       int encrypted_size, int next_coeff_modulus_size, int coeff_count) {
        // SEAL_ITERATE(iter(in_iter, out_iter), encrypted_size, [&](auto I)
        for (int i = 0; i < encrypted_size; i++) {
            // SEAL_ITERATE(iter(I), next_coeff_modulus_size, [&](auto J)
            for (int j = 0; j < next_coeff_modulus_size; j++) {
                CoeffIterator get_0_J = in_iter.rnsIter[i].coeffIter[j];
                CoeffIterator get_1_J = out_iter.rnsIter[i].coeffIter[j];
                UintCore.setUint(get_0_J.coeff(), get_0_J.pos(), coeff_count, get_1_J.coeff(), get_1_J.pos(), coeff_count);
            }
        }
    }

    private void mod_switch_drop_to_next(Plaintext plain) {
        // Assuming at this point plain is already validated.
        ContextData contextData = context.getContextData(plain.parmsId());
        if (!plain.isNttForm()) {
            throw new IllegalArgumentException("plain is not in NTT form");
        }
        if (contextData.nextContextData() == null) {
            throw new IllegalArgumentException("end of modulus switching chain reached");
        }

        // Extract encryption parameters.
        ContextData nextContextData = contextData.nextContextData();
        EncryptionParameters nextParms = nextContextData.parms();

        if (!is_scale_within_bounds(plain.scale(), nextContextData)) {
            throw new IllegalArgumentException("scale out of bounds");
        }

        // q_1,...,q_{k-1}
        Modulus[] nextCoeffModulus = nextParms.coeffModulus();
        int nextCoeffModulusSize = nextCoeffModulus.length;
        int coeffCount = nextParms.polyModulusDegree();

        // Compute destination size first for exception safety
        int destSize = Common.mulSafe(nextCoeffModulusSize, coeffCount, false);

        plain.setParmsId(ParmsId.parmsIdZero());
        plain.resize(destSize);
        plain.setParmsId(nextContextData.parmsId());
    }

    /**
     * Modulus switches an NTT transformed plaintext from modulo q_1...q_k down to modulo q_1...q_{k-1} and stores the
     * result in the destination parameter.
     *
     * @param plain       the plaintext to be switched to a smaller modulus.
     * @param destination the plaintext to overwrite with the modulus switched result.
     */
    public void modSwitchToNext(Plaintext plain, Plaintext destination) {
        destination.copyFrom(plain);
        modSwitchToNextInplace(destination);
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down until the parameters
     * reach the given parms_id.
     *
     * @param encrypted     the ciphertext to be switched to a smaller modulus.
     * @param targetParmsId the target parms_id.
     */
    public void modSwitchToInplace(Ciphertext encrypted, ParmsId targetParmsId) {
        ContextData contextData = context.getContextData(encrypted.parmsId());
        ContextData targetContextData = context.getContextData(targetParmsId);

        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (targetContextData == null) {
            throw new IllegalArgumentException("targetParmsId is not valid for encryption parameters");
        }
        // 只能从 模数多的 往 模数少的 转换
        // keyContext 的模数最多（输入参数的全部模数）, 位于 chain 顶端(chainIndex 最大)，firstContextData 其次
        if (contextData.chainIndex() < targetContextData.chainIndex()) {
            throw new IllegalArgumentException("cannot switch to higher level modulus");
        }

        // 一直往后切换，直到达到目标 参数
        while (!encrypted.parmsId().equals(targetParmsId)) {
            modSwitchToNextInplace(encrypted);
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down until the parameters
     * reach the given parms_id and stores the result in the destination parameter.
     *
     * @param encrypted     the ciphertext to be switched to a smaller modulus.
     * @param targetParmsId the target parms_id.
     * @param destination   the ciphertext to overwrite with the modulus switched result.
     */
    public void modSwitchTo(Ciphertext encrypted, ParmsId targetParmsId, Ciphertext destination) {
        destination.copyFrom(encrypted);
        modSwitchToInplace(destination, targetParmsId);
    }

    /**
     * Given an NTT transformed plaintext modulo q_1...q_k, this function switches the modulus down until the
     * parameters reach the given parms_id.
     *
     * @param plain   the plaintext to be switched to a smaller modulus.
     * @param parmsId the target parms_id.
     */
    public void modSwitchToInplace(Plaintext plain, ParmsId parmsId) {
        // Verify parameters.
        ContextData contextData = context.getContextData(plain.parmsId());
        ContextData targetContextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        if (targetContextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        if (!plain.isNttForm()) {
            throw new IllegalArgumentException("plain is not in NTT form");
        }
        if (contextData.chainIndex() < targetContextData.chainIndex()) {
            throw new IllegalArgumentException("cannot switch to higher level modulus");
        }

        while (!plain.parmsId().equals(parmsId)) {
            modSwitchToNextInplace(plain);
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down to q_1...q_{k-1}, scales
     * the message down accordingly, and stores the result in the destination parameter. Dynamic memory allocations in
     * the process are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to be switched to a smaller modulus.
     * @param destination The ciphertext to overwrite with the modulus switched result.
     */
    public void rescaleToNext(Ciphertext encrypted, Ciphertext destination) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (context.lastParmsId().equals(encrypted.parmsId())) {
            throw new IllegalArgumentException("end of modulus switching chain reached");
        }

        switch (context.firstContextData().parms().scheme()) {
            case BFV:
                /* Fall through */
            case BGV:
                throw new IllegalArgumentException("unsupported operation for scheme type");
            case CKKS:
                // Modulus switching with scaling
                mod_switch_scale_to_next(encrypted, destination);
                break;
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }
        // Transparent ciphertext output is not allowed.
        if (destination.isTransparent()) {
            throw new IllegalStateException("result ciphertext is transparent");
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down to q_1...q_{k-1} and
     * scales the message down accordingly. Dynamic memory allocations in the process are allocated from the memory
     * pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted The ciphertext to be switched to a smaller modulus.
     */
    public void rescaleToNextInplace(Ciphertext encrypted) {
        rescaleToNext(encrypted, encrypted);
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down until the parameters
     * reach the given parms_id and scales the message down accordingly. Dynamic memory allocations in the process are
     * allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted The ciphertext to be switched to a smaller modulus.
     * @param parmsId   The target parms_id.
     */
    public void rescaleToInplace(Ciphertext encrypted, ParmsId parmsId) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        ContextData context_data_ptr = context.getContextData(encrypted.parmsId());
        ContextData target_context_data_ptr = context.getContextData(parmsId);
        if (context_data_ptr == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (target_context_data_ptr == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        if (context_data_ptr.chainIndex() < target_context_data_ptr.chainIndex()) {
            throw new IllegalArgumentException("cannot switch to higher level modulus");
        }

        switch (context_data_ptr.parms().scheme()) {
            case BFV:
                /* Fall through */
            case BGV:
                throw new IllegalArgumentException("unsupported operation for scheme type");
            case CKKS:
                while (!encrypted.parmsId().equals(parmsId)) {
                    // Modulus switching with scaling
                    mod_switch_scale_to_next(encrypted, encrypted);
                }
                break;
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new IllegalStateException("result ciphertext is transparent");
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function switches the modulus down until the parameters
     * reach the given parms_id, scales the message down accordingly, and stores the result in the destination
     * parameter. Dynamic memory allocations in the process are allocated from the memory pool pointed to by the given
     * MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to be switched to a smaller modulus.
     * @param parmsId     The target parms_id.
     * @param destination The ciphertext to overwrite with the modulus switched result.
     */
    public void rescaleTo(final Ciphertext encrypted, ParmsId parmsId, Ciphertext destination) {
        // destination = encrypted;
        destination.copyFrom(encrypted);
        rescaleToInplace(destination, parmsId);
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function reduces the modulus down to q_1...q_{k-1}. Dynamic
     * memory allocations in the process are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted The ciphertext to be reduced to a smaller modulus
     */
    public void mod_reduce_to_next_inplace(Ciphertext encrypted) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // ContextData context_data_ptr = context.getContextData(encrypted.parmsId());
        if (context.lastParmsId().equals(encrypted.parmsId())) {
            throw new IllegalArgumentException("end of modulus switching chain reached");
        }

        mod_switch_drop_to_next(encrypted, encrypted);
        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new IllegalStateException("result ciphertext is transparent");
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function reduces the modulus down to q_1...q_{k-1} and
     * stores the result in the destination parameter. Dynamic memory allocations in the process are allocated from the
     * memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to be reduced to a smaller modulus.
     * @param destination The ciphertext to overwrite with the modular reduced result.
     */
    public void mod_reduce_to_next(final Ciphertext encrypted, Ciphertext destination) {
        destination.copyFrom(encrypted);
        mod_reduce_to_next_inplace(destination);
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function reduces the modulus down until the parameters
     * reach the given parms_id. Dynamic memory allocations in the process are allocated from the memory pool pointed
     * to by the given MemoryPoolHandle.
     *
     * @param encrypted The ciphertext to be reduced to a smaller modulus.
     * @param parms_id  The target parms_id.
     */
    public void mod_reduce_to_inplace(Ciphertext encrypted, ParmsId parms_id) {
        // Verify parameters.
        ContextData context_data_ptr = context.getContextData(encrypted.parmsId());
        ContextData target_context_data_ptr = context.getContextData(parms_id);
        if (context_data_ptr == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (target_context_data_ptr == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        if (context_data_ptr.chainIndex() < target_context_data_ptr.chainIndex()) {
            throw new IllegalArgumentException("cannot switch to higher level modulus");
        }

        while (!encrypted.parmsId().equals(parms_id)) {
            mod_reduce_to_next_inplace(encrypted);
        }
    }

    /**
     * Given a ciphertext encrypted modulo q_1...q_k, this function reduces the modulus down until the parameters
     * reach the given parms_id and stores the result in the destination parameter. Dynamic memory allocations in the
     * process are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to be reduced to a smaller modulus
     * @param parms_id    The target parms_id
     * @param destination The ciphertext to overwrite with the modulus reduced result
     */
    public void mod_reduce_to(final Ciphertext encrypted, ParmsId parms_id, Ciphertext destination) {
        destination.copyFrom(encrypted);
        mod_reduce_to_inplace(destination, parms_id);
    }

    /**
     * Multiplies several ciphertexts together. This function computes the product of several ciphertext given as an
     * std::vector and stores the result in the destination parameter. The multiplication is done in a depth-optimal
     * order, and relinearization is performed automatically after every multiplication in the process. In
     * relinearization the given relinearization keys are used.
     *
     * @param encrypteds  the ciphertexts to multiply.
     * @param relinKeys   the relinearization keys.
     * @param destination the ciphertext to overwrite with the multiplication result.
     */
    public void multiplyMany(Ciphertext[] encrypteds, RelinKeys relinKeys, Ciphertext destination) {
        // Verify parameters.
        if (encrypteds.length == 0) {
            throw new IllegalArgumentException("encrypteds vector must not be empty");
        }

        for (Ciphertext encrypted : encrypteds) {
            if (encrypted == destination) {
                throw new IllegalArgumentException("encrypteds must be different from destination");
            }
        }

        // There is at least one ciphertext
        ContextData contextData = context.getContextData(encrypteds[0].parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypteds is not valid for encryption parameters");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = contextData.parms();
        if (parms.scheme() != SchemeType.BFV && parms.scheme() != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        // If there is only one ciphertext, return it.
        if (encrypteds.length == 1) {
            destination.copyFrom(encrypteds[0]);
            return;
        }

        // Do first level of multiplications
        ArrayList<Ciphertext> productVector = new ArrayList<>(encrypteds.length);
        for (int i = 0; i < encrypteds.length - 1; i += 2) {
            Ciphertext temp = new Ciphertext(context, contextData.parmsId());
            if (encrypteds[i].dynArray() == encrypteds[i + 1].dynArray()) {
                square(encrypteds[i], temp);
            } else {
                multiply(encrypteds[i], encrypteds[i + 1], temp);
            }
            relinearizeInplace(temp, relinKeys);
            productVector.add(temp);
        }
        if ((encrypteds.length & 1) == 1) {
            productVector.add(encrypteds[encrypteds.length - 1]);
        }

        // Repeatedly multiply and add to the back of the vector until the end is reached
        for (int i = 0; i < productVector.size() - 1; i += 2) {
            Ciphertext temp = new Ciphertext(context, contextData.parmsId());
            multiply(productVector.get(i), productVector.get(i + 1), temp);
            relinearizeInplace(temp, relinKeys);
            productVector.add(temp);
        }

        destination.copyFrom(productVector.get(productVector.size() - 1));
    }

    /**
     * Exponentiates a ciphertext. This functions raises encrypted to a power. The exponentiation is done in a
     * depth-optimal order, and relinearization is performed automatically after every multiplication in the
     * process. In relinearization the given relinearization keys are used.
     *
     * @param encrypted the ciphertext to exponentiate.
     * @param exponent  the power to raise the ciphertext to.
     * @param relinKeys the relinearization keys.
     */
    public void exponentiateInplace(Ciphertext encrypted, long exponent, RelinKeys relinKeys) {
        // Verify parameters.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (context.getContextData(relinKeys.parmsId()) == null) {
            throw new IllegalArgumentException("relin_keys is not valid for encryption parameters");
        }
        if (exponent == 0) {
            throw new IllegalArgumentException("exponent cannot be 0");
        }

        // Fast case
        if (exponent == 1) {
            return;
        }

        // Create a vector of copies of encrypted
        Ciphertext encryptedClone = new Ciphertext();
        encryptedClone.copyFrom(encrypted);
        Ciphertext[] expVector = new Ciphertext[(int) exponent];
        for (int i = 0; i < exponent; i++) {
            expVector[i] = encryptedClone;
        }
        multiplyMany(expVector, relinKeys, encrypted);
    }

    /**
     * Exponentiates a ciphertext. This functions raises encrypted to a power and stores the result in the destination
     * parameter. The exponentiation is done in a depth-optimal order, and relinearization is performed automatically
     * after every multiplication in the process. In relinearization the given relinearization keys are used.
     *
     * @param encrypted   the ciphertext to exponentiate.
     * @param exponent    the power to raise the ciphertext to.
     * @param relinKeys   the relinearization keys.
     * @param destination the ciphertext to overwrite with the power.
     */
    public void exponentiate(Ciphertext encrypted, long exponent, RelinKeys relinKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        exponentiateInplace(destination, exponent, relinKeys);
    }

    /**
     * Adds a ciphertext and a plaintext.
     *
     * @param encrypted the ciphertext to add.
     * @param plain     the plaintext to add.
     */
    public void addPlainInplace(Ciphertext encrypted, Plaintext plain) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!ValCheck.isMetaDataValidFor(plain, context) || !ValCheck.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        if (parms.scheme().equals(SchemeType.BFV)) {
            if (encrypted.isNttForm()) {
                throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
            }
            if (plain.isNttForm()) {
                throw new IllegalArgumentException("BFV plain cannot be in NTT form");
            }
        } else if (parms.scheme().equals(SchemeType.CKKS)) {
            if (!encrypted.isNttForm()) {
                throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
            }
            if (!plain.isNttForm()) {
                throw new IllegalArgumentException("CKKS plain must be in NTT form");
            }
            if (!encrypted.parmsId().equals(plain.parmsId())) {
                throw new IllegalArgumentException("encrypted and plain parameter mismatch");
            }
            if (!are_same_scale(encrypted, plain)) {
                throw new IllegalArgumentException("scale mismatch");
            }
        } else if (parms.scheme().equals(SchemeType.BGV)) {
            if (!encrypted.isNttForm()) {
                throw new IllegalArgumentException("BGV encrypted must be in NTT form");
            }
            if (plain.isNttForm()) {
                throw new IllegalArgumentException("BGV plain cannot be in NTT form");
            }
        }

        // Extract encryption parameters.
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeff_count, coeff_modulus_size)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        switch (parms.scheme()) {
            case BFV:
                RnsIterator c0 = RnsIterator.wrap(encrypted.data(), encrypted.getPolyOffset(0), coeff_count, encrypted.getCoeffModulusSize());
                ScalingVariant.multiplyAddPlainWithScalingVariant(plain, contextData, c0);
                break;
            case CKKS:
                RnsIterator encrypted_iter = RnsIterator.wrap(encrypted.data(), coeff_count, coeff_modulus_size);
                RnsIterator plain_iter = RnsIterator.wrap(plain.data(), coeff_count, coeff_modulus_size);
                PolyArithmeticSmallMod.addPolyCoeffMod(
                    encrypted_iter, plain_iter, coeff_modulus_size, coeff_modulus, encrypted_iter
                );
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Adds a ciphertext and a plaintext. This function adds a ciphertext and a plaintext and stores the result in the
     * destination parameter. Note that in many cases it can be much more efficient to perform any computations on raw
     * unencrypted data before encoding it, rather than using this function to compute on the plaintext objects.
     *
     * @param encrypted   the ciphertext to add.
     * @param plain       the plaintext to add.
     * @param destination the ciphertext to overwrite with the addition result.
     */
    public void addPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {
        destination.copyFrom(encrypted);
        addPlainInplace(destination, plain);
    }

    /**
     * Subtracts a plaintext from a ciphertext.
     *
     * @param encrypted the ciphertext to subtract from.
     * @param plain     the plaintext to subtract.
     */
    public void subPlainInplace(Ciphertext encrypted, Plaintext plain) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        if (!ValCheck.isMetaDataValidFor(plain, context) || !ValCheck.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        if (parms.scheme().equals(SchemeType.BFV)) {
            if (encrypted.isNttForm()) {
                throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
            }
            if (plain.isNttForm()) {
                throw new IllegalArgumentException("BFV plain cannot be in NTT form");
            }
        } else if (parms.scheme().equals(SchemeType.CKKS)) {
            if (!encrypted.isNttForm()) {
                throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
            }
            if (!plain.isNttForm()) {
                throw new IllegalArgumentException("CKKS plain must be in NTT form");
            }
            if (!encrypted.parmsId().equals(plain.parmsId())) {
                throw new IllegalArgumentException("encrypted and plain parameter mismatch");
            }
            if (!are_same_scale(encrypted, plain)) {
                throw new IllegalArgumentException("scale mismatch");
            }
        } else if (parms.scheme().equals(SchemeType.BGV)) {
            if (!encrypted.isNttForm()) {
                throw new IllegalArgumentException("BGV encrypted must be in NTT form");
            }
            if (plain.isNttForm()) {
                throw new IllegalArgumentException("BGV plain cannot be in NTT form");
            }
        }

        // Extract encryption parameters.
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeff_count, coeff_modulus_size)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        switch (parms.scheme()) {
            case BFV:
                RnsIterator c0 = RnsIterator.wrap(encrypted.data(), encrypted.getPolyOffset(0), coeff_count, encrypted.getCoeffModulusSize());
                ScalingVariant.multiplySubPlainWithScalingVariant(plain, contextData, c0);
                break;
            case CKKS:
                RnsIterator encrypted_iter = RnsIterator.wrap(encrypted.data(), coeff_count, coeff_modulus_size);
                RnsIterator plain_iter = RnsIterator.wrap(plain.data(), coeff_count, coeff_modulus_size);
                PolyArithmeticSmallMod.subPolyCoeffMod(
                    encrypted_iter, plain_iter, coeff_modulus_size, coeff_modulus, encrypted_iter
                );
                break;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Subtracts a plaintext from a ciphertext. This function subtracts a plaintext from a ciphertext and stores the
     * result in the destination parameter.
     *
     * @param encrypted   the ciphertext to subtract from.
     * @param plain       the plaintext to subtract.
     * @param destination the ciphertext to overwrite with the subtraction result.
     */
    public void subPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {
        destination.copyFrom(encrypted);
        subPlainInplace(destination, plain);
    }

    /**
     * Multiplies a ciphertext with a plaintext. The plaintext cannot be identically 0.
     *
     * @param encrypted the ciphertext to multiply.
     * @param plain     the plaintext to multiply.
     */
    public void multiplyPlainInplace(Ciphertext encrypted, Plaintext plain) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!ValCheck.isMetaDataValidFor(plain, context) || !ValCheck.isBufferValid(plain)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }

        if (encrypted.isNttForm() && plain.isNttForm()) {
            multiply_plain_ntt(encrypted, plain);
        } else if (!encrypted.isNttForm() && !plain.isNttForm()) {
            multiply_plain_normal(encrypted, plain);
        } else if (encrypted.isNttForm() && !plain.isNttForm()) {
            Plaintext plain_copy = new Plaintext();
            plain_copy.copyFrom(plain);
            transformToNttInplace(plain_copy, encrypted.parmsId());
            multiply_plain_ntt(encrypted, plain_copy);
        } else {
            transformToNttInplace(encrypted);
            multiply_plain_ntt(encrypted, plain);
            transformFromNttInplace(encrypted);
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    private void multiply_plain_normal(Ciphertext encrypted, Plaintext plain) {
        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;

        long plain_upper_half_threshold = context_data.plainUpperHalfThreshold();
        long[] plain_upper_half_increment = context_data.plainUpperHalfIncrement();
        NttTables[] ntt_tables = context_data.smallNttTables();

        int encrypted_size = encrypted.size();
        int plain_coeff_count = plain.coeffCount();
        int plain_nonzero_coeff_count = plain.nonZeroCoeffCount();

        // Size check
        if (!Common.productFitsIn(false, encrypted_size, coeff_count, coeff_modulus_size)) {
            throw new IllegalArgumentException("invalid parameter");
        }

        /*
         * Optimizations for constant / monomial multiplication can lead to the presence of a timing side-channel in
         * use-cases where the plaintext data should also be kept private.
         */
        PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);
        if (plain_nonzero_coeff_count == 1) {
            // Multiplying by a monomial?
            int mono_exponent = plain.significantCoeffCount() - 1;
            if (plain.at(mono_exponent) >= plain_upper_half_threshold) {
                if (!context_data.qualifiers().isUsingFastPlainLift()) {
                    // Allocate temporary space for a single RNS coefficient
                    CoeffIterator temp = CoeffIterator.allocate(coeff_modulus_size);

                    // We need to adjust the monomial modulo each coeff_modulus prime separately when the coeff_modulus
                    // primes may be larger than the plain_modulus. We add plain_upper_half_increment (i.e., q-t) to
                    // the monomial to ensure it is smaller than coeff_modulus and then do an RNS multiplication. Note
                    // that in this case plain_upper_half_increment contains a multi-precision integer, so after the
                    // addition we decompose the multi-precision integer into RNS components, and then multiply.
                    UintArithmetic.addUint(
                        plain_upper_half_increment, coeff_modulus_size, plain.at(mono_exponent), temp.coeff()
                    );
                    context_data.rnsTool().baseQ().decompose(temp.coeff());
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                        encrypted_iter, encrypted_size, temp, mono_exponent, coeff_modulus, encrypted_iter
                    );
                } else {
                    // Every coeff_modulus prime is larger than plain_modulus, so there is no need to adjust the
                    // monomial. Instead, just do an RNS multiplication.
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                        encrypted_iter, encrypted_size, plain.at(mono_exponent), mono_exponent, coeff_modulus, encrypted_iter
                    );
                }
            } else {
                // The monomial represents a positive number, so no RNS multiplication is needed.
                PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                    encrypted_iter, encrypted_size, plain.at(mono_exponent), mono_exponent, coeff_modulus, encrypted_iter
                );
            }

            // Set the scale
            if (parms.scheme().equals(SchemeType.CKKS)) {
                encrypted.setScale(encrypted.scale() * plain.scale());
                if (!is_scale_within_bounds(encrypted.scale(), context_data)) {
                    throw new IllegalArgumentException("scale out of bounds");
                }
            }

            return;
        }

        // Generic case: any plaintext polynomial
        // Allocate temporary space for an entire RNS polynomial
        long[] temp = new long[coeff_count * coeff_modulus_size];

        if (!context_data.qualifiers().isUsingFastPlainLift()) {
            StrideIterator temp_iter = StrideIterator.wrap(temp, coeff_modulus_size);

            // SEAL_ITERATE(iter(plain.data(), temp_iter), plain_coeff_count, [&](auto I)
            for (int I = 0; I < plain_coeff_count; I++, temp_iter.next()) {
                long plain_value = plain.at(I);
                if (plain_value >= plain_upper_half_threshold) {
                    UintArithmetic.addUint(
                        plain_upper_half_increment, 0, coeff_modulus_size, plain_value, temp_iter.coeff(), temp_iter.ptr()
                    );
                } else {
                    temp_iter.setCoeff(plain_value);
                }
            }
            context_data.rnsTool().baseQ().decomposeArray(temp, coeff_count);
        } else {
            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.
            RnsIterator temp_ter = RnsIterator.wrap(temp, coeff_count, coeff_modulus_size);
            // SEAL_ITERATE(iter(temp_iter, plain_upper_half_increment), coeff_modulus_size, [&](auto I)
            for (int I = 0; I < coeff_modulus_size; I++) {
                // SEAL_ITERATE(iter(get<0>(I), plain.data()), plain_coeff_count, [&](auto J)
                for (int J = 0; J < plain_coeff_count; J++) {
                    long coefficient = plain.at(J) >= plain_upper_half_threshold ?
                        plain.at(J) + plain_upper_half_increment[I] : plain.at(J);
                    temp_ter.coeffIter[I].setCoeff(J, coefficient);
                }
            }
        }

        // Need to multiply each component in encrypted with temp; first step is to transform to NTT form
        RnsIterator temp_iter = RnsIterator.wrap(temp, coeff_count, coeff_modulus_size);
        NttTool.nttNegacyclicHarveyRns(temp_iter, coeff_modulus_size, ntt_tables);

        for (int I = 0; I < encrypted_size; I++) {
            for (int J = 0; J < coeff_modulus_size; J++) {
                // Lazy Reduction
                NttTool.nttNegacyclicHarveyLazy(encrypted_iter.rnsIter[I].coeffIter[J], ntt_tables[J]);
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    encrypted_iter.rnsIter[I].coeffIter[J],
                    temp_iter.coeffIter[J],
                    coeff_count, coeff_modulus[J],
                    encrypted_iter.rnsIter[I].coeffIter[J]
                );
                NttTool.inverseNttNegacyclicHarvey(encrypted_iter.rnsIter[I].coeffIter[J], ntt_tables[J]);
            }
        }

        // Set the scale
        if (parms.scheme().equals(SchemeType.CKKS)) {
            encrypted.setScale(encrypted.scale() * plain.scale());
            if (!is_scale_within_bounds(encrypted.scale(), context_data)) {
                throw new IllegalArgumentException("scale out of bounds");
            }
        }
    }

    private void multiply_plain_ntt(Ciphertext encrypted_ntt, Plaintext plain_ntt) {
        // Verify parameters.
        if (!plain_ntt.isNttForm()) {
            throw new IllegalArgumentException("plainNtt is not in NTT form");
        }
        if (!encrypted_ntt.parmsId().equals(plain_ntt.parmsId())) {
            throw new IllegalArgumentException("encrypted_ntt and plain_ntt parameter mismatch");
        }

        // Extract encryption parameters.
        ContextData context_data = context.getContextData(encrypted_ntt.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;
        int encrypted_ntt_size = encrypted_ntt.size();

        // Size check
        if (!Common.productFitsIn(false, encrypted_ntt_size, coeff_count, coeff_modulus_size)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        RnsIterator plain_ntt_iter = RnsIterator.wrap(plain_ntt.data(), coeff_count, coeff_modulus_size);
        PolyIterator encrypted_ntt_iter = PolyIterator.fromCiphertext(encrypted_ntt);
        // iter(encrypted_ntt), encrypted_ntt_size, [&](auto I)
        for (int i = 0; i < encrypted_ntt_size; i++) {
            PolyArithmeticSmallMod.dyadicProductCoeffMod(
                encrypted_ntt_iter.rnsIter[i], plain_ntt_iter, coeff_modulus_size,
                coeff_modulus, encrypted_ntt_iter.rnsIter[i]
            );
        }
        // Set the scale
        encrypted_ntt.setScale(encrypted_ntt.scale() * plain_ntt.scale());
        if (!is_scale_within_bounds(encrypted_ntt.scale(), context_data)) {
            throw new IllegalArgumentException("scale out of bounds");
        }
    }

    /**
     * Multiplies a ciphertext with a plaintext. This function multiplies a ciphertext with a plaintext and stores the
     * result in the destination parameter. The plaintext cannot be identically 0.
     *
     * @param encrypted   the ciphertext to multiply.
     * @param plain       the plaintext to multiply.
     * @param destination the ciphertext to overwrite with the multiplication result.
     */
    public void multiplyPlain(Ciphertext encrypted, Plaintext plain, Ciphertext destination) {
        destination.copyFrom(encrypted);
        multiplyPlainInplace(destination, plain);
    }

    /**
     * Transforms a plaintext to NTT domain. This functions applies the Number Theoretic Transform to a plaintext by
     * first embedding integers modulo the plaintext modulus to integers modulo the coefficient modulus and then
     * performing David Harvey's NTT on the resulting polynomial. The transformation is done with respect to encryption
     * parameters corresponding to a given parms_id. For the operation to be valid, the plaintext must have degree less
     * than poly_modulus_degree and each coefficient must be less than the plaintext modulus, i.e., the plaintext must
     * be a valid plaintext under the current encryption parameters.
     *
     * @param plain   the plaintext to transform.
     * @param parmsId the parms_id with respect to which the NTT is done.
     */
    public void transformToNttInplace(Plaintext plain, ParmsId parmsId) {
        // Verify parameters.
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        ContextData context_data = context.getContextData(parmsId);
        if (context_data == null) {
            throw new IllegalArgumentException("parms_id is not valid for the current context");
        }
        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain is already in NTT form");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;
        int plain_coeff_count = plain.coeffCount();

        long plain_upper_half_threshold = context_data.plainUpperHalfThreshold();
        long[] plain_upper_half_increment = context_data.plainUpperHalfIncrement();

        NttTables[] ntt_tables = context_data.smallNttTables();

        // Size check
        if (!Common.productFitsIn(false, coeff_count, coeff_modulus_size)) {
            throw new RuntimeException("invalid parameters");
        }
        // Resize to fit the entire NTT transformed (ciphertext size) polynomial
        // Note that the new coefficients are automatically set to 0
        plain.resize(coeff_count * coeff_modulus_size);

        if (!context_data.qualifiers().isUsingFastPlainLift()) {
            // Allocate temporary space for an entire RNS polynomial
            // Slight semantic misuse of RNSIter here, but this works well
            RnsIterator temp = RnsIterator.allocate(coeff_modulus_size, coeff_count);

            // iter(plain.data(), temp), plain_coeff_count, [&](auto I)
            for (int i = 0; i < plain_coeff_count; i++) {
                long plainValue = plain.at(i);
                if (plainValue >= plain_upper_half_threshold) {
                    UintArithmetic.addUint(plain_upper_half_increment, 0, coeff_modulus_size, plainValue, temp.coeff(), i * coeff_modulus_size);
                } else {
                    temp.coeffIter[i].setCoeff(0, plainValue);
                }
            }
            context_data.rnsTool().baseQ().decomposeArray(temp.coeff(), coeff_count);

            // Copy data back to plain
            System.arraycopy(temp.coeff(), 0, plain.data(), 0, coeff_count * coeff_modulus_size);
        } else {
            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.

            // Create a "reversed" helper iterator that iterates in the reverse order both plain RNS components and
            // the plain_upper_half_increment values.
            // auto helper_iter = reverse_iter(plain_iter, plain_upper_half_increment);
            // advance(helper_iter, -safe_cast<ptrdiff_t>(coeff_modulus_size - 1));
            // SEAL_ITERATE(helper_iter, coeff_modulus_size, [&](auto I)
            for (int i = coeff_modulus_size - 1; i >= 0; i--) {
                int startIndex = i * coeff_count;
                // iter(*plain_iter, get<0>(I)), plain_coeff_count, [&](auto J)
                for (int j = 0; j < plain_coeff_count; j++) {
                    plain.data()[startIndex + j] = plain.data()[j] >= plain_upper_half_threshold
                        ? plain.data()[j] + plain_upper_half_increment[i] : plain.data()[j];
                }
            }
        }
        RnsIterator plain_iter = RnsIterator.wrap(plain.data(), coeff_count, coeff_modulus_size);

        // Transform to NTT domain
        NttTool.nttNegacyclicHarveyRns(plain_iter, coeff_modulus_size, ntt_tables);

        plain.setParmsId(parmsId);
    }

    /**
     * Transforms a plaintext to NTT domain. This functions applies the Number Theoretic Transform to a plaintext by
     * first embedding integers modulo the plaintext modulus to integers modulo the coefficient modulus and then
     * performing David Harvey's NTT on the resulting polynomial. The transformation is done with respect to encryption
     * parameters corresponding to a given parms_id. The result is stored in the destination_ntt parameter. For the
     * operation to be valid, the plaintext must have degree less than poly_modulus_degree and each coefficient must be
     * less than the plaintext modulus, i.e., the plaintext must be a valid plaintext under the current encryption
     * parameters.
     *
     * @param plain          the plaintext to transform.
     * @param parmsId        the parms_id with respect to which the NTT is done.
     * @param destinationNtt the plaintext to overwrite with the transformed result.
     */
    public void transformToNtt(Plaintext plain, ParmsId parmsId, Plaintext destinationNtt) {
        destinationNtt.copyFrom(plain);
        transformToNttInplace(destinationNtt, parmsId);
    }

    /**
     * Transforms a ciphertext to NTT domain. This functions applies David Harvey's Number Theoretic Transform
     * separately to each polynomial of a ciphertext.
     *
     * @param encrypted the ciphertext to transform.
     */
    public void transformToNttInplace(Ciphertext encrypted) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted is already in NTT form");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedSize = encrypted.size();

        NttTables[] nttTables = contextData.smallNttTables();

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Transform each polynomial to NTT domain
        PolyIterator encryptedPoly = PolyIterator.fromCiphertext(encrypted);
        NttTool.nttNegacyclicHarveyPoly(encryptedPoly, encryptedSize, nttTables);

        // Finally change the is_ntt_transformed flag
        encrypted.setNttForm(true);

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Transforms a ciphertext to NTT domain. This functions applies David Harvey's Number Theoretic Transform
     * separately to each polynomial of a ciphertext. The result is stored in the destination_ntt parameter.
     *
     * @param encrypted      the ciphertext to transform.
     * @param destinationNtt the ciphertext to overwrite with the transformed result.
     */
    public void transformToNtt(Ciphertext encrypted, Ciphertext destinationNtt) {
        destinationNtt.copyFrom(encrypted);
        transformToNttInplace(destinationNtt);
    }

    /**
     * Transforms a ciphertext back from NTT domain. This functions applies the inverse of David Harvey's Number
     * Theoretic Transform separately to each polynomial of a ciphertext.
     *
     * @param encryptedNtt the ciphertext to transform.
     */
    public void transformFromNttInplace(Ciphertext encryptedNtt) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encryptedNtt, context) || !ValCheck.isBufferValid(encryptedNtt)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        ContextData contextData = context.getContextData(encryptedNtt.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!encryptedNtt.isNttForm()) {
            throw new IllegalArgumentException("encrypted is not in NTT form");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedSize = encryptedNtt.size();

        NttTables[] nttTables = contextData.smallNttTables();

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Transform each polynomial to NTT domain
        PolyIterator encryptedNttPoly = PolyIterator.fromCiphertext(encryptedNtt);
        NttTool.inverseNttNegacyclicHarveyPoly(encryptedNttPoly, encryptedSize, nttTables);

        // Finally change the is_ntt_transformed flag
        encryptedNtt.setNttForm(false);

        // Transparent ciphertext output is not allowed
        if (encryptedNtt.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    /**
     * Transforms a ciphertext back from NTT domain. This functions applies the inverse of David Harvey's Number
     * Theoretic Transform separately to each polynomial of a ciphertext. The result is stored in the destination
     * parameter.
     *
     * @param encryptedNtt the ciphertext to transform.
     * @param destination  the ciphertext to overwrite with the transformed result.
     */
    public void transformFromNtt(Ciphertext encryptedNtt, Ciphertext destination) {
        destination.copyFrom(encryptedNtt);
        transformFromNttInplace(destination);
    }

    /**
     * Applies a Galois automorphism to a ciphertext. To evaluate the Galois automorphism, an appropriate set of Galois
     * keys must also be provided.
     * <p></p>
     * The desired Galois automorphism is given as a Galois element, and must be an odd integer in the interval
     * [1, M-1], where M = 2*N, and N = poly_modulus_degree. Used with batching, a Galois element 3^i % M corresponds
     * to a cyclic row rotation i steps to the left, and a Galois element 3^(N/2-i) % M corresponds to a cyclic row
     * rotation i steps to the right. The Galois element M-1 corresponds to a column rotation (row swap) in BFV/BGV,
     * and complex conjugation in CKKS. In the polynomial view (not batching), a Galois automorphism by a Galois
     * element p changes Enc(plain(x)) to Enc(plain(x^p)).
     *
     * @param encrypted  the ciphertext to apply the Galois automorphism to.
     * @param galoisElt  the Galois element.
     * @param galoisKeys the Galois keys.
     */
    public void applyGaloisInplace(Ciphertext encrypted, int galoisElt, GaloisKeys galoisKeys) {
        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // Don't validate all of galois_keys but just check the parms_id.
        if (!galoisKeys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("galois_keys is not valid for encryption parameters");
        }

        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;
        int encrypted_size = encrypted.size();
        // Use key_context_data where permutation tables exist since previous runs.
        GaloisTool galois_tool = context.keyContextData().galoisTool();

        // size check
        if (!Common.productFitsIn(false, coeff_modulus_size, coeff_count)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Check if Galois key is generated or not.
        if (!galoisKeys.hasKey(galoisElt)) {
            throw new IllegalArgumentException("Galois key not present");
        }

        long m = Common.mulSafe(coeff_count, 2L, false);

        // Verify parameters
        if (((galoisElt & 1) == 0) || Common.unsignedGeq(galoisElt, m)) {
            throw new IllegalArgumentException("Galois element is not valid");
        }
        if (encrypted_size > 2) {
            throw new IllegalArgumentException("encrypted size must be 2");
        }

        // SEAL_ALLOCATE_GET_RNS_ITER(temp, coeff_count, coeff_modulus_size, pool);
        RnsIterator temp = RnsIterator.allocate(coeff_count, coeff_modulus_size);

        // DO NOT CHANGE EXECUTION ORDER OF FOLLOWING SECTION
        // BEGIN: Apply Galois for each ciphertext
        // Execution order is sensitive, since apply_galois is not inplace!
        if (parms.scheme().equals(SchemeType.BFV)) {
            // !!! DO NOT CHANGE EXECUTION ORDER!!!

            // First transform encrypted.data(0)
            PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);
            galois_tool.applyGalois(encrypted_iter.rnsIter[0], coeff_modulus_size, galoisElt, coeff_modulus, temp);
            // Copy result to encrypted.data(0)
            PolyCore.setPoly(temp.coeff(), coeff_count, coeff_modulus_size, encrypted_iter.coeff());

            // Next transform encrypted.data(1)
            galois_tool.applyGalois(encrypted_iter.rnsIter[1], coeff_modulus_size, galoisElt, coeff_modulus, temp);
        } else if (parms.scheme().equals(SchemeType.CKKS) || parms.scheme().equals(SchemeType.BGV)) {
            // !!! DO NOT CHANGE EXECUTION ORDER!!!

            // First transform encrypted.data(0)
            PolyIterator encrypted_iter = PolyIterator.fromCiphertext(encrypted);
            galois_tool.applyGaloisNtt(encrypted_iter.rnsIter[0], coeff_modulus_size, galoisElt, temp);

            // Copy result to encrypted.data(0)
            PolyCore.setPoly(temp.coeff(), coeff_count, coeff_modulus_size, encrypted.data());

            // Next transform encrypted.data(1)
            galois_tool.applyGaloisNtt(encrypted_iter.rnsIter[1], coeff_modulus_size, galoisElt, temp);
        } else {
            throw new IllegalArgumentException("scheme not implemented");
        }

        // Wipe encrypted.data(1)
        PolyCore.setZeroPoly(coeff_count, coeff_modulus_size, encrypted.data(), encrypted.getPolyOffset(1));

        // END: Apply Galois for each ciphertext
        // REORDERING IS SAFE NOW

        // Calculate (temp * galois_key[0], temp * galois_key[1]) + (ct[0], 0)
        switch_key_inplace(encrypted, temp, galoisKeys, GaloisKeys.getIndex(galoisElt));

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    /**
     * Applies a Galois automorphism to a ciphertext and writes the result to the destination parameter. To evaluate
     * the Galois automorphism, an appropriate set of Galois keys must also be provided.
     * <p></p>
     * The desired Galois automorphism is given as a Galois element, and must be an odd integer in the interval
     * [1, M-1], where M = 2*N, and N = poly_modulus_degree. Used with batching, a Galois element 3^i % M corresponds
     * to a cyclic row rotation i steps to the left, and a Galois element 3^(N/2-i) % M corresponds to a cyclic row
     * rotation i steps to the right. The Galois element M-1 corresponds to a column rotation (row swap) in BFV/BGV,
     * and complex conjugation in CKKS. In the polynomial view (not batching), a Galois automorphism by a Galois
     * element p changes Enc(plain(x)) to Enc(plain(x^p)).
     *
     * @param encrypted   the ciphertext to apply the Galois automorphism to.
     * @param galoisElt   the Galois element.
     * @param galoisKeys  the Galois keys.
     * @param destination the ciphertext to overwrite with the result.
     */
    public void applyGalois(Ciphertext encrypted, int galoisElt, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        applyGaloisInplace(destination, galoisElt, galoisKeys);
    }

    /**
     * Rotates plaintext matrix rows cyclically. When batching is used with the BFV/BGV scheme, this function rotates
     * the encrypted plaintext matrix rows cyclically to the left (steps > 0) or to the right (steps < 0). Since the
     * size of the batched matrix is 2-by-(N/2), where N is the degree of the polynomial modulus, the number of steps
     * to rotate must have absolute value at most N/2-1.
     *
     * @param encrypted  the ciphertext to rotate.
     * @param steps      the number of steps to rotate (positive left, negative right).
     * @param galoisKeys the Galois keys.
     */
    public void rotateRowsInplace(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {
        SchemeType scheme = context.keyContextData().parms().scheme();
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        rotateInternal(encrypted, steps, galoisKeys);
    }

    /**
     * Rotates plaintext matrix rows cyclically. When batching is used with the BFV/BGV scheme, this function rotates
     * the encrypted plaintext matrix rows cyclically to the left (steps > 0) or to the right (steps < 0) and writes
     * the result to the destination parameter. Since the size of the batched matrix is 2-by-(N/2), where N is the
     * degree of the polynomial modulus, the number of steps to rotate must have absolute value at most N/2-1.
     *
     * @param encrypted   the ciphertext to rotate.
     * @param steps       the number of steps to rotate (positive left, negative right).
     * @param galoisKeys  the Galois keys.
     * @param destination the ciphertext to overwrite with the rotated result.
     */
    public void rotateRows(Ciphertext encrypted, int steps, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateRowsInplace(destination, steps, galoisKeys);
    }

    private void rotateInternal(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {
        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!contextData.qualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }
        if (!galoisKeys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("galois_keys is not valid for encryption parameters");
        }

        // Is there anything to do?
        if (steps == 0) {
            return;
        }

        int coeffCount = contextData.parms().polyModulusDegree();
        GaloisTool galoisTool = contextData.galoisTool();

        // Check if Galois key is generated or not.
        if (galoisKeys.hasKey(galoisTool.getEltFromStep(steps))) {
            // Perform rotation and key switching
            applyGaloisInplace(encrypted, galoisTool.getEltFromStep(steps), galoisKeys);
        } else {
            // Convert the steps to NAF: guarantees using smallest HW
            TIntArrayList nafSteps = Numth.naf(steps);

            // If naf_steps contains only one element, then this is a power-of-two
            // rotation and we would have expected not to get to this part of the
            // if-statement.
            if (nafSteps.size() == 1) {
                throw new IllegalArgumentException("Galois key not present");
            }

            for (int i = 0; i < nafSteps.size(); i++) {
                int step = nafSteps.get(i);
                if (Math.abs(step) != (coeffCount >> 1)) {
                    // We might have a NAF-term of size coeff_count / 2; this corresponds
                    // to no rotation so we skip it. Otherwise call rotate_internal.
                    rotateInternal(encrypted, step, galoisKeys);
                }
            }
        }
    }

    /**
     * Rotates plaintext matrix columns cyclically. When batching is used with the BFV scheme, this function rotates
     * the encrypted plaintext matrix columns cyclically. Since the size of the batched matrix is 2-by-(N/2), where N
     * is the degree of the polynomial modulus, this means simply swapping the two rows.
     *
     * @param encrypted  the ciphertext to rotate.
     * @param galoisKeys the Galois keys.
     */
    public void rotateColumnsInplace(Ciphertext encrypted, GaloisKeys galoisKeys) {
        SchemeType scheme = context.keyContextData().parms().scheme();
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        conjugateInternal(encrypted, galoisKeys);
    }

    /**
     * Rotates plaintext matrix columns cyclically. When batching is used with the BFV/BGV scheme, this function
     * rotates the encrypted plaintext matrix columns cyclically, and writes the result to the destination parameter.
     * Since the size of the batched matrix is 2-by-(N/2), where N is the degree of the polynomial modulus, this means
     * simply swapping the two rows.
     *
     * @param encrypted   the ciphertext to rotate.
     * @param galoisKeys  the Galois keys.
     * @param destination the ciphertext to overwrite with the rotated result.
     */
    public void rotateColumns(Ciphertext encrypted, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateColumnsInplace(destination, galoisKeys);
    }

    /**
     * Rotates plaintext vector cyclically. When using the CKKS scheme, this function rotates the encrypted plaintext
     * vector cyclically to the left (steps > 0) or to the right (steps < 0) and writes the result to the destination
     * parameter. Since the size of the batched matrix is 2-by-(N/2), where N is the degree of the polynomial modulus,
     * the number of steps to rotate must have absolute value at most N/2-1.
     *
     * @param encrypted  the ciphertext to rotate.
     * @param steps      the number of steps to rotate (positive left, negative right).
     * @param galoisKeys the Galois keys.
     */
    public void rotateVectorInplace(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {
        if (!context.keyContextData().parms().scheme().equals(SchemeType.CKKS)) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        rotateInternal(encrypted, steps, galoisKeys);
    }

    /**
     * Rotates plaintext vector cyclically. When using the CKKS scheme, this function rotates the encrypted plaintext
     * vector cyclically to the left (steps > 0) or to the right (steps < 0) and writes the result to the destination
     * parameter. Since the size of the batched matrix is 2-by-(N/2), where N is the degree of the polynomial modulus,
     * the number of steps to rotate must have absolute value at most N/2-1.
     *
     * @param encrypted   the ciphertext to rotate.
     * @param steps       the number of steps to rotate (positive left, negative right).
     * @param galoisKeys  the Galois keys.
     * @param destination the ciphertext to overwrite with the rotated result.
     */
    public void rotateVector(Ciphertext encrypted, int steps, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateVectorInplace(destination, steps, galoisKeys);
    }

    /**
     * Complex conjugates plaintext slot values. When using the CKKS scheme, this function complex conjugates all
     * values in the underlying plaintext. Dynamic memory allocations in the process are allocated from the memory pool
     * pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to rotate
     * @param galois_keys The Galois keys
     */
    public void complexConjugateInplace(Ciphertext encrypted, final GaloisKeys galois_keys) {
        if (!context.keyContextData().parms().scheme().equals(SchemeType.CKKS)) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        conjugateInternal(encrypted, galois_keys);
    }

    /**
     * Complex conjugates plaintext slot values. When using the CKKS scheme, this function complex conjugates all
     * values in the underlying plaintext, and writes the result to the destination parameter. Dynamic memory
     * allocations in the process are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param encrypted   The ciphertext to rotate
     * @param galois_keys The Galois keys
     * @param destination The ciphertext to overwrite with the rotated result
     */
    public void complexConjugate(final Ciphertext encrypted, final GaloisKeys galois_keys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        complexConjugateInplace(destination, galois_keys);
    }

    private void conjugateInternal(Ciphertext encrypted, GaloisKeys galoisKeys) {
        // Verify parameters.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // Extract encryption parameters.
        if (!contextData.qualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }

        GaloisTool galoisTool = contextData.galoisTool();

        // Perform rotation and key switching
        applyGaloisInplace(encrypted, galoisTool.getEltFromStep(0), galoisKeys);
    }

    private void switch_key_inplace(Ciphertext encrypted, RnsIterator target_iter, KswitchKeys kswitch_keys,
                                    int kswitch_keys_index) {
        ParmsId parms_id = encrypted.parmsId();
        ContextData context_data = context.getContextData(parms_id);
        EncryptionParameters parms = context_data.parms();
        ContextData key_context_data = context.keyContextData();
        EncryptionParameters key_parms = key_context_data.parms();
        SchemeType scheme = parms.scheme();

        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (target_iter == null) {
            throw new IllegalArgumentException("target_iter cannot be null");
        }
        if (!context.usingKeySwitching()) {
            throw new IllegalArgumentException("keyswitching is not supported by the context");
        }

        // Don't validate all of kswitch_keys but just check the parms_id.
        if (!kswitch_keys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("parameter mismatch");
        }

        if (kswitch_keys_index >= kswitch_keys.data().length) {
            throw new ArrayIndexOutOfBoundsException("keySwitchKeysIndex");
        }
        if (scheme.equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (scheme.equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (scheme.equals(SchemeType.BGV) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted must be in NTT form");
        }

        // Extract encryption parameters.
        int coeff_count = parms.polyModulusDegree();
        int decomp_modulus_size = parms.coeffModulus().length;
        Modulus[] key_modulus = key_parms.coeffModulus();
        int key_modulus_size = key_modulus.length;
        int rns_modulus_size = decomp_modulus_size + 1;
        NttTables[] key_ntt_tables = key_context_data.smallNttTables();
        MultiplyUintModOperand[] modswitch_factors = key_context_data.rnsTool().getInvQLastModQ();

        assert target_iter.n() == coeff_count;
        assert target_iter.k() == decomp_modulus_size;

        // Size check
        if (!Common.productFitsIn(false, coeff_count, rns_modulus_size, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Prepare input
        PublicKey[] key_vector = kswitch_keys.data()[kswitch_keys_index];
        int key_component_count = key_vector[0].data().size();

        // Check only the used component in KSwitchKeys.
        for (PublicKey each_key : key_vector) {
            if (!ValCheck.isMetaDataValidFor(each_key, context) || !ValCheck.isBufferValid(each_key)) {
                throw new IllegalArgumentException("kswitch_keys is not valid for encryption parameters");
            }
        }

        // Create a copy of target_iter
        // SEAL_ALLOCATE_GET_RNS_ITER(t_target, coeff_count, decomp_modulus_size, pool)
        RnsIterator t_target = RnsIterator.allocate(coeff_count, decomp_modulus_size);
        UintCore.setUint(
            target_iter.coeff(), target_iter.ptr(), decomp_modulus_size * coeff_count,
            t_target.coeff(), 0, decomp_modulus_size * coeff_count
        );

        // In CKKS or BGV, t_target is in NTT form; switch back to normal form
        if (scheme.equals(SchemeType.CKKS) || scheme.equals(SchemeType.BGV)) {
            NttTool.inverseNttNegacyclicHarveyRns(t_target, decomp_modulus_size, key_ntt_tables);
        }

        // Temporary result
        PolyIterator t_poly_prod = PolyIterator.allocate(key_component_count, coeff_count, rns_modulus_size);

        // SEAL_ITERATE(iter(size_t(0)), rns_modulus_size, [&](auto I)
        for (int I = 0; I < rns_modulus_size; I++) {
            int key_index = (I == decomp_modulus_size ? key_modulus_size - 1 : I);
            // Product of two numbers is up to 60 + 60 = 120 bits, so we can sum up to 256 of them without reduction.
            int lazy_reduction_summand_bound = Constants.MULTIPLY_ACCUMULATE_USER_MOD_MAX;
            int lazy_reduction_counter = lazy_reduction_summand_bound;

            // Allocate memory for a lazy accumulator (128-bit coefficients)
            // auto t_poly_lazy(allocate_zero_poly_array(key_component_count, coeff_count, 2, pool));
            long[] t_poly_lazy = new long[key_component_count * coeff_count * 2];

            // Semantic misuse of PolyIter; this is really pointing to the data for a single RNS factor

            // Multiply with keys and perform lazy reduction on product's coefficients
            // SEAL_ITERATE(iter(size_t(0)), decomp_modulus_size, [&](auto J)
            for (int J = 0; J < decomp_modulus_size; J++) {
                // SEAL_ALLOCATE_GET_COEFF_ITER(t_ntt, coeff_count, pool);
                CoeffIterator t_ntt = CoeffIterator.allocate(coeff_count);
                CoeffIterator t_operand;

                // RNS-NTT form exists in input
                //noinspection DuplicateExpressions
                if (((scheme.equals(SchemeType.CKKS)) || scheme.equals(SchemeType.BGV)) && (I == J)) {
                    t_operand = target_iter.coeffIter[J];
                } else {
                    // Perform RNS-NTT conversion
                    if (key_modulus[J].value() <= key_modulus[key_index].value()) {
                        // No need to perform RNS conversion (modular reduction)
                        UintCore.setUint(
                            t_target.coeff(), J * coeff_count, coeff_count,
                            t_ntt.coeff(), 0, coeff_count
                        );
                    } else {
                        // Perform RNS conversion (modular reduction)
                        PolyArithmeticSmallMod.moduloPolyCoeff(
                            t_target.coeffIter[J], coeff_count, key_modulus[key_index], t_ntt
                        );
                    }
                    // NTT conversion lazy outputs in [0, 4q)
                    NttTool.nttNegacyclicHarveyLazy(t_ntt, key_ntt_tables[key_index]);
                    t_operand = t_ntt;
                }

                // Multiply with keys and modular accumulate products in a lazy fashion
                // SEAL_ITERATE(iter(key_vector[J].data(), accumulator_iter), key_component_count, [&](auto K)
                Ciphertext key_vector_J = key_vector[J].data();
                for (int K = 0; K < key_component_count; K++) {
                    int key_vector_J_K = K * key_vector_J.getCoeffModulusSize() * key_vector_J.polyModulusDegree();
                    int tPolyLazyK = K * coeff_count * 2;
                    //noinspection ConstantValue
                    if (lazy_reduction_counter == 0) {
                        // SEAL_ITERATE(iter(t_operand, get<0>(K)[key_index], get<1>(K)), coeff_count, [&](auto L)
                        for (int L = 0; L < coeff_count; L++) {
                            // unsigned long long qword[2]{ 0, 0 };
                            long[] qword = new long[2];
                            // multiply_uint64(get<0>(L), get<1>(L), qword);
                            UintArithmetic.multiplyUint64(
                                t_operand.getCoeff(L),
                                key_vector_J.data()[key_vector_J_K + key_index * key_vector_J.polyModulusDegree() + L],
                                qword
                            );

                            // Accumulate product of t_operand and t_key_acc to t_poly_lazy and reduce
                            // add_uint128(qword, get<2>(L).ptr(), qword);
                            long[] uint128_temp = new long[]{
                                t_poly_lazy[tPolyLazyK + L * 2], t_poly_lazy[tPolyLazyK + L * 2 + 1],
                            };
                            UintArithmetic.addUint128(qword, uint128_temp, qword);
                            // get<2>(L)[0] = barrett_reduce_128(qword, key_modulus[key_index]);
                            t_poly_lazy[tPolyLazyK + L * 2] = UintArithmeticSmallMod.barrettReduce128(qword, key_modulus[key_index]);
                            // get<2>(L)[1] = 0;
                            t_poly_lazy[tPolyLazyK + L * 2 + 1] = 0;
                        }
                    } else {
                        // Same as above but no reduction
                        // SEAL_ITERATE(iter(t_operand, get<0>(K)[key_index], get<1>(K)), coeff_count, [&](auto L)
                        for (int L = 0; L < coeff_count; L++) {
                            // unsigned long long qword[2]{ 0, 0 };
                            long[] qword = new long[2];
                            // multiply_uint64(get<0>(L), get<1>(L), qword);
                            UintArithmetic.multiplyUint64(
                                t_operand.getCoeff(L),
                                key_vector_J.data()[key_vector_J_K + key_index * key_vector_J.polyModulusDegree() + L],
                                qword
                            );
                            // add_uint128(qword, get<2>(L).ptr(), qword);
                            long[] uint128_temp = new long[]{
                                t_poly_lazy[tPolyLazyK + L * 2], t_poly_lazy[tPolyLazyK + L * 2 + 1],
                            };
                            UintArithmetic.addUint128(qword, uint128_temp, qword);
                            // get<2>(L)[0] = qword[0];
                            t_poly_lazy[tPolyLazyK + L * 2] = qword[0];
                            // get<2>(L)[1] = qword[1];
                            t_poly_lazy[tPolyLazyK + L * 2 + 1] = qword[1];
                        }
                    }
                }

                if (--lazy_reduction_counter == 0) {
                    lazy_reduction_counter = lazy_reduction_summand_bound;
                }
            }
            // PolyIter pointing to the destination t_poly_prod, shifted to the appropriate modulus
            // here we directly use t_poly_prod instead of creating a new t_poly_prod_iter.

            // Final modular reduction
            // SEAL_ITERATE(iter(accumulator_iter, t_poly_prod_iter), key_component_count, [&](auto K)
            for (int K = 0; K < key_component_count; K++) {
                if (lazy_reduction_counter == lazy_reduction_summand_bound) {
                    // SEAL_ITERATE(iter(get<0>(K), *get<1>(K)), coeff_count, [&](auto L)
                    for (int L = 0; L < coeff_count; L++) {
                        t_poly_prod.rnsIter[K].coeffIter[I].setCoeff(
                            L, t_poly_lazy[K * coeff_count * 2 + 2 * L]
                        );
                    }
                } else {
                    // Same as above except need to still do reduction
                    long[] uint128_temp = new long[2];
                    for (int l = 0; l < coeff_count; l++) {
                        uint128_temp[0] = t_poly_lazy[K * coeff_count * 2 + 2 * l];
                        uint128_temp[1] = t_poly_lazy[K * coeff_count * 2 + 2 * l + 1];

                        t_poly_prod.rnsIter[K].coeffIter[I].setCoeff(
                            l, UintArithmeticSmallMod.barrettReduce128(uint128_temp, key_modulus[key_index]
                            )
                        );
                    }
                }
            }
        }
        // Accumulated products are now stored in t_poly_prod

        // Perform modulus switching with scaling
        // SEAL_ITERATE(iter(encrypted, t_poly_prod_iter), key_component_count, [&](auto I)
        PolyIterator encrypted_poly_iter = PolyIterator.fromCiphertext(encrypted);
        for (int I = 0; I < key_component_count; I++) {
            if (scheme.equals(SchemeType.BGV)) {
                // TODO: implement BGV
                throw new IllegalArgumentException("unsupport BGV");
            } else {
                // Lazy reduction; this needs to be then reduced mod qi
                CoeffIterator t_last = t_poly_prod.rnsIter[I].coeffIter[decomp_modulus_size];
                NttTool.inverseNttNegacyclicHarveyLazy(t_last, key_ntt_tables[key_modulus_size - 1]);

                // Add (p-1)/2 to change from flooring to rounding.
                long qk = key_modulus[key_modulus_size - 1].value();
                long qk_half = qk >>> 1;

                // SEAL_ITERATE(t_last, coeff_count, [&](auto &J)
                for (int J = 0; J < coeff_count; J++) {
                    t_last.setCoeff(
                        J,
                        UintArithmeticSmallMod.barrettReduce64(t_last.getCoeff(J) + qk_half, key_modulus[key_modulus_size - 1])
                    );
                }

                // SEAL_ITERATE(iter(I, key_modulus, key_ntt_tables, modswitch_factors), decomp_modulus_size, [&](auto J)
                for (int J = 0; J < decomp_modulus_size; J++) {
                    // SEAL_ALLOCATE_GET_COEFF_ITER(t_ntt, coeff_count, pool);
                    CoeffIterator t_ntt = CoeffIterator.allocate(coeff_count);

                    // (ct mod 4qk) mod qi
                    long qi = key_modulus[J].value();
                    if (qk > qi) {
                        // This cannot be spared. NTT only tolerates input that is less than 4*modulus (i.e. qk <=4*qi).
                        PolyArithmeticSmallMod.moduloPolyCoeff(t_last, coeff_count, key_modulus[J], t_ntt);
                    } else {
                        UintCore.setUint(
                            t_last.coeff(), t_last.ptr(), coeff_count,
                            t_ntt.coeff(), 0, coeff_count
                        );
                    }

                    // Lazy substraction, results in [0, 2*qi), since fix is in [0, qi].
                    long fix = qi - UintArithmeticSmallMod.barrettReduce64(qk_half, key_modulus[J]);
                    // SEAL_ITERATE(t_ntt, coeff_count, [fix](auto &K) { K += fix; });
                    for (int K = 0; K < coeff_count; K++) {
                        t_ntt.setCoeff(K, t_ntt.getCoeff(K) + fix);
                    }

                    // some multiples of qi
                    long qi_lazy = qi << 1;
                    CoeffIterator get_0_1_J = t_poly_prod.rnsIter[I].coeffIter[J];
                    if (scheme.equals(SchemeType.CKKS)) {
                        // This ntt_negacyclic_harvey_lazy results in [0, 4*qi).
                        // ntt_negacyclic_harvey_lazy(t_ntt, get<2>(J));
                        NttTool.nttNegacyclicHarveyLazy(t_ntt, key_ntt_tables[J]);
                        // Since SEAL uses at most 60bit moduli, 8*qi < 2^63.
                        qi_lazy = qi << 2;
                    } else if (scheme.equals(SchemeType.BFV)) {
                        NttTool.inverseNttNegacyclicHarveyLazy(get_0_1_J, key_ntt_tables[J]);
                    }

                    // ((ct mod qi) - (ct mod qk)) mod qi with output in [0, 2 * qi_lazy)
                    // SEAL_ITERATE(
                    //     iter(get<0, 1>(J), t_ntt), coeff_count, [&](auto K) { get<0>(K) += qi_lazy - get<1>(K); });
                    for (int K = 0; K < coeff_count; K++) {
                        get_0_1_J.setCoeff(
                            K, get_0_1_J.getCoeff(K) + qi_lazy - t_ntt.getCoeff(K)
                        );
                    }

                    // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                        get_0_1_J, coeff_count, modswitch_factors[J], key_modulus[J], get_0_1_J
                    );
                    CoeffIterator get_0_0_J = encrypted_poly_iter.rnsIter[I].coeffIter[J];
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        get_0_1_J, get_0_0_J, coeff_count, key_modulus[J], get_0_0_J
                    );
                }
            }
        }
    }
}
