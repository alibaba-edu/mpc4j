package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

/**
 * Generates matching secret key and public key. An existing KeyGenerator can
 * also at any time be used to generate relinearization keys and Galois keys.
 * Constructing a KeyGenerator requires only a SEALContext.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/keygenerator.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class KeyGenerator {
    /**
     * the SEALContext
     */
    private final SealContext context;
    /**
     * the secret key
     */
    private SecretKey secretKey;
    /**
     * array size of secret keys
     */
    private int secretKeyArraySize = 0;
    /**
     * the secret key array
     */
    private long[] secretKeyArray;
    /**
     * whether the secret key is generated
     */
    private boolean skGenerated;

    /**
     * Creates a KeyGenerator initialized with the specified SEALContext.
     *
     * @param context the SEALContext.
     */
    public KeyGenerator(SealContext context) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
        // Secret key has not been generated
        skGenerated = false;
        // Generate the secret and public key
        generateSk(false);
    }

    /**
     * Creates an KeyGenerator instance initialized with the specified SEALContext
     * and specified previously secret key. This can e.g. be used to increase
     * the number of relinearization keys from what had earlier been generated,
     * or to generate Galois keys in case they had not been generated earlier.
     *
     * @param context   the SEALContext.
     * @param secretKey a previously generated secret key.
     */
    public KeyGenerator(SealContext context, SecretKey secretKey) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        if (!ValCheck.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("secret key is not valid for encryption parameters");
        }
        this.context = context;
        this.secretKey = secretKey;
        skGenerated = true;
        // only need to compute secretKeyArray
        generateSk(true);
    }

    private void generateSk(boolean isInitialized) {
        // Extract encryption parameters.
        ContextData contextData = context.keyContextData();
        EncryptionParameters params = contextData.parms();
        Modulus[] coeffModulus = params.coeffModulus();
        int coeffCount = params.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        if (!isInitialized) {
            secretKey = new SecretKey();
            skGenerated = false;
            secretKey.data().resize(Common.mulSafe(coeffCount, coeffModulusSize, false));

            // Generate secret key
            RingLwe.samplePolyTernary(params.randomGeneratorFactory().create(), params, secretKey.data().getData());

            // Transform the secret s into NTT representation.
            NttTables[] nttTables = contextData.smallNttTables();
            NttTool.nttNegacyclicHarveyRns(secretKey.data().getData(), coeffCount, coeffModulusSize, nttTables);

            // Set the parms_id for secret key
            secretKey.setParmsId(contextData.parmsId());
        }

        // Set the secret_key_array to have size 1 (first power of secret)
        secretKeyArray = new long[coeffCount * coeffModulusSize];
        System.arraycopy(secretKey.data().getData(), 0, secretKeyArray, 0, coeffCount * coeffModulusSize);
        secretKeyArraySize = 1;

        // Secret key has been generated
        skGenerated = true;
    }

    /**
     * Generates a public key and stores the result in destination. Every time
     * this function is called, a new public key will be generated.
     *
     * @param destination the public key to overwrite with the generated public key.
     */
    public void createPublicKey(PublicKey destination) {
        generatePk(false, destination);
    }

    /**
     * Generates and returns a public key as a serializable object. Every time
     * this function is called, a new public key will be generated.
     * <p></p>
     * Half of the key data is pseudo-randomly generated from a seed to reduce
     * the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     *
     * @return a serializable public key.
     */
    public SealSerializable<PublicKey> createPublicKey() {
        PublicKey publicKey = new PublicKey();
        generatePk(true, publicKey);
        return new SealSerializable<>(publicKey);
    }

    /**
     * Generates relinearization keys and stores the result in destination.
     * Every time this function is called, new relinearization keys will be
     * generated.
     *
     * @param destination the relinearization keys to overwrite with the generated relinearization keys.
     */
    public void createRelinKeys(RelinKeys destination) {
        createRelinKeys(1, false, destination);
    }

    /**
     * Generates and returns relinearization keys as a serializable object.
     * Every time this function is called, new relinearization keys will be
     * generated.
     * <p></p>
     * Half of the key data is pseudo-randomly generated from a seed to reduce
     * the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     *
     * @return the relinearization keys.
     */
    public SealSerializable<RelinKeys> createRelinKeys() {
        RelinKeys relinKeys = new RelinKeys();
        createRelinKeys(1, true, relinKeys);
        return new SealSerializable<>(relinKeys);
    }

    /**
     * Generates Galois keys and stores the result in destination. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * This function creates specific Galois keys that can be used to apply
     * specific Galois automorphisms on encrypted data. The user needs to give
     * as input a vector of Galois elements corresponding to the keys that are
     * to be created.
     * <p></p>
     * The Galois elements are odd integers in the interval [1, M-1], where
     * M = 2*N, and N = poly_modulus_degree. Used with batching, a Galois element
     * 3^i % M corresponds to a cyclic row rotation i steps to the left, and
     * a Galois element 3^(N/2-i) % M corresponds to a cyclic row rotation i
     * steps to the right. The Galois element M-1 corresponds to a column rotation
     * (row swap) in BFV, and complex conjugation in CKKS. In the polynomial view
     * (not batching), a Galois automorphism by a Galois element p changes
     * Enc(plain(x)) to Enc(plain(x^p)).
     *
     * @param galoisElts  the Galois elements for which to generate keys.
     * @param destination the Galois keys to overwrite with the generated Galois keys.
     */
    public void createGaloisKeys(int[] galoisElts, GaloisKeys destination) {
        createGaloisKeys(galoisElts, false, destination);
    }

    /**
     * Generates and returns Galois keys as a serializable object. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * Half of the key data is pseudo-randomly generated from a seed to reduce
     * the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * This function creates specific Galois keys that can be used to apply
     * specific Galois automorphisms on encrypted data. The user needs to give
     * as input a vector of Galois elements corresponding to the keys that are
     *  to be created.
     * <p></p>
     * The Galois elements are odd integers in the interval [1, M-1], where
     * M = 2*N, and N = poly_modulus_degree. Used with batching, a Galois element
     * 3^i % M corresponds to a cyclic row rotation i steps to the left, and
     * a Galois element 3^(N/2-i) % M corresponds to a cyclic row rotation i
     * steps to the right. The Galois element M-1 corresponds to a column rotation
     * (row swap) in BFV, and complex conjugation in CKKS. In the polynomial view
     * (not batching), a Galois automorphism by a Galois element p changes
     * Enc(plain(x)) to Enc(plain(x^p)).
     *
     * @param galoisElts the Galois elements for which to generate keys.
     * @return the generated serializable Galois keys.
     */
    public SealSerializable<GaloisKeys> createGaloisKeys(int[] galoisElts) {
        GaloisKeys galoisKeys = new GaloisKeys();
        createGaloisKeys(galoisElts, true, galoisKeys);
        return new SealSerializable<>(galoisKeys);
    }

    /**
     * Generates Galois keys and stores the result in destination. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * The user needs to give as input a vector of desired Galois rotation step
     * counts, where negative step counts correspond to rotations to the right
     * and positive step counts correspond to rotations to the left. A step
     * count of zero can be used to indicate a column rotation in the BFV scheme
     * and complex conjugation in the CKKS scheme.
     *
     * @param steps       the rotation step counts for which to generate keys.
     * @param destination the Galois keys to overwrite with the generated Galois keys.
     */
    public void createStepGaloisKeys(int[] steps, GaloisKeys destination) {
        if (!context.keyContextData().qualifiers().isUsingBatching()) {
            throw new IllegalArgumentException("encryption parameters do not support batching");
        }
        createGaloisKeys(context.keyContextData().galoisTool().getEltsFromSteps(steps), false, destination);
    }

    /**
     * Generates and returns Galois keys as a serializable object. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * Half of the key data is pseudo-randomly generated from a seed to reduce
     * the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * The user needs to give as input a vector of desired Galois rotation step
     * counts, where negative step counts correspond to rotations to the right
     * and positive step counts correspond to rotations to the left. A step
     * count of zero can be used to indicate a column rotation in the BFV scheme
     * and complex conjugation in the CKKS scheme.
     *
     * @param steps the rotation step counts for which to generate keys.
     * @return the generated serializable Galois keys.
     */
    public SealSerializable<GaloisKeys> createStepGaloisKeys(int[] steps) {
        GaloisKeys galoisKeys = new GaloisKeys();
        createGaloisKeys(context.keyContextData().galoisTool().getEltsFromSteps(steps), true, galoisKeys);
        return new SealSerializable<>(galoisKeys);
    }

    /**
     * Generates Galois keys and stores the result in destination. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * This function creates logarithmically many (in degree of the polynomial
     * modulus) Galois keys that is sufficient to apply any Galois automorphism
     * (e.g., rotations) on encrypted data. Most users will want to use this
     * overload of the function.
     * <p></p>
     * Precisely it generates 2*log(n)-1 number of Galois keys where n is the
     * degree of the polynomial modulus. When used with batching, these keys
     * support direct left and right rotations of power-of-2 steps of rows in BFV
     * or vectors in CKKS and rotation of columns in BFV or conjugation in CKKS.
     *
     * @param destination the Galois keys to overwrite with the generated Galois keys.
     */
    public void createGaloisKeys(GaloisKeys destination) {
        createGaloisKeys(context.keyContextData().galoisTool().getEltsAll(), destination);
    }

    /**
     * Generates and returns Galois keys as a serializable object. Every time
     * this function is called, new Galois keys will be generated.
     * <p></p>
     * Half of the key data is pseudo-randomly generated from a seed to reduce
     * the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * This function creates logarithmically many (in degree of the polynomial
     * modulus) Galois keys that is sufficient to apply any Galois automorphism
     * (e.g., rotations) on encrypted data. Most users will want to use this
     * overload of the function.
     * <p></p>
     * Precisely it generates 2*log(n)-1 number of Galois keys where n is the
     * degree of the polynomial modulus. When used with batching, these keys
     * support direct left and right rotations of power-of-2 steps of rows in BFV
     * or vectors in CKKS and rotation of columns in BFV or conjugation in CKKS.
     *
     * @return the generated serializable Galois keys.
     */
    public SealSerializable<GaloisKeys> createStepGaloisKeys() {
        GaloisKeys galoisKeys = new GaloisKeys();
        createGaloisKeys(context.keyContextData().galoisTool().getEltsAll(), true, galoisKeys);
        return new SealSerializable<>(galoisKeys);
    }

    /**
     * Generates new public key matching to existing secret key.
     *
     * @param saveSeed  if true, save seed instead of a polynomial.
     * @param publicKey the public key to overwrite with the generated public key.
     */
    private void generatePk(boolean saveSeed, PublicKey publicKey) {
        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate public key for unspecified secret key");
        }
        ContextData contextData = context.keyContextData();
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        // size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("valid parameters");
        }
        // Generate ciphertext: (c[0], c[1]) = ([-(as + e)]_q, a)
        RingLwe.encryptZeroSymmetric(secretKey, context, contextData.parmsId(), true, saveSeed, publicKey.data());
        // set the parmsId
        publicKey.setParmsId(contextData.parmsId());
    }

    private void generateOneKeySwitchKey(long[] newKeys, PublicKey[][] destinations, int index, boolean saveSeed) {
        if (!context.usingKeySwitching()) {
            throw new IllegalArgumentException("key switching is not supported by the context");
        }
        int coeffCount = context.keyContextData().parms().polyModulusDegree();
        // q_1, ..., q_{k-1}
        int decomposeModCount = context.firstContextData().parms().coeffModulus().length;
        ContextData keyContextData = context.keyContextData();
        EncryptionParameters keyParms = keyContextData.parms();
        // q_1, ..., q_{k-1}, q_k
        Modulus[] keyModulus = keyParms.coeffModulus();

        // Size check
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        destinations[index] = new PublicKey[decomposeModCount];
        for (int i = 0; i < decomposeModCount; i++) {
            destinations[index][i] = new PublicKey();
        }

        for (int i = 0; i < decomposeModCount; i++) {
            long[] temp = new long[coeffCount];
            // Generate ciphertext: (c[0], c[1]) = ([-(as + e)]_q, a), represented in NTT form. RnsBase is q_1, ..., q_{k-1}, q_k
            RingLwe.encryptZeroSymmetric(
                secretKey, context, keyContextData.parmsId(), true, saveSeed, destinations[index][i].data()
            );
            // factor = q_{k-1} mod q_i
            long factor = UintArithmeticSmallMod.barrettReduce64(
                keyModulus[keyModulus.length - 1].value(), keyModulus[i]
            );
            // temp = key' * factor mod q_i
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(
                newKeys, i * coeffCount, coeffCount, factor, keyModulus[i], temp, 0
            );
            // ([-(as + e)]_q_0, a), ..., ([-(as + e) + new_keys * q_k]_q_i, a), ...,([-(as + e)]_q_{k-1}, a)
            PolyArithmeticSmallMod.addPolyCoeffMod(
                destinations[index][i].data().data(), i * coeffCount, temp, 0,
                coeffCount, keyModulus[i], destinations[index][i].data().data(), i * coeffCount
            );
        }
    }

    private void createRelinKeys(@SuppressWarnings("SameParameterValue") int count, boolean saveSeed, RelinKeys destination) {
        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate relinearization keys for unspecified secret key");
        }
        if (count == 0 || count > Constants.SEAL_CIPHERTEXT_SIZE_MAX - 2) {
            throw new IllegalArgumentException("invalid count");
        }

        // Extract encryption parameters.
        ContextData contextData = context.keyContextData();
        EncryptionParameters parms = contextData.parms();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = parms.coeffModulus().length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // Make sure we have enough secret keys, sk^1, ..., sk^{count+1}
        computeSecretKeyArray(contextData, count + 1);

        // Assume the secret key is already transformed into NTT form.
        // destination[0][0] = [(-(a*s + e) + q_k * s^2)_q_1, a], [(-(a*s + e))_q_2, a], ..., [(-(a*s + e))_q_{k-1}, a], [(-(a*s + e))_qk, a]
        // destination[0][1] = [(-(a*s + e))_q_1, a], [(-(a*s + e) + q_k * s^2)_q2, a], ..., [(-(a*s + e))_q_{k-1}, a], [(-(a*s + e))_qk, a]
        // ...
        // destination[0][k-2] = [(-(a*s + e))_q_1, a], [(-(a*s + e))_q_2, a], ..., [(-(a*s + e) + q_{k} * s^2)_q_{k-1}, a], [(-(a*s + e))_qk, a]
        generateKeySwitchKeys(secretKeyArray, coeffCount * coeffModulusSize, count, destination, saveSeed);

        // Set the parms_id
        destination.setParmsId(contextData.parmsId());
    }

    private void createGaloisKeys(int[] galoisElts, boolean saveSeed, GaloisKeys destination) {
        // Check to see if secret key and public key have been generated
        if (!skGenerated) {
            throw new IllegalArgumentException("cannot generate Galois keys for unspecified secret key");
        }

        ContextData contextData = context.keyContextData();
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        GaloisTool galoisTool = contextData.galoisTool();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // The max number of keys is equal to number of coefficients
        destination.resize(coeffCount);

        for (int galoisElt : galoisElts) {
            // Verify coprime conditions. all galois_element should be odd number.
            if ((galoisElt & 1) == 0 || (galoisElt >= coeffCount << 1)) {
                throw new IllegalArgumentException("Galois element is not valid");
            }

            // Do we already have the key?
            if (destination.hasKey(galoisElt)) {
                continue;
            }

            // Rotate secret key for each coeff_modulus
            long[] rotatedSecretKey = RnsIterator.allocateZeroRns(coeffCount, coeffModulusSize);
            galoisTool.applyGaloisNttRnsIter(
                secretKey.data().getData(), coeffCount, coeffModulusSize, galoisElt, rotatedSecretKey, coeffCount, coeffModulusSize
            );

            // Initialize Galois key
            // This is the location in the galois_keys vector
            int index = GaloisKeys.getIndex(galoisElt);

            // Create Galois keys.
            generateOneKeySwitchKey(rotatedSecretKey, destination.data(), index, saveSeed);
        }

        // Set the parms_id
        destination.setParmsId(contextData.parmsId());
    }

    /**
     * Returns a reference to the secret key.
     *
     * @return a reference to the secret key.
     */
    public SecretKey secretKey() {
        if (!skGenerated) {
            throw new IllegalArgumentException("secret key has not been generated");
        }
        return secretKey;
    }

    private void computeSecretKeyArray(ContextData contextData, int maxPower) {
        if (maxPower < 1) {
            throw new IllegalArgumentException("max_power must be at least 1");
        }
        if (secretKeyArraySize <= 0 || secretKeyArray == null) {
            throw new IllegalArgumentException("secret_key_array_ is uninitialized");
        }

        // Extract encryption parameters.
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, maxPower)) {
            throw new IllegalArgumentException("invalid parameter");
        }

        int oldSize = secretKeyArraySize;
        int newSize = Math.max(oldSize, maxPower);

        if (oldSize == newSize) {
            return;
        }

        // Need to extend the array
        // Compute powers of secret key until max_power
        long[] newSecretKeyArray = new long[newSize * coeffCount * coeffModulusSize];
        PolyCore.setPolyArray(secretKeyArray, oldSize, coeffCount, coeffModulusSize, newSecretKeyArray);

        // Since all the key powers in secret_key_array_ are already NTT transformed, to get the next one we simply
        // need to compute a dyadic product of the last one with the first one [which is equal to NTT(secret_key_)].
        // compute sk^1, sk^2 = sk * sk, sk^3 = sk^2 * sk
        int oldStartIndex = (oldSize - 1) * coeffCount * coeffModulusSize;
        for (int i = oldSize - 1; i < oldSize - 1 + newSize - oldSize; i++) {
            int newStartIndex = i * coeffCount * coeffModulusSize;
            int newStartIndexPlusOne = (i + 1) * coeffCount * coeffModulusSize;
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(
                newSecretKeyArray, newStartIndex, coeffCount, coeffModulusSize,
                secretKeyArray, oldStartIndex, coeffCount, coeffModulusSize, coeffModulus,
                newSecretKeyArray, newStartIndexPlusOne, coeffCount, coeffModulusSize
            );
        }

        // Acquire new array
        secretKeyArraySize = newSize;
        secretKeyArray = newSecretKeyArray;
    }

    private void generateOneKeySwitchKey(long[] newKeys, int pos,
                                         PublicKey[][] destinations, int index, boolean saveSeed) {
        if (!context.usingKeySwitching()) {
            throw new IllegalArgumentException("keyswitching is not supported by the context");
        }

        int coeffCount = context.keyContextData().parms().polyModulusDegree();
        int decomposeModCount = context.firstContextData().parms().coeffModulus().length;
        ContextData keyContextData = context.keyContextData();
        EncryptionParameters keyParms = keyContextData.parms();
        Modulus[] keyModulus = keyParms.coeffModulus();

        // Size check
        if (!Common.productFitsIn(false, coeffCount, decomposeModCount)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        // KSwitchKeys data allocated
        destinations[index] = new PublicKey[decomposeModCount];

        for (int i = 0; i < decomposeModCount; i++) {
            destinations[index][i] = new PublicKey();
        }
        for (int i = 0; i < decomposeModCount; i++) {
            long[] temp = new long[coeffCount];
            // destination[index][i] = (c[0], c[1]) = ([-(as + e)], a) under RNS base in NTT form
            RingLwe.encryptZeroSymmetric(secretKey, context, keyContextData.parmsId(), true, saveSeed, destinations[index][i].data());
            // factor = q_k mod q_i
            long factor = UintArithmeticSmallMod.barrettReduce64(keyModulus[keyModulus.length - 1].value(), keyModulus[i]);
            // new_keys * q_k mod q_i
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(newKeys, pos + i * coeffCount, coeffCount, factor, keyModulus[i], temp, 0);
            // ([-(as + e)]_q_0, a), ..., ([-(as + e) + new_keys * q_k]_q_i, a), ...,([-(as + e)]_q_{k-1}, a)
            PolyArithmeticSmallMod.addPolyCoeffMod(destinations[index][i].data().data(), i * coeffCount, temp, 0, coeffCount, keyModulus[i], destinations[index][i].data().data(), i * coeffCount);
        }
    }

    private void generateKeySwitchKeys(long[] newKeys, int offset, int numKeys, KswitchKeys destination, boolean saveSeed) {
        int coeffCount = context.keyContextData().parms().polyModulusDegree();
        ContextData keyContextData = context.keyContextData();
        EncryptionParameters keyParms = keyContextData.parms();
        int coeffModulusSize = keyParms.coeffModulus().length;

        // Size check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, numKeys)) {
            throw new IllegalArgumentException("invalid parameters");
        }

        destination.resize(numKeys);
        for (int i = 0; i < numKeys; i++) {
            generateOneKeySwitchKey(newKeys, offset + i * coeffCount * coeffModulusSize, destination.data(), i, saveSeed);
        }
    }
}