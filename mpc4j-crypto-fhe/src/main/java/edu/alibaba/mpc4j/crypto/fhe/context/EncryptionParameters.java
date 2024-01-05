package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;


import java.io.*;
import java.util.Arrays;

/**
 * Represents user-customizable encryption scheme settings. The parameters (most
 * importantly poly_modulus, coeff_modulus, plain_modulus) significantly affect
 * the performance, capabilities, and security of the encryption scheme. Once
 * an instance of EncryptionParameters is populated with appropriate parameters,
 * it can be used to create an instance of the SEALContext class, which verifies
 * the validity of the parameters, and performs necessary pre-computations.
 * <p></p>
 * Picking appropriate encryption parameters is essential to enable a particular
 * application while balancing performance and security. Some encryption settings
 * will not allow some inputs (e.g. attempting to encrypt a polynomial with more
 * coefficients than poly_modulus or larger coefficients than plain_modulus) or,
 * support the desired computations (with noise growing too fast due to too large
 * plain_modulus and too small coeff_modulus).
 * <p></p>
 * <p>params_id</p>
 * The EncryptionParameters class maintains at all times a 256-bit hash of the
 * currently set encryption parameters called the parms_id. This hash acts as
 * a unique identifier of the encryption parameters and is used by all further
 * objects created for these encryption parameters. The parms_id is not intended
 * to be directly modified by the user but is used internally for pre-computation
 * data lookup and input validity checks. In modulus switching the user can use
 * the parms_id to keep track of the chain of encryption parameters. The parms_id
 * is not exposed in the public API of EncryptionParameters, but can be accessed
 * through the SEALContext::ContextData class once the SEALContext has been created.
 * <p></p>
 * <p>warning</p>
 * Choosing inappropriate encryption parameters may lead to an encryption
 * scheme that is not secure, does not perform well, and/or does not support the
 * input and computation of the desired application. We highly recommend consulting
 * an expert in RLWE-based encryption when selecting parameters, as this is where
 * inexperienced users seem to most often make critical mistakes.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L86
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/30
 */
public class EncryptionParameters implements SealCloneable {
    /**
     * scheme
     */
    private SchemeType scheme;
    /**
     * poly_modulus_degree
     */
    private int polyModulusDegree;
    /**
     * coeff_modulus
     */
    private Modulus[] coeffModulus;
    /**
     * random_generator_factory
     */
    private UniformRandomGeneratorFactory randomGeneratorFactory;
    /**
     * plain_modulus
     */
    private Modulus plainModulus;
    /**
     * parms_id
     */
    private ParmsId parmsId;

    /**
     * Creates an empty set of encryption parameters.
     */
    public EncryptionParameters() {
        this(SchemeType.NONE);
    }

    /**
     * Creates an empty set of encryption parameters.
     *
     * @param scheme the encryption scheme to be used.
     */
    public EncryptionParameters(SchemeType scheme) {
        init(scheme);
    }

    /**
     * We separate init() in Java to ensure serialization.
     *
     * @param scheme the encryption scheme to be used.
     */
    private void init(SchemeType scheme) {
        // Check that a valid scheme is given
        if (!isValidScheme(scheme)) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        this.scheme = scheme;
        polyModulusDegree = 0;
        coeffModulus = new Modulus[0];
        randomGeneratorFactory = null;
        plainModulus = null;
        parmsId = ParmsId.parmsIdZero();
        computeParmsId();
    }

    /**
     * create encryption parameters with given scheme type.
     *
     * @param scheme the encryption scheme to be used.
     */
    public EncryptionParameters(int scheme) {
        this(SchemeType.getByValue(scheme));
    }

    /**
     * Creates a copy of a given instance of EncryptionParameters.
     *
     * @param copy the EncryptionParameters to copy from.
     */
    public EncryptionParameters(EncryptionParameters copy) {
        this.scheme = copy.scheme;
        this.polyModulusDegree = copy.polyModulusDegree;
        this.coeffModulus = Arrays.stream(copy.coeffModulus)
            .map(Modulus::new)
            .toArray(Modulus[]::new);
        this.plainModulus = new Modulus(copy.plainModulus);
        this.randomGeneratorFactory = copy.randomGeneratorFactory;
        this.parmsId = new ParmsId(copy.parmsId);
    }

