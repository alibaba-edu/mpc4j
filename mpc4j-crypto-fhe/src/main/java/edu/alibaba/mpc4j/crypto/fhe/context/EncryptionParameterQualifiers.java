package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus.SecLevelType;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Stores a set of attributes (qualifiers) of a set of encryption parameters.
 * These parameters are mainly used internally in various parts of the library,
 * e.g., to determine which algorithmic optimizations the current support. The
 * qualifiers are automatically created by the SEALContext class, silently passed
 * on to classes such as Encryptor, Evaluator, and Decryptor, and the only way to
 * change them is by changing the encryption parameters themselves. In other
 * words, a user will never have to create their own instance of this class, and
 * in most cases never have to worry about it at all.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/context.h#L28
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/11
 */
public class EncryptionParameterQualifiers {
    /**
     * The variable parameter_error is set to:
     * <li>none, if parameters are not validated;</li>
     * <li>success, if parameters are considered valid;</li>
     * <li>other values.</li>
     */
    ErrorType parameterError;
    /**
     * Tells whether FFT can be used for polynomial multiplication. If the
     * polynomial modulus is of the form X^N+1, where N is a power of two, then
     * FFT can be used for fast multiplication of polynomials modulo the polynomial
     * modulus. In this case the variable using_fft will be set to true. However,
     * currently Microsoft SEAL requires this to be the case for the parameters
     * to be valid. Therefore, parameters_set can only be true if using_fft is true.
     */
    boolean usingFft;
    /**
     * Tells whether NTT can be used for polynomial multiplication. If the primes
     * in the coefficient modulus are congruent to 1 modulo 2N, where X^N+1 is the
     * polynomial modulus and N is a power of two, then the number-theoretic
     * transform (NTT) can be used for fast multiplications of polynomials modulo
     * the polynomial modulus and coefficient modulus. In this case the variable
     * using_ntt will be set to true. However, currently Microsoft SEAL requires
     * this to be the case for the parameters to be valid. Therefore, parameters_set
     * can only be true if using_ntt is true.
     */
    boolean usingNtt;
    /**
     * Tells whether batching is supported by the encryption parameters. If the
     * plaintext modulus is congruent to 1 modulo 2N, where X^N+1 is the polynomial
     * modulus and N is a power of two, then it is possible to use the BatchEncoder
     * class to view plaintext elements as 2-by-(N/2) matrices of integers modulo
     * the plaintext modulus. This is called batching, and allows the user to
     * operate on the matrix elements (slots) in a SIMD fashion, and rotate the
     * matrix rows and columns. When the computation is easily vectorizable, using
     * batching can yield a huge performance boost. If the encryption parameters
     * support batching, the variable using_batching is set to true.
     */
    boolean usingBatching;
    /**
     * Tells whether fast plain lift is supported by the encryption parameters.
     * A certain performance optimization in multiplication of a ciphertext by
     * a plaintext (Evaluator::multiply_plain) and in transforming a plaintext
     * element to NTT domain (Evaluator::transform_to_ntt) can be used when the
     * plaintext modulus is smaller than each prime in the coefficient modulus.
     * In this case the variable using_fast_plain_lift is set to true.
     */
    boolean usingFastPlainLift;
    /**
     * Tells whether the coefficient modulus consists of a set of primes that
     * are in decreasing order. If this is true, certain modular reductions in
     * base conversion can be omitted, improving performance.
     */
    boolean usingDescendingModulusChain;
    /**
     * Tells whether the encryption parameters are secure based on the standard
     * parameters from HomomorphicEncryption.org security standard.
     */
    SecLevelType securityLevel;

    /**
     * Creates an EncryptionParameterQualifiers with the default value.
     */
    public EncryptionParameterQualifiers() {
        parameterError = ErrorType.NONE;
        usingFft = false;
        usingNtt = false;
        usingBatching = false;
        usingFastPlainLift = false;
        usingDescendingModulusChain = false;
        securityLevel = SecLevelType.NONE;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }

