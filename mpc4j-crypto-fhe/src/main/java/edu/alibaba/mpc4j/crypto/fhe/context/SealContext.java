package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsBase;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Performs sanity checks (validation) and pre-computations for a given set of encryption
 * parameters. While the EncryptionParameters class is intended to be a light-weight class
 * to store the encryption parameters, the SEALContext class is a heavy-weight class that
 * is constructed from a given set of encryption parameters. It validates the parameters
 * for correctness, evaluates their properties, and performs and stores the results of
 * several costly pre-computations.
 * <p></p>
 * After the user has set at least the poly_modulus, coeff_modulus, and plain_modulus
 * parameters in a given EncryptionParameters instance, the parameters can be validated
 * for correctness and functionality by constructing an instance of SEALContext. The
 * constructor of SEALContext does all of its work automatically, and concludes by
 * constructing and storing an instance of the EncryptionParameterQualifiers class, with
 * its flags set according to the properties of the given parameters. If the created
 * instance of EncryptionParameterQualifiers has the parameters_set flag set to true, the
 * given parameter set has been deemed valid and is ready to be used. If the parameters
 * were for some reason not appropriately set, the parameters_set flag will be false,
 * and a new SEALContext will have to be created after the parameters are corrected.
 * <p></p>
 * By default, SEALContext creates a chain of SEALContext::ContextData instances. The
 * first one in the chain corresponds to special encryption parameters that are reserved
 * to be used by the various key classes (SecretKey, PublicKey, etc.). These are the exact
 * same encryption parameters that are created by the user and passed to th constructor of
 * SEALContext. The functions key_context_data() and key_parms_id() return the ContextData
 * and the parms_id corresponding to these special parameters. The rest of the ContextData
 * instances in the chain correspond to encryption parameters that are derived from the
 * first encryption parameters by always removing the last one of the moduli in the
 * coeff_modulus, until the resulting parameters are no longer valid, e.g., there are no
 * more primes left. These derived encryption parameters are used by ciphertexts and
 * plaintexts and their respective ContextData can be accessed through the
 * get_context_data(parms_id_type) function. The functions first_context_data() and
 * last_context_data() return the ContextData corresponding to the first and the last
 * set of parameters in the "data" part of the chain, i.e., the second and the last element
 * in the full chain. The chain itself is a doubly linked list, and is referred to as the
 * modulus switching chain.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L250
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/11
 */
public class SealContext {
    /**
     * key_parms_id
     */
    private final ParmsId keyParmsId;
    /**
     * first_parms_id
     */
    private final ParmsId firstParmsId;
    /**
     * last_parms_id
     */
    private ParmsId lastParmsId;
    /**
     * context_data_map
     */
    private final HashMap<ParmsId, ContextData> contextDataMap = new HashMap<>();
    /**
     * is HomomorphicEncryption.org security standard enforced?
     */
    private final SecLevelType secLevel;
    /**
     * is keyswitching supported by the encryption parameters?
     */
    private final boolean usingKeySwitching;

    /**
     * Creates an instance of SEALContext and performs several pre-computations
     * on the given EncryptionParameters.
     *
     * @param params the encryption parameters.
     */
    public SealContext(EncryptionParameters params) {
        this(params, true, SecLevelType.TC128);
    }

    /**
     * Creates an instance of SEALContext and performs several pre-computations
     * on the given EncryptionParameters.
     *
     * @param params         the encryption parameters.
     * @param expandModChain determines whether the modulus switching chain should be created.
     */
    public SealContext(EncryptionParameters params, boolean expandModChain) {
        this(params, expandModChain, SecLevelType.TC128);
    }

