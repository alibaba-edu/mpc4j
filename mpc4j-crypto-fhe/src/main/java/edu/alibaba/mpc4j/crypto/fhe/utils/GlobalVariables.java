package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Given the security strength and polynomial modulus, this class provides the corresponding coeff modulus in Ciphertext.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/globals.cpp
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class GlobalVariables {
    /**
     * private constructor.
     */
    private GlobalVariables() {
        // empty
    }

    /**
     * Default value for the standard deviation of the noise (error) distribution.
     */
    public static final double NOISE_STANDARD_DEVIATION = HeStdParms.HE_STD_PARMS_ERROR_STD_DEV;
    /**
     * the bounded noise is 6σ
     */
    public static final double NOISE_DISTRIBUTION_WIDTH_MULTIPLIER = 6;
    /**
     * the noise is bounded in [-6σ, 6σ]
     */
    public static final double NOISE_MAX_DEVIATION = NOISE_STANDARD_DEVIATION * NOISE_DISTRIBUTION_WIDTH_MULTIPLIER;
    /**
     * default coeff_modulus (modulus in the ciphertext space) for 128-bit security
     */
    public static final TIntObjectMap<Modulus[]> DEFAULT_COEFF_MUDULUS_128 = new TIntObjectHashMap<>();

    static {
        /*
             Polynomial modulus: 1x^1024 + 1
           ` Modulus count: 1
             Total bit count: 27`
         */
        DEFAULT_COEFF_MUDULUS_128.put(1024, Modulus.createModulus(new long[]{
            0x7e00001L
        }));
          /*
              Polynomial modulus: 1x^2048 + 1
              Modulus count: 1
              Total bit count: 54
         */
        DEFAULT_COEFF_MUDULUS_128.put(2048, Modulus.createModulus(new long[]{
            0x3fffffff000001L
        }));
        /*
             Polynomial modulus: 1x^4096 + 1
             Modulus count: 3
             Total bit count: 109 = 2 * 36 + 37
         */
        DEFAULT_COEFF_MUDULUS_128.put(4096, Modulus.createModulus(new long[]{
            0xffffee001L, 0xffffc4001L, 0x1ffffe0001L
        }));
        /*
            Polynomial modulus: 1x^8192 + 1
            Modulus count: 5
            Total bit count: 218 = 2 * 43 + 3 * 44
         */
        DEFAULT_COEFF_MUDULUS_128.put(8192, Modulus.createModulus(new long[]{
            0x7fffffd8001L, 0x7fffffc8001L, 0xfffffffc001L, 0xffffff6c001L, 0xfffffebc001L
        }));
        /*
            Polynomial modulus: 1x^16384 + 1
            Modulus count: 9
            Total bit count: 438 = 3 * 48 + 6 * 49
         */
        DEFAULT_COEFF_MUDULUS_128.put(16384, Modulus.createModulus(new long[]{
            0xfffffffd8001L, 0xfffffffa0001L, 0xfffffff00001L, 0x1fffffff68001L, 0x1fffffff50001L,
            0x1ffffffee8001L, 0x1ffffffea0001L, 0x1ffffffe88001L, 0x1ffffffe48001L
        }));
        /*
            Polynomial modulus: 1x^32768 + 1
            Modulus count: 16
            Total bit count: 881 = 15 * 55 + 56
         */
        DEFAULT_COEFF_MUDULUS_128.put(32768, Modulus.createModulus(new long[]{
            0x7fffffffe90001L, 0x7fffffffbf0001L, 0x7fffffffbd0001L, 0x7fffffffba0001L, 0x7fffffffaa0001L,
            0x7fffffffa50001L, 0x7fffffff9f0001L, 0x7fffffff7e0001L, 0x7fffffff770001L, 0x7fffffff380001L,
            0x7fffffff330001L, 0x7fffffff2d0001L, 0x7fffffff170001L, 0x7fffffff150001L, 0x7ffffffef00001L,
            0xfffffffff70001L
        }));
    }

    /**
     * default coeff_modulus (modulus in the ciphertext space) for 192-bit security
     */
    public static final TIntObjectMap<Modulus[]> DEFAULT_COEFF_MUDULUS_192 = new TIntObjectHashMap<>();

    static {
        /*
            Polynomial modulus: 1x^1024 + 1
            Modulus count: 1
            Total bit count: 19
         */
        DEFAULT_COEFF_MUDULUS_192.put(1024, Modulus.createModulus(new long[]{
            0x7f001L
        }));
          /*
            Polynomial modulus: 1x^2048 + 1
            Modulus count: 1
            Total bit count: 37
         */
        DEFAULT_COEFF_MUDULUS_192.put(2048, Modulus.createModulus(new long[]{
            0x1ffffc0001L
        }));
        /*
            Polynomial modulus: 1x^4096 + 1
            Modulus count: 3
            Total bit count: 75 = 3 * 25
         */
        DEFAULT_COEFF_MUDULUS_192.put(4096, Modulus.createModulus(new long[]{
            0x1ffc001L, 0x1fce001L, 0x1fc0001L
        }));
        /*
            Polynomial modulus: 1x^8192 + 1
            Modulus count: 4
            Total bit count: 152 = 4 * 38
         */
        DEFAULT_COEFF_MUDULUS_192.put(8192, Modulus.createModulus(new long[]{
            0x3ffffac001L, 0x3ffff54001L, 0x3ffff48001L, 0x3ffff28001L
        }));
        /*
            Polynomial modulus: 1x^16384 + 1
            Modulus count: 6
            Total bit count: 300 = 6 * 50
         */
        DEFAULT_COEFF_MUDULUS_192.put(16384, Modulus.createModulus(new long[]{
            0x3ffffffdf0001L, 0x3ffffffd48001L, 0x3ffffffd20001L, 0x3ffffffd18001L, 0x3ffffffcd0001L,
            0x3ffffffc70001L
        }));
        /*
            Polynomial modulus: 1x^32768 + 1
            Modulus count: 11
            Total bit count: 600 = 5 * 54 + 6 * 55
         */
        DEFAULT_COEFF_MUDULUS_192.put(32768, Modulus.createModulus(new long[]{
            0x3fffffffd60001L, 0x3fffffffca0001L, 0x3fffffff6d0001L, 0x3fffffff5d0001L, 0x3fffffff550001L,
            0x7fffffffe90001L, 0x7fffffffbf0001L, 0x7fffffffbd0001L, 0x7fffffffba0001L, 0x7fffffffaa0001L,
            0x7fffffffa50001L
        }));
    }

    /**
     * default coeff_modulus (modulus in the ciphertext space) for 256-bit security
     */
    public static final TIntObjectMap<Modulus[]> DEFAULT_COEFF_MUDULUS_256 = new TIntObjectHashMap<>();

    static {
        /*
            Polynomial modulus: 1x^1024 + 1
            Modulus count: 1
            Total bit count: 14
         */
        DEFAULT_COEFF_MUDULUS_256.put(1024, Modulus.createModulus(new long[]{
            0x3001L
        }));
          /*
            Polynomial modulus: 1x^2048 + 1
            Modulus count: 1
            Total bit count: 29
         */
        DEFAULT_COEFF_MUDULUS_256.put(2048, Modulus.createModulus(new long[]{
            0x1ffc0001L
        }));
        /*
            Polynomial modulus: 1x^4096 + 1
            Modulus count: 1
            Total bit count: 58
         */
        DEFAULT_COEFF_MUDULUS_256.put(4096, Modulus.createModulus(new long[]{
            0x3ffffffff040001L
        }));
        /*
            Polynomial modulus: 1x^8192 + 1
            Modulus count: 3
            Total bit count: 118 = 2 * 39 + 40
         */
        DEFAULT_COEFF_MUDULUS_256.put(8192, Modulus.createModulus(new long[]{
            0x7ffffec001L, 0x7ffffb0001L, 0xfffffdc001L
        }));
        /*
            Polynomial modulus: 1x^16384 + 1
            Modulus count: 5
            Total bit count: 237 = 3 * 47 + 2 * 48
         */
        DEFAULT_COEFF_MUDULUS_256.put(16384, Modulus.createModulus(new long[]{
            0x7ffffffc8001L, 0x7ffffff00001L, 0x7fffffe70001L, 0xfffffffd8001L, 0xfffffffa0001L
        }));
        /*
            Polynomial modulus: 1x^32768 + 1
            Modulus count: 9
            Total bit count: 476 = 52 + 8 * 53
         */
        DEFAULT_COEFF_MUDULUS_256.put(32768, Modulus.createModulus(new long[]{
            0xffffffff00001L, 0x1fffffffe30001L, 0x1fffffffd80001L, 0x1fffffffd10001L, 0x1fffffffc50001L,
            0x1fffffffbf0001L, 0x1fffffffb90001L, 0x1fffffffb60001L, 0x1fffffffa50001L
        }));
    }
}
