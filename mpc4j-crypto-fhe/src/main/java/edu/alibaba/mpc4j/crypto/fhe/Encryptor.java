package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rns.RnsTool;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLwe;
import edu.alibaba.mpc4j.crypto.fhe.utils.ScalingVariant;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;

/**
 * Encrypts Plaintext objects into Ciphertext objects. Constructing an Encryptor
 * requires a SEALContext with valid encryption parameters, the public key and/or
 * the secret key. If an Encrytor is given a secret key, it supports symmetric-key
 * encryption. If an Encryptor is given a public key, it supports asymmetric-key
 * encryption.
 * <p></p>
 * <p>NTT form</p>
 * When using the BFV/BGV scheme (scheme_type::bfv/bgv), all plaintext and ciphertexts should
 * remain by default in the usual coefficient representation, i.e. not in NTT form.
 * When using the CKKS scheme (scheme_type::ckks), all plaintexts and ciphertexts
 * should remain by default in NTT form. We call these scheme-specific NTT states
 * the "default NTT form". Decryption requires the input ciphertexts to be in
 * the default NTT form, and will throw an exception if this is not the case.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptor.h
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
        encryptInternal(plain, true, false, destination);
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
        encryptInternal(plain, true, false, destination);
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
        encryptZeroInternal(parmsId, true, true, destination);
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
        encryptZeroInternal(parmsId, true, false, destination);
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
        encryptInternal(plain, false, false, destination);
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
        encryptInternal(plain, false, true, destination);
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
        encryptZeroInternal(parmsId, false, false, destination);
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
        encryptZeroInternal(parmsId, false, true, destination);
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

    private void encryptZeroInternal(ParmsId parmsId, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {
        ContextData contextData = context.getContextData(parmsId);
        EncryptionParameters parms = contextData.parms();
        int coeffModulusSize = parms.coeffModulus().length;
        int coeffCount = parms.polyModulusDegree();
        boolean isNttForm = false;

        if (parms.scheme().equals(SchemeType.CKKS)) {
            isNttForm = true;
        } else if (!parms.scheme().equals(SchemeType.BFV) && !parms.scheme().equals(SchemeType.BGV)) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        // Resize destination and save results
        destination.resize(context, parmsId, 2);

        // If asymmetric key encryption
        if (isAsymmetric) {
            ContextData prevContextData = contextData.prevContextData();
            if (prevContextData != null) {
                // Requires modulus switching
                ParmsId prevParmsId = prevContextData.parmsId();
                RnsTool rnsTool = prevContextData.rnsTool();

                // Zero encryption without modulus switching
                Ciphertext temp = new Ciphertext();
                RingLwe.encryptZeroAsymmetric(publicKey, context, prevParmsId, isNttForm, temp);

                // Modulus switching
                for (int i = 0; i < temp.size(); i++) {
                    if (isNttForm) {
                        // temp in ciphertext RnsBase
                        rnsTool.divideAndRoundQLastNttInplace(
                            temp.data(),
                            temp.getPolyOffset(i),
                            temp.polyModulusDegree(),
                            temp.getCoeffModulusSize(),
                            prevContextData.smallNttTables()
                        );
                    } else if (parms.scheme() != SchemeType.BGV) {
                        // bfv switch-to-next
                        rnsTool.divideAndRoundQLastInplace(
                            temp.data(), temp.getPolyOffset(i), temp.polyModulusDegree(), temp.getCoeffModulusSize()
                        );
                    } else {
                        // bgv switch-to-next
                        // TODO: implement BGV
                        throw new IllegalArgumentException("now cannot support BGV");
                    }
                    PolyCore.setPoly(
                        temp.data(), i * coeffCount * temp.getCoeffModulusSize(), coeffCount, coeffModulusSize,
                        destination.data(), i * coeffCount * destination.getCoeffModulusSize());
                }

                destination.setParmsId(parmsId);
                destination.setIsNttForm(isNttForm);
                destination.setScale(temp.scale());
                destination.setCorrectionFactor(temp.correctionFactor());
            } else {
                // Does not require modulus switching
                RingLwe.encryptZeroAsymmetric(publicKey, context, parmsId, isNttForm, destination);
            }
        } else {
            // Does not require modulus switching
            RingLwe.encryptZeroSymmetric(secretKey, context, parmsId, isNttForm, saveSeed, destination);
        }
    }

    private void encryptInternal(Plaintext plain, boolean isAsymmetric, boolean saveSeed, Ciphertext destination) {
        // Minimal verification that the keys are set
        if (isAsymmetric) {
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
            encryptZeroInternal(context.firstParmsId(), isAsymmetric, saveSeed, destination);

            // Multiply plain by scalar coeff_div_plaintext and reposition if in upper-half.
            // Result gets added into the c_0 term of ciphertext (c_0,c_1).
            ScalingVariant.multiplyAddPlainWithScalingVariant(
                plain, context.firstContextData(), destination.data(), 0, destination.polyModulusDegree()
                );
        } else if (scheme.equals(SchemeType.CKKS)) {
            // TODO: implement CKKS
            throw new IllegalArgumentException("now cannot support CKKS");
        } else if (scheme.equals(SchemeType.BGV)) {
            // TODO: implement BGV
            throw new IllegalArgumentException("now cannot support BGV");
        } else {
            throw new IllegalArgumentException("unsupported scheme");
        }
    }
}
