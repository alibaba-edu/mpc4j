package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SEAL PIR Java utils.
 *
 * @author @alarst13
 * @date   9/4/2024
 */
public class SealStdIdxPirUtils {

    private SealStdIdxPirUtils() {
        // empty
    }

    private static List<Plaintext> decomposeToPlaintexts(EncryptionParameters params, Ciphertext ct) {
        int ptBitsPerCoeff = (int) (Math.log(params.plainModulus().value()) / Math.log(2));
        int coeffCount = params.polyModulusDegree();
        int coeffModCount = params.coeffModulus().length;
        int ptBitMask = (1 << ptBitsPerCoeff) - 1;
        List<Plaintext> result = new ArrayList<>(computeExpansionRatio(params) * ct.size());

        for (int polyIndex = 0; polyIndex < ct.size(); ++polyIndex) {
            for (int coeffModIndex = 0; coeffModIndex < coeffModCount; ++coeffModIndex) {
                double coeffBitSize = Math.log(params.coeffModulus()[coeffModIndex].value()) / Math.log(2);
                int localExpansionRatio = (int) (Math.ceil(coeffBitSize / ptBitsPerCoeff));
                int shift = 0;
                for (int i = 0; i < localExpansionRatio; ++i) {
                    Plaintext pt  = new Plaintext(coeffCount);

                    for (int c = 0; c < coeffCount; ++c) {
                        long coeff = ct.data()[ct.getPolyOffset(polyIndex) + (coeffModIndex * coeffModCount + c)]; // [Question: Correct?]
                        pt.set(c, (coeff >> shift) & ptBitMask);
                    }
                    result.add(pt);
                    shift += ptBitsPerCoeff;
                }
            }
        }

        return result;
    }

    private static void composeToCiphertext(EncryptionParameters params, List<Plaintext> pts, Ciphertext ct) {
        composeToCiphertext(params, pts, pts.size() / computeExpansionRatio(params), ct);
    }

