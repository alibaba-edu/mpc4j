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
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;

import java.util.Arrays;

/**
 * Decrypts Ciphertext objects into Plaintext objects. Constructing a Decryptor
 * requires a SEALContext with valid encryption parameters, and the secret key.
 * The Decryptor is also used to compute the invariant noise budget in a given
 * ciphertext.
 * <p>
 * NTT form
 * <p>
 * When using the BFV scheme (scheme_type::bfv), all plaintext and ciphertexts
 * should remain by default in the usual coefficient representation, i.e. not in
 * NTT form. When using the CKKS scheme (scheme_type::ckks), all plaintexts and
 * ciphertexts should remain by default in NTT form. We call these scheme-specific
 * NTT states the "default NTT form". Decryption requires the input ciphertexts
 * to be in the default NTT form, and will throw an exception if this is not the
 * case.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/decryptor.h">decryptor.h</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/26
 */
public class Decryptor {
    /**
     * the SEALContext
     */
    private final SealContext context;
    /**
     * the size of secret key array
     */
    private int secretKeyArraySize;
    /**
     * the secret key array
     */
    private long[] secretKeyArray;

    /**
     * Creates a Decryptor instance initialized with the specified SEALContext
     * and secret key.
     *
     * @param context   the SEALContext.
     * @param secretKey the secret key.
     */
    public Decryptor(SealContext context, SecretKey secretKey) {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        if (!ValCheck.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("secret key is not valid for encryption parameters");
        }
        this.context = context;
        EncryptionParameters parms = context.keyContextData().parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Set the secret_key_array to have size 1 (first power of secret)
        // and copy over data
        secretKeyArray = new long[coeffCount * coeffModulusSize];
        System.arraycopy(secretKey.data().data(), 0, secretKeyArray, 0, coeffCount * coeffModulusSize);
        secretKeyArraySize = 1;
    }

    /**
     * Decrypts a Ciphertext and stores the result in the destination parameter.
     *
     * @param encrypted   the ciphertext to decrypt.
     * @param destination the plaintext to overwrite with the decrypted ciphertext.
     */
    public void decrypt(Ciphertext encrypted, Plaintext destination) {
        // Verify that encrypted is valid.
        if (!ValCheck.isValidFor(encrypted, context)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // Additionally, check that ciphertext doesn't have trivial size
        if (encrypted.size() < Constants.SEAL_CIPHERTEXT_SIZE_MIN) {
            throw new IllegalArgumentException("encrypted is empty");
        }

        ContextData contextData = context.firstContextData();
        EncryptionParameters parms = contextData.parms();
        switch (parms.scheme()) {
            case BFV:
                bfvDecrypt(encrypted, destination);
                return;
            case CKKS:
                ckks_decrypt(encrypted, destination);
                return;
            case BGV:
                // TODO: implement BGV
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }
    }

    private void bfvDecrypt(Ciphertext encrypted, Plaintext destination) {
        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }

        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Firstly find c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q
        // This is equal to Delta m + v where ||v|| < Delta/2.
        // Add Delta / 2, and now we have something which is Delta * (m + epsilon) where epsilon < 1
        // Therefore, we can (integer) divide by Delta and the answer will round down to m.
        // Make a temp destination for all the arithmetic mod qi before calling FastBConverse
        RnsIterator tempDestModQ = RnsIterator.allocate(coeffCount, coeffModulusSize);

        // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
        // Now do the dot product of encrypted_copy and the secret key array using NTT.
        // The secret key powers are already NTT transformed.
        dot_product_ct_sk_array(encrypted, tempDestModQ);

        // Allocate a full size destination to write to
        destination.setParmsId(ParmsId.parmsIdZero());
        destination.resize(coeffCount);
        CoeffIterator destinationCoeff = CoeffIterator.wrap(destination.data(), coeffCount);

        // Divide scaling variant using BEHZ FullRNS techniques
        contextData.rnsTool().decryptScaleAndRound(tempDestModQ, destinationCoeff);

        // How many non-zero coefficients do we really have in the result?
        int plainCoeffCount = UintCore.getSignificantUint64CountUint(destination.data(), coeffCount);

        // Resize destination to appropriate size
        destination.resize(Math.max(plainCoeffCount, 1));
    }

