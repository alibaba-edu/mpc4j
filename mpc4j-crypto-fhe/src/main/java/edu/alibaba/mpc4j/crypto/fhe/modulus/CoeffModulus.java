package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GlobalVariables;
import edu.alibaba.mpc4j.crypto.fhe.utils.HeStdParms;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains static methods for creating a coefficient modulus easily. Note that while these functions take a
 * sec_level_type argument, all security guarantees are lost if the output is used with encryption parameters with a
 * mismatching value for the poly_modulus_degree.
 * <p></p>
 * The default value sec_level_type::tc128 provides a very high level of security and is the default security level
 * enforced by Microsoft SEAL when constructing a SEALContext object. Normal users should not have to specify the
 * security level explicitly anywhere.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h#L424
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/27
 */
public class CoeffModulus {
    /**
     * Represents a standard security level according to the homomorphicencryption.org
     * security standard. The value sec_level_type::none signals that no standard
     * security level should be imposed. The value sec_level_type::tc128 provides
     * a very high level of security and is the default security level enforced by
     * Microsoft SEAL when constructing a SEALContext object. Normal users should not
     * have to specify the security level explicitly anywhere.
     */
    public enum SecLevelType {
        /**
         * No security level specified.
         */
        NONE,
        /**
         * 128-bit security level, where the secret key is from a ternary {-1, 0, 1} distribution.
         */
        TC128,
        /**
         * 192-bit security level, where the secret key is from a ternary {-1, 0, 1} distribution.
         */
        TC192,
        /**
         * 256-bit security level, where the secret key is from a ternary {-1, 0, 1} distribution.
         */
        TC256,
    }

    /**
     * Returns the largest bit-length of the coefficient modulus (modulus in the
     * ciphertext space), i.e., bit-length of the product of the primes in the
     * coefficient modulus, that guarantees a given security level when using a
     * given poly_modulus_degree, according to the homomorphicencryption.org security
     * standard. Some special cases:
     *
     * <li>Returns Integer.MAX_VALUE if no security level is specified.</li>
     * <li>Returns 0 if poly_modulus_degree is not a power-of-two or is too large.</li>
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param securityLevel     the desired standard security level.
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int maxBitCount(int polyModulusDegree, SecLevelType securityLevel) {
        switch (securityLevel) {
            case TC128:
                return HeStdParms.heStdParms128Tc(polyModulusDegree);
            case TC192:
                return HeStdParms.heStdParms192Tc(polyModulusDegree);
            case TC256:
                return HeStdParms.heStdParms256Tc(polyModulusDegree);
            case NONE:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }

    /**
     * Returns the largest bit-length of the coefficient modulus (modulus in the
     * ciphertext space), i.e., bit-length of the product of the primes in the
     * coefficient modulus, that guarantees 128-bit security level when using a
     * given poly_modulus_degree, according to the homomorphicencryption.org
     * security standard.
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @return the largest allowed bit counts for coeff_modulus.
     */
    public static int maxBitCount(int polyModulusDegree) {
        return HeStdParms.heStdParms128Tc(polyModulusDegree);
    }

    /**
     * Gets the default coeff_modulus (modulus in the ciphertext space) for BFV scheme.
     *
     * @param polyModulusDegree N.
     * @return the default modulus for BFV scheme.
     */
    public static Modulus[] bfvDefault(int polyModulusDegree) {
        return bfvDefault(polyModulusDegree, SecLevelType.TC128);
    }

    /**
     * Returns a default coefficient modulus (modulus in the ciphertext space)
     * for the BFV scheme that guarantees a given security level when using a
     * given poly_modulus_degree, according to the homomorphicencryption.org
     * security standard. Note that all security guarantees are lost if the
     * output is used with encryption parameters witha mismatching value for
     * the poly_modulus_degree.
     * <p></p>
     * The coefficient modulus returned by this function will not perform well
     * if used with the CKKS scheme.
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param securityLevel     the desired standard security level.
     * @return the default modulus for BFV scheme.
     */
    public static Modulus[] bfvDefault(int polyModulusDegree, SecLevelType securityLevel) {
        if (maxBitCount(polyModulusDegree) == 0) {
            throw new IllegalArgumentException("non-standard poly_modulus_degree");
        }
        if (securityLevel == SecLevelType.NONE) {
            throw new IllegalArgumentException("invalid security level");
        }
        switch (securityLevel) {
            case TC128:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_128.get(polyModulusDegree);
            case TC192:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_192.get(polyModulusDegree);
            case TC256:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_256.get(polyModulusDegree);
            default:
                throw new IllegalArgumentException("invalid security level");
        }
    }

    /**
     * Returns a custom coefficient modulus (modulus in the ciphertext space)
     * suitable for use with the specified poly_modulus_degree. The return value
     * will be an array consisting of Modulus elements representing distinct
     * prime numbers such that:
     *
     * <li>have bit-lengths as given in the bit_sizes parameter (at most 60 bits);</li>
     * <li>are congruent to 1 modulo 2 * poly_modulus_degree.</li>
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param bitSize           the bit-lengths of the prime to be generated.
     * @return the generated modulus.
     */
    public static Modulus create(int polyModulusDegree, int bitSize) {
        if (polyModulusDegree > Constants.SEAL_POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.SEAL_POLY_MOD_DEGREE_MIN
            || UintCore.getPowerOfTwo(polyModulusDegree) < 0) {
            throw new IllegalArgumentException("polyModulusDegree is invalid");
        }
        if (bitSize < Constants.SEAL_USER_MOD_BIT_COUNT_MIN || bitSize > Constants.SEAL_USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bitSize is invalid");
        }
        // factor = 2N
        long factor = Common.mulSafe(2L, polyModulusDegree, true);
        // Numth.gerPrime helps to generate a prime with "bit_size" bits and p = 1 mod factor.
        return Numth.getPrime(factor, bitSize);
    }