    /**
     * Sets the degree of the polynomial modulus parameter to the specified value.
     * The polynomial modulus directly affects the number of coefficients in
     * plaintext polynomials, the size of ciphertext elements, the computational
     * performance of the scheme (bigger is worse), and the security level (bigger
     * is better). In Microsoft SEAL the degree of the polynomial modulus must be
     * a power of 2 (e.g.  1024, 2048, 4096, 8192, 16384, or 32768).
     *
     * @param polyModulusDegree the new polynomial modulus degree.
     */
    public void setPolyModulusDegree(int polyModulusDegree) {
        if (scheme.equals(SchemeType.NONE) && polyModulusDegree != 0) {
            throw new IllegalArgumentException("polyModulusDegree is not supported for this scheme");
        }

        // Set the degree
        this.polyModulusDegree = polyModulusDegree;

        // Re-compute the parms_id
        computeParmsId();
    }

    /**
     * Sets the coefficient modulus parameter. The coefficient modulus consists
     * of a list of distinct prime numbers, and is represented by a vector of
     * Modulus objects. The coefficient modulus directly affects the size
     * of ciphertext elements, the amount of computation that the scheme can
     * perform (bigger is better), and the security level (bigger is worse). In
     * our implementation(ref SEAL-4.0) each of the prime numbers in the coefficient modulus must
     * be at most 60 bits, and must be congruent to 1 modulo 2*poly_modulus_degree.
     *
     * @param coeffModulus the new coefficient modulus.
     */
    public void setCoeffModulus(Modulus[] coeffModulus) {
        // Check that a scheme is set
        if (scheme.equals(SchemeType.NONE)) {
            if (coeffModulus.length != 0) {
                throw new IllegalArgumentException("coeffModulus is not supported for this scheme");
            }
        } else if (coeffModulus.length > Constants.SEAL_COEFF_MOD_COUNT_MAX
            || coeffModulus.length < Constants.SEAL_COEFF_MOD_COUNT_MIN) {
            throw new IllegalArgumentException("coeffModulus size is invalid");
        }

        this.coeffModulus = coeffModulus;

        // Re-compute the parms_id
        computeParmsId();
    }

    /**
     * Sets the coefficient modulus parameter. The coefficient modulus consists
     * of a list of distinct prime numbers, and is represented by a vector of
     * Modulus objects. The coefficient modulus directly affects the size
     * of ciphertext elements, the amount of computation that the scheme can
     * perform (bigger is better), and the security level (bigger is worse). In
     * our implementation(ref SEAL-4.0) each of the prime numbers in the coefficient modulus must
     * be at most 60 bits, and must be congruent to 1 modulo 2*poly_modulus_degree.
     *
     * @param coeffModulus the new coefficient modulus.
     */
    public void setCoeffModulus(long[] coeffModulus) {
        setCoeffModulus(Modulus.createModulus(coeffModulus));
    }

    /**
     * Sets the plaintext modulus parameter. The plaintext modulus is an integer
     * modulus represented by the Modulus class. The plaintext modulus
     * determines the largest coefficient that plaintext polynomials can represent.
     * It also affects the amount of computation that the scheme can perform
     * (bigger is worse). In our implementation(ref SEAL-4.0), the plaintext modulus can be at most
     * 60 bits long, but can otherwise be any integer. Note, however, that some
     * features (e.g. batching) require the plaintext modulus to be of a particular form.
     *
     * @param plainModulus the new plaintext modulus.
     */
    public void setPlainModulus(Modulus plainModulus) {
        // Check that scheme is BFV
        if (scheme != SchemeType.BFV && scheme != SchemeType.BGV && !plainModulus.isZero()) {
            throw new IllegalArgumentException("plainModulus is not supported for this scheme");
        }

        this.plainModulus = plainModulus;

        // Re-compute the parms_id
        computeParmsId();
    }

    /**
     * Sets the plaintext modulus parameter. The plaintext modulus is an integer
     * modulus represented by the Modulus class. The plaintext modulus
     * determines the largest coefficient that plaintext polynomials can represent.
     * It also affects the amount of computation that the scheme can perform
     * (bigger is worse). In our implementation(ref SEAL-4.0), the plaintext modulus can be at most
     * 60 bits long, but can otherwise be any integer. Note, however, that some
     * features (e.g. batching) require the plaintext modulus to be of a particular form.
     *
     * @param plainModulus the new plaintext modulus.
     */
    public void setPlainModulus(long plainModulus) {
        setPlainModulus(new Modulus(plainModulus));
    }

    /**
     * Sets the random number generator factory to use for encryption. By default,
     * the random generator is set to UniformRandomGeneratorFactory::default_factory().
     * Setting this value allows a user to specify a custom random number generator
     * source.
     *
     * @param randomGeneratorFactory pointer to the random generator factory
     */
    public void setRandomGeneratorFactory(UniformRandomGeneratorFactory randomGeneratorFactory) {
        this.randomGeneratorFactory = randomGeneratorFactory;
    }

    /**
     * Returns the encryption scheme type.
     *
     * @return the encryption scheme type.
     */
    public SchemeType scheme() {
        return scheme;
    }