    void ckks_decrypt(final Ciphertext encrypted, Plaintext destination) {
        if (!encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted must be in NTT form");
        }

        // We already know that the parameters are valid
        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;
        int rns_poly_uint64_count = Common.mulSafe(coeff_count, coeff_modulus_size, false);

        // Decryption consists in finding
        // c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q_1 * q_2 * q_3
        // as long as ||m + v|| < q_1 * q_2 * q_3.
        // This is equal to m + v where ||v|| is small enough.

        // Since we overwrite destination, we zeroize destination parameters
        // This is necessary, otherwise resize will throw an exception.
        destination.setParmsId(ParmsId.parmsIdZero());

        // Resize destination to appropriate size
        destination.resize(rns_poly_uint64_count);

        // Do the dot product of encrypted and the secret key array using NTT.
        dot_product_ct_sk_array(encrypted, RnsIterator.wrap(destination.data(), coeff_count, coeff_modulus_size));

        // Set destination parameters as in encrypted
        destination.setParmsId(encrypted.parmsId());
        destination.setScale(encrypted.scale());
    }

    // TODO: bgv_decrypt

    private void computeSecretKeyArray(int maxPower) {
        assert maxPower >= 1;
        assert !(secretKeyArraySize == 0 || secretKeyArray == null);

        // WARNING: This function must be called with the original context_data
        ContextData contextData = context.keyContextData();
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        int oldSize = secretKeyArraySize;
        int newSize = Math.max(oldSize, maxPower);

        if (oldSize == newSize) {
            return;
        }

        // Need to extend the array
        // Compute powers of secret key until max_power
        long[] newSecretKeyArray = new long[newSize * coeffCount * coeffModulusSize];
        PolyCore.setPolyArray(secretKeyArray, 0, oldSize, coeffCount, coeffModulusSize, newSecretKeyArray, 0);

        // Since all of the key powers in secret_key_array_ are already NTT transformed,
        // to get the next one we simply need to compute a dyadic product of the last
        // one with the first one [which is equal to NTT(secret_key_)].
        for (int i = oldSize - 1; i < (oldSize - 1) + newSize - oldSize; i++) {
            int ptr = i * coeffCount * coeffModulusSize;
            int ptrPlusOne = (i + 1) * coeffCount * coeffModulusSize;
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                newSecretKeyArray, ptr, coeffCount, coeffModulusSize,
                secretKeyArray, 0, coeffCount, coeffModulusSize, coeffModulus,
                newSecretKeyArray, ptrPlusOne, coeffCount, coeffModulusSize
            );
        }

        // Do we still need to update size?
        oldSize = secretKeyArraySize;
        newSize = Math.max(maxPower, secretKeyArraySize);

        if (oldSize == newSize) {
            return;
        }

