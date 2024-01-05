package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.Ciphertext;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.SecretKey;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.rand.*;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

/**
 * This class provides some operations under Ring LWE.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/rlwe.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class RingLwe {
    /**
     * private constructor.
     */
    private RingLwe() {
        // empty
    }

    /**
     * Generate a uniform ternary polynomial and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     */
    public static void samplePolyTernary(UniformRandomGenerator prng, EncryptionParameters parms, long[] destination) {
        samplePolyTernary(prng, parms, destination, 0);
    }

    /**
     * Generate a uniform ternary polynomial and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     * @param offset      offset of allocated space.
     */
    public static void samplePolyTernary(UniformRandomGenerator prng, EncryptionParameters parms,
                                         long[] destination, int offset) {
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();

        for (int j = 0; j < coeffCount; j++) {
            // rand in [0, 2]
            long rand = prng.nextInt(3);
            // use flag to change rand to be [q_i - 1, 0, 1], where q_i is the RNS base.
            long flag = rand == 0 ? -1 : 0;
            for (int i = 0; i < coeffModulusSize; i++) {
                destination[offset + j + i * coeffCount] = rand + (flag & coeffModulus[i].value()) - 1;
            }
        }
    }

    /**
     * Generate a polynomial from a normal distribution and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     */
    public static void samplePolyNormal(UniformRandomGenerator prng, EncryptionParameters parms, long[] destination) {
        samplePolyNormal(prng, parms, destination, 0);
    }

    /**
     * Generate a polynomial from a normal distribution and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     * @param offset      offset of allocated space.
     */
    public static void samplePolyNormal(UniformRandomGenerator prng, EncryptionParameters parms,
                                        long[] destination, int offset) {
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            PolyCore.setZeroPoly(coeffCount, coeffModulusSize, destination, offset);
            return;
        }

        ClippedNormalDistribution dist = new ClippedNormalDistribution(
            0, GlobalVariables.NOISE_STANDARD_DEVIATION, GlobalVariables.NOISE_MAX_DEVIATION
        );

        for (int j = 0; j < coeffCount; j++) {
            // sample based on the normal distribution
            long noise = (long) dist.sample(prng);
            // use flag to change rand to be [q_i - r, q_i + r], where q_i is the RNS base.
            long flag = noise < 0 ? -1 : 0;
            for (int i = 0; i < coeffModulusSize; i++) {
                destination[offset + j + i * coeffCount] = noise + (flag & coeffModulus[i].value());
            }
        }
    }

    private static int cbd(UniformRandomGenerator prng) {
        byte[] x = new byte[6];
        prng.generate(x);
        // 0001 1111
        x[2] &= 0x1F;
        x[5] &= 0x1F;
        return Common.hammingWeight(x[0]) + Common.hammingWeight(x[1]) + Common.hammingWeight(x[2])
            - Common.hammingWeight(x[3]) - Common.hammingWeight(x[4]) - Common.hammingWeight(x[5]);
    }

    /**
     * Generate a polynomial from a centered binomial distribution and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     */
    public static void samplePolyCbd(UniformRandomGenerator prng, EncryptionParameters parms, long[] destination) {
        samplePolyCbd(prng, parms, destination, 0);
    }

    /**
     * Generate a polynomial from a centered binomial distribution and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     * @param offset      offset of allocated space.
     */
    public static void samplePolyCbd(UniformRandomGenerator prng, EncryptionParameters parms,
                                     long[] destination, int offset) {
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();

        if (Common.areClose(GlobalVariables.NOISE_MAX_DEVIATION, 0.0)) {
            PolyCore.setZeroPoly(coeffCount, coeffModulusSize, destination, offset);
            return;
        }

        if (!Common.areClose(GlobalVariables.NOISE_STANDARD_DEVIATION, 3.2)) {
            throw new IllegalArgumentException(
                "centered binomial distribution only supports standard deviation 3.2; use rounded Gaussian instead"
            );
        }

        for (int j = 0; j < coeffCount; j++) {
            // sample a CBD noise
            int noise = cbd(prng);
            long flag = (noise < 0) ? -1 : 0;
            for (int i = 0; i < coeffModulusSize; i++) {
                destination[offset + j + i * coeffCount] = (long) noise + (flag & coeffModulus[i].value());
            }
        }
    }

    /**
     * Generate a uniformly random polynomial and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     */
    public static void samplePolyUniform(UniformRandomGenerator prng, EncryptionParameters parms, long[] destination) {
        samplePolyUniform(prng, parms, destination, 0);
    }


    /**
     * Generate a uniformly random polynomial and store in RNS representation.
     *
     * @param prng        a uniform random generator.
     * @param parms       EncryptionParameters used to parameterize an RNS polynomial.
     * @param destination allocated space to store a random polynomial.
     * @param offset      offset of allocated space.
     */
    public static void samplePolyUniform(UniformRandomGenerator prng, EncryptionParameters parms,
                                         long[] destination, int offset) {

        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();
        int destByteCount = Common.mulSafe(coeffModulusSize, coeffCount, false, Constants.BYTES_PER_UINT64);

        long maxRandom = 0xFFFFFFFFFFFFFFFFL;

        // Fill the destination buffer with fresh randomness
        prng.generate(destByteCount, destination, offset);

        for (int j = 0; j < coeffModulusSize; j++) {
            Modulus modulus = coeffModulus[j];
            long maxMultiple = maxRandom - UintArithmeticSmallMod.barrettReduce64(maxRandom, modulus) - 1;
            for (int i = 0; i < coeffCount; i++) {
                // This ensures uniform distribution
                while (Long.compareUnsigned(destination[offset + j * coeffCount + i], maxMultiple) >= 0) {
                    destination[offset + j * coeffCount + i] = prng.nextLong();
                }
                destination[offset + j * coeffCount + i] = UintArithmeticSmallMod.barrettReduce64(
                    destination[offset + j * coeffCount + i], modulus
                );
            }
        }
    }

    /**
     * Creates an encryption of zero with a public key and store in a ciphertext.
     *
     * @param publicKey   the public key used for encryption.
     * @param context     the SEALContext containing a chain of ContextData.
     * @param parmsId     indicates the level of encryption.
     * @param isNttForm   if true, store ciphertext in NTT form.
     * @param destination the output ciphertext - an encryption of zero.
     */
    public static void encryptZeroAsymmetric(PublicKey publicKey, SealContext context, ParmsId parmsId,
                                             boolean isNttForm, Ciphertext destination) {
        if (!ValCheck.isValidFor(publicKey, context)) {
            throw new IllegalArgumentException("public key is not valid for the encryption parameters");
        }

        ContextData contextData = context.getContextData(parmsId);
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();
        NttTables[] nttTables = contextData.smallNttTables();
        int encryptedSize = publicKey.data().size();
        SchemeType type = parms.scheme();

        // Make destination have right size and parms_id
        // Ciphertext (c_0,c_1, ...)
        destination.resize(context, parmsId, encryptedSize);
        destination.setIsNttForm(isNttForm);
        destination.setScale(1.0);
        destination.setCorrectionFactor(1);

        // c[j] = public_key[j] * u + e[j] in BFV/CKKS = public_key[j] * u + p * e[j] in BGV,
        // where e[j] <-- chi, u <-- R_3

        // Create a PRNG; u and the noise/error share the same PRNG
        UniformRandomGenerator prng = parms.randomGeneratorFactory().create();

        // Generate u <-- R_3
        long[] u = new long[coeffCount * coeffModulusSize];
        samplePolyTernary(prng, parms, u);

        // c[j] = u * public_key[j]
        for (int i = 0; i < coeffModulusSize; i++) {
            NttTool.nttNegacyclicHarveyRns(u, coeffCount, coeffModulusSize, i, nttTables);
            for (int j = 0; j < encryptedSize; j++) {
                PolyArithmeticSmallMod.dyadicProductCoeffMod(
                    u, i * coeffCount, publicKey.data().data(), publicKey.data().getPolyOffset(j) + i * coeffCount,
                    coeffCount, coeffModulus[i], destination.data(), destination.getPolyOffset(j) + i * coeffCount
                );

                // Addition with e_0, e_1 is in non-NTT form
                if (!isNttForm) {
                    NttTool.inverseNttNegacyclicHarvey(
                        destination.data(), destination.getPolyOffset(j) + i * coeffCount, nttTables[i]
                    );
                }
            }
        }

        // Generate e_j <-- chi
        // c[j] = public_key[j] * u + e[j] in BFV/CKKS, = public_key[j] * u + p * e[j] in BGV,
        for (int j = 0; j < encryptedSize; j++) {
            samplePolyCbd(prng, parms, u);

            // In BGV, p * e is used
            if (type.equals(SchemeType.BGV)) {
                // TODO: implement BGV
                throw new IllegalArgumentException("now cannot support BGV");
            } else {
                if (isNttForm) {
                    NttTool.nttNegacyclicHarveyRns(u, coeffCount, coeffModulusSize, nttTables);
                }
            }
            for (int i = 0; i < coeffModulusSize; i++) {
                PolyArithmeticSmallMod.addPolyCoeffMod(u, i * coeffCount,
                    destination.data(), destination.getPolyOffset(j) + i * coeffCount,
                    coeffCount, coeffModulus[i],
                    destination.data(), destination.getPolyOffset(j) + i * coeffCount
                );
            }
        }
    }

    /**
     * Creates an encryption of zero with a secret key and store in a ciphertext.
     *
     * @param secretKey   the secret key used for encryption.
     * @param context     the SEALContext containing a chain of ContextData.
     * @param parmsId     indicates the level of encryption.
     * @param isNttForm   if true, store ciphertext in NTT form.
     * @param saveSeed    if true, the second component of ciphertext is
     *                    replaced with the random seed used to sample this component.
     * @param destination the output ciphertext - an encryption of zero.
     */
    public static void encryptZeroSymmetric(SecretKey secretKey, SealContext context, ParmsId parmsId, boolean isNttForm,
                                            boolean saveSeed, Ciphertext destination) {
        if (!ValCheck.isValidFor(secretKey, context)) {
            throw new IllegalArgumentException("secret key is not valid for the encryption parameters");
        }

        ContextData contextData = context.getContextData(parmsId);
        EncryptionParameters parms = contextData.parms();
        Modulus[] coeffModulus = parms.coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        int coeffCount = parms.polyModulusDegree();
        NttTables[] nttTables = contextData.smallNttTables();
        int encryptedSize = 2;
        SchemeType type = parms.scheme();

        // If a polynomial is too small to store UniformRandomGeneratorInfo,
        // it is best to just disable save_seed. Note that the size needed is
        // the size of UniformRandomGeneratorInfo plus one (uint64_t) because
        // of an indicator word that indicates a seeded ciphertext.
        int polyUint64Count = Common.mulSafe(coeffCount, coeffModulusSize, false);
        int prngInfoByteCount = UniformRandomGeneratorInfo.saveSize();
        int prngInfoUint64Count = Common.divideRoundUp(prngInfoByteCount, polyUint64Count);
        if (saveSeed && polyUint64Count < prngInfoUint64Count + 1) {
            saveSeed = false;
        }

        destination.resize(context, parmsId, encryptedSize);
        destination.setIsNttForm(isNttForm);
        destination.setScale(1.0);
        destination.setCorrectionFactor(1);

        // Create an instance of a random number generator. We use this for sampling
        // a seed for a second PRNG used for sampling u (the seed can be public
        // information. This PRNG is also used for sampling the noise/error below.
        UniformRandomGenerator bootstrapPrng = parms.randomGeneratorFactory().create();

        // Sample a public seed for generating uniform randomness
        long[] publicPrngSeed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
        bootstrapPrng.generate(publicPrngSeed);

        // Set up a new default PRNG for expanding u from the seed sampled above
        UniformRandomGenerator ciphertextPrng = UniformRandomGeneratorFactory.defaultFactory().create(publicPrngSeed);

        // Generate ciphertext: (c[0], c[1]) = ([-(as+ e)]_q, a) in BFV/CKKS
        // Generate ciphertext: (c[0], c[1]) = ([-(as+pe)]_q, a) in BGV
        int c0Offset = 0;
        int c1Offset = destination.getPolyOffset(1);

        // Sample a uniformly at random
        if (isNttForm || !saveSeed) {
            // Sample the NTT form directly
            samplePolyUniform(ciphertextPrng, parms, destination.data(), c1Offset);
        } else {
            // Sample non-NTT form and store the seed
            samplePolyUniform(ciphertextPrng, parms, destination.data(), c1Offset);
            NttTool.nttNegacyclicHarveyPoly(destination.data(), 2, coeffCount, coeffModulusSize, 1, nttTables);
        }

        // Sample e <-- chi
        long[] noise = new long[coeffCount * coeffModulusSize];
        samplePolyCbd(bootstrapPrng, parms, noise);

        // Calculate -(as+ e) (mod q) and store in c[0] in BFV/CKKS
        // Calculate -(as+pe) (mod q) and store in c[0] in BGV
        for (int i = 0; i < coeffModulusSize; i++) {
            PolyArithmeticSmallMod.dyadicProductCoeffMod(
                secretKey.data().getData(), i * coeffCount, destination.data(), c1Offset + i * coeffCount,
                coeffCount, coeffModulus[i], destination.data(), c0Offset + i * coeffCount
            );
            if (isNttForm) {
                // Transform the noise e into NTT representation
                NttTool.nttNegacyclicHarveyRns(noise, coeffCount, coeffModulusSize, i, nttTables);
            } else {
                // Transform as into coefficient representation.
                NttTool.inverseNttNegacyclicHarvey(destination.data(), c0Offset + i * coeffCount, nttTables[i]);
            }
            if (type.equals(SchemeType.BGV)) {
                // TODO: implement BGV
                throw new IllegalArgumentException("can not support BGV");
            }

            // c0 = as + e
            PolyArithmeticSmallMod.addPolyCoeffMod(
                noise, i * coeffCount, destination.data(), c0Offset + i * coeffCount,
                coeffCount, coeffModulus[i], destination.data(), c0Offset + i * coeffCount
            );
            // (as + noise, a) -> (-(as + noise), a)
            PolyArithmeticSmallMod.negatePolyCoeffMod(
                destination.data(), c0Offset + i * coeffCount,
                coeffCount, coeffModulus[i], destination.data(), c0Offset + i * coeffCount
            );
        }
        if (!isNttForm && !saveSeed) {
            for (int i = 0; i < coeffModulusSize; i++) {
                // Transform the c1 into non-NTT representation
                NttTool.inverseNttNegacyclicHarvey(destination.data(), c1Offset + i * coeffCount, nttTables[i]);
            }
        }
        if (saveSeed) {
            UniformRandomGeneratorInfo prngInfo = ciphertextPrng.getInfo();

            // Write prng_info to destination.data(1) after an indicator word
            destination.data()[c1Offset] = UniformRandomGeneratorInfo.PRNG_INFO_INDICATOR;
            // here we only write flag and seed into the ciphertext.
            prngInfo.save(destination.data(), c1Offset + 1);
        }
    }
}