    /**
     * Creates an instance of SEALContext and performs several pre-computations
     * on the given EncryptionParameters.
     *
     * @param parms          the encryption parameters.
     * @param expandModChain determines whether the modulus switching chain should be created.
     * @param secLevel       determines whether a specific security level should be enforced according to
     *                       HomomorphicEncryption.org security standard.
     */
    public SealContext(EncryptionParameters parms, boolean expandModChain, SecLevelType secLevel) {
        this.secLevel = secLevel;

        // Set random generator
        if (parms.randomGeneratorFactory() == null) {
            parms.setRandomGeneratorFactory(UniformRandomGeneratorFactory.defaultFactory());
        }

        // Validate parameters and add new ContextData to the map.
        // Note that this happens even if parameters are not valid.

        // First create key_parms_id_.
        contextDataMap.put(parms.parmsId(), validate(parms));
        keyParmsId = parms.parmsId();

        // Then create first_parms_id_ if the parameters are valid and there is more than one modulus in coeff_modulus.
        // This is equivalent to expanding the chain by one step. Otherwise, we set first_parms_id_ to equal
        // key_parms_id_.
        // firstParmsId [q1, q2, .., q(k-1)]
        if (!contextDataMap.get(keyParmsId).qualifiers.isParametersSet() || parms.coeffModulus().length == 1) {
            firstParmsId = keyParmsId;
        } else {
            ParmsId nextParmsId = createNextContextData(keyParmsId);
            firstParmsId = nextParmsId.isZero() ? keyParmsId : nextParmsId;
        }

        // Set last_parms_id_ to point to first_parms_id_
        lastParmsId = firstParmsId;
        // Check if key switching is available
        usingKeySwitching = !firstParmsId.equals(keyParmsId);

        // If modulus switching chain is to be created, compute the remaining parameter sets as long as they are valid
        // to use (i.e., parameters_set() == true).
        if (expandModChain && contextDataMap.get(firstParmsId).qualifiers.isParametersSet()) {
            ParmsId prevParmsId = firstParmsId;
            // 从 first [q1, q2, ..., q(k-1)] 递减计算至： [q1]
            while (contextDataMap.get(prevParmsId).parms.coeffModulus().length > 1) {
                ParmsId nextParmsId = createNextContextData(prevParmsId);
                if (nextParmsId.isZero()) {
                    break;
                }
                prevParmsId = nextParmsId;
                lastParmsId = nextParmsId;
            }
        }
        // Set the chain_index for each context_data
        int parmsCount = contextDataMap.size();
        ContextData contextDataPtr = contextDataMap.get(keyParmsId);
        // [q1] ---> [q1, q2] ---> [q1, q2, q3] --> .... --> [q1, q2, ..., qk]
        while (contextDataPtr != null) {
            contextDataPtr.chainIndex = --parmsCount;
            contextDataPtr = contextDataPtr.nextContextData;
        }
    }

    /**
     * Creates context data of a given encryption parameters.
     *
     * @param parms the encryption parameters.
     * @return context data includes pre-computation data for a given set of encryption parameters.
     */
    private ContextData validate(EncryptionParameters parms) {
        ContextData contextData = new ContextData(parms);
        contextData.qualifiers.parameterError = ErrorType.SUCCESS;

        // Is a scheme set?
        if (parms.scheme().equals(SchemeType.NONE)) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_SCHEME;
            return contextData;
        }

        Modulus[] coeffModulus = parms.coeffModulus();
        Modulus plainModulus = parms.plainModulus();

