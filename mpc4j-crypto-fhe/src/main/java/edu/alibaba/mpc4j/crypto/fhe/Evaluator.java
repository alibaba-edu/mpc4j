package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

/**
 * Provides operations on ciphertexts. Due to the properties of the encryption scheme, the arithmetic operations pass
 * through the encryption layer to the underlying plaintext, changing it according to the type of the operation. Since
 * the plaintext elements are fundamentally polynomials in the polynomial quotient ring Z_T[x]/(X^N+1), where T is the
 * plaintext modulus and X^N+1 is the polynomial modulus, this is the ring where the arithmetic operations will take
 * place. BatchEncoder (batching) provider an alternative possibly more convenient view of the plaintext elements as
 * 2-by-(N2/2) matrices of integers modulo the plaintext modulus. In the batching view the arithmetic operations act on
 * the matrices element-wise. Some of the operations only apply in the batching view, such as matrix row and column
 * rotations. Other operations such as relinearization have no semantic meaning but are necessary for performance
 * reasons.
 * <p></p>
 * <p>Arithmetic Operations</p>
 * The core operations are arithmetic operations, in particular multiplication and addition of ciphertexts. In addition
 * to these, we also provide negation, subtraction, squaring, exponentiation, and multiplication and addition of
 * several ciphertexts for convenience. in many cases some of the inputs to a computation are plaintext elements rather
 * than ciphertexts. For this we provide fast "plain" operations: plain addition, plain subtraction, and plain
 * multiplication.
 * <p></p>
 * <p>Relinearization</p>
 * One of the most important non-arithmetic operations is relinearization, which takes as input a ciphertext of size
 * K+1 and relinearization keys (at least K-1 keys are needed), and changes the size of the ciphertext down to 2
 * (minimum size). For most use-cases only one relinearization key suffices, in which case relinearization should be
 * performed after every multiplication. Homomorphic multiplication of ciphertexts of size K+1 and L+1 outputs a
 * ciphertext of size K+L+1, and the computational cost of multiplication is proportional to K*L. Plain multiplication
 * and addition operations of any type do not change the size. Relinearization requires relinearization keys to have
 * been generated.
 * <p></p>
 * <p>Rotations</p>
 * When batching is enabled, we provide operations for rotating the plaintext matrix rows cyclically left or right, and
 * for rotating the columns (swapping the rows). Rotations require Galois keys to have been generated.
 * <p></p>
 * <p><pOther Operations</p>
 * We also provide operations for transforming ciphertexts to NTT form and back, and for transforming plaintext
 * polynomials to NTT form. These can be used in a very fast plain multiplication variant, that assumes the inputs to
 * be in NTT form. Since the NTT has to be done in any case in plain multiplication, this function can be used when
 * e.g. one plaintext input is used in several plain multiplication, and transforming it several times would not make
 * sense.
 * <p></p>
 * <p>NTT form</p>
 * When using the BFV/BGV scheme (scheme_type::bfv/bgv), all plaintexts and ciphertexts should remain by default in the
 * usual coefficient representation, i.e., not in NTT form. When using the CKKS scheme (scheme_type::ckks), all
 * plaintexts and ciphertexts should remain by default in NTT form. We call these scheme-specific NTT states the
 * "default NTT form". Some functions, such as add, work even if the inputs are not in the default state, but others,
 * such as multiply, will throw an exception. The output of all evaluation functions will be in the same state as the
 * input(s), with the exception of the transform_to_ntt and transform_from_ntt functions, which change the state.
 * Ideally, unless these two functions are called, all other functions should "just work".
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/evaluator.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/25
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Evaluator {
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

    private boolean areSameScale(Ciphertext value1, Ciphertext value2) {
        return Common.areClose(value1.scale(), value2.scale());
    }

    private boolean areSameScale(Ciphertext value1, Plaintext value2) {
        return Common.areClose(value1.scale(), value2.getScale());
    }

    private boolean isScaleWithinBounds(double scale, ContextData contextData) {
        int scaleBitCountBound = 0;
        switch (contextData.parms().scheme()) {
            case BFV:
            case BGV:
                scaleBitCountBound = contextData.parms().plainModulus().bitCount();
                break;
            case CKKS:
                scale = contextData.totalCoeffModulusBitCount();
                break;
            default:
                // Unsupported scheme; check will fail
                scaleBitCountBound = -1;
        }
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
    private long[] balanceCorrectionFactors(long factor1, long factor2, Modulus plainModulus) {
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

    private long sumAbs(long x, long y, long t, long halfT) {
        long xBal = Long.compareUnsigned(x, halfT) > 0 ? x - t : x;
        long yBal = Long.compareUnsigned(y, halfT) > 0 ? y - t : y;

        return Math.abs(xBal) + Math.abs(yBal);
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
        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int encryptedSize = encrypted.size();

        // Negate each poly in the array
        PolyArithmeticSmallMod.negatePolyCoeffModPoly(
            encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize(),
            encryptedSize, coeffModulus,
            encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
        );
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
        if (!areSameScale(encrypted1, encrypted2)) {
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

        if (encrypted1.correctionFactor() != encrypted2.correctionFactor()) {
            // Balance correction factors and multiply by scalars before addition in BGV
            long[] factors = balanceCorrectionFactors(
                encrypted1.correctionFactor(), encrypted2.correctionFactor(), plainModulus
            );

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
                encrypted1.data(), coeffCount, coeffModulusSize, encrypted1.size(),
                factors[1], coeffModulus,
                encrypted1.data(), coeffCount, coeffModulusSize
            );

            Ciphertext encrypted2Copy = new Ciphertext();
            encrypted2Copy.copyFrom(encrypted2);
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
                encrypted2.data(), coeffCount, coeffModulusSize, encrypted2.size(),
                factors[2], coeffModulus, encrypted2Copy.data(), coeffCount, coeffModulusSize
            );

            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2Copy.setCorrectionFactor(factors[0]);

            addInplace(encrypted1, encrypted2Copy);
        } else {
            // prepare destination
            encrypted1.resize(context, contextData.parmsId(), maxCount);
            // AddCiphertexts
            PolyArithmeticSmallMod.addPolyCoeffModPoly(
                encrypted1.data(), encrypted1.polyModulusDegree(), encrypted1.getCoeffModulusSize(),
                encrypted2.data(), encrypted2.polyModulusDegree(), encrypted2.getCoeffModulusSize(),
                minCount, coeffModulus,
                encrypted1.data(), encrypted1.polyModulusDegree(), encrypted1.getCoeffModulusSize()
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
        if (!areSameScale(encrypted1, encrypted2)) {
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

        if (encrypted1.correctionFactor() != encrypted2.correctionFactor()) {
            // Balance correction factors and multiply by scalars before subtraction in BGV
            long[] factors = balanceCorrectionFactors(encrypted1.correctionFactor(), encrypted2.correctionFactor(), plainModulus);

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
                encrypted1.data(), coeffCount, coeffModulusSize, encrypted1.size(),
                factors[1], coeffModulus,
                encrypted1.data(), coeffCount, coeffModulusSize
            );

            Ciphertext encrypted2Copy = new Ciphertext();
            encrypted2Copy.copyFrom(encrypted2);
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(
                encrypted2.data(), coeffCount, coeffModulusSize, encrypted2.size(),
                factors[2], coeffModulus,
                encrypted2Copy.data(), coeffCount, coeffModulusSize
            );

            // Set new correction factor
            encrypted1.setCorrectionFactor(factors[0]);
            encrypted2Copy.setCorrectionFactor(factors[0]);

            subInplace(encrypted1, encrypted2Copy);
        } else {
            // prepare destination
            encrypted1.resize(context, contextData.parmsId(), maxCount);

            // Subtract ciphertexts
            PolyArithmeticSmallMod.subPolyCoeffModPoly(
                encrypted1.data(), encrypted1.polyModulusDegree(), encrypted1.getCoeffModulusSize(),
                encrypted2.data(), encrypted2.polyModulusDegree(), encrypted2.getCoeffModulusSize(),
                minCount, coeffModulus,
                encrypted1.data(), encrypted1.polyModulusDegree(), encrypted1.getCoeffModulusSize()
            );

            // If encrypted2 has larger count, negate remaining entries
            if (encrypted1Size < encrypted2Size) {
                for (int i = minCount; i < encrypted2Size; i++) {
                    PolyArithmeticSmallMod.negatePolyCoeffModRns(
                        encrypted2.data(), encrypted2.getPolyOffset(i), encrypted2.polyModulusDegree(), coeffModulusSize,
                        coeffModulus,
                        encrypted1.data(), encrypted1.getPolyOffset(i), encrypted1.polyModulusDegree(), coeffModulusSize
                    );
                }
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
                bfvMultiply(encrypted1, encrypted2);
                break;
            case CKKS:
                // TODO: implement CKKS
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted1.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    private void bfvMultiply(Ciphertext encrypted1, Ciphertext encrypted2) {
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
        Modulus[] baseQ = parms.coeffModulus();
        Modulus[] baseBsk = rnsTool.baseBsk().getBase();

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
        long[] encrypted1Q = new long[encrypted1Size * coeffCount * baseQSize];
        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encrypted1Bsk = new long[encrypted1Size * coeffCount * baseBskSize];

        // Perform BEHZ steps (1)-(3) for encrypted1
        behzExtendBaseConvertToNtt(
            encrypted1, coeffCount, encrypted1.getCoeffModulusSize(), encrypted1Size,
            rnsTool, baseQNttTables, encrypted1Q, encrypted1Bsk
        );

        // Repeat for encrypted2
        long[] encrypted2Q = new long[encrypted2Size * coeffCount * baseQSize];
        long[] encrypted2Bsk = new long[encrypted2Size * coeffCount * baseBskSize];
        behzExtendBaseConvertToNtt(
            encrypted2, coeffCount, encrypted2.getCoeffModulusSize(), encrypted2Size,
            rnsTool, baseQNttTables, encrypted2Q, encrypted2Bsk
        );

        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        long[] tempDestinationQ = new long[destinationSize * coeffCount * baseQSize];
        long[] tempDestinationBsk = new long[destinationSize * coeffCount * baseBskSize];

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
            // behz_ciphertext_product(encrypted1_q, encrypted2_q, base_q, base_q_size, temp_dest_q);
            behzCiphertextProduct(
                encrypted1Q, currEncrypted1First, encrypted2Q, currEncrypted2First,
                coeffCount, steps, baseQ, tempDestinationQ, i
            );
            // behz_ciphertext_product(encrypted1_Bsk, encrypted2_Bsk, base_Bsk, base_Bsk_size, temp_dest_Bsk);
            behzCiphertextProduct(
                encrypted1Bsk, currEncrypted1First, encrypted2Bsk, currEncrypted2First,
                coeffCount, steps, baseBsk, tempDestinationBsk, i
            );
        }

        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationQ, destinationSize, coeffCount, baseQSize, baseQNttTables);
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationBsk, destinationSize, coeffCount, baseBskSize, baseBskNttTables);

        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < destinationSize; i++) {
            // Bring together the base q and base Bsk components into a single allocation a RnsIter
            long[] tempQBsk = new long[coeffCount * (baseQSize + baseBskSize)];

            // Step (6): multiply base q components by t (plain_modulus), ct_i * t mod q
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationQ, i * coeffCount * baseQSize, coeffCount, baseQSize,
                plainModulus, baseQ,
                tempQBsk, 0, coeffCount, baseQSize
            );
            // multiply base bsk components by t (plain_modulus), ct_i * t mod bsk
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationBsk, i * coeffCount * baseBskSize, coeffCount, baseBskSize,
                plainModulus, baseBsk,
                tempQBsk, baseQSize * coeffCount, coeffCount, baseBskSize
            );

            // Allocate yet another temporary for fast divide-and-floor result in base Bsk a RnsIter
            long[] tempBsk = new long[coeffCount * baseBskSize];

            // Step (7): divide by q and floor, producing a result in base Bsk
            rnsTool.fastFloorRnsIter(
                tempQBsk, 0, coeffCount, baseQSize + baseBskSize, tempBsk,
                0, coeffCount, baseBskSize
            );

            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rnsTool.fastBConvSkRnsIter(
                tempBsk, 0, coeffCount, baseBskSize,
                encrypted1.data(), i * coeffCount * encrypted1.getCoeffModulusSize(),
                encrypted1.polyModulusDegree(), encrypted1.getCoeffModulusSize()
            );
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
     * @param n              coefficient count.
     * @param k              coefficient modulus size.
     * @param size           ciphertext size.
     * @param rnsTool        RNS tool.
     * @param baseQNttTables NTT tables for the base Q.
     * @param encryptedQ     allocated space for a base q output of behz_extend_base_convert_to_ntt for ciphertext.
     * @param encryptedBsk   allocate space for a base B_sk output of behz_extend_base_convert_to_ntt for ciphertext.
     */
    private void behzExtendBaseConvertToNtt(Ciphertext encrypt, int n, int k, int size,
                                            RnsTool rnsTool, NttTables[] baseQNttTables,
                                            long[] encryptedQ, long[] encryptedBsk) {
        int baseQSize = rnsTool.baseQ().size();
        int baseBskMTildeSize = rnsTool.baseBskMTilde().size();
        int baseBskSize = rnsTool.baseBsk().size();
        NttTables[] baseBskNttTables = rnsTool.baseBskNttTables();
        for (int i = 0; i < size; i++) {
            // Make copy of input polynomial (in base q) and convert to NTT form
            PolyCore.setPoly(
                encrypt.data(), i * n * k,
                n, baseQSize,
                encryptedQ, i * n * baseQSize
            );
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedQ, size, n, baseQSize, i, baseQNttTables);

            // Allocate temporary space for a polynomial in base {B_sk, {m_tilde}}
            long[] temp = new long[n * baseBskMTildeSize];

            // 1) Convert from base q to base {B_sk, {m_tilde}}
            rnsTool.fastBConvMTildeRnsIter(encrypt.data(), i * n * k, n, k, temp, 0, n, baseBskMTildeSize);

            // (2) Reduce q-overflows in with Montgomery reduction, switching base to B_sk
            rnsTool.smMrqRnsIter(
                temp, 0, n, baseBskMTildeSize,
                encryptedBsk, i * n * baseBskSize, n, baseBskSize
            );
            // Transform to NTT form in base B_sk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedBsk, size, n, baseBskSize, i, baseBskNttTables);
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
    private void behzCiphertextProduct(long[] in1, int in1Index, long[] in2, int in2Index, int n,
                                       int steps, Modulus[] base,
                                       long[] destination, int desIndex) {
        int baseSize = base.length;
        // Create a shifted iterator for the first input
        int shiftedIn1Iter = in1Index * n * baseSize;

        // Create a shifted reverse iterator for the second input
        int shiftedReversedIn2Iter = in2Index * n * baseSize;

        // Create a shifted iterator for the output
        int shiftedOutIter = desIndex * n * baseSize;
        for (int j = 0; j < steps; j++) {
            for (int k = 0; k < baseSize; k++) {
                long[] temp = new long[n];
                // c_1 mod q_i, c_2 mod q_i in NTT form, compute c_1 * c_2 mod q_i
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    in1, shiftedIn1Iter + j * n * baseSize + k * n,
                    in2, (shiftedReversedIn2Iter - j * n * baseSize) + k * n,
                    n, base[k],
                    temp, 0
                );
                PolyArithmeticSmallMod.addPolyCoeffMod(
                    temp, 0, destination, shiftedOutIter + k * n,
                    n, base[k],
                    destination, shiftedOutIter + k * n
                );
            }
        }
    }

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
                bfvSquare(encrypted);
                break;
            case CKKS:
                // TODO: implement CKKS
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (encrypted.isTransparent()) {
            throw new IllegalArgumentException("result ciphertext is transparent");
        }
    }

    // TODO: ckks_multiply

    // TODO: bgv_multiply

    private void bfvSquare(Ciphertext encrypted) {
        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }

        // Extract encryption parameters.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        int coeffCount = parms.polyModulusDegree();
        int baseQSize = parms.coeffModulus().length;
        int encryptedSize = encrypted.size();
        long plainModulus = parms.plainModulus().value();

        RnsTool rnsTool = contextData.rnsTool();
        int baseBskSize = rnsTool.baseBsk().size();
        int baseBskMTildeSize = rnsTool.baseBskMTilde().size();

        // Optimization implemented currently only for size 2 ciphertexts
        if (encryptedSize != 2) {
            bfvMultiply(encrypted, encrypted);
            return;
        }

        // Determine destination.size(), which should be c1.size() * c2.size() - 1
        int destinationSize = Common.subSafe(Common.addSafe(encryptedSize, encryptedSize, false), 1, false);

        // Size check
        if (!Common.productFitsIn(false, destinationSize, coeffCount, baseBskMTildeSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        Modulus[] baseQ = parms.coeffModulus();
        Modulus[] baseBsk = rnsTool.baseBsk().getBase();
        NttTables[] baseQNttTables = contextData.smallNttTables();
        NttTables[] baseBskNttTables = rnsTool.baseBskNttTables();

        // Microsoft SEAL uses BEHZ-style RNS multiplication. For details, see Evaluator::bfv_multiply. This function
        // uses additionally Karatsuba multiplication to reduce the complexity of squaring a size-2 ciphertext, but the
        // steps are otherwise the same as in Evaluator::bfv_multiply.

        // Resize encrypted1 to destination size
        encrypted.resize(context, contextData.parmsId(), destinationSize);

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
        long[] encryptedQ = new long[encryptedSize * coeffCount * baseQSize];

        // Allocate space for a base Bsk output of behz_extend_base_convert_to_ntt for encrypted1
        long[] encryptedBsk = new long[encryptedSize * coeffCount * baseBskSize];

        // Perform BEHZ steps (1)-(3) for encrypted1
        for (int i = 0; i < encryptedSize; i++) {
            // Make copy of input polynomial (in base q) and convert to NTT form
            PolyCore.setPoly(
                encrypted.data(), i * coeffCount * encrypted.getCoeffModulusSize(),
                coeffCount, baseQSize,
                encryptedQ, i * coeffCount * baseQSize
            );
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedQ, encryptedSize, coeffCount, baseQSize, i, baseQNttTables);

            // Allocate temporary space for a polynomial in base {B_sk, {m_tilde}}
            long[] temp = new long[coeffCount * baseBskMTildeSize];

            // 1) Convert from base q to base Bsk U {m_tilde}
            rnsTool.fastBConvMTildeRnsIter(
                encrypted.data(), i * coeffCount * encrypted.getCoeffModulusSize(),
                coeffCount, encrypted.getCoeffModulusSize(),
                temp, 0, coeffCount, baseBskMTildeSize
            );

            // (2) Reduce q-overflows in with Montgomery reduction, switching base to B_sk
            rnsTool.smMrqRnsIter(
                temp, 0, coeffCount, baseBskMTildeSize,
                encryptedBsk, i * coeffCount * baseBskSize, coeffCount, baseBskSize
            );

            // Transform to NTT form in base B_sk
            // Lazy reduction
            NttTool.nttNegacyclicHarveyLazyPoly(encryptedBsk, encryptedSize, coeffCount, baseBskSize, i, baseBskNttTables);
        }

        // Allocate temporary space for the output of step (4)
        // We allocate space separately for the base q and the base Bsk components
        long[] tempDestinationQ = new long[destinationSize * coeffCount * baseQSize];
        long[] tempDestinationBsk = new long[destinationSize * coeffCount * baseBskSize];

        // Perform BEHZ step (4): dyadic multiplication on arbitrary size ciphertexts
        // behz_ciphertext_square(encrypted_q, base_q, base_q_size, temp_dest_q);
        behzCiphertextSquare(encryptedQ, coeffCount, baseQ, tempDestinationQ);
        // behz_ciphertext_square(encrypted_Bsk, base_Bsk, base_Bsk_size, temp_dest_Bsk);
        behzCiphertextSquare(encryptedBsk, coeffCount, baseBsk, tempDestinationBsk);

        // Perform BEHZ step (5): transform data from NTT form
        // Lazy reduction here. The following multiply_poly_scalar_coeffmod will correct the value back to [0, p)
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationQ, destinationSize, coeffCount, baseQSize, baseQNttTables);
        NttTool.inverseNttNegacyclicHarveyLazyPoly(tempDestinationBsk, destinationSize, coeffCount, baseBskSize, baseBskNttTables);

        // Perform BEHZ steps (6)-(8)
        for (int i = 0; i < destinationSize; i++) {
            // Bring together the base q and base Bsk components into a single allocation
            long[] tempQBsk = new long[coeffCount * (baseQSize + baseBskSize)];

            // Step (6): multiply base q components by t (plain_modulus)
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationQ, i * coeffCount * baseQSize, coeffCount, baseQSize,
                plainModulus, baseQ,
                tempQBsk, 0, coeffCount, baseQSize
            );

            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                tempDestinationBsk, i * coeffCount * baseBskSize, coeffCount, baseBskSize,
                plainModulus, baseBsk,
                tempQBsk, baseQSize * coeffCount, coeffCount, baseBskSize
            );

            // Allocate yet another temporary for fast divide-and-floor result in base Bsk
            long[] tempBsk = new long[coeffCount * baseBskSize];

            // Step (7): divide by q and floor, producing a result in base Bsk
            rnsTool.fastFloorRnsIter(
                tempQBsk, 0, coeffCount, baseQSize + baseBskSize,
                tempBsk,
                0, coeffCount, baseBskSize
            );

            // Step (8): use Shenoy-Kumaresan method to convert the result to base q and write to encrypted1
            rnsTool.fastBConvSkRnsIter(
                tempBsk, 0, coeffCount, baseBskSize,
                encrypted.data(), i * coeffCount * encrypted.getCoeffModulusSize(),
                encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
            );
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
    private void behzCiphertextSquare(long[] in, int n, Modulus[] baseQ, long[] destination) {
        int baseQSize = baseQ.length;
        // compute c0^2
        PolyArithmeticSmallMod.dyadicProductCoeffModRns(
            in, 0, n, baseQSize, in, 0, n, baseQSize,
            baseQ,
            destination, 0, n, baseQSize
        );

        // compute 2 * c0 * c1
        PolyArithmeticSmallMod.dyadicProductCoeffModRns(
            in, 0, n, baseQSize, in, n * baseQSize, n, baseQSize,
            baseQ,
            destination, n * baseQSize, n, baseQSize
        );
        PolyArithmeticSmallMod.addPolyCoeffModRns(
            destination, n * baseQSize, n, baseQSize,
            destination, n * baseQSize, n, baseQSize,
            baseQ,
            destination, n * baseQSize, n, baseQSize
        );

        // compute c1^2
        PolyArithmeticSmallMod.dyadicProductCoeffModRns(
            in, n * baseQSize, n, baseQSize,
            in, n * baseQSize, n, baseQSize,
            baseQ,
            destination, 2 * n * baseQSize, n, baseQSize
        );
    }

    // TODO: ckks_square

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
        relinearizeInternal(encrypted, relinKeys, 2);
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
        relinearizeInternal(destination, relinKeys, 2);
    }

    private void relinearizeInternal(Ciphertext encrypted, RelinKeys relinKeys, int destinationSize) {
        // Verify parameters.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData == null) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (!relinKeys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("relinKeys is not valid for encryption parameters");
        }

        int encryptedSize = encrypted.size();

        // Verify parameters.
        if (destinationSize < 2 || destinationSize > encryptedSize) {
            throw new IllegalArgumentException("destinationSize must be at least 2 and less than or equal to current count");
        }
        if (relinKeys.size() < Common.subSafe(encryptedSize, 2, false)) {
            throw new IllegalArgumentException("not enough relinearization keys");
        }

        // If encrypted is already at the desired level, return
        if (destinationSize == encryptedSize) {
            return;
        }

        // Calculate number of relinearize_one_step calls needed
        int reLinsNeeded = encryptedSize - destinationSize;

        // Iterator pointing to the last component of encrypted
        int encryptedIter = (encryptedSize - 1) * encrypted.polyModulusDegree() * encrypted.getCoeffModulusSize();

        for (int i = 0; i < reLinsNeeded; i++) {
            switchKeyInplace(
                encrypted, encrypted.data(), encryptedIter, encrypted.polyModulusDegree(),
                encrypted.getCoeffModulusSize(), relinKeys, RelinKeys.getIndex(encryptedSize - 1 - i)
            );
        }

        // Put the output of final relinearization into destination.
        // Prepare destination only at this point because we are resizing down
        encrypted.resize(context, contextData.parmsId(), destinationSize);

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

        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (context.lastParmsId().equals(encrypted.parmsId())) {
            throw new IllegalArgumentException("end of modulus switching chain reached ");
        }

        switch (context.firstContextData().parms().scheme()) {
            case BFV:
                // Modulus switching with scaling
                modSwitchScaleToNext(encrypted, destination);
                break;
            case CKKS:
                // TODO: implement CKKS
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        if (destination.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    private void modSwitchScaleToNext(Ciphertext encrypted, Ciphertext destination) {
        // Assuming at this point encrypted is already validated.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        if (contextData.parms().scheme().equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (contextData.parms().scheme().equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (contextData.parms().scheme().equals(SchemeType.BGV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        // Extract encryption parameters.
        ContextData nextContextData = contextData.nextContextData();
        EncryptionParameters nextParms = nextContextData.parms();
        RnsTool rnsTool = contextData.rnsTool();

        int encryptedSize = encrypted.size();
        int coeffCount = nextParms.polyModulusDegree();
        int nextCoeffModulusSize = nextParms.coeffModulus().length;

        Ciphertext encryptedCopy = new Ciphertext();
        encryptedCopy.copyFrom(encrypted);

        switch (nextParms.scheme()) {
            case BFV:
                for (int i = 0; i < encryptedSize; i++) {
                    rnsTool.divideAndRoundQLastInplace(
                        encryptedCopy.data(), i * encryptedCopy.polyModulusDegree() * encryptedCopy.getCoeffModulusSize(),
                        encryptedCopy.polyModulusDegree(), encryptedCopy.getCoeffModulusSize()
                    );
                }
                break;
            case CKKS:
                // TODO: implement CKKS
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }

        // Copy result to destination
        destination.resize(context, nextContextData.parmsId(), encryptedSize);
        for (int i = 0; i < encryptedSize; i++) {
            PolyCore.setPoly(
                encryptedCopy.data(), encryptedCopy.getPolyOffset(i),
                coeffCount, nextCoeffModulusSize,
                destination.data(), destination.getPolyOffset(i)
            );
        }

        // Set other attributes
        destination.setIsNttForm(encrypted.isNttForm());

        if (nextParms.scheme().equals(SchemeType.CKKS)) {
            // Change the scale when using CKKS
            Modulus[] coeffModulus = contextData.parms().coeffModulus();
            destination.setScale(encrypted.scale() / (double) coeffModulus[coeffModulus.length - 1].value());
        } else if (nextParms.scheme().equals(SchemeType.BGV)) {
            // Change the correction factor when using BGV
            destination.setCorrectionFactor(UintArithmeticSmallMod.multiplyUintMod(
                encrypted.correctionFactor(), rnsTool.invQLastModT(), nextParms.plainModulus()
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
        modSwitchDropToNext(plain);
    }

    private void modSwitchDropToNext(Plaintext plain) {
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

        if (!isScaleWithinBounds(plain.scale(), nextContextData)) {
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

    // TODO: rescale_to_next (only for CKKS)

    // TODO: rescale_to_next_inplace (only for CKKS)

    // TODO: rescale_to_inplace (only for CKKS)

    // TODO: rescale_to (only for CKKS)

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
        if (parms.scheme().equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (parms.scheme().equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (parms.scheme().equals(SchemeType.BGV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }
        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }
        if (encrypted.isNttForm() && (!encrypted.parmsId().equals(plain.parmsId()))) {
            throw new IllegalArgumentException("encrypted and plain parameter mismatch");
        }
        if (!areSameScale(encrypted, plain)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        // Extract encryption parameters.
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }
        switch (parms.scheme()) {
            case BFV:
                ScalingVariant.multiplyAddPlainWithScalingVariant(
                    plain, contextData, encrypted.data(), encrypted.getPolyOffset(0), coeffCount
                );
                break;
            case CKKS:
                // TODO: implement CKKS
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
        if (parms.scheme().equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (parms.scheme().equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (parms.scheme().equals(SchemeType.BGV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }
        if (plain.isNttForm() != encrypted.isNttForm()) {
            throw new IllegalArgumentException("NTT form mismatch");
        }
        if (encrypted.isNttForm() && (!encrypted.parmsId().equals(plain.parmsId()))) {
            throw new IllegalArgumentException("encrypted and plain parameter mismatch");
        }
        if (!areSameScale(encrypted, plain)) {
            throw new IllegalArgumentException("scale mismatch");
        }

        // Extract encryption parameters.
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        switch (parms.scheme()) {
            case BFV:
                ScalingVariant.multiplySubPlainWithScalingVariant(
                    plain, contextData, encrypted.data(), encrypted.getPolyOffset(0), coeffCount
                );
                break;
            case CKKS:
                // TODO: implement CKKS
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

        if (encrypted.isNttForm()) {
            multiplyPlainNtt(encrypted, plain);
        } else {
            multiplyPlainNormal(encrypted, plain);
        }

        // Transparent ciphertext output is not allowed.
        if (encrypted.isTransparent()) {
            throw new RuntimeException("result ciphertext is transparent");
        }
    }

    private void multiplyPlainNormal(Ciphertext encrypted, Plaintext plain) {
        // Extract encryption parameters.
        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        long plainUpperHalfThreshold = contextData.plainUpperHalfThreshold();
        long[] plainUpperHalfIncrement = contextData.plainUpperHalfIncrement();
        NttTables[] nttTables = contextData.smallNttTables();

        int encryptedSize = encrypted.size();
        int plainCoeffCount = plain.coeffCount();
        int plainNonZeroCoeffCount = plain.nonZeroCoeffCount();

        // Size check
        if (!Common.productFitsIn(false, encryptedSize, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameter");
        }

        /*
        Optimizations for constant / monomial multiplication can lead to the presence of a timing side-channel in
        use-cases where the plaintext data should also be kept private.
        */
        if (plainNonZeroCoeffCount == 1) {
            // Multiplying by a monomial?
            int monoExponent = plain.significantCoeffCount() - 1;
            if (plain.at(monoExponent) >= plainUpperHalfThreshold) {

                if (!contextData.qualifiers().isUsingFastPlainLift()) {
                    // Allocate temporary space for a single RNS coefficient
                    long[] temp = new long[coeffModulusSize];

                    // We need to adjust the monomial modulo each coeff_modulus prime separately when the coeff_modulus
                    // primes may be larger than the plain_modulus. We add plain_upper_half_increment (i.e., q-t) to
                    // the monomial to ensure it is smaller than coeff_modulus and then do an RNS multiplication. Note
                    // that in this case plain_upper_half_increment contains a multi-precision integer, so after the
                    // addition we decompose the multi-precision integer into RNS components, and then multiply.
                    UintArithmetic.addUint(plainUpperHalfIncrement, coeffModulusSize, plain.at(monoExponent), temp);
                    contextData.rnsTool().baseQ().decompose(temp);
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                        encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize(), encryptedSize,
                        temp, monoExponent, coeffModulus,
                        encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
                    );
                } else {
                    // Every coeff_modulus prime is larger than plain_modulus, so there is no need to adjust the
                    // monomial. Instead, just do an RNS multiplication.
                    PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                        encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize(), encryptedSize,
                        plain.at(monoExponent), monoExponent, coeffModulus,
                        encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
                    );
                }
            } else {
                // The monomial represents a positive number, so no RNS multiplication is needed.
                PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                    encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize(), encryptedSize,
                    plain.at(monoExponent), monoExponent, coeffModulus,
                    encrypted.data(), encrypted.polyModulusDegree(), encrypted.getCoeffModulusSize()
                );
            }

            // Set the scale
            if (parms.scheme().equals(SchemeType.CKKS)) {
                // todo: implement CKKS
                throw new IllegalArgumentException("now cannot support CKKS");
            }
            return;
        }

        // Generic case: any plaintext polynomial
        // Allocate temporary space for an entire RNS polynomial
        long[] temp = new long[coeffCount * coeffModulusSize];

        if (!contextData.qualifiers().isUsingFastPlainLift()) {
            for (int i = 0; i < plainCoeffCount; i++) {
                long plainValue = plain.at(i);
                if (plainValue >= plainUpperHalfThreshold) {
                    UintArithmetic.addUint(plainUpperHalfIncrement, 0, coeffModulusSize, plainValue, temp, i * coeffModulusSize);
                } else {
                    temp[i * coeffModulusSize] = plainValue;
                }
            }
            contextData.rnsTool().baseQ().decomposeArray(temp, coeffCount);
        } else {
            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.
            for (int i = 0; i < coeffModulusSize; i++) {
                for (int j = 0; j < plainCoeffCount; j++) {
                    temp[i * coeffCount + j] = plain.at(j) >= plainUpperHalfThreshold ?
                        plain.at(j) + plainUpperHalfIncrement[i] : plain.at(j);
                }
            }
        }

        // Need to multiply each component in encrypted with temp; first step is to transform to NTT form
        NttTool.nttNegacyclicHarveyRns(temp, coeffCount, coeffModulusSize, nttTables);

        for (int i = 0; i < encryptedSize; i++) {
            int rnsStartIndex = i * coeffCount * coeffModulusSize;
            for (int j = 0; j < coeffModulusSize; j++) {
                // Lazy Reduction
                NttTool.nttNegacyclicHarveyLazyPoly(encrypted.data(), encryptedSize, coeffCount, coeffModulusSize, i, j, nttTables);
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    encrypted.data(), rnsStartIndex + j * coeffCount, temp, j * coeffCount,
                    coeffCount, coeffModulus[j],
                    encrypted.data(), rnsStartIndex + j * coeffCount
                );
                NttTool.inverseNttNegacyclicHarvey(encrypted.data(), rnsStartIndex + j * coeffCount, nttTables[j]);
            }
        }

        if (parms.scheme().equals(SchemeType.CKKS)) {
            // TODO: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        }
    }

    private void multiplyPlainNtt(Ciphertext encryptedNtt, Plaintext plainNtt) {
        // Verify parameters.
        if (!plainNtt.isNttForm()) {
            throw new IllegalArgumentException("plainNtt is not in NTT form");
        }
        if (!encryptedNtt.parmsId().equals(plainNtt.parmsId())) {
            throw new IllegalArgumentException("encryptedNtt and plainNtt parameter mismatch");
        }

        // Extract encryption parameters.
        ContextData contextData = context.getContextData(encryptedNtt.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int encryptedNttSize = encryptedNtt.size();

        // Size check
        if (!Common.productFitsIn(false, encryptedNttSize, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        for (int i = 0; i < encryptedNttSize; i++) {
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                encryptedNtt.data(), encryptedNtt.getPolyOffset(i), coeffCount, coeffModulusSize,
                plainNtt.getData(), 0, coeffCount, coeffModulusSize, coeffModulus,
                encryptedNtt.data(), encryptedNtt.getPolyOffset(i), coeffCount, coeffModulusSize
            );
        }

        // Set the scale
        encryptedNtt.setScale(encryptedNtt.scale() * plainNtt.scale());
        if (!isScaleWithinBounds(encryptedNtt.scale(), contextData)) {
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

        ContextData contextData = context.getContextData(parmsId);
        if (contextData == null) {
            throw new IllegalArgumentException("parms_id is not valid for the current context");
        }
        if (plain.isNttForm()) {
            throw new IllegalArgumentException("plain is already in NTT form");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int plainCoeffCount = plain.coeffCount();

        long plainUpperHalfThreshold = contextData.plainUpperHalfThreshold();
        long[] plainUpperHalfIncrement = contextData.plainUpperHalfIncrement();

        NttTables[] nttTables = contextData.smallNttTables();

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new RuntimeException("invalid parameters");
        }
        // Resize to fit the entire NTT transformed (ciphertext size) polynomial
        // Note that the new coefficients are automatically set to 0
        plain.resize(coeffCount * coeffModulusSize);

        if (!contextData.qualifiers().isUsingFastPlainLift()) {
            // Allocate temporary space for an entire RNS polynomial
            // Slight semantic misuse of RNSIter here, but this works well
            long[] temp = new long[coeffCount * coeffModulusSize];

            for (int i = 0; i < plainCoeffCount; i++) {
                long plainValue = plain.at(i);
                if (plainValue >= plainUpperHalfThreshold) {
                    UintArithmetic.addUint(plainUpperHalfIncrement, 0, coeffModulusSize, plainValue, temp, i * coeffModulusSize);
                } else {
                    temp[i * coeffModulusSize] = plainValue;
                }
            }
            contextData.rnsTool().baseQ().decomposeArray(temp, coeffCount);

            // Copy data back to plain
            System.arraycopy(temp, 0, plain.getData(), 0, coeffCount * coeffModulusSize);
        } else {
            // Note that in this case plain_upper_half_increment holds its value in RNS form modulo the coeff_modulus
            // primes.

            // Create a "reversed" helper iterator that iterates in the reverse order both plain RNS components and
            // the plain_upper_half_increment values.
            for (int i = coeffModulusSize - 1; i >= 0; i--) {
                int startIndex = i * coeffCount;
                for (int j = 0; j < plainCoeffCount; j++) {
                    plain.getData()[startIndex + j] = plain.getData()[j] >= plainUpperHalfThreshold
                        ? plain.getData()[j] + plainUpperHalfIncrement[i] : plain.getData()[j];
                }
            }
        }

        // Transform to NTT domain
        NttTool.nttNegacyclicHarveyRns(plain.getData(), coeffCount, coeffModulusSize, nttTables);
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
        NttTool.nttNegacyclicHarveyPoly(encrypted.data(), encryptedSize, coeffCount, coeffModulusSize, nttTables);

        // Finally change the is_ntt_transformed flag
        encrypted.setIsNttForm(true);

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
        NttTool.inverseNttNegacyclicHarveyPoly(encryptedNtt.data(), encryptedSize, coeffCount, coeffModulusSize, nttTables);
        // Finally change the is_ntt_transformed flag
        encryptedNtt.setIsNttForm(false);

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
     * @param encrypted the ciphertext to apply the Galois automorphism to.
     * @param galoisElt the Galois element.
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

        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();
        int encryptedSize = encrypted.size();
        // Use key_context_data where permutation tables exist since previous runs.
        GaloisTool galoisTool = context.keyContextData().galoisTool();

        // size check
        if (!Common.productFitsIn(false, coeffModulusSize, coeffCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Check if Galois key is generated or not.
        if (!galoisKeys.hasKey(galoisElt)) {
            throw new IllegalArgumentException("Galois key not present");
        }

        long m = Common.mulSafe(coeffCount, 2L, false);

        // Verify parameters
        if (((galoisElt & 1) == 0) || Common.unsignedGeq(galoisElt, m)) {
            throw new IllegalArgumentException("Galois element is not valid");
        }
        if (encryptedSize > 2) {
            throw new IllegalArgumentException("encrypted size must be 2");
        }

        long[] temp = new long[coeffCount * coeffModulusSize];

        // DO NOT CHANGE EXECUTION ORDER OF FOLLOWING SECTION
        // BEGIN: Apply Galois for each ciphertext
        // Execution order is sensitive, since apply_galois is not inplace!
        if (parms.scheme().equals(SchemeType.BFV) || parms.scheme().equals(SchemeType.BGV)) {
            // !!! DO NOT CHANGE EXECUTION ORDER!!!

            // First transform encrypted.data(0)
            galoisTool.applyGaloisRnsIter(
                encrypted.data(), 0, encrypted.polyModulusDegree(), coeffModulusSize,
                galoisElt, coeffModulus,
                temp, 0, coeffCount, coeffModulusSize
            );

            // Copy result to encrypted.data(0)
            PolyCore.setPoly(temp, coeffCount, coeffModulusSize, encrypted.data());

            // Next transform encrypted.data(1)
            galoisTool.applyGaloisRnsIter(
                encrypted.data(), coeffCount * coeffModulusSize, coeffCount, coeffModulusSize,
                galoisElt, coeffModulus,
                temp, 0, coeffCount, coeffModulusSize
            );
        } else if (parms.scheme().equals(SchemeType.CKKS)) {
            // TODO: implement CKKS
            throw new IllegalArgumentException("scheme not implemented");
        } else {
            throw new IllegalArgumentException("scheme not implemented");
        }

        // Wipe encrypted.data(1)
        PolyCore.setZeroPoly(coeffCount, coeffModulusSize, encrypted.data(), encrypted.getPolyOffset(1));

        // END: Apply Galois for each ciphertext
        // REORDERING IS SAFE NOW

        // Calculate (temp * galois_key[0], temp * galois_key[1]) + (ct[0], 0)
        switchKeyInplace(
            encrypted, temp, 0, coeffCount, coeffModulusSize, galoisKeys, GaloisKeys.getIndex(galoisElt)
        );

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
     * @param encrypted the ciphertext to apply the Galois automorphism to.
     * @param galoisElt the Galois element.
     * @param galoisKeys the Galois keys.
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
     * @param encrypted the ciphertext to rotate.
     * @param steps the number of steps to rotate (positive left, negative right).
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
     * @param encrypted the ciphertext to rotate.
     * @param steps the number of steps to rotate (positive left, negative right).
     * @param galoisKeys the Galois keys.
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
     * @param encrypted the ciphertext to rotate.
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
     * @param encrypted the ciphertext to rotate.
     * @param galoisKeys the Galois keys.
     * @param destination the ciphertext to overwrite with the rotated result.
     */
    public void rotateColumns(Ciphertext encrypted, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateColumnsInplace(destination, galoisKeys);
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


    /**
     * Rotates plaintext vector cyclically. When using the CKKS scheme, this function rotates the encrypted plaintext
     * vector cyclically to the left (steps > 0) or to the right (steps < 0) and writes the result to the destination
     * parameter. Since the size of the batched matrix is 2-by-(N/2), where N is the degree of the polynomial modulus,
     * the number of steps to rotate must have absolute value at most N/2-1.
     *
     * @param encrypted the ciphertext to rotate.
     * @param steps the number of steps to rotate (positive left, negative right).
     * @param galoisKeys the Galois keys.
     */
    public void rotateVectorInplace(Ciphertext encrypted, int steps, GaloisKeys galoisKeys) {
        if (context.keyContextData().parms().scheme() != SchemeType.CKKS) {
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
     * @param encrypted the ciphertext to rotate.
     * @param steps the number of steps to rotate (positive left, negative right).
     * @param galoisKeys the Galois keys.
     * @param destination the ciphertext to overwrite with the rotated result.
     */
    public void rotateVector(Ciphertext encrypted, int steps, GaloisKeys galoisKeys, Ciphertext destination) {
        destination.copyFrom(encrypted);
        rotateVectorInplace(destination, steps, galoisKeys);
    }

    private void switchKeyInplace(Ciphertext encrypted, long[] targetIter, int pos, int targetN, int targetK,
                                  KswitchKeys kswitchKeys, int keySwitchKeysIndex) {
        ParmsId parmsId = encrypted.parmsId();
        ContextData contextData = context.getContextData(parmsId);
        EncryptionParameters parms = contextData.parms();
        ContextData keyContextData = context.keyContextData();
        EncryptionParameters keyParms = keyContextData.parms();
        SchemeType scheme = parms.scheme();

        // Verify parameters.
        if (!ValCheck.isMetaDataValidFor(encrypted, context) || !ValCheck.isBufferValid(encrypted)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }
        if (targetIter == null) {
            throw new IllegalArgumentException("target_iter cannot be null");
        }
        if (!context.usingKeySwitching()) {
            throw new IllegalArgumentException("keyswitching is not supported by the context");
        }

        // Don't validate all of kswitch_keys but just check the parms_id.
        if (!kswitchKeys.parmsId().equals(context.keyParmsId())) {
            throw new IllegalArgumentException("parameter mismatch");
        }

        if (keySwitchKeysIndex >= kswitchKeys.data().length) {
            throw new ArrayIndexOutOfBoundsException("keySwitchKeysIndex");
        }
        if (scheme.equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (scheme.equals(SchemeType.CKKS) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("CKKS encrypted must be in NTT form");
        }
        if (scheme.equals(SchemeType.BGV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted cannot be in NTT form");
        }

        // Extract encryption parameters.
        int coeffCount = parms.polyModulusDegree();
        int decompModulusSize = parms.coeffModulus().length;
        Modulus[] keyModulus = keyParms.coeffModulus();
        int keyModulusSize = keyModulus.length;
        int rnsModulusSize = decompModulusSize + 1;
        NttTables[] keyNttTables = keyContextData.smallNttTables();
        MultiplyUintModOperand[] modSwitchFactors = keyContextData.rnsTool().getInvQLastModQ();

        assert targetN == coeffCount;
        assert targetK == decompModulusSize;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, rnsModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Prepare input
        PublicKey[] keyVector = kswitchKeys.data()[keySwitchKeysIndex];
        int keyComponentCount = keyVector[0].data().size();

        // Check only the used component in KSwitchKeys.
        for (PublicKey eachKey : keyVector) {
            if (!ValCheck.isMetaDataValidFor(eachKey, context) || !ValCheck.isBufferValid(eachKey)) {
                throw new IllegalArgumentException("kswitch_keys is not valid for encryption parameters");
            }
        }

        // Create a copy of target_iter
        long[] tTarget = new long[coeffCount * decompModulusSize];
        UintCore.setUint(targetIter, pos, decompModulusSize * coeffCount, tTarget, 0, decompModulusSize * coeffCount);

        // In CKKS t_target is in NTT form; switch back to normal form
        if (scheme.equals(SchemeType.CKKS)) {
            NttTool.inverseNttNegacyclicHarveyRns(tTarget, coeffCount, decompModulusSize, keyNttTables);
        }

        // Temporary result
        long[] tPolyProd = new long[keyComponentCount * coeffCount * rnsModulusSize];
        for (int i = 0; i < rnsModulusSize; i++) {
            int keyIndex = (i == decompModulusSize ? keyModulusSize - 1 : i);
            // Product of two numbers is up to 60 + 60 = 120 bits, so we can sum up to 256 of them without reduction.
            int lazyReductionSummandBound = Constants.MULTIPLY_ACCUMULATE_USER_MOD_MAX;
            int lazyReductionCounter = lazyReductionSummandBound;

            // Allocate memory for a lazy accumulator (128-bit coefficients)
            long[] tPolyLazy = new long[keyComponentCount * coeffCount * 2];

            // Semantic misuse of PolyIter; this is really pointing to the data for a single RNS factor
            for (int j = 0; j < decompModulusSize; j++) {
                long[] tNtt = new long[coeffCount];
                long[] tOperand;
                int tOperandPos;

                // RNS-NTT form exists in input
                if ((scheme.equals(SchemeType.CKKS)) && (i == j)) {
                    // TODO: implement CKKS, tOperandPos = j * coeffCount may be incorrect
                    tOperand = targetIter;
                    tOperandPos = j * coeffCount;
                } else {
                    // Perform RNS-NTT conversion
                    if (keyModulus[j].value() <= keyModulus[keyIndex].value()) {
                        // No need to perform RNS conversion (modular reduction)
                        UintCore.setUint(tTarget, j * coeffCount, coeffCount, tNtt, 0, coeffCount);
                    } else {
                        // Perform RNS conversion (modular reduction)
                        PolyArithmeticSmallMod.moduloPolyCoeff(tTarget, j * coeffCount, coeffCount,
                            keyModulus[keyIndex],
                            tNtt, 0
                        );
                    }
                    // NTT conversion lazy outputs in [0, 4q)
                    NttTool.nttNegacyclicHarveyLazy(tNtt, keyNttTables[keyIndex]);
                    tOperand = tNtt;
                    tOperandPos = 0;
                }

                // Multiply with keys and modular accumulate products in a lazy fashion
                Ciphertext keyVectorJ = keyVector[j].data();
                for (int k = 0; k < keyComponentCount; k++) {
                    int keyVectorJ_K = k * keyVectorJ.getCoeffModulusSize() * keyVectorJ.polyModulusDegree();
                    int tPolyLazyK = k * coeffCount * 2;
                    if (lazyReductionCounter == 0) {
                        for (int l = 0; l < coeffCount; l++) {
                            long[] qWord = new long[2];
                            UintArithmetic.multiplyUint64(
                                tOperand[tOperandPos + l],
                                keyVectorJ.data()[keyVectorJ_K + keyIndex * keyVectorJ.polyModulusDegree() + l],
                                qWord
                            );

                            // Accumulate product of t_operand and t_key_acc to t_poly_lazy and reduce
                            long[] uint128Temp = new long[]{
                                tPolyLazy[tPolyLazyK + l * 2], tPolyLazy[tPolyLazyK + l * 2 + 1],
                            };
                            UintArithmetic.addUint128(qWord, uint128Temp, qWord);
                            tPolyLazy[tPolyLazyK + l * 2] = UintArithmeticSmallMod.barrettReduce128(qWord, keyModulus[keyIndex]);
                            tPolyLazy[tPolyLazyK + l * 2 + 1] = 0;
                        }
                    } else {
                        // Same as above but no reduction
                        for (int l = 0; l < coeffCount; l++) {
                            long[] qWord = new long[2];
                            UintArithmetic.multiplyUint64(
                                tOperand[tOperandPos + l],
                                keyVectorJ.data()[keyVectorJ_K + keyIndex * keyVectorJ.polyModulusDegree() + l],
                                qWord
                            );
                            long[] uint128Temp = new long[]{
                                tPolyLazy[tPolyLazyK + l * 2], tPolyLazy[tPolyLazyK + l * 2 + 1],
                            };
                            UintArithmetic.addUint128(qWord, uint128Temp, qWord);
                            tPolyLazy[tPolyLazyK + l * 2] = qWord[0];
                            tPolyLazy[tPolyLazyK + l * 2 + 1] = qWord[1];
                        }
                    }
                }

                if (--lazyReductionCounter == 0) {
                    lazyReductionCounter = lazyReductionSummandBound;
                }
            }
            // PolyIter pointing to the destination t_poly_prod, shifted to the appropriate modulus
            int tPolyProdIter = i * coeffCount;

            // Final modular reduction
            for (int k = 0; k < keyComponentCount; k++) {
                if (lazyReductionCounter == lazyReductionSummandBound) {
                    for (int l = 0; l < coeffCount; l++) {
                        tPolyProd[tPolyProdIter + k * coeffCount * rnsModulusSize + l]
                            = tPolyLazy[k * coeffCount * 2 + 2 * l];
                    }
                } else {
                    long[] uint128Temp = new long[2];
                    for (int l = 0; l < coeffCount; l++) {
                        uint128Temp[0] = tPolyLazy[k * coeffCount * 2 + 2 * l];
                        uint128Temp[1] = tPolyLazy[k * coeffCount * 2 + 2 * l + 1];
                        tPolyProd[tPolyProdIter + k * coeffCount * rnsModulusSize + l] =
                            UintArithmeticSmallMod.barrettReduce128(uint128Temp, keyModulus[keyIndex]);
                    }
                }
            }
        }
        // Accumulated products are now stored in t_poly_prod

        // Perform modulus switching with scaling
        for (int i = 0; i < keyComponentCount; i++) {
            if (scheme.equals(SchemeType.BGV)) {
                // TODO: implement BGV
                throw new IllegalArgumentException("unsupport BGV");
            } else {
                // Lazy reduction; this needs to be then reduced mod qi
                int tLastIndex = i * coeffCount * rnsModulusSize + decompModulusSize * coeffCount;
                NttTool.inverseNttNegacyclicHarveyLazy(tPolyProd, tLastIndex, keyNttTables[keyModulusSize - 1]);

                // Add (p-1)/2 to change from flooring to rounding.
                long qk = keyModulus[keyModulusSize - 1].value();
                long qkHalf = qk >>> 1;

                for (int j = 0; j < coeffCount; j++) {
                    tPolyProd[tLastIndex + j] = UintArithmeticSmallMod.barrettReduce64(
                        tPolyProd[tLastIndex + j] + qkHalf, keyModulus[keyModulusSize - 1]
                    );
                }

                for (int j = 0; j < decompModulusSize; j++) {
                    // (ct mod 4qk) mod qi
                    long[] tNtt = new long[coeffCount];
                    long qi = keyModulus[j].value();
                    if (qk > qi) {
                        // This cannot be spared. NTT only tolerates input that is less than 4*modulus (i.e. qk <=4*qi).
                        PolyArithmeticSmallMod.moduloPolyCoeff(
                            tPolyProd, tLastIndex, coeffCount, keyModulus[j], tNtt, 0
                        );
                    } else {
                        UintCore.setUint(tPolyProd, tLastIndex, coeffCount, tNtt, 0, coeffCount);
                    }

                    // Lazy substraction, results in [0, 2*qi), since fix is in [0, qi].
                    long fix = qi - UintArithmeticSmallMod.barrettReduce64(qkHalf, keyModulus[j]);
                    for (int k = 0; k < coeffCount; k++) {
                        tNtt[k] = tNtt[k] + fix;
                    }
                    // 用来定位 get<0, 1>(J), tPolyProd 中 第 i 个 RnsIter 中的 第 j 个 CoeffIter
                    int zeroOneJ = i * coeffCount * rnsModulusSize + j * coeffCount;
                    long qiLazy = qi << 1;
                    if (scheme.equals(SchemeType.CKKS)) {
                        // TODO: implement CKKS
                        throw new IllegalArgumentException("unsupported CKKS");
                    } else if (scheme.equals(SchemeType.BFV)) {
                        NttTool.inverseNttNegacyclicHarveyLazy(tPolyProd, zeroOneJ, keyNttTables[j]);
                    }
                    // ((ct mod qi) - (ct mod qk)) mod qi with output in [0, 2 * qi_lazy)
                    for (int k = 0; k < coeffCount; k++) {
                        tPolyProd[zeroOneJ + k] = tPolyProd[zeroOneJ + k] + qiLazy - tNtt[k];
                    }

                    // qk^(-1) * ((ct mod qi) - (ct mod qk)) mod qi
                    PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                        tPolyProd, zeroOneJ, coeffCount, modSwitchFactors[j],
                        keyModulus[j],
                        tPolyProd, zeroOneJ);

                    int zeroZeroJ = i * encrypted.polyModulusDegree() * encrypted.getCoeffModulusSize() + j * encrypted.polyModulusDegree();
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        tPolyProd, zeroOneJ, encrypted.data(), zeroZeroJ,
                        coeffCount, keyModulus[j],
                        encrypted.data(), zeroZeroJ
                    );
                }
            }
        }
    }
}
