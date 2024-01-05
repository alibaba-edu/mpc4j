package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;

/**
 * This class provides some static methods to check whether Plaintext, Ciphertext, and EncryptionParams correspond to a
 * given Context object.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/valcheck.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/19
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ValCheck {
    /**
     * private constructor
     */
    private ValCheck() {
        // empty
    }

    /**
     * Check whether the given plaintext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * plaintext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * plaintext data itself.
     *
     * @param in      the plaintext to check.
     * @param context the SEALContext.
     * @return true if the given plaintext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(Plaintext in, SealContext context) {
        return isMetaDataValidFor(in, context, false);
    }

    /**
     * Check whether the given plaintext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * plaintext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * plaintext data itself.
     *
     * @param in                 the plaintext to check.
     * @param context            the SEALContext.
     * @param allowPureKeyLevels determines whether pure key levels (i.e., non-data levels) should be considered valid.
     * @return true if the given plaintext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(Plaintext in, SealContext context, boolean allowPureKeyLevels) {
        if (!context.isParametersSet()) {
            return false;
        }

        if (in.isNttForm()) {
            // Are the parameters valid for the plaintext?
            ContextData contextData = context.getContextData(in.parmsId());
            if (contextData == null) {
                return false;
            }
            // Check whether the parms_id is in the pure key range
            boolean isParamsPureKey = contextData.chainIndex() > context.firstContextData().chainIndex();
            if (!allowPureKeyLevels && isParamsPureKey) {
                return false;
            }

            EncryptionParameters parms = contextData.parms();
            Modulus[] coeffModulus = parms.coeffModulus();
            int polyModulusDegree = parms.polyModulusDegree();
            // Check that coeff_count is appropriately set
            return Common.mulSafe(coeffModulus.length, polyModulusDegree, false) == in.coeffCount();
        } else {
            EncryptionParameters parms = context.firstContextData().parms();
            int polyModulusDegree = parms.polyModulusDegree();
            return in.coeffCount() <= polyModulusDegree;
        }
    }

    /**
     * Check whether the given ciphertext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * ciphertext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * ciphertext data itself.
     *
     * @param in      the ciphertext to check.
     * @param context the SEALContext.
     * @return true if the given ciphertext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(Ciphertext in, SealContext context) {
        return isMetaDataValidFor(in, context, false);
    }

    /**
     * Check whether the given ciphertext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * ciphertext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * ciphertext data itself.
     *
     * @param in                 the ciphertext to check.
     * @param context            the SEALContext.
     * @param allowPureKeyLevels determines whether pure key levels (i.e., non-data levels) should be considered valid.
     * @return true if the given ciphertext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(Ciphertext in, SealContext context, boolean allowPureKeyLevels) {
        if (!context.isParametersSet()) {
            return false;
        }

        // Are the parameters valid for the ciphertext?
        ContextData contextData = context.getContextData(in.parmsId());
        if (contextData == null) {
            return false;
        }

        // Check whether the parms_id is in the pure key range
        boolean isParamsPureKey = contextData.chainIndex() > context.firstContextData().chainIndex();
        if (!allowPureKeyLevels && isParamsPureKey) {
            return false;
        }

        // Check that the metadata matches
        Modulus[] coeffModulus = contextData.parms().coeffModulus();
        int polyModulusDegree = contextData.parms().polyModulusDegree();

        if ((coeffModulus.length != in.getCoeffModulusSize()) || (polyModulusDegree != in.polyModulusDegree())) {
            return false;
        }

        // Check that size is either 0 or within right bounds
        int size = in.size();
        if ((size < Constants.SEAL_CIPHERTEXT_SIZE_MIN && size != 0) || (size > Constants.SEAL_CIPHERTEXT_SIZE_MAX)) {
            return false;
        }

        // Check that scale is 1.0 in BFV and BGV or not 0.0 in CKKS
        double scale = in.scale();
        SchemeType scheme = context.firstContextData().parms().scheme();
        if (!Common.areClose(scale, 1.0) && ((scheme.equals(SchemeType.BFV) || scheme.equals(SchemeType.BGV)))) {
            return false;
        }
        if (Common.areClose(scale, 0.0) && scheme.equals(SchemeType.CKKS)) {
            return false;
        }

        // Check that correction factor is 1 in BFV and CKKS or within the right bound in BGV
        long correctionFactor = in.correctionFactor();
        long plainModulus = context.firstContextData().parms().plainModulus().value();

        return ((correctionFactor == 1) || (scheme != SchemeType.BFV && scheme != SchemeType.BGV))
            && ((correctionFactor != 0 && correctionFactor <= plainModulus) || scheme != SchemeType.BGV);
    }

    /**
     * Check whether the given secret key is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * secret key data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * secret key data itself.
     *
     * @param in      the secret key to check.
     * @param context the SEALContext.
     * @return true if the given secret key is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(SecretKey in, SealContext context) {
        // Note: we check the underlying Plaintext and allow pure key levels in
        // this check. Then, also need to check that the parms_id matches the
        // key level parms_id; this also means the Plaintext is in NTT form.
        ParmsId keyParmsId = context.keyParmsId();

        return isMetaDataValidFor(in.data(), context, true) && (in.parmsId().equals(keyParmsId));
    }

    /**
     * Check whether the given public key is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * public key data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * public key data itself.
     *
     * @param in      the public key to check.
     * @param context the SEALContext.
     * @return true if the given public key is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(PublicKey in, SealContext context) {
        // Note: we check the underlying Ciphertext and allow pure key levels in
        // this check. Then, also need to check that the parms_id matches the
        // key level parms_id, that the Ciphertext is in NTT form, and that the
        // size is minimal (i.e., SEAL_CIPHERTEXT_SIZE_MIN).
        ParmsId keyParmsId = context.keyParmsId();

        return isMetaDataValidFor(in.data(), context, true)
            && in.data().isNttForm()
            && (in.parmsId().equals(keyParmsId))
            && in.data().size() == Constants.SEAL_CIPHERTEXT_SIZE_MIN;
    }

    /**
     * Check whether the given KSwitchKeys is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * KSwitchKeys data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * KSwitchKeys data itself.
     *
     * @param in      the KSwitchKeys to check.
     * @param context the SEALContext.
     * @return true if the given KSwitchKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(KswitchKeys in, SealContext context) {
        if (!context.isParametersSet()) {
            return false;
        }
        if (!in.parmsId().equals(context.keyParmsId())) {
            return false;
        }

        int decompModCount = context.firstContextData().parms().coeffModulus().length;
        for (PublicKey[] a : in.data()) {
            // Check that each highest level component has right size
            if (a.length > 0 && (a.length != decompModCount)) {
                return false;
            }
            for (PublicKey b : a) {
                // Check that b is a valid public key (metadata only); this also
                // checks that its parms_id matches key_parms_id.
                if (!isMetaDataValidFor(b, context)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given RelinKeys is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * RelinKeys data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * RelinKeys data itself.
     *
     * @param in      the RelinKeys to check.
     * @param context the SEALContext.
     * @return true if the given RelinKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(RelinKeys in, SealContext context) {
        boolean sizeCheck = in.size() == 0 || (in.size() <= Constants.SEAL_CIPHERTEXT_SIZE_MAX - 2
            && in.size() >= Constants.SEAL_CIPHERTEXT_SIZE_MIN - 2);

        return isMetaDataValidFor((KswitchKeys) in, context) && sizeCheck;
    }

    /**
     * Check whether the given GaloisKeys is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * GaloisKeys data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function only checks the metadata and not the
     * GaloisKeys data itself.
     *
     * @param in      the GaloisKeys to check.
     * @param context the SEALContext.
     * @return true if the given GaloisKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isMetaDataValidFor(GaloisKeys in, SealContext context) {
        boolean metaDataCheck = isMetaDataValidFor((KswitchKeys) in, context);
        boolean sizeCheck = in.size() == 0
            || (in.size() <= context.keyContextData().parms().polyModulusDegree());

        return metaDataCheck && sizeCheck;
    }

    /**
     * Check whether the given plaintext data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the plaintext data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the plaintext data itself.
     *
     * @param in the plaintext to check.
     * @return true if the given plaintext data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(Plaintext in) {
        return in.coeffCount() == in.getDynArray().size();
    }

    /**
     * Check whether the given ciphertext data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the ciphertext data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the ciphertext data itself.
     *
     * @param in the ciphertext to check.
     * @return true the given ciphertext data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(Ciphertext in) {
        return in.dynArray().size() == Common.mulSafe(in.size(), in.getCoeffModulusSize(), false, in.polyModulusDegree());
    }

    /**
     * Check whether the given secret key data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the secret key data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the secret key data itself.
     *
     * @param in the secret key to check.
     * @return true if the given secret key data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(SecretKey in) {
        return isBufferValid(in.data());
    }

    /**
     * Check whether the given public key data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the public key data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the public key data itself.
     *
     * @param in the public key to check.
     * @return true if the given public key data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(PublicKey in) {
        return isBufferValid(in.data());
    }

    /**
     * Check whether the given KSwitchKeys data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the KSwitchKeys data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the KSwitchKeys data itself.
     *
     * @param in the KSwitchKeys to check.
     * @return true if the given KSwitchKeys data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(KswitchKeys in) {
        for (PublicKey[] a : in.data()) {
            for (PublicKey b : a) {
                if (!isBufferValid(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given RelinKeys data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the RelinKeys data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the RelinKeys data itself.
     *
     * @param in the RelinKeys to check.
     * @return true if the given RelinKeys data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(RelinKeys in) {
        return isBufferValid((KswitchKeys) in);
    }

    /**
     * Check whether the given GaloisKeys data buffer is valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the GaloisKeys data buffer does not match the SEALContext, this function
     * returns false. Otherwise, returns true. This function only checks the size of
     * the data buffer and not the GaloisKeys data itself.
     *
     * @param in the GaloisKeys to check.
     * @return true if the given GaloisKeys data buffer is valid for a given SEALContext; false otherwise.
     */
    public static boolean isBufferValid(GaloisKeys in) {
        return isBufferValid((KswitchKeys) in);
    }

    /**
     * Check whether the given plaintext data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the plaintext data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire plaintext data buffer.
     *
     * @param in      the plaintext to check.
     * @param context the SEALContext.
     * @return true if the given plaintext data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(Plaintext in, SealContext context) {
        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        if (in.isNttForm()) {
            ContextData contextData = context.getContextData(in.parmsId());
            EncryptionParameters params = contextData.parms();
            Modulus[] coeffModulus = params.coeffModulus();
            long[] inData = in.getData();
            int inDataIndex = 0;
            for (Modulus value : coeffModulus) {
                long modulus = value.value();
                int polyModulusDegree = params.polyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndex++] > modulus) {
                        return false;
                    }
                }
            }
        } else {
            EncryptionParameters params = context.firstContextData().parms();
            long modulus = params.plainModulus().value();
            long[] inData = in.getData();
            int size = in.coeffCount();
            for (int i = 0; i < size; i++) {
                if (inData[i] >= modulus) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given ciphertext data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the ciphertext data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire ciphertext data buffer.
     *
     * @param in      the ciphertext to check.
     * @param context the SEALContext.
     * @return true if the given ciphertext data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(Ciphertext in, SealContext context) {
        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        ContextData contextData = context.getContextData(in.parmsId());
        Modulus[] coeffModulus = contextData.parms().coeffModulus();
        long[] inData = in.data();
        int inDataIndx = 0;
        int size = in.size();
        for (int i = 0; i < size; i++) {
            for (Modulus value : coeffModulus) {
                long modulus = value.value();
                int polyModulusDegree = in.polyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndx++] >= modulus) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given secret key data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the secret key data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire secret key data buffer.
     *
     * @param in      the secret key to check.
     * @param context the SEALContext.
     * @return true if the given secret key data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(SecretKey in, SealContext context) {
        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        ContextData contextData = context.keyContextData();
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        long[] inData = in.data().getData();
        int inDataIndex = 0;
        for (Modulus value : coeffModulus) {
            long modulus = value.value();
            int polyModulusDegree = parms.polyModulusDegree();
            while (polyModulusDegree-- > 0) {
                if (inData[inDataIndex++] >= modulus) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given public key data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the public key data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire public key data buffer.
     *
     * @param in      the public key to check.
     * @param context the SEALContext.
     * @return true if the given public key data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(PublicKey in, SealContext context) {
        if (!isMetaDataValidFor(in, context)) {
            return false;
        }

        // check the data
        ContextData contextData = context.keyContextData();
        Modulus[] coeffModulus = contextData.parms().coeffModulus();
        long[] inData = in.data().data();
        int inDataIndex = 0;
        int size = in.data().size();
        for (int i = 0; i < size; i++) {
            for (Modulus value : coeffModulus) {
                long modulus = value.value();
                int polyModulusDegree = in.data().polyModulusDegree();
                while (polyModulusDegree-- > 0) {
                    if (inData[inDataIndex++] >= modulus) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given KSwitchKeys data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the KSwitchKeys data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire KSwitchKeys data buffer.
     *
     * @param in      the KSwitchKeys to check.
     * @param context the SEALContext.
     * @return true if the given KSwitchKeys data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(KswitchKeys in, SealContext context) {
        if (!context.isParametersSet()) {
            return false;
        }
        if (!in.parmsId().equals(context.keyParmsId())) {
            return false;
        }

        for (PublicKey[] a : in.data()) {
            for (PublicKey b : a) {
                // Check that b is a valid public key; this also checks that its
                // parms_id matches key_parms_id.
                if (!isDataValidFor(b, context)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the given RelinKeys data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the RelinKeys data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire RelinKeys data buffer.
     *
     * @param in      the RelinKeys to check.
     * @param context the SEALContext.
     * @return true if the given RelinKeys data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(RelinKeys in, SealContext context) {
        return isDataValidFor((KswitchKeys) in, context);
    }

    /**
     * Check whether the given GaloisKeys data and metadata are valid for a given SEALContext.
     * If the given SEALContext is not set, the encryption parameters are invalid,
     * or the GaloisKeys data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow, as it checks the
     * correctness of the entire GaloisKeys data buffer.
     *
     * @param in      the GaloisKeys to check.
     * @param context the SEALContext.
     * @return true if the given GaloisKeys data and metadata are valid for a given SEALContext; false otherwise.
     */
    public static boolean isDataValidFor(GaloisKeys in, SealContext context) {
        return isDataValidFor((KswitchKeys) in, context);
    }

    /**
     * Check whether the given plaintext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * plaintext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire plaintext data buffer.
     *
     * @param in      the plaintext to check.
     * @param context the SEALContext.
     * @return true if the given plaintext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(Plaintext in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given ciphertext is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * ciphertext data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire ciphertext data buffer.
     *
     * @param in      the ciphertext to check.
     * @param context the SEALContext.
     * @return true if the given ciphertext is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(Ciphertext in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given secret key is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * secret key data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire secret key data buffer.
     *
     * @param in      the secret key to check.
     * @param context the SEALContext.
     * @return true if the given secret key is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(SecretKey in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given public key is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * public key data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire public key data buffer.
     *
     * @param in      the public key to check.
     * @param context the SEALContext.
     * @return true if the given public key is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(PublicKey in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given KSwitchKeys is valid for a given SEALContext. If
     * the given SEALContext is not set, the encryption parameters are invalid,
     * or the KSwitchKeys data does not match the SEALContext, this function returns
     * false. Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire KSwitchKeys data buffer.
     *
     * @param in      the KSwitchKeys to check.
     * @param context the SEALContext.
     * @return true if the given KSwitchKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(KswitchKeys in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given RelinKeys is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * RelinKeys data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire RelinKeys data buffer.
     *
     * @param in      the RelinKeys to check.
     * @param context the SEALContext.
     * @return true if the given RelinKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(RelinKeys in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }

    /**
     * Check whether the given GaloisKeys is valid for a given SEALContext. If the
     * given SEALContext is not set, the encryption parameters are invalid, or the
     * GaloisKeys data does not match the SEALContext, this function returns false.
     * Otherwise, returns true. This function can be slow as it checks the validity
     * of all metadata and of the entire GaloisKeys data buffer.
     *
     * @param in the GaloisKeys to check.
     * @param context the SEALContext.
     * @return true if the given GaloisKeys is valid for a given SEALContext; false otherwise.
     */
    public static boolean isValidFor(GaloisKeys in, SealContext context) {
        return isBufferValid(in) && isDataValidFor(in, context);
    }
}