    /**
     * Returns the name of parameter_error.
     *
     * @return the name of parameter_error.
     */
    public String parameterErrorName() {
        switch (parameterError) {
            case NONE:
                return "none";
            case SUCCESS:
                return "success";
            case INVALID_SCHEME:
                return "invalid scheme";
            case INVALID_COEFF_MODULUS_SIZE:
                return "invalid coeff modulus size";
            case INVALID_COEFF_MODULUS_BIT_COUNT:
                return "invalid coeff modulus bit count";
            case INVALID_COEFF_MODULUS_NO_NTT:
                return "invalid coeff modulus no Ntt";
            case INVALID_POLY_MODULUS_DEGREE:
                return "invalid poly modulus degree";
            case INVALID_POLY_MODULUS_DEGREE_NON_POWER_OF_TWO:
                return "invalid poly modulus degree non power of two";
            case INVALID_PARAMETERS_TOO_LARGE:
                return "invalid parameters too large";
            case INVALID_PARAMETERS_INSECURE:
                return "invalid parameters insecure";
            case FAILED_CREATING_RNS_BASE:
                return "failed creating rns base";
            case INVALID_PLAIN_MODULUS_BIT_COUNT:
                return "invalid plain modulus bit count";
            case INVALID_PLAIN_MODULUS_CO_PRIMALITY:
                return "invalid plain modulus co-primality";
            case INVALID_PLAIN_MODULUS_TOO_LARGE:
                return "invalid plain modulus too large";
            case INVALID_PLAIN_MODULUS_NONZERO:
                return "invalid plain modulus nonzero";
            case FAILED_CREATING_RNS_TOOL:
                return "failed creating rns tool";
            default:
                return "invalid parameter_error";
        }
    }

    /**
     * Returns a comprehensive message that interprets parameter_error.
     *
     * @return a comprehensive message that interprets parameter_error.
     */
    public String parameterErrorMessage() {
        switch (parameterError) {
            case NONE:
                return "constructed but not yet validated";
            case SUCCESS:
                return "valid";
            case INVALID_SCHEME:
                return "scheme must be BFV or CKKS or BGV";
            case INVALID_COEFF_MODULUS_SIZE:
                return "coeffModulus's primes' count is not bounded by COEFF_MOD_COUNT_MIN(MAX)";
            case INVALID_COEFF_MODULUS_BIT_COUNT:
                return "coeffModulus's primes' bit counts are not bounded by USER_MOD_BIT_COUNT_MIN(MAX)";
            case INVALID_COEFF_MODULUS_NO_NTT:
                return "coeffModulus's primes are not congruent to 1 modulo (2 * poly_modulus_degree)";
            case INVALID_POLY_MODULUS_DEGREE:
                return "polyModulusDegree is not bounded by POLY_MOD_DEGREE_MIN(MAX)";
            case INVALID_POLY_MODULUS_DEGREE_NON_POWER_OF_TWO:
                return "polyModulusDegree is not a power of two";
            case INVALID_PARAMETERS_TOO_LARGE:
                return "parameters are too large to fit in size_t type";
            case INVALID_PARAMETERS_INSECURE:
                return "parameters are not compliant with HomomorphicEncryption.org security standard";
            case FAILED_CREATING_RNS_BASE:
                return "RNSBase cannot be constructed";
            case INVALID_PLAIN_MODULUS_BIT_COUNT:
                return "coeffModulus's bit count is not bounded by PLAIN_MOD_BIT_COUNT_MIN(MAX)";
            case INVALID_PLAIN_MODULUS_CO_PRIMALITY:
                return "plainModulus is not coprime to coeffModulus";
            case INVALID_PLAIN_MODULUS_TOO_LARGE:
                return "plainModulus is not smaller than coeff_modulus";
            case INVALID_PLAIN_MODULUS_NONZERO:
                return "plain_modulus is not zero";
            case FAILED_CREATING_RNS_TOOL:
                return "RNSTool cannot be constructed";
            default:
                return "invalid parameter_error";
        }
    }

    /**
     * Tells whether parameter_error is error_type::success.
     *
     * @return true if parameter_error is error_type::success; false otherwise.
     */
    public boolean isParametersSet() {
        return parameterError == ErrorType.SUCCESS;
    }

    /**
     * Tells whether using_descending_modulus_chain is true.
     *
     * @return true if using_descending_modulus_chain is true; false otherwise.
     */
    public boolean isUsingDescendingModulusChain() {
        return usingDescendingModulusChain;
    }

    /**
     * Tells whether using_fast_plain_lift is true.
     *
     * @return true if using_fast_plain_lift is true; false otherwise.
     */
    public boolean isUsingFastPlainLift() {
        return usingFastPlainLift;
    }

    /**
     * Tells whether using_fft is true.
     *
     * @return true if using_fft is true; false otherwise.
     */
    public boolean isUsingFft() {
        return usingFft;
    }

    /**
     * Tells whether using_ntt is true.
     *
     * @return true if using_ntt is true; false otherwise.
     */
    public boolean isUsingNtt() {
        return usingNtt;
    }

    /**
     * Tells whether using_batching is true.
     *
     * @return true if using_batching is true; false otherwise.
     */
    public boolean isUsingBatching() {
        return usingBatching;
    }
}