    private static void composeToCiphertext(EncryptionParameters params, List<Plaintext> pts, int ctPolyCount, Ciphertext ct) {
        int ptBitsPerCoeff = (int) (Math.log(params.plainModulus().value()) / Math.log(2));
        int coeffCount = params.polyModulusDegree();
        int coeffModCount = params.coeffModulus().length;
        ct.resize(ctPolyCount);
        int ptIndex = 0;
        for (int polyIndex = 0; polyIndex < ctPolyCount; ++polyIndex) {
            for (int coeffModIndex = 0; coeffModIndex < coeffModCount; ++coeffModIndex) {
                double coeffBitSize = Math.log(params.coeffModulus()[coeffModIndex].value()) / Math.log(2);
                int localExpansionRatio = (int) (Math.ceil(coeffBitSize / ptBitsPerCoeff));
                int shift = 0;
                for (int i = 0; i < localExpansionRatio; ++i) {
                    Plaintext pt  = pts.get(ptIndex);

                    for (int c = 0; c < pt.coeffCount(); ++c) {
                        if (shift == 0)
                            ct.data()[ct.getPolyOffset(polyIndex) + (coeffModIndex * coeffCount + c)] = pt.get(c);
                        else
                            ct.data()[ct.getPolyOffset(polyIndex) + (coeffModIndex * coeffCount + c)] += (pt.get(c) << shift);
                    }
                    ptIndex++;
                    shift += ptBitsPerCoeff;
                }
            }
        }
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

    private static void multiplyPowerOfX(Ciphertext encrypted, Ciphertext destination, int index, SealContext context) {
        SealContext.ContextData contextData = context.firstContextData();
        EncryptionParameters params = contextData.parms();
        int coeffModCount = params.coeffModulus().length;
        int coeffCount = params.polyModulusDegree();
        int encryptedCount = encrypted.size();
        destination.copyFrom(encrypted); // [Question: Correct?]

        for (int i = 0; i < encryptedCount; i++) {
            int offset = encrypted.getPolyOffset(i);
            for (int j = 0; j < coeffModCount; j++) {
                PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(encrypted.data(), offset + (j * coeffCount), coeffCount, index, params.coeffModulus()[j], destination.data(), offset + (j * coeffCount)); // [Question: Correct?]
            }
        }
    }

    private static List<Ciphertext> expandQuery(EncryptionParameters params, Ciphertext encrypted,
                                                GaloisKeys galoisKeys, int m) {
        SealContext context = new SealContext(params);
        Evaluator evaluator = new Evaluator(context);

        // If `m` is not a power of 2, round it up to the nearest power of 2.
        int logm = (int) Math.ceil(Math.log(m) / Math.ceil(Math.log(2)));

        Plaintext two = new Plaintext("2");
        List<Integer> galoisElts = new ArrayList<>();

        int n = params.polyModulusDegree();
        int logn = (int) Math.ceil(Math.log(n) / Math.log(2));
        if (logm > logn) {
            throw new IllegalArgumentException("m > n is not allowed.");
        }

        for (int j = 0; j < logn; j++) {
            galoisElts.add((n + (1 << j)) / (1 << j));
        }

        List<Ciphertext> temp = new ArrayList<>();
        temp.add(encrypted);
        Ciphertext tempctxtRotated = new Ciphertext();
        Ciphertext tempctxtShifted = new Ciphertext();
        Ciphertext tempctxtRotatedshifted = new Ciphertext();

        for (int j = 0; j < logm - 1; j++) {
            List<Ciphertext> newtemp = new ArrayList<>(temp.size() << 1);
            for (int k = 0; k < (temp.size() << 1); k++) {
                newtemp.add(new Ciphertext());
            }
            int indexRaw = (n << 1) - (1 << j); // [Question: Why `2n - 2^j`? Why not just `-2^j`?]
            int index = (indexRaw * galoisElts.get(j)) % (n << 1); // [Question: Why?]

            for (int k = 0; k < temp.size(); k++) {
                Ciphertext tmpctxt = temp.get(k);
                evaluator.applyGalois(tmpctxt, galoisElts.get(j), galoisKeys, tempctxtRotated); // Sub(c_0, N/2^j + 1)
                evaluator.add(tmpctxt, tempctxtRotated, newtemp.get(k)); // c'_k <- c_0 + Sub(c_0, N/2^j + 1)
                multiplyPowerOfX(tmpctxt, tempctxtShifted, indexRaw, context); // c_1 <- c_0 * x^{-2^j}
                multiplyPowerOfX(tempctxtRotated, tempctxtRotatedshifted, index, context); // Sub(c_1, N/2^j + 1) [Question: How does this work?]
                evaluator.add(tempctxtShifted, tempctxtRotatedshifted, newtemp.get(k + temp.size())); // c'_{k+2^j} <- c_1 + Sub(c_1, N/2^j + 1)
            }

            temp = newtemp;
        }
        // Last step of the loop
        List<Ciphertext> newtemp = new ArrayList<>(temp.size() << 1);
        for (int k = 0; k < (temp.size() << 1); k++) {
            newtemp.add(new Ciphertext());
        }
        int indexRaw = (n << 1) - (1 << (logm - 1));
        int index = (indexRaw * galoisElts.get(logm - 1)) % (n << 1);

        for (int k = 0; k < temp.size(); k++) {
            Ciphertext tmpctxt = temp.get(k);
            if (k >= (m - (1 << (logm - 1)))) {
                evaluator.multiplyPlain(tmpctxt, two, newtemp.get(k)); // Plain multiplication by 2 [Question: Why?]
            } else {
                evaluator.applyGalois(tmpctxt, galoisElts.get(logm - 1), galoisKeys, tempctxtRotated);
                evaluator.add(tmpctxt, tempctxtRotated, newtemp.get(k));
                multiplyPowerOfX(tmpctxt, tempctxtShifted, indexRaw, context);
                multiplyPowerOfX(tempctxtRotated, tempctxtRotatedshifted, index, context);
                evaluator.add(tempctxtShifted, tempctxtRotatedshifted, newtemp.get(k + temp.size()));
            }
        }

        return new ArrayList<>(newtemp.subList(0, m));
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

    private static byte[] serializeCiphertext(Ciphertext ciphertext) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ciphertext.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
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
        Ciphertext ct = new Ciphertext();
        try {
            ct.load(context, ctBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ct;
    }

    private static <T> List<byte[]> serializeCiphertexts(List<T> ciphertexts) {
        List<byte[]> list = new ArrayList<>();
        for (T ciphertext : ciphertexts) {
            byte[] bytes;
            if (ciphertext instanceof Ciphertext)
                bytes = serializeCiphertext((Ciphertext) ciphertext);
            else if (ciphertext instanceof SealSerializable)
                bytes = serializeCiphertext((SealSerializable<Ciphertext>) ciphertext);
            else {
                throw  new IllegalArgumentException("Unsupported type");
            }
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
        params.setCoeffModulus(CoeffModulus.bfvDefault(polyModulusDegree, CoeffModulus.SecLevelType.TC128));

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

        int n = params.polyModulusDegree();
        int logn = (int) Math.ceil(Math.log(n) / Math.log(2));
        int[] galoisElts = new int[logn];
        for (int j = 0; j < logn; j++) {
            galoisElts[j] = (n + (1 << j)) / (1 << j);
        }
        SealSerializable<GaloisKeys> gk = keygen.createGaloisKeys(galoisElts);

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
        int product = 1; // [Question: What is the `product` for?]
        for (int n : nvec) {
            int numPtxts = (int) Math.ceil((n + 0.0) / coeffCount);
            List<Ciphertext> queryi = new ArrayList<>();
            product = product * n;
            for (int j = 0; j < numPtxts; j++) {
                queryi.add(queries.get(index++));
            }
            query.add(queryi);
        }

        List<Plaintext> cur = db;
        List<Plaintext> intermediatePTs = new ArrayList<>();
        int expansionRatio = computeExpansionRatio(params);
        for (int i = 0; i < nvec.length; i++) {
            List<Ciphertext> expandedQuery = new ArrayList<>();
            for (int j = 0; j < query.get(i).size(); j++) {
                int total = coeffCount;
                if (j == query.get(i).size() - 1) {
                    total = nvec[i] % coeffCount;
                    if (total == 0) {
                        total = coeffCount;
                    }
                }
                List<Ciphertext> expandedQuery_j = expandQuery(params, query.get(i).get(j), galoisKeys, total);
                expandedQuery.addAll(expandedQuery_j);
                expandedQuery_j.clear();
            }
            // [Start from here...]
            if (expandedQuery.size() != nvec[i]) {
                throw new IllegalArgumentException("Size mismatch! Expected size: " + nvec[i] + ", but got: " + expandedQuery.size());
            }
            for (Ciphertext c : expandedQuery) {
                evaluator.transformToNttInplace(c);
            }
            if (i > 0) {
                for (Plaintext p : cur) {
                    evaluator.transformToNttInplace(p, context.firstParmsId());
                }
            }
            product /= nvec[i];
            List<Ciphertext> intermediateCiphertexts = new ArrayList<>(product);
            for (int j = 0; j < product; j++) {
                intermediateCiphertexts.add(new Ciphertext());
            }
            Ciphertext temp = new Ciphertext();
            for (int j = 0; j < product; j++) {
                evaluator.multiplyPlain(expandedQuery.getFirst(), cur.get(j), intermediateCiphertexts.get(j));
                for (int k = 1; k < nvec[i]; k++) {
                    evaluator.multiplyPlain(expandedQuery.get(k), cur.get(j + k * product), temp);
                    evaluator.addInplace(intermediateCiphertexts.get(j), temp);
                }
            }
            for (Ciphertext intermediateCiphertext : intermediateCiphertexts) {
                evaluator.transformFromNttInplace(intermediateCiphertext);
            }
            if (i == nvec.length - 1) {
                return  serializeCiphertexts(intermediateCiphertexts);
            } else {
                intermediatePTs.clear();
                intermediatePTs = new ArrayList<>(expansionRatio * product);
                cur = intermediatePTs;
                for (int rr = 0; rr < product; rr++) {
                    evaluator.modSwitchToInplace(intermediateCiphertexts.get(rr), context.lastParmsId());
                    List<Plaintext> plains = decomposeToPlaintexts(context.lastContextData().parms(), intermediateCiphertexts.get(rr));
                    intermediatePTs.addAll(plains);
                }

                product = intermediatePTs.size();
            }
        }
        throw new RuntimeException("Generate response failed!");
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
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);
        SecretKey sk = deserializeSecretKey(secretKey, context);
        Decryptor decryptor = new Decryptor(context, sk);
        params = context.lastContextData().parms();
        ParmsId parmsId = context.lastParmsId();
        int expRatio = computeExpansionRatio(params);
        List<Ciphertext> temp = deserializeCiphertexts(response, context);
        int ciphertextSize = temp.getFirst().size();
        for (int i = 0; i < dimension; i++) {
            List<Ciphertext> newtemp = new ArrayList<>();
            List<Plaintext> tempplain = new ArrayList<>();
            for (int j = 0; j < temp.size(); j++) {
                Plaintext ptxt = new Plaintext();
                decryptor.decrypt(temp.get(j), ptxt);
                tempplain.add(ptxt);
                if ((j + 1) % (expRatio * ciphertextSize) == 0 && j > 0) {
                    Ciphertext combined = new Ciphertext(context, parmsId);
                    composeToCiphertext(params, tempplain, combined);
                    System.out.println(combined.size());
                    newtemp.add(combined);
                    tempplain.clear();
                }
            }
            if (i == dimension - 1) {
                if (temp.size() != 1) {
                    throw new RuntimeException("Decode response failed!");
                }

                Plaintext firstPlain = tempplain.getFirst();
                long[] coeffArray = new long[firstPlain.coeffCount()];

                for (int ii = 0; ii < firstPlain.coeffCount(); ii++) {
                    coeffArray[ii] = firstPlain.get(ii);
                }

                return  coeffArray;
            } else {
                tempplain.clear();
                temp = newtemp;
            }
        }
        throw new RuntimeException("Generate response failed!");
    }

    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param encryptionParams encryption params.
     * @return expansion ratio.
     */
    static int expansionRatio(byte[] encryptionParams) {
        EncryptionParameters params = deserializeEncryptionParams(encryptionParams);
        SealContext context = new SealContext(params);

        return computeExpansionRatio(context.lastContextData().parms()) << 1;
    }
}
