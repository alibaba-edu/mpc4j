package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmetic;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.util.Arrays;

/**
 * Decrypts Ciphertext objects into Plaintext objects. Constructing a Decryptor
 * requires a SEALContext with valid encryption parameters, and the secret key.
 * The Decryptor is also used to compute the invariant noise budget in a given
 * ciphertext.
 * <p></p>
 * <p>NTT form</p>
 * When using the BFV scheme (scheme_type::bfv), all plaintext and ciphertexts
 * should remain by default in the usual coefficient representation, i.e. not in
 * NTT form. When using the CKKS scheme (scheme_type::ckks), all plaintexts and
 * ciphertexts should remain by default in NTT form. We call these scheme-specific
 * NTT states the "default NTT form". Decryption requires the input ciphertexts
 * to be in the default NTT form, and will throw an exception if this is not the
 * case.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/decryptor.h
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
        System.arraycopy(secretKey.data().getData(), 0, secretKeyArray, 0, coeffCount * coeffModulusSize);
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
            case BGV:
                // TODO: implement BGV
            case CKKS:
                // TODO: implement CKKS
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
        long[] tempDestModQ = new long[coeffCount * coeffModulusSize];

        // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
        // Now do the dot product of encrypted_copy and the secret key array using NTT.
        // The secret key powers are already NTT transformed.
        dotProductCtSkArray(encrypted, tempDestModQ);

        // Allocate a full size destination to write to
        destination.setParmsId(ParmsId.parmsIdZero());
        destination.resize(coeffCount);

        // Divide scaling variant using BEHZ FullRNS techniques
        contextData.rnsTool().decryptScaleAndRound(tempDestModQ, coeffCount, destination.getData());

        // How many non-zero coefficients do we really have in the result?
        int plainCoeffCount = UintCore.getSignificantUint64CountUint(destination.getData(), coeffCount);

        // Resize destination to appropriate size
        destination.resize(Math.max(plainCoeffCount, 1));
    }

    // TODO: ckks_decrypt

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
     * @param encrypted ciphertext.
     * @param destination destination.
     */
    private void dotProductCtSkArray(Ciphertext encrypted, long[] destination) {
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
            int c0Ptr = 0;
            int c1Ptr = encrypted.getPolyOffset(1);
            if (isNttForm) {
                for (int i = 0; i < coeffModulusSize; i++) {
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encrypted.data(), c1Ptr + i * coeffCount, secretKeyArray, i * coeffCount,
                        coeffCount, coeffModulus[i], destination, i * coeffCount
                    );
                    // add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination, i * coeffCount, encrypted.data(), c0Ptr + i * coeffCount,
                        coeffCount, coeffModulus[i], destination, i * coeffCount
                    );
                }
            } else {
                for (int i = 0; i < coeffModulusSize; i++) {
                    UintCore.setUint(encrypted.data(), c1Ptr + i * coeffCount, coeffCount, destination, i * coeffCount, coeffCount);
                    // transform c_1 to NTT form
                    NttTool.nttNegAcyclicHarveyLazyRns(destination, coeffCount, coeffModulusSize, i, nttTables);
                    // put < c_1 * s > mod q in destination
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        destination, i * coeffCount, secretKeyArray, i * coeffCount,
                        coeffCount, coeffModulus[i], destination, i * coeffCount
                    );
                    // transform back
                    NttTool.inverseNttNegacyclicHarvey(destination, i * coeffCount, nttTables[i]);
                    // add c0 to the result; note that destination should be in the same (NTT) form as encrypted
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination, i * coeffCount, encrypted.data(), c0Ptr + i * coeffCount,
                        coeffCount, coeffModulus[i], destination, i * coeffCount
                    );
                }
            }
        } else {
            // we are in the case where we have more than 2 ciphertext elements.
            // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q in destination
            // Now do the dot product of encrypted_copy and the secret key array using NTT.
            // The secret key powers are already NTT transformed.
            long[] encryptedCopy = new long[(encryptedSize - 1) * coeffCount * coeffModulusSize];
            PolyCore.setPolyArray(
                encrypted.data(), encrypted.getPolyOffset(1),
                encryptedSize - 1, coeffCount, coeffModulusSize,
                encryptedCopy, 0
            );

            // Transform c_1, c_2, ... to NTT form unless they already are
            if (!isNttForm) {
                for (int i = 0; i < (encryptedSize - 1); i++) {
                    NttTool.nttNegacyclicHarveyPoly(encryptedCopy, encryptedSize, coeffCount, coeffModulusSize, i, nttTables);
                }
            }

            // Compute dyadic product with secret power array: c1 * s, c2 * s^2 ...
            for (int i = 0; i < (encryptedSize - 1); i++) {
                int ptr1 = i * coeffCount * coeffModulusSize;
                int ptr2 = i * coeffCount * keyCoeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    // 处理单个 CoeffIter
                    PolyArithmeticSmallMod.dyadicProductCoeffMod(
                        encryptedCopy, ptr1 + j * coeffCount, secretKeyArray, ptr2 + j * coeffCount,
                        coeffCount, coeffModulus[j], encryptedCopy, ptr1 + j * coeffCount
                    );
                }
            }

            // Aggregate all polynomials together to complete the dot product
            Arrays.fill(destination, 0, coeffCount * coeffModulusSize, 0);
            for (int i = 0; i < (encryptedSize - 1); i++) {
                int ptr = i * coeffCount * coeffModulusSize;
                for (int j = 0; j < coeffModulusSize; j++) {
                    PolyArithmeticSmallMod.addPolyCoeffMod(
                        destination, j * coeffCount, encryptedCopy, ptr + j * coeffCount,
                        coeffCount, coeffModulus[j], destination, j * coeffCount
                    );
                }
            }
            if (!isNttForm) {
                // If the input was not in NTT form, need to transform back
                for (int i = 0; i < coeffModulusSize; i++) {
                    NttTool.inverseNttNegacyclicHarvey(destination, i * coeffCount, nttTables[i]);
                }
            }

            // Finally add c_0 to the result; note that destination should be in the same (NTT) form as encrypted
            for (int i = 0; i < coeffModulusSize; i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(
                    destination, i * coeffCount, encrypted.data(), i * coeffCount,
                    coeffCount, coeffModulus[i], destination, i * coeffCount
                );
            }
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
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        if (encrypted.isNttForm()) {
            throw new IllegalArgumentException("encrypted cannot be in NTT form");
        }

        ContextData contextData = context.getContextData(encrypted.parmsId());
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        Modulus plainModulus = parms.plainModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Storage for the infinity norm of noise poly
        long[] norm = new long[coeffModulusSize];

        // Storage for noise poly
        long[] noisePoly = new long[coeffCount * coeffModulusSize];
        // Now need to compute c(s) - Delta*m (mod q)
        // Firstly find c_0 + c_1 *s + ... + c_{count-1} * s^{count-1} mod q
        // This is equal to Delta m + v where ||v|| < Delta/2.
        // put < (c_1 , c_2, ... , c_{count-1}) , (s,s^2,...,s^{count-1}) > mod q
        // in destination_poly.
        // Now do the dot product of encrypted_copy and the secret key array using NTT.
        // The secret key powers are already NTT transformed.
        dotProductCtSkArray(encrypted, noisePoly);

        // Multiply by plain_modulus and reduce mod coeff_modulus to get
        // coeff_modulus()*noise.
        if (scheme.equals(SchemeType.BFV)) {
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(
                noisePoly, 0, coeffCount, coeffModulusSize,
                plainModulus.value(), coeffModulus, noisePoly,
                0, coeffCount, coeffModulusSize
            );
        }

        // CRT-compose the noise
        contextData.rnsTool().baseQ().composeArray(noisePoly, coeffCount);

        // Next we compute the infinity norm mod parms.coeff_modulus()
        polyInftyNormCoeffModStrideIter(noisePoly, coeffModulusSize, coeffCount, contextData.totalCoeffModulus(), norm);

        // The -1 accounts for scaling the invariant noise by 2;
        // note that we already took plain_modulus into account in compose
        // so no need to subtract log(plain_modulus) from this
        int bifCountDiff = contextData.totalCoeffModulusBitCount() - UintCore.getSignificantBitCountUint(norm, coeffModulusSize) - 1;
        return Math.max(0, bifCountDiff);
    }

    private void polyInftyNormCoeffModStrideIter(long[] poly, int coeffUint64Count, int coeffCount, long[] modulus,
                                                 long[] result) {
        // Construct negative threshold: (modulus + 1) / 2
        long[] modulusNegThreshold = new long[coeffUint64Count];
        UintArithmetic.halfRoundUpUint(modulus, coeffUint64Count, modulusNegThreshold);

        // Mod out the poly coefficients and choose a symmetric representative from [-modulus,modulus)
        UintCore.setZeroUint(coeffUint64Count, result);
        long[] coeffAbsValue = new long[coeffUint64Count];
        for (int i = 0; i < coeffCount; i++) {
            if (UintCore.isGreaterThanOrEqualUint(poly, i * coeffUint64Count, modulusNegThreshold, 0, coeffUint64Count)) {
                UintArithmetic.subUint(modulus, 0, poly, i * coeffUint64Count, coeffUint64Count, coeffAbsValue);
            } else {
                UintCore.setUint(poly, i * coeffUint64Count, coeffUint64Count, coeffAbsValue, 0, coeffUint64Count);
            }
            if (UintCore.isGreaterThanUint(coeffAbsValue, result, coeffUint64Count)) {
                // Store the new max
                UintCore.setUint(coeffAbsValue, coeffUint64Count, result);
            }
        }
    }
}