    /**
     * Returns a custom coefficient modulus (modulus in the ciphertext space)
     * suitable for use with the specified poly_modulus_degree. The return value
     * will be an array consisting of Modulus elements representing distinct
     * prime numbers such that:
     * <li>have bit-lengths as given in the bit_sizes parameter (at most 60 bits);</li>
     * <li>are congruent to 1 modulo 2 * poly_modulus_degree.</li>
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param bitSizes          the bit-lengths of the primes to be generated.
     * @return the generated modulus.
     */
    public static Modulus[] create(int polyModulusDegree, int[] bitSizes) {
        if (polyModulusDegree > Constants.SEAL_POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.SEAL_POLY_MOD_DEGREE_MIN
            || UintCore.getPowerOfTwo(polyModulusDegree) < 0) {
            throw new IllegalArgumentException("polyModulusDegree is invalid");
        }
        if (bitSizes.length > Constants.SEAL_COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("bitSizes is invalid");
        }
        // ensure we can create Modulus[] with 0 length
        if (bitSizes.length == 0) {
            return new Modulus[0];
        }
        if (Arrays.stream(bitSizes).min().orElse(0) < Constants.SEAL_USER_MOD_BIT_COUNT_MIN
            || Arrays.stream(bitSizes).max().orElse(Integer.MAX_VALUE) > Constants.SEAL_USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bitSizes is invalid");
        }

        // we support bit_sizes with same values, here we count each of bit_size.
        TIntIntMap countTables = new TIntIntHashMap();
        for (int size : bitSizes) {
            if (!countTables.containsKey(size)) {
                countTables.put(size, 1);
                continue;
            }
            countTables.put(size, countTables.get(size) + 1);
        }
        long factor = Common.mulSafe(2L, polyModulusDegree, true);
        // we use table to ensure that the order of elements Modulus are the same as order of bit_sizes.
        TIntObjectMap<ArrayList<Modulus>> primeTable = new TIntObjectHashMap<>();
        for (int bitSize : countTables.keys()) {
            ArrayList<Modulus> bitSizeModulus = Arrays.stream(Numth.getPrimes(factor, bitSize, countTables.get(bitSize)))
                .collect(Collectors.toCollection(ArrayList::new));
            primeTable.put(bitSize, bitSizeModulus);
        }
        Modulus[] result = new Modulus[bitSizes.length];
        int i = 0;
        for (int size : bitSizes) {
            result[i] = primeTable.get(size).remove(primeTable.get(size).size() - 1);
            i++;
        }
        return result;
    }

    /**
     * Returns a custom coefficient modulus (modulus in the ciphertext space)
     * suitable for use with the specified poly_modulus_degree. The return value
     * will be a vector consisting of Modulus elements representing distinct
     * prime numbers such that:
     * <li>have bit-lengths as given in the bit_sizes parameter (at most 60 bits);</li>
     * <li>are congruent to 1 modulo LCM(2*poly_modulus_degree, plain_modulus).</li>
     *
     * @param polyModulusDegree the value of the poly_modulus_degree encryption parameter (N).
     * @param plainModulus      the value of the plain_modulus encryption parameter.
     * @param bitSizes          the bit-lengths of the primes to be generated.
     * @return the generated modulus.
     */
    public static Modulus[] create(int polyModulusDegree, Modulus plainModulus, int[] bitSizes) {
        if (polyModulusDegree > Constants.SEAL_POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.SEAL_POLY_MOD_DEGREE_MIN
            || UintCore.getPowerOfTwo(polyModulusDegree) < 0) {
            throw new IllegalArgumentException("poly_modulus_degree is invalid");
        }
        if (bitSizes.length > Constants.SEAL_COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("bit_sizes is invalid");
        }
        // ensure we can create Modulus[] with 0 length
        if (bitSizes.length == 0) {
            return new Modulus[0];
        }
        if (Arrays.stream(bitSizes).min().orElse(0) < Constants.SEAL_USER_MOD_BIT_COUNT_MIN
            || Arrays.stream(bitSizes).max().orElse(Integer.MAX_VALUE) > Constants.SEAL_USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bitSizes is invalid");
        }

        // we support bit_sizes with same values, here we count each of bit_size.
        TIntIntMap countTables = new TIntIntHashMap();
        for (int size : bitSizes) {
            if (!countTables.containsKey(size)) {
                countTables.put(size, 1);
                continue;
            }
            countTables.put(size, countTables.get(size) + 1);
        }
        // factor = 2N * (t / gcd(p, 2N))
        long factor = Common.mulSafe(2L, polyModulusDegree, true);
        factor = Common.mulSafe(factor, plainModulus.value() / Numth.gcd(plainModulus.value(), factor), true);
        // we use table to ensure that the order of elements Modulus are the same as order of bit_sizes.
        TIntObjectMap<ArrayList<Modulus>> primeTable = new TIntObjectHashMap<>();
        for (int bitSize : countTables.keys()) {
            ArrayList<Modulus> bitSizeModulus = Arrays.stream(Numth.getPrimes(factor, bitSize, countTables.get(bitSize)))
                .collect(Collectors.toCollection(ArrayList::new));
            primeTable.put(bitSize, bitSizeModulus);
        }
        Modulus[] result = new Modulus[bitSizes.length];
        int i = 0;
        for (int size : bitSizes) {
            result[i] = primeTable.get(size).remove(primeTable.get(size).size() - 1);
            i++;
        }
        return result;
    }

}
