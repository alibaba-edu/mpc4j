package edu.alibaba.mpc4j.crypto.fhe.seal;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.seal.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;

/**
 * Encrypts Plaintext objects into Ciphertext objects. Constructing an Encryptor
 * requires a SEALContext with valid encryption parameters, the public key and/or
 * the secret key. If an Encrytor is given a secret key, it supports symmetric-key
 * encryption. If an Encryptor is given a public key, it supports asymmetric-key
 * encryption.
 * <p>
 * NTT form
 * <p>
 * When using the BFV/BGV scheme (scheme_type::bfv/bgv), all plaintext and ciphertexts should
 * remain by default in the usual coefficient representation, i.e. not in NTT form.
 * When using the CKKS scheme (scheme_type::ckks), all plaintexts and ciphertexts
 * should remain by default in NTT form. We call these scheme-specific NTT states
 * the "default NTT form". Decryption requires the input ciphertexts to be in
 * the default NTT form, and will throw an exception if this is not the case.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptor.h">encryptor.h</a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/25
 */
public class Encryptor {
    /**
     * the SEALContext
     */
    private final SealContext context;
    /**
     * public key
     */
    private PublicKey publicKey;
    /**
     * secret key
     */
    private SecretKey secretKey;

    /**
     * Creates an Encryptor instance initialized with the specified SEALContext
     * and public key.
     *
     * @param context   the SEALContext.
     * @param publicKey the public key.
     */
    public Encryptor(SealContext context, PublicKey publicKey) {
        this.context = context;
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        setPublicKey(publicKey);
        EncryptionParameters parms = context.keyContextData().parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    /**
     * Creates an Encryptor instance initialized with the specified SEALContext
     * and secret key.
     *
     * @param context   the SEALContext.
     * @param secretKey the secret key.
     */
    public Encryptor(SealContext context, SecretKey secretKey) {
        this.context = context;
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        setSecretKey(secretKey);
        EncryptionParameters parms = context.keyContextData().parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    /**
     * Creates an Encryptor instance initialized with the specified SEALContext,
     * secret key, and public key.
     *
     * @param context   the SEALContext.
     * @param publicKey the public key.
     * @param secretKey the secret key.
     */
    public Encryptor(SealContext context, PublicKey publicKey, SecretKey secretKey) {
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }
        this.context = context;
        setPublicKey(publicKey);
        setSecretKey(secretKey);
        EncryptionParameters parms = context.keyContextData().parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffCount = parms.polyModulusDegree();
        int coeffModulusSize = coeffModulus.length;

        // Quick sanity check
        if (!Common.productFitsIn(false, coeffCount, coeffModulusSize, 2)) {
            throw new IllegalArgumentException("invalid parameters");
        }
    }

    /**
     * Give a new instance of public key.
     *
     * @param publicKey the public key.
     */
    public void setPublicKey(PublicKey publicKey) {
        if (!ValCheck.isValidFor(publicKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.publicKey = publicKey;
    }

    /**
     * Give a new instance of secret key.
     *
     * @param secretKey the secret key.
     */
    public void setSecretKey(SecretKey secretKey) {
        if (!ValCheck.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("public key is not valid for encryption parameters");
        }
        this.secretKey = secretKey;
    }

    /**
     * Encrypts a plaintext with the public key and store the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * <p>1) in BFV/BGV, the highest (data) level in the modulus switching chain,</p>
     * <p>2) in CKKS, the encryption parameters of the plaintext.</p>
     *
     * @param plain       the plaintext to encrypt.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encrypt(Plaintext plain, Ciphertext destination) {
        encrypt_internal(plain, true, false, destination);
    }

    /**
     * Encrypts a plaintext with the public key and returns the ciphertext as
     * a serializable object.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * <p>1) in BFV/BGV, the highest (data) level in the modulus switching chain,</p>
     * <p>2) in CKKS, the encryption parameters of the plaintext.</p>
     *
     * @param plain the plaintext to encrypt.
     * @return the ciphertext.
     */
    public Ciphertext encrypt(Plaintext plain) {
        Ciphertext destination = new Ciphertext();
        encrypt_internal(plain, true, false, destination);
        return destination;
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZero(Ciphertext destination) {
        encryptZero(context.firstParmsId(), destination);
    }

    /**
     * Encrypts a zero plaintext with the public key and returns the ciphertext
     * as a serializiable object.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id.
     *
     * @param parmsId the parms_id for the resulting ciphertext.
     */
    public SealSerializable<Ciphertext> encryptZero(ParmsId parmsId) {
        Ciphertext destination = new Ciphertext();
        encrypt_zero_internal(parmsId, true, true, destination);
        return new SealSerializable<>(destination);
    }

    /**
     * Encrypts a zero plaintext with the public key and stores the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id.
     *
     * @param parmsId     the parms_id for the resulting ciphertext.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZero(ParmsId parmsId, Ciphertext destination) {
        encrypt_zero_internal(parmsId, true, false, destination);
    }


    /**
     * Encrypts a zero plaintext with the public key and returns the ciphertext
     * as a serializable object.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     */
    public SealSerializable<Ciphertext> encryptZero() {
        return encryptZero(context.firstParmsId());
    }

    /**
     * Encrypts a plaintext with the secret key and stores the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * <p>1) in BFV/BGV, the highest (data) level in the modulus switching chain,</p>
     * <p>2) in CKKS, the encryption parameters of the plaintext.</p>
     *
     * @param plain       the plaintext to encrypt.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptSymmetric(Plaintext plain, Ciphertext destination) {
        encrypt_internal(plain, false, false, destination);
    }

    /**
     * Encrypts a plaintext with the secret key and returns the ciphertext as
     * a serializable object.
     * <p></p>
     * Half of the ciphertext data is pseudo-randomly generated from a seed to
     * reduce the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to:
     * <p>1) in BFV/BGV, the highest (data) level in the modulus switching chain,</p>
     * <p>2) in CKKS, the encryption parameters of the plaintext.</p>
     *
     * @param plain the plaintext to encrypt.
     * @return a serializable ciphertext.
     */
    public SealSerializable<Ciphertext> encryptSymmetric(Plaintext plain) {
        Ciphertext destination = new Ciphertext();
        encrypt_internal(plain, false, true, destination);
        return new SealSerializable<>(destination);
    }

    /**
     * Encrypts a zero plaintext with the secret key and stores the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id.
     *
     * @param parmsId     the parms_id for the resulting ciphertext.
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZeroSymmetric(ParmsId parmsId, Ciphertext destination) {
        encrypt_zero_internal(parmsId, false, false, destination);
    }

    /**
     * Encrypts a zero plaintext with the secret key and returns the ciphertext
     * as a serializable object.
     * <p></p>
     * Half of the ciphertext data is pseudo-randomly generated from a seed to
     * reduce the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * given parms_id.
     *
     * @param parmsId the parms_id for the resulting ciphertext.
     * @return a serializable ciphertext.
     */
    public SealSerializable<Ciphertext> encryptZeroSymmetric(ParmsId parmsId) {
        Ciphertext destination = new Ciphertext();
        encrypt_zero_internal(parmsId, false, true, destination);
        return new SealSerializable<>(destination);
    }

    /**
     * Encrypts a zero plaintext using the secret key and stores the result in
     * destination.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * highest (data) level in the modulus switching chain.
     *
     * @param destination the ciphertext to overwrite with the encrypted plaintext.
     */
    public void encryptZeroSymmetric(Ciphertext destination) {
        encryptZeroSymmetric(context.firstParmsId(), destination);
    }

    /**
     * Encrypts a zero plaintext with the secret key and returns the ciphertext
     * as a serializable object.
     * <p></p>
     * Half of the ciphertext data is pseudo-randomly generated from a seed to
     * reduce the object size. The resulting serializable object cannot be used
     * directly and is meant to be serialized for the size reduction to have an
     * impact.
     * <p></p>
     * The encryption parameters for the resulting ciphertext correspond to the
     * ighest (data) level in the modulus switching chain.
     *
     * @return a serializable ciphertext.
     */
    public SealSerializable<Ciphertext> encryptZeroSymmetric() {
        return encryptZeroSymmetric(context.firstParmsId());
    }

    private void encrypt_zero_internal(ParmsId parms_id, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {
        ContextData context_data = context.getContextData(parms_id);
        EncryptionParameters parms = context_data.parms();
        int coeff_modulus_size = parms.coeffModulus().length;
        int coeff_count = parms.polyModulusDegree();
        boolean is_ntt_form = false;

        if (parms.scheme().equals(SchemeType.CKKS) || parms.scheme().equals(SchemeType.BGV)) {
            is_ntt_form = true;
        } else if (!parms.scheme().equals(SchemeType.BFV)) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        // Resize destination and save results
        destination.resize(context, parms_id, 2);

        // If asymmetric key encryption
        if (isAsymmetric) {
            ContextData prev_context_data = context_data.prevContextData();
            if (prev_context_data != null) {
                // Requires modulus switching
                ParmsId prev_parms_id = prev_context_data.parmsId();
                RnsTool rns_tool = prev_context_data.rnsTool();

                // Zero encryption without modulus switching
                Ciphertext temp = new Ciphertext();
                RingLwe.encryptZeroAsymmetric(publicKey, context, prev_parms_id, is_ntt_form, temp);

                // Modulus switching
                PolyIterator tempIterator = PolyIterator.fromCiphertext(temp);
                for (int i = 0; i < temp.size(); i++) {
                    if (parms.scheme().equals(SchemeType.CKKS)) {
                        // temp in ciphertext RnsBase
                        rns_tool.divideAndRoundQLastNttInplace(tempIterator.rnsIter[i], prev_context_data.smallNttTables());
                    } else if (parms.scheme().equals(SchemeType.BFV)) {
                        // bfv switch-to-next
                        rns_tool.divideAndRoundQLastInplace(tempIterator.rnsIter[i]);
                    } else {
                        // bgv switch-to-next
                        // TODO: implement BGV
                        throw new IllegalArgumentException("now cannot support BGV");
                    }
                    PolyCore.setPoly(
                        temp.data(), i * coeff_count * temp.getCoeffModulusSize(), coeff_count, coeff_modulus_size,
                        destination.data(), i * coeff_count * destination.getCoeffModulusSize());
                }

                destination.setParmsId(parms_id);
                destination.setNttForm(is_ntt_form);
                destination.setScale(temp.scale());
                destination.setCorrectionFactor(temp.correctionFactor());
            } else {
                // Does not require modulus switching
                RingLwe.encryptZeroAsymmetric(publicKey, context, parms_id, is_ntt_form, destination);
            }
        } else {
            // Does not require modulus switching
            RingLwe.encryptZeroSymmetric(secretKey, context, parms_id, is_ntt_form, saveSeed, destination);
        }
    }

    private void encrypt_internal(Plaintext plain, boolean is_asymmetric, boolean save_seed, Ciphertext destination) {
        // Minimal verification that the keys are set
        if (is_asymmetric) {
            if (!ValCheck.isMetaDataValidFor(publicKey, context)) {
                throw new IllegalArgumentException("public key is not set");
            }
        } else {
            if (!ValCheck.isMetaDataValidFor(secretKey, context)) {
                throw new IllegalArgumentException("secret key is not set");
            }
        }

        // Verify that plain is valid
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }

        SchemeType scheme = context.keyContextData().parms().scheme();
        if (scheme.equals(SchemeType.BFV)) {
            if (plain.isNttForm()) {
                throw new IllegalArgumentException("plain cannot be in NTT form");
            }
            encrypt_zero_internal(context.firstParmsId(), is_asymmetric, save_seed, destination);

            // Multiply plain by scalar coeff_div_plaintext and reposition if in upper-half.
            // Result gets added into the c_0 term of ciphertext (c_0,c_1).
            RnsIterator c0 = RnsIterator.wrap(destination.data(), destination.polyModulusDegree(), destination.getCoeffModulusSize());
            ScalingVariant.multiplyAddPlainWithScalingVariant(plain, context.firstContextData(), c0);
        } else if (scheme.equals(SchemeType.CKKS)) {
            if (!plain.isNttForm()) {
                throw new IllegalArgumentException("plain must be in NTT form");
            }
            ContextData context_data = context.getContextData(plain.parmsId());
            if (context_data == null) {
                throw new IllegalArgumentException("plain is not valid for encryption parameters");
            }
            encrypt_zero_internal(plain.parmsId(), is_asymmetric, save_seed, destination);

            EncryptionParameters parms = context.getContextData(plain.parmsId()).parms();
            Modulus[] coeff_modulus = parms.coeffModulus();
            int coeff_modulus_size = coeff_modulus.length;
            int coeff_count = parms.polyModulusDegree();

            // The plaintext gets added into the c_0 term of ciphertext (c_0,c_1).
            RnsIterator plain_iter = RnsIterator.wrap(plain.data(), coeff_count, coeff_modulus_size);
            RnsIterator destination_iter = RnsIterator.wrap(destination.data(), coeff_count, coeff_modulus_size);
            // add_poly_coeffmod(destination_iter, plain_iter, coeff_modulus_size, coeff_modulus, destination_iter);
            PolyArithmeticSmallMod.addPolyCoeffMod(
                destination_iter, plain_iter, coeff_modulus_size, coeff_modulus, destination_iter
            );

            destination.setScale(plain.scale());
        } else if (scheme.equals(SchemeType.BGV)) {
            // TODO: implement BGV
            throw new IllegalArgumentException("now cannot support BGV");
        } else {
            throw new IllegalArgumentException("unsupported scheme");
        }
    }
}
