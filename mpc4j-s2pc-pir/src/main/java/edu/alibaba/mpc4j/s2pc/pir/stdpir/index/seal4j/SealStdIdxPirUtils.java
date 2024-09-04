package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private static long invertMod(int m, Modulus mod) {
        long[] inverse = new long[1];
        boolean success = Numth.tryInvertUintMod(m, mod.value(), inverse);

        if(!success) {
            throw new ArithmeticException("Modular inversion failed.");
        }

        return inverse[0];
    }

    private static int computeExpansionRatio(EncryptionParameters params) {
        int expansionRatio = 0;
        int ptBitsPerCoeff = (int) (Math.log(params.plainModulus().value()) / Math.log(2));

        for (Modulus mod : params.coeffModulus()) {
            double coeffBitSize = (int) (Math.log(mod.value()) / Math.log(2));
            expansionRatio += (int) Math.ceil(coeffBitSize / ptBitsPerCoeff);
        }
        return expansionRatio;
    }

    private static byte[] serializeEncryptionParams(EncryptionParameters params) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            params.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static EncryptionParameters deserializeEncryptionParams(byte[] paramsBytes) {
        EncryptionParameters params = new EncryptionParameters();
        try {
            params.load(null, paramsBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return params;
    }

    private static byte[] serializePublicKey(PublicKey pk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            pk.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static PublicKey deserializePublicKey(byte[] pkBytes, SealContext context) {
        PublicKey pk = new PublicKey();
        try {
            pk.load(context, pkBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pk;
    }

    private static byte[] serializeSecretKey(SecretKey sk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            sk.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static SecretKey deserializeSecretKey(byte[] skBytes, SealContext context) {
        SecretKey sk = new SecretKey();
        try {
            sk.load(context, skBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sk;
    }

    static byte[] serializeGaloisKeys(SealSerializable<GaloisKeys> gk) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            gk.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static GaloisKeys deserializeGaloisKeys(byte[] gkBytes, SealContext context) {
        GaloisKeys gk = new GaloisKeys();
        try {
            gk.load(context, gkBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return gk;
    }

    private static byte[] serializeCiphertext(SealSerializable<Ciphertext> ciphertext) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ciphertext.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static Ciphertext deserializeCiphertext(byte[] ctBytes, SealContext context) {
        Plaintext pt = new Plaintext();
        try {
            pt.load(context, ctBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pt;
    }

    private static List<byte[]> serializeCiphertexts(List<SealSerializable<Ciphertext>> ciphertexts) {
        List<byte[]> list = new ArrayList<>();
        for (SealSerializable<Ciphertext> ciphertext : ciphertexts) {
            byte[] bytes = serializeCiphertext(ciphertext);
            list.add(bytes);
        }

        return list;
    }

    private static List<Ciphertext> deserializeCiphertexts(List<byte[]> ciphertextList, SealContext context) {
        List<Ciphertext> ciphertexts = new ArrayList<>();

        for (byte[] ctBytes : ciphertextList) {
            Ciphertext ciphertext = deserializeCiphertext(ctBytes, context);
            ciphertexts.add(ciphertext);
        }

        return ciphertexts;
    }

    private static byte[] serializePlaintext(Plaintext plaintext) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            plaintext.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }

    private static Plaintext deserializePlaintext(byte[] ptBytes, SealContext context) {
        Plaintext pt = new Plaintext();
        try {
            pt.load(context, ptBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return pt;
    }

    private static List<byte[]> serializePlaintexts(List<Plaintext> plaintexts) {
        List<byte[]> list = new ArrayList<>();
        for (Plaintext plaintext : plaintexts) {
            byte[] plaintextBytes = serializePlaintext(plaintext);
            list.add(plaintextBytes);
        }

        return list;
    }

    private static List<Plaintext> deserializePlaintextsArray(byte[][] database, SealContext context) {
        List<Plaintext> plaintexts = new ArrayList<>();
        for (byte[] row : database) {
            plaintexts.add(deserializePlaintext(row, context));
        }

        return plaintexts;
    }

    private static List<Plaintext> deserializePlaintextsFromCoeffWithoutBatchEncode(List<long[]> coeffList, SealContext context) {
        List<Plaintext> plaintexts = new ArrayList<>();
        int size = coeffList.size();
        for (long[] coeffs : coeffList) {
            Plaintext plaintext = new Plaintext(context.firstContextData().parms().polyModulusDegree());
            plaintext.set(coeffs); // [Question: Correct?]
            plaintexts.add(plaintext);
        }

        return  plaintexts;
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
        keygen.createPublicKey(pk); // [Question: Should this be `SealSerializable`?]

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
    static List<byte[]> nttTransform(byte[] encryptionParams, List<long[]> plaintextList) {
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);
        Evaluator evaluator = new Evaluator(context);
        List<Plaintext> plaintexts = deserializePlaintextsFromCoeffWithoutBatchEncode(plaintextList, context);
        for (Plaintext plaintext : plaintexts) {
            evaluator.transformToNttInplace(plaintext, context.firstParmsId());
        }
        return serializePlaintexts(plaintexts);
    }

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
    static List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices,
                                      int[] nvec) {
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);

        PublicKey pk = deserializePublicKey(publicKey, context);
        SecretKey sk = deserializeSecretKey(secretKey, context);

        Encryptor encryptor = new Encryptor(context, pk, sk);

        int dim = indices.length;

        List<SealSerializable<Ciphertext>> result = new ArrayList<>();

        int coeffCount = params.polyModulusDegree();
        Plaintext pt = new Plaintext(coeffCount);

        for (int i = 0; i < dim; i++) {
            int numPtxts = (int) Math.ceil((nvec[i] + 0.0) / coeffCount);
            for (int j = 0; j < numPtxts; j++) {
                pt.setZero();
                if (indices[i] >= coeffCount * j && indices[i] <= coeffCount * (j + 1)) {
                    int realIndex = indices[i] - coeffCount * j;
                    int n_i = nvec[i];
                    int total = coeffCount;
                    if (j == numPtxts - 1) {
                        total = n_i % coeffCount;
                        if (total == 0) {
                            total = coeffCount;
                        }
                    }
                    int logTotal = (int) Math.ceil(Math.log(total) / Math.log(2));
                    pt.set(realIndex, invertMod((int) Math.pow(2, logTotal), params.plainModulus()));
                }
                result.add(encryptor.encryptSymmetric(pt));
            }
        }
        return serializeCiphertexts(result);
    }

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
    static List<byte[]> generateReply(byte[] encryptionParams, byte[] galoisKey, List<byte[]> queryList,
                                      byte[][] database, int[] nvec) {
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);
        Evaluator evaluator = new Evaluator(context);
        GaloisKeys galoisKeys = deserializeGaloisKeys(galoisKey, context);
        List<Plaintext> db = deserializePlaintextsArray(database, context);
        List<Ciphertext> queries = deserializeCiphertexts(queryList, context);
        List<List<Ciphertext>> query = new ArrayList<>();
        int coeffCount = params.polyModulusDegree();

        int index = 0;
        int product = 1;
        for (int n : nvec) {
            int numPtxts = (int) Math.ceil((n + 0.0) / coeffCount);
            List<Ciphertext> queryi = new ArrayList<>();
            product *= n;
            for (int j = 0; j < numPtxts; j++) {
                queryi.add(queries.get(index++));
            }
            query.add(queryi);
        }

        return null;
    }

    /**
     * decode response.
     *
     * @param encryptionParams encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param dimension        dimension.
     * @return BFV plaintext.
     */
    static long[] decryptReply(byte[] encryptionParams, byte[] secretKey, List<byte[]> response, int dimension) {
        return null;
    }

    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param encryptionParams encryption params.
     * @return expansion ratio.
     */
    static int expansionRatio(byte[] encryptionParams) { // [Question: Which one should I pass, `EncryptionParams` vs `byte[]`?
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);

        return computeExpansionRatio(context.lastContextData().parms()) << 1;
    }
}