        // The number of coeff moduli is restricted to 64 to prevent unexpected behaviors
        if (coeffModulus.length > Constants.SEAL_COEFF_MOD_COUNT_MAX
            || coeffModulus.length < Constants.SEAL_COEFF_MOD_COUNT_MIN) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_SIZE;
            return contextData;
        }

        int coeffModulusSize = coeffModulus.length;
        for (Modulus modulus : coeffModulus) {
            // Check coefficient moduli bounds, each q_i should be in [2, 61)
            if (modulus.bitCount() > Constants.SEAL_USER_MOD_BIT_COUNT_MAX ||
                modulus.bitCount() < Constants.SEAL_USER_MOD_BIT_COUNT_MIN) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_BIT_COUNT;
                return contextData;
            }
        }

        // Compute the product of all coeff moduli
        contextData.totalCoeffModulus = new long[coeffModulusSize];
        long[] coeffModulusValues = Arrays.stream(coeffModulus).mapToLong(Modulus::value).toArray();
        UintArithmetic.multiplyManyUint64(coeffModulusValues, coeffModulusSize, contextData.totalCoeffModulus);
        contextData.totalCoeffModulusBitCount =
            UintCore.getSignificantBitCountUint(contextData.totalCoeffModulus, coeffModulusSize);

        // Check polynomial modulus degree and create poly_modulus, x^N + 1, N ∈ [2, 131072]
        int polyModulusDegree = parms.polyModulusDegree();
        if (polyModulusDegree < Constants.SEAL_POLY_MOD_DEGREE_MIN
            || polyModulusDegree > Constants.SEAL_POLY_MOD_DEGREE_MAX) {
            // Parameters are not valid
            contextData.qualifiers.parameterError = ErrorType.INVALID_POLY_MODULUS_DEGREE;
            return contextData;
        }
        int coeffCountPower = UintCore.getPowerOfTwo(polyModulusDegree);
        if (coeffCountPower < 0) {
            // Parameters are not valid
            contextData.qualifiers.parameterError = ErrorType.INVALID_POLY_MODULUS_DEGREE_NON_POWER_OF_TWO;
            return contextData;
        }

        // Quick sanity check
        if (!Common.productFitsIn(false, coeffModulusSize, polyModulusDegree)) {
            contextData.qualifiers.parameterError = ErrorType.INVALID_PARAMETERS_TOO_LARGE;
            return contextData;
        }

        // Polynomial modulus X^(2^k) + 1 is guaranteed at this point
        contextData.qualifiers.usingFft = true;

        // Assume parameters satisfy desired security level
        contextData.qualifiers.securityLevel = secLevel;

        // Check if the parameters are secure according to HomomorphicEncryption.org security standard
        if (contextData.totalCoeffModulusBitCount > CoeffModulus.maxBitCount(polyModulusDegree, secLevel)) {
            // Not secure according to HomomorphicEncryption.org security standard
            contextData.qualifiers.securityLevel = SecLevelType.NONE;
            if (secLevel != SecLevelType.NONE) {
                // Parameters are not valid
                contextData.qualifiers.parameterError = ErrorType.INVALID_PARAMETERS_INSECURE;
                return contextData;
            }
        }

        // Set up RNSBase for coeff_modulus
        // RNSBase's constructor may fail due to:
        //   (1) coeff_mod not coprime
        //   (2) cannot find inverse of punctured products (because of (1))
        RnsBase coeffModulusBase;
        try {
            coeffModulusBase = new RnsBase(coeffModulus);
        } catch (IllegalArgumentException e) {
            // Parameters are not valid
            contextData.qualifiers.parameterError = ErrorType.FAILED_CREATING_RNS_BASE;
            return contextData;
        }

        // Can we use NTT with coeff_modulus?
        contextData.qualifiers.usingNtt = true;
        try {
            NttTables.createNttTables(coeffCountPower, coeffModulus, contextData.smallNttTables);
        } catch (IllegalArgumentException e) {
            contextData.qualifiers.usingNtt = false;
            // Parameters are not valid
            contextData.qualifiers.parameterError = ErrorType.INVALID_COEFF_MODULUS_NO_NTT;
            return contextData;
        }

        if (parms.scheme().equals(SchemeType.BFV) || parms.scheme().equals(SchemeType.BGV)) {
            // Plain modulus must be at least 2 and at most 60 bits
            if (plainModulus.bitCount() > Constants.SEAL_PLAIN_MOD_BIT_COUNT_MAX ||
                plainModulus.bitCount() < Constants.SEAL_PLAIN_MOD_BIT_COUNT_MIN) {
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_BIT_COUNT;
                return contextData;
            }

            // Check that all coeff modulus are relatively prime to plain_modulus
            for (Modulus modulus : coeffModulus) {
                if (!Numth.areCoPrime(modulus.value(), plainModulus.value())) {
                    contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_CO_PRIMALITY;
                    return contextData;
                }
            }
            // Check that plain_modulus is smaller than total coeff modulus
            if (!UintCore.isLessThanUint(
                new long[]{plainModulus.value()}, plainModulus.uint64Count(), contextData.totalCoeffModulus,
                coeffModulusSize)) {
                // Parameters are not valid
                contextData.qualifiers.parameterError = ErrorType.INVALID_PLAIN_MODULUS_TOO_LARGE;
                return contextData;
            }

            // Can we use batching? (NTT with plain_modulus)
            contextData.qualifiers.usingBatching = true;
            try {
                // create small NTT table for plain modulus
                contextData.plainNttTables = new NttTables(coeffCountPower, plainModulus);
            } catch (IllegalArgumentException e) {
                contextData.qualifiers.usingBatching = false;
            }
            // Check for plain_lift
            // If all the small coefficient modulus are larger than plain modulus, we can quickly
            // lift plain coefficients to RNS form
            contextData.qualifiers.usingFastPlainLift = true;
            for (Modulus modulus : coeffModulus) {
                contextData.qualifiers.usingFastPlainLift &= (modulus.value() > plainModulus.value());
            }

            // Calculate coeff_div_plain_modulus (BFV-"Delta") and the remainder upper_half_increment q/t
            long[] tempCoeffDivPlainModulus = new long[coeffModulusSize];
            contextData.coeffDivPlainModulus = new MultiplyUintModOperand[parms.coeffModulus().length];
            for (int i = 0; i < parms.coeffModulus().length; i++) {
                contextData.coeffDivPlainModulus[i] = new MultiplyUintModOperand();
            }
            contextData.upperHalfIncrement = new long[coeffModulusSize];
            // extend plain_modulus's length to coeff_modulus_size
            long[] widePlainModulus = UintCore.duplicateUintIfNeeded(
                new long[]{plainModulus.value()}, plainModulus.uint64Count(), coeffModulusSize, false
            );
            // q / t, stores in temp_coeff_div_plain_modulus, remainder stores in context_data.upper_half_increment
            UintArithmetic.divideUint(
                contextData.totalCoeffModulus, widePlainModulus, coeffModulusSize,
                tempCoeffDivPlainModulus, contextData.upperHalfIncrement
            );

            // Store the non-RNS form of upper_half_increment for BFV encryption
            contextData.coeffModulusModPlainModulus = contextData.upperHalfIncrement[0];

            // Decompose coeff_div_plain_modulus into RNS factors, floor(q/t) % q_i
            coeffModulusBase.decompose(tempCoeffDivPlainModulus);

            for (int i = 0; i < coeffModulusSize; i++) {
                contextData.coeffDivPlainModulus[i].set(tempCoeffDivPlainModulus[i], coeffModulusBase.getBase(i));
            }

            // Decompose upper_half_increment into RNS factors
            coeffModulusBase.decompose(contextData.upperHalfIncrement);

            // Calculate (plain_modulus + 1) / 2.
            contextData.plainUpperHalfThreshold = (plainModulus.value() + 1) >>> 1;

            // Calculate coeff_modulus - plain_modulus.
            contextData.plainUpperHalfIncrement = new long[coeffModulusSize];
            if (contextData.qualifiers.usingFastPlainLift) {
                // Calculate coeff_modulus[i] - plain_modulus if using_fast_plain_lift
                for (int i = 0; i < coeffModulusSize; i++) {
                    contextData.plainUpperHalfIncrement[i] = coeffModulus[i].value() - plainModulus.value();
                }
            } else {
                UintArithmetic.subUint(
                    contextData.totalCoeffModulus, widePlainModulus, coeffModulusSize,
                    contextData.plainUpperHalfIncrement);
            }
        } else if (parms.scheme().equals(SchemeType.CKKS)) {
            // TODO: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        } else {
            contextData.qualifiers.parameterError = ErrorType.INVALID_SCHEME;
            return contextData;
        }

        // Create RNS Tool
        // RNSTool's constructor may fail due to:
        //   (1) auxiliary base being too large
        //   (2) cannot find inverse of punctured products in auxiliary base
        try {
            contextData.rnsTool = new RnsTool(polyModulusDegree, coeffModulusBase, plainModulus);
        } catch (IllegalArgumentException e) {
            // Parameters are not valid
            contextData.qualifiers.parameterError = ErrorType.FAILED_CREATING_RNS_TOOL;
            return contextData;
        }

        // Check whether the coefficient modulus consists of a set of primes that are in decreasing order
        contextData.qualifiers.usingDescendingModulusChain = true;
        for (int i = 0; i < coeffModulusSize - 1; i++) {
            contextData.qualifiers.usingDescendingModulusChain &= coeffModulus[i].value() > coeffModulus[i + 1].value();
        }

        // Create GaloisTool
        contextData.galoisTool = new GaloisTool(coeffCountPower);

        // Done with validation and pre-computations
        return contextData;
    }

    /**
     * Create the next context_data by dropping the last element from coeff_modulus.
     * If the new encryption parameters are not valid, returns parms_id_zero.
     * Otherwise, returns the parms_id of the next parameter and appends the next
     * context_data to the chain.
     *
     * @param prevParms parms_id of previous context_data.
     * @return parms_id of next context data.
     */
    private ParmsId createNextContextData(ParmsId prevParms) {
        // Create the next set of parameters by removing last modulus, copy to let context_data hold a new object.
        EncryptionParameters nextParms = new EncryptionParameters(contextDataMap.get(prevParms).parms);
        Modulus[] nextCoeffModulus = nextParms.coeffModulus();
        // Create the next set of parameters by removing last modulus
        Modulus[] removedLastModulus = new Modulus[nextCoeffModulus.length - 1];
        System.arraycopy(nextCoeffModulus, 0, removedLastModulus, 0, nextCoeffModulus.length - 1);
        // re-compute parms_id
        nextParms.setCoeffModulus(removedLastModulus);
        ParmsId nextParmsId = nextParms.parmsId();

        // Validate next parameters and create next context_data
        ContextData nextContextData = validate(nextParms);

        // If not valid then return zero parms_id
        if (!nextContextData.qualifiers.isParametersSet()) {
            return ParmsId.parmsIdZero();
        }

        // Add them to the context_data_map_
        contextDataMap.put(nextParmsId, nextContextData);

        // Add pointer to next context_data to the previous one (linked list)
        // Add pointer to previous context_data to the next one (doubly linked list)
        contextDataMap.get(prevParms).nextContextData = contextDataMap.get(nextParmsId);
        contextDataMap.get(nextParmsId).prevContextData = contextDataMap.get(prevParms);
        return nextParmsId;
    }

    /**
     * Returns the context data corresponding to encryption parameters with a given parms id.
     * If parameters with the given parms_id are not found then the function returns nullptr.
     *
     * @param parmsId the parms id of the encryption parameters.
     * @return the context data corresponding to encryption parameters with a given parms id.
     */
    public ContextData getContextData(ParmsId parmsId) {
        return contextDataMap.getOrDefault(parmsId, null);
    }

    /**
     * Returns the context data corresponding to the first encryption parameters that are used for data.
     *
     * @return the context data corresponding to the first encryption parameters that are used for data.
     */
    public ContextData firstContextData() {
        return contextDataMap.getOrDefault(firstParmsId, null);
    }

    /**
     * Returns the context data corresponding to encryption parameters that are used for keys.
     *
     * @return the context data corresponding to encryption parameters that are used for keys.
     */
    public ContextData keyContextData() {
        return contextDataMap.getOrDefault(keyParmsId, null);
    }

    /**
     * Returns the context data corresponding to the last encryption parameters that are used for data.
     *
     * @return the context data corresponding to the last encryption parameters that are used for data.
     */
    public ContextData lastContextData() {
        return contextDataMap.getOrDefault(lastParmsId, null);
    }

    /**
     * Returns whether the first_context_data's encryption parameters are valid.
     *
     * @return whether the first_context_data's encryption parameters are valid.
     */
    public boolean isParametersSet() {
        return firstContextData() != null && firstContextData().qualifiers.isParametersSet();
    }

    /**
     * Returns the name of encryption parameters' error.
     *
     * @return the name of encryption parameters' error.
     */
    public String parametersErrorName() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorName() : "Context is empty";
    }

    /**
     * Returns a comprehensive message that interprets encryption parameters' error.
     *
     * @return a comprehensive message that interprets encryption parameters' error.
     */
    public String parametersErrorMessage() {
        return firstContextData() != null ? firstContextData().qualifiers.parameterErrorMessage() : "Context is empty";
    }

    /**
     * Returns whether the coefficient modulus supports keyswitching. In practice,
     * support for key switching is required by Evaluator::relinearize,
     * Evaluator::apply_galois, and all rotation and conjugation operations. For
     * keyswitching to be available, the coefficient modulus parameter must consist
     * of at least two prime number factors.
     *
     * @return whether the coefficient modulus supports keyswitching
     */
    public boolean usingKeySwitching() {
        return usingKeySwitching;
    }

    /**
     * Returns a parms_id_type corresponding to the last encryption parameters that are used for data.
     *
     * @return a parms_id_type corresponding to the last encryption parameters that are used for data.
     */
    public ParmsId lastParmsId() {
        return lastParmsId;
    }

    /**
     * Returns a parms_id_type corresponding to the first encryption parameters that are used for data.
     *
     * @return a parms_id_type corresponding to the first encryption parameters that are used for data.
     */
    public ParmsId firstParmsId() {
        return firstParmsId;
    }

    /**
     * Returns a parms_id_type corresponding to the set of encryption parameters that are used for keys.
     *
     * @return a parms_id_type corresponding to the set of encryption parameters that are used for keys.
     */
    public ParmsId keyParmsId() {
        return keyParmsId;
    }

    /**
     * Class to hold pre-computation data for a given set of encryption parameters.
     * <p></p>
     * The implementation is from: https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L256
     */
    public static class ContextData {
        /**
         * encryption parameters
         */
        private final EncryptionParameters parms;
        /**
         * attributes (qualifiers) of the encryption parameters
         */
        private final EncryptionParameterQualifiers qualifiers;
        /**
         * RNS tool
         */
        private RnsTool rnsTool;
        /**
         * small NTT tables
         */
        private final NttTables[] smallNttTables;
        /**
         * plain NTT table
         */
        private NttTables plainNttTables;
        /**
         * Galois tool
         */
        private GaloisTool galoisTool;
        /**
         * q = Π_{i} q_i
         */
        private long[] totalCoeffModulus;
        /**
         * bit length of q
         */
        private int totalCoeffModulusBitCount = 0;
        /**
         * floor(q / t) mod q_i
         */
        private MultiplyUintModOperand[] coeffDivPlainModulus;
        /**
         * (t + 1) / 2
         */
        private long plainUpperHalfThreshold = 0;
        /**
         * q_i - t
         */
        private long[] plainUpperHalfIncrement;
        /**
         * (t + 1) / 2
         */
        private long[] upperHalfThreshold;
        /**
         * (q mod t) mod q_i
         */
        private long[] upperHalfIncrement;
        /**
         * q mod t
         */
        private long coeffModulusModPlainModulus = 0;
        /**
         * the context data corresponding to the previous parameters in the modulus switching chain
         */
        private ContextData prevContextData;
        /**
         * the context data corresponding to the next parameters in the modulus switching chain
         */
        private ContextData nextContextData;
        /**
         * the index of the parameter set in a chain
         */
        private int chainIndex = 0;

        /**
         * Creates the ContextData for the given encryption parameters.
         *
         * @param parms the given encryption parameters.
         */
        private ContextData(EncryptionParameters parms) {
            this.parms = parms;
            qualifiers = new EncryptionParameterQualifiers();
            smallNttTables = new NttTables[parms.coeffModulus().length];
        }

        /**
         * Returns a reference to the underlying encryption parameters.
         *
         * @return a reference to the underlying encryption parameters.
         */
        public EncryptionParameters parms() {
            return parms;
        }

        /**
         * Returns the parms_id of the current parameters.
         *
         * @return the parms_id of the current parameters.
         */
        public ParmsId parmsId() {
            return parms.parmsId();
        }

        /**
         * Returns a reference of EncryptionParameterQualifiers corresponding to the
         * current encryption parameters. Note that to change the qualifiers it is
         * necessary to create a new instance of SEALContext once appropriate changes
         * to the encryption parameters have been made.
         *
         * @return a reference of EncryptionParameterQualifiers.
         */
        public EncryptionParameterQualifiers qualifiers() {
            return qualifiers;
        }

        /**
         * Returns the pre-computed product of all primes in the coefficient modulus.
         * The security of the encryption parameters largely depends on the bit-length
         * of this product, and on the degree of the polynomial modulus.
         *
         * @return the pre-computed product of all primes in the coefficient modulus.
         */
        public long[] totalCoeffModulus() {
            return totalCoeffModulus;
        }

        /**
         * Returns the significant bit count of the total coefficient modulus.
         *
         * @return the significant bit count of the total coefficient modulus.
         */
        public int totalCoeffModulusBitCount() {
            return totalCoeffModulusBitCount;
        }

        /**
         * Returns a pointer to the RNSTool.
         *
         * @return a pointer to the RNSTool.
         */
        public RnsTool rnsTool() {
            return rnsTool;
        }

        /**
         * Returns a pointer to the NTT tables for R_q = (R_{q_1}, ..., R_{q_k}).
         *
         * @return a pointer to the NTT tables for R_q.
         */
        public NttTables[] smallNttTables() {
            return smallNttTables;
        }

        /**
         * Returns a pointer to the NTT tables for R_t.
         *
         * @return a pointer to the NTT tables for R_t.
         */
        public NttTables plainNttTables() {
            return plainNttTables;
        }

        /**
         * Returns a pointer to the GaloisTool.
         *
         * @return a pointer to the GaloisTool.
         */
        public GaloisTool galoisTool() {
            return galoisTool;
        }

        /**
         * Return a pointer to BFV "Delta", i.e. coefficient modulus divided by
         * plaintext modulus, i.e., (q / t).
         *
         * @return a pointer to BFV "Delta".
         */
        public MultiplyUintModOperand[] coeffDivPlainModulus() {
            return coeffDivPlainModulus;
        }

        /**
         * Return the threshold for the upper half of integers modulo plain_modulus.
         * This is simply (plain_modulus + 1) / 2, i.e., (t + 1) / 2.
         *
         * @return the threshold for the upper half of integers modulo plain_modulus.
         */
        public long plainUpperHalfThreshold() {
            return plainUpperHalfThreshold;
        }

        /**
         * Return a pointer to the plaintext upper half increment, i.e. coeff_modulus
         * minus plain_modulus. The upper half increment is represented as an integer
         * for the full product coeff_modulus if using_fast_plain_lift is false and is
         * otherwise represented modulo each of the coeff_modulus primes in order.
         * <p></p>
         * This is simply q_i - t, where q_i is the i-th RNS base.
         *
         * @return a pointer to the plaintext upper half increment.
         */
        public long[] plainUpperHalfIncrement() {
            return plainUpperHalfIncrement;
        }

        /**
         * Return a pointer to the upper half threshold with respect to the total
         * coefficient modulus. This is needed in CKKS decryption.
         *
         * @return a pointer to the upper half threshold.
         */
        public long[] upperHalfThreshold() {
            return upperHalfThreshold;
        }

        /**
         * Return a pointer to the upper half increment used for computing Delta*m
         * and converting the coefficients to modulo coeff_modulus. For example,
         * t-1 in plaintext should change into
         * <p>q - Delta = Delta*t + r_t(q) - Delta = Delta*(t-1) + r_t(q)</p>
         * so multiplying the message by Delta is not enough and requires also an
         * addition of r_t(q). This is precisely the upper_half_increment. Note that
         * this operation is only done for negative message coefficients, i.e. those
         * that exceed plain_upper_half_threshold.
         *
         * @return a pointer to the upper half increment.
         */
        public long[] upperHalfIncrement() {
            return upperHalfIncrement;
        }

        /**
         * Return the non-RNS form of upper_half_increment which is q mod t.
         *
         * @return q mod t.
         */
        public long coeffModulusModPlainModulus() {
            return coeffModulusModPlainModulus;
        }

        /**
         * Returns the context data corresponding to the previous parameters in the modulus switching chain.
         * If the current data is the first one in the chain, then the result is nullptr.
         *
         * @return the context data corresponding to the previous parameters in the modulus switching chain.
         */
        public ContextData prevContextData() {
            return prevContextData;
        }

        /**
         * Returns the context data corresponding to the next parameters in the modulus switching chain.
         * If the current data is the last one in the chain, then the result is nullptr.
         *
         * @return the context data corresponding to the next parameters in the modulus switching chain.
         */
        public ContextData nextContextData() {
            return nextContextData;
        }

        /**
         * Returns the index of the parameter set in a chain. The initial parameters have index 0
         * and the index increases sequentially in the parameter chain.
         *
         * @return the index of the parameter set in a chain.
         */
        public int chainIndex() {
            return chainIndex;
        }
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}