        // Acquire new array
        secretKeyArraySize = newSize;
        secretKeyArray = newSecretKeyArray;
    }

    /**
     * Computes c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q.
     * Store result in destination in RNS form.
     *
     * @param encrypted   ciphertext.
     * @param destination destination.
     */
    private void dot_product_ct_sk_array(Ciphertext encrypted, RnsIterator destination) {
        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        int keyCoeffModulusSize = context.keyContextData().parms().coeffModulus().length;
        int encryptedSize = encrypted.size();
        boolean isNttForm = encrypted.isNttForm();

        NttTables[] nttTables = contextData.smallNttTables();

        // Make sure we have enough secret key powers computed, that is, extend sk to sk sk^2 sk^3 ... sk^n.
        computeSecretKeyArray(encryptedSize - 1);
        if (encryptedSize == 2) {

            RnsIterator secretKeyArrayRns = RnsIterator.wrap(secretKeyArray, coeffCount, coeffModulusSize);
            RnsIterator c0 = RnsIterator.wrap(encrypted.data(), encrypted.getPolyOffset(0), coeffCount, coeffModulusSize);
            RnsIterator c1 = RnsIterator.wrap(encrypted.data(), encrypted.getPolyOffset(1), coeffCount, coeffModulusSize);

            if (isNttForm) {
                for (int i = 0; i < coeffModulusSize; i++) {
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(c1.coeffIter[i], secretKeyArrayRns.coeffIter[i],
                        coeffCount,
                        coeffModulus[i],
                        destination.coeffIter[i]
                    );
                    // add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination.coeffIter[i], c0.coeffIter[i],
                        coeffCount, coeffModulus[i], destination.coeffIter[i]
                    );
                }
            } else {
                for (int i = 0; i < coeffModulusSize; i++) {
                    System.arraycopy(
                        c1.coeffIter[i].coeff(), c1.coeffIter[i].ptr(),
                        destination.coeffIter[i].coeff(), destination.coeffIter[i].ptr(),
                        coeffCount
                    );

                    // transform c_1 to NTT form
                    NttTool.nttNegacyclicHarveyLazy(destination.coeffIter[i], nttTables[i]);
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        destination.coeffIter[i], secretKeyArrayRns.coeffIter[i],
                        coeffCount, coeffModulus[i], destination.coeffIter[i]
                    );
                    // transform back
                    NttTool.inverseNttNegacyclicHarvey(destination.coeffIter[i], nttTables[i]);
                    // add c0 to the result; note that destination should be in the same (NTT) form as encrypted
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination.coeffIter[i], c0.coeffIter[i],
                        coeffCount, coeffModulus[i], destination.coeffIter[i]
                    );
                }
            }
        } else {
            // we are in the case where we have more than 2 ciphertext elements.
            // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
            // Now do the dot product of encrypted_copy and the secret key array using NTT.
            // The secret key powers are already NTT transformed.
            PolyIterator encryptedCopy = PolyIterator.allocate(encryptedSize - 1, coeffCount, coeffModulusSize);

            PolyCore.setPolyArray(
                encrypted.data(), encrypted.getPolyOffset(1),
                encryptedSize - 1, coeffCount, coeffModulusSize,
                encryptedCopy.coeff(), 0
            );

            // Transform c_1, c_2, ... to NTT form unless they already are
            if (!isNttForm) {
                NttTool.nttNegacyclicHarveyPoly(encryptedCopy, encryptedSize - 1, nttTables);
            }
            // note that here we use the keyCoeffModulusSize, not the coeffModulusSize
            PolyIterator secretKeyArrayPoly = PolyIterator.dynamicWrap(secretKeyArray, coeffCount, keyCoeffModulusSize);

            // Compute dyadic product with secret power array: c1 * s, c2 * s^2 ...
            for (int i = 0; i < (encryptedSize - 1); i++) {
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    encryptedCopy.rnsIter[i],
                    secretKeyArrayPoly.rnsIter[i],
                    coeffModulusSize,
                    coeffModulus,
                    encryptedCopy.rnsIter[i]
                );
            }

            // Aggregate all polynomials together to complete the dot product
            Arrays.fill(destination.coeff(), 0, coeffCount * coeffModulusSize, 0);
            for (int i = 0; i < (encryptedSize - 1); i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(destination, encryptedCopy.rnsIter[i], coeffModulusSize, coeffModulus, destination);
            }
            if (!isNttForm) {
                // If the input was not in NTT form, need to transform back
                NttTool.inverseNttNegacyclicHarveyRns(destination, coeffModulusSize, nttTables);
            }

            // Finally add c_0 to the result; note that destination should be in the same (NTT) form as encrypted

            // extract c0
            RnsIterator c0 = RnsIterator.wrap(encrypted.data(), encrypted.getPolyOffset(0), coeffCount, coeffModulusSize);
            // add
            PolyArithmeticSmallMod.addPolyCoeffMod(destination, c0, coeffModulusSize, coeffModulus, destination);
        }
    }

    /**
     * Computes the invariant noise budget (in bits) of a ciphertext. The
     * invariant noise budget measures the amount of room there is for the noise
     * to grow while ensuring correct decryptions. This function works only with
     * the BFV scheme.
     * <p></p>
     * <p>Invariant Noise Budget</p>
     * The invariant noise polynomial of a ciphertext is a rational coefficient
     * polynomial, such that a ciphertext decrypts correctly as long as the
     * coefficients of the invariant noise polynomial are of absolute value less
     * than 1/2. Thus, we call the infinity-norm of the invariant noise polynomial
     * the invariant noise, and for correct decryption require it to be less than
     * 1/2. If v denotes the invariant noise, we define the invariant noise budget
     * as -log2(2v). Thus, the invariant noise budget starts from some initial
     * value, which depends on the encryption parameters, and decreases when
     * computations are performed. When the budget reaches zero, the ciphertext
     * becomes too noisy to decrypt correctly.
     *
     * @param encrypted the ciphertext.
     * @return the invariant noise budget.
     */
    public int invariantNoiseBudget(Ciphertext encrypted) {
        // Verify that encrypted is valid.
        if (!ValCheck.isValidFor(encrypted, context)) {
            throw new IllegalArgumentException("encrypted is not valid for encryption parameters");
        }

        // Additionally check that ciphertext doesn't have trivial size
        if (encrypted.size() < Constants.SEAL_CIPHERTEXT_SIZE_MIN) {
            throw new IllegalArgumentException("encrypted is empty");
        }

        SchemeType scheme = context.keyContextData().parms().scheme();
        if (!scheme.equals(SchemeType.BFV) && !scheme.equals(SchemeType.BGV)) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        if (scheme.equals(SchemeType.BFV) && encrypted.isNttForm()) {
            throw new IllegalArgumentException("BFV encrypted cannot be in NTT form");
        }
        if (scheme.equals(SchemeType.BGV) && !encrypted.isNttForm()) {
            throw new IllegalArgumentException("BGV encrypted must be in NTT form");
        }

        ContextData context_data = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        Modulus plain_modulus = parms.plainModulus();
        int coeff_count = parms.polyModulusDegree();
        int coeff_modulus_size = coeff_modulus.length;
        NttTables[] ntt_tables = context_data.smallNttTables();

        // Storage for the infinity norm of noise poly
        long[] norm = new long[coeff_modulus_size];

        // Storage for noise poly
        RnsIterator noise_poly = RnsIterator.allocate(coeff_count, coeff_modulus_size);

        // Now need to compute c(s) - Delta*m (mod q)
        // Firstly find c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q
        // This is equal to Delta m + v where ||v|| < Delta/2.
        // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q
        // in destination_poly.
        // Now do the dot product of encrypted_copy and the secret key array using NTT.
        // The secret key powers are already NTT transformed.
        dot_product_ct_sk_array(encrypted, noise_poly);

        if (scheme.equals(SchemeType.BGV)) {
            NttTool.inverseNttNegacyclicHarveyRns(noise_poly, coeff_modulus_size, ntt_tables);
        }

        // Multiply by plain_modulus and reduce mod coeff_modulus to get
        // coeff_modulus()*noise.
        if (scheme.equals(SchemeType.BFV)) {
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                noise_poly, coeff_modulus_size, plain_modulus.value(), coeff_modulus, noise_poly
            );
        }

        // CRT-compose the noise
        context_data.rnsTool().baseQ().composeArray(noise_poly.coeff(), coeff_count);

        // Next we compute the infinity norm mod parms.coeff_modulus()
        StrideIterator wide_noise_poly = StrideIterator.wrap(noise_poly.coeff(), 0, coeff_modulus_size);
        poly_infty_norm_coeffmod(wide_noise_poly, coeff_count, context_data.totalCoeffModulus(), norm);

        // The -1 accounts for scaling the invariant noise by 2;
        // note that we already took plain_modulus into account in compose
        // so no need to subtract log(plain_modulus) from this
        int bit_count_diff = context_data.totalCoeffModulusBitCount() -
            UintCore.getSignificantBitCountUint(norm, coeff_modulus_size) - 1;
        return Math.max(0, bit_count_diff);
    }

    private void poly_infty_norm_coeffmod(StrideIterator poly, int coeff_count, long[] modulus, long[] result) {
        // size_t coeff_uint64_count = poly.stride();
        int coeff_uint64_count = poly.stepSize();
        // Construct negative threshold: (modulus + 1) / 2
        long[] modulus_neg_threshold = new long[coeff_uint64_count];
        UintArithmetic.halfRoundUpUint(modulus, coeff_uint64_count, modulus_neg_threshold);

        // Mod out the poly coefficients and choose a symmetric representative from [-modulus,modulus)
        UintCore.setZeroUint(coeff_uint64_count, result);
        long[] coeff_abs_value = new long[coeff_uint64_count];
        for (int I = 0; I < coeff_count; I++, poly.next()) {
            if (UintCore.isGreaterThanOrEqualUint(poly.coeff(), poly.ptr(), modulus_neg_threshold, 0, coeff_uint64_count)) {
                UintArithmetic.subUint(modulus, 0, poly.coeff(), poly.ptr(), coeff_uint64_count, coeff_abs_value);
            } else {
                UintCore.setUint(poly.coeff(), poly.ptr(), coeff_uint64_count, coeff_abs_value, 0, coeff_uint64_count);
            }
            if (UintCore.isGreaterThanUint(coeff_abs_value, result, coeff_uint64_count)) {
                // Store the new max
                UintCore.setUint(coeff_abs_value, coeff_uint64_count, result);
            }
        }
    }
}