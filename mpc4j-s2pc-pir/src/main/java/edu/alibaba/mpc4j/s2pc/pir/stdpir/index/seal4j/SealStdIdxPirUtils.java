package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.crypto.fhe.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.KeyGenerator;
import edu.alibaba.mpc4j.crypto.fhe.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SEAL PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class SealStdIdxPirUtils {

    private SealStdIdxPirUtils() {
        // empty
    }

    private static byte[] serializeEncryptionParams(EncryptionParameters params) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        params.save(outputStream, Serialization.COMPR_MODE_DEFAULT); // [Question: What should I do about the warning?]

        return outputStream.toByteArray();
    }

    private static EncryptionParameters deserializeEncryptionParams(byte[] paramsByte) {
        EncryptionParameters params = new EncryptionParameters();
        // [Questions:
        //  -   What should I do about the warning?
        //  -   Is it OK to pas `null` here instead of the context?]
        params.load(null, paramsByte);

        return params;
    }

    private static byte[] serializePublicKey(PublicKey pk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        pk.save(outputStream, Serialization.COMPR_MODE_DEFAULT); // [Question: What should I do about the warning?]

        return outputStream.toByteArray();
    }

    private static PublicKey deserializePublicKey(byte[] pkByte) {
        PublicKey pk = new PublicKey();
        // [Questions:
        //  -   What should I do about the warning?
        //  -   Is it OK to pas `null` here instead of the context?]
        pk.load(null, pkByte);

        return pk;
    }

    private static byte[] serializeSecretKey(SecretKey sk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sk.save(outputStream, Serialization.COMPR_MODE_DEFAULT); // [Question: What should I do about the warning?]

        return outputStream.toByteArray();
    }

    private static SecretKey deserializeSecretKey(byte[] skByte) {
        SecretKey sk = new SecretKey();
        // [Questions:
        //  -   What should I do about the warning?
        //  -   Is it OK to pas `null` here instead of the context?]
        sk.load(null, skByte);

        return sk;
    }

    static byte[] serializeGaloisKeys(SealSerializable<GaloisKeys> gk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gk.save(outputStream, Serialization.COMPR_MODE_DEFAULT); // [Question: What should I do about the warning?]

        return outputStream.toByteArray();
    }

    private static GaloisKeys deserializeGaloisKeys(byte[] gkByte) {
        GaloisKeys gk = new GaloisKeys();
        // [Questions:
        //  -   What should I do about the warning?
        //  -   Is it OK to pass `null` here instead of the context?]
        gk.load(null, gkByte);

        return gk;
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @return encryption params.
     */
    static byte[] generateEncryptionParams(int polyModulusDegree, long plainModulus) {
        EncryptionParameters params = new EncryptionParameters(SchemeType.BFV);
        params.setPolyModulusDegree(polyModulusDegree);
        params.setPlainModulus(plainModulus);

        return serializeEncryptionParams(params);
    }

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    static List<byte[]> keyGen(byte[] encryptionParams) {
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);

        KeyGenerator keygen = new KeyGenerator(context);
        SecretKey sk = keygen.secretKey();
        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk); // [Question: Should this be `Serializable`?]

        // [Question: `createStepGaloisKeys` or `createGaloisKeys`. If the Latter, what should I pass for the arguments? Else, what are the differences?]
        SealSerializable<GaloisKeys> gk = keygen.createStepGaloisKeys();

        byte[] pkByte = serializePublicKey(pk);
        byte[] skByte = serializeSecretKey(sk);
        byte[] gkByte = serializeGaloisKeys(gk);

        List<byte[]> bytes = new ArrayList<>();
        bytes.add(pkByte);
        bytes.add(skByte);
        bytes.add(gkByte);

        return bytes;
    }

    /**
     * NTT transformation.
     *
     * @param encryptionParams encryption params.
     * @param plaintextList    plaintext list.
     * @return BFV plaintexts in NTT form.
     */
    static native List<byte[]> nttTransform(byte[] encryptionParams, List<long[]> plaintextList);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          indices.
     * @param nvec             dimension size.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices,
                                             int[] nvec);

    /**
     * generate response.
     *
     * @param encryptionParams encryption params.
     * @param galoisKey        Galois keys.
     * @param queryList        query ciphertexts.
     * @param database         database.
     * @param nvec             dimension size.
     * @return response ciphertexts。
     */
    static native List<byte[]> generateReply(byte[] encryptionParams, byte[] galoisKey, List<byte[]> queryList,
                                             byte[][] database, int[] nvec);

    /**
     * decode response.
     *
     * @param encryptionParams encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param dimension        dimension.
     * @return BFV plaintext.
     */
    static native long[] decryptReply(byte[] encryptionParams, byte[] secretKey, List<byte[]> response, int dimension);

    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param encryptionParams encryption params.
     * @return expansion ratio.
     */
    static native int expansionRatio(byte[] encryptionParams);
}