    /**
     * Returns the degree of the polynomial modulus parameter.
     *
     * @return the degree of the polynomial modulus parameter.
     */
    public int polyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * Returns a reference to the currently set coefficient modulus parameter.
     *
     * @return a reference to the currently set coefficient modulus parameter.
     */
    public Modulus[] coeffModulus() {
        return coeffModulus;
    }

    /**
     * Returns a reference to the currently set plaintext modulus parameter.
     *
     * @return a reference to the currently set plaintext modulus parameter.
     */
    public Modulus plainModulus() {
        return plainModulus;
    }

    /**
     * Returns a pointer to the random number generator factory to use for encryption.
     *
     * @return a pointer to the random number generator factory to use for encryption.
     */
    public UniformRandomGeneratorFactory randomGeneratorFactory() {
        return randomGeneratorFactory;
    }

    /**
     * Returns a reference to the parms_id of the current parameters.
     *
     * @return a reference to the parms_id of the current parameters.
     */
    public ParmsId parmsId() {
        return parmsId;
    }

    private void computeParmsId() {
        int coeffModulusSize = coeffModulus == null ? 0 : coeffModulus.length;
        int plainModulusUint64Count = plainModulus == null ? 0 : plainModulus.uint64Count();
        int totalUint64Count = Common.addSafe(1, 1, true, coeffModulusSize, plainModulusUint64Count);

        long[] paramData = new long[totalUint64Count];
        int paramDataPtr = 0;

        // Write the scheme identifier
        paramData[paramDataPtr++] = scheme.getValue();

        // Write the poly_modulus_degree. Note that it will always be positive.
        paramData[paramDataPtr++] = polyModulusDegree;

        if (coeffModulusSize > 0) {
            for (Modulus modulus : coeffModulus) {
                paramData[paramDataPtr++] = modulus.value();
            }
        }

        if (plainModulus != null && plainModulus.uint64Count() > 0) {
            paramData[paramDataPtr] = plainModulus.value();
        }

        HashFunction.hash(paramData, totalUint64Count, parmsId.value);

        // Did we somehow manage to get a zero block as result? This is reserved for
        // plaintexts to indicate non-NTT-transformed form.
        if (parmsId.isZero()) {
            throw new RuntimeException("parms_id cannot be zero");
        }
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }

    /**
     * check the validity of the scheme type.
     *
     * @param scheme scheme type.
     * @return whether the scheme type is valid.
     */
    private boolean isValidScheme(SchemeType scheme) {
        switch (scheme) {
            case NONE:
            case BFV:
                return true;
            case BGV:
            case CKKS:
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EncryptionParameters that = (EncryptionParameters) o;
        return new EqualsBuilder()
            .append(parmsId, that.parmsId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return parmsId.hashCode();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        stream.writeByte(scheme.getValue());
        stream.writeLong(polyModulusDegree);
        stream.writeLong(coeffModulus.length);

        for (Modulus mod : coeffModulus) {
            mod.save(outputStream, ComprModeType.NONE);
        }

        // Only BFV and BGV uses plain_modulus but save it in any case for simplicity
        plainModulus.save(outputStream, ComprModeType.NONE);
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        DataInputStream stream = new DataInputStream(inputStream);

        // Read the scheme identifier
        // This constructor will throw if scheme is invalid
        init(SchemeType.getByValue(stream.readByte()));

        // Read the poly_modulus_degree
        int polyModulusDegree64 = (int) stream.readLong();

        // Only check for upper bound; lower bound is zero for scheme_type::none
        if (polyModulusDegree64 > Constants.SEAL_POLY_MOD_DEGREE_MAX) {
            throw new IllegalArgumentException("poly_modulus_degree is invalid");
        }

        // Read the coeff_modulus size
        int coeffModulusSize64 = (int) stream.readLong();

        // Only check for upper bound; lower bound is zero for scheme_type::none
        if (coeffModulusSize64 > Constants.SEAL_COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("coeff_modulus is invalid");
        }

        // Read the coeff_modulus
        Modulus[] coeffModulus = new Modulus[coeffModulusSize64];
        for (int i = 0; i < coeffModulusSize64; i++) {
            coeffModulus[i] = new Modulus();
            coeffModulus[i].load(context, inputStream);
        }

        // Read the plain_modulus
        Modulus plainModulus = new Modulus();
        plainModulus.load(context, stream);

        // Supposedly everything worked so set the values of member variables
        setPolyModulusDegree(polyModulusDegree64);
        setCoeffModulus(coeffModulus);

        // Only BFV and BGV uses plain_modulus; set_plain_modulus checks that for
        // other schemes it is zero
        setPlainModulus(plainModulus);

        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        return unsafeLoad(context, inputStream);
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
    }
}
