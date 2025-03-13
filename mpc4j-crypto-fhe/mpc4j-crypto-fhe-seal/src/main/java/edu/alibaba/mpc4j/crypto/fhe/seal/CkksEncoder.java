package edu.alibaba.mpc4j.crypto.fhe.seal;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext.ContextData;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTables;
import edu.alibaba.mpc4j.crypto.fhe.seal.ntt.NttTool;
import edu.alibaba.mpc4j.crypto.fhe.seal.utils.*;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Provides functionality for encoding vectors of complex or real numbers into
 * plaintext polynomials to be encrypted and computed on using the CKKS scheme.
 * If the polynomial modulus degree is N, then CKKSEncoder converts vectors of
 * N/2 complex numbers into plaintext elements. Homomorphic operations performed
 * on such encrypted vectors are applied coefficient (slot-)wise, enabling
 * powerful SIMD functionality for computations that are vectorizable. This
 * functionality is often called "batching" in the homomorphic encryption
 * literature.
 * <p>
 * Mathematical Background
 * <p>
 * Mathematically speaking, if the polynomial modulus is X^N+1, N is a power of
 * two, the CKKSEncoder implements an approximation of the canonical embedding
 * of the ring of integers Z[X]/(X^N+1) into C^(N/2), where C denotes the complex
 * numbers. The Galois group of the extension is (Z/2NZ)* ~= Z/2Z x Z/(N/2)
 * whose action on the primitive roots of unity modulo coeff_modulus is easy to
 * describe. Since the batching slots correspond 1-to-1 to the primitive roots
 * of unity, applying Galois automorphisms on the plaintext acts by permuting
 * the slots. By applying generators of the two cyclic subgroups of the Galois
 * group, we can effectively enable cyclic rotations and complex conjugations
 * of the encrypted complex vectors.
 * <p>
 * The implementation comes from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/ckks.h">ckks.h</a>
 * and
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/ckks.cpp">ckks.cpp</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/14
 */
public class CkksEncoder {
    /**
     * {@code SEALContext context_;}
     */
    private final SealContext context;
    /**
     * {@code std::size_t slots_;}
     */
    private final int slots;
    /**
     * Holds 1~(n-1)-th powers of root in bit-reversed order, the 0-th power is left unset.
     * {@code util::Pointer<std::complex<double>> root_powers_;}
     */
    private final double[][] root_powers;
    /**
     * Holds 1~(n-1)-th powers of inverse root in scrambled order, the 0-th power is left unset.
     * {@code util::Pointer<std::complex<double>> inv_root_powers_;}
     */
    private final double[][] inv_root_powers;
    /**
     * <code>util::Pointer<std::size_t> matrix_reps_index_map_;</code>
     */
    private final int[] matrix_reps_index_map;

    /**
     * Creates a CKKSEncoder instance initialized with the specified SEALContext.
     *
     * @param context The SEALContext.
     */
    public CkksEncoder(SealContext context) {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        this.context = context;
        ContextData context_data = context.firstContextData();
        if (context_data.parms().scheme() != SchemeType.CKKS) {
            throw new IllegalArgumentException("unsupported scheme");
        }

        int coeff_count = context_data.parms().polyModulusDegree();
        slots = coeff_count >> 1;
        int logn = UintCore.getPowerOfTwo(coeff_count);

        matrix_reps_index_map = new int[coeff_count];

        // Copy from the matrix to the value vectors
        long gen = 3;
        long pos = 1;
        long m = Integer.toUnsignedLong(coeff_count) << 1;
        for (int i = 0; i < slots; i++) {
            // Position in normal bit order
            long index1 = (pos - 1) >> 1;
            long index2 = (m - pos - 1) >> 1;

            // Set the bit-reversed locations
            matrix_reps_index_map[i] = (int) (Common.reverseBits(index1, logn));
            matrix_reps_index_map[slots | i] = (int) (Common.reverseBits(index2, logn));

            // Next primitive root
            pos *= gen;
            pos &= (m - 1);
        }

        // We need 1~(n-1)-th powers of the primitive 2n-th root, m = 2n
        root_powers = new double[coeff_count][];
        inv_root_powers = new double[coeff_count][];
        // Powers of the primitive 2n-th root have 4-fold symmetry
        if (m >= 8) {
            // complex_roots_ = make_shared<util::ComplexRoots>(util::ComplexRoots(static_cast<size_t>(m), pool_));
            ComplexRoots complex_roots = new ComplexRoots((int) m);
            for (int i = 1; i < coeff_count; i++) {
                // root_powers_[i] = complex_roots_->get_root(reverse_bits(i, logn));
                root_powers[i] = complex_roots.get_root(Common.reverseBits(i, logn));
                // inv_root_powers_[i] = conj(complex_roots_->get_root(reverse_bits(i - 1, logn) + 1));
                inv_root_powers[i] = complex_roots.get_root(Common.reverseBits(i - 1, logn) + 1);
                Arithmetic.conji(inv_root_powers[i]);
            }
        } else if (m == 4) {
            root_powers[1] = new double[]{0, 1};
            inv_root_powers[1] = new double[]{0, -1};
        }
    }

    /**
     * Encodes a double-precision floating-point real number into a plaintext
     * polynomial. The number repeats for N/2 times to fill all slots. The
     * encryption parameters used are the top level parameters for the given
     * context. Dynamic memory allocations in the process are allocated from
     * the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param values      The double-precision floating-point number to encode.
     * @param scale       Scaling parameter defining encoding precision.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(double[] values, double scale, Plaintext destination) {
        double[][] complexValues = Arrays.stream(values)
            .mapToObj(Arithmetic::create)
            .toArray(double[][]::new);
        encode_internal(complexValues, context.firstParmsId(), scale, destination);
    }

    /**
     * Encodes a double-precision floating-point real number into a plaintext
     * polynomial. The number repeats for N/2 times to fill all slots. The
     * encryption parameters used are the top level parameters for the given
     * context. Dynamic memory allocations in the process are allocated from
     * the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param values      The double-precision floating-point number to encode.
     * @param scale       Scaling parameter defining encoding precision.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(double[][] values, double scale, Plaintext destination) {
        encode_internal(values, context.firstParmsId(), scale, destination);
    }

    /**
     * Encodes a double-precision complex number into a plaintext polynomial.
     * Append zeros to fill all slots. Dynamic memory allocations in the process
     * are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param values      The double-precision complex number to encode.
     * @param parms_id    parms_id determining the encryption parameters to be used by the result plaintext.
     * @param scale       Scaling parameter defining encoding precision.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(double[][] values, ParmsId parms_id, double scale, Plaintext destination) {
        encode_internal(values, parms_id, scale, destination);
    }

    private void encode_internal(double[][] values, ParmsId parms_id, double scale, Plaintext destination) {
        // Verify parameters.
        ContextData context_data = context.getContextData(parms_id);
        if (context_data == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }
        // if (!values && values_size > 0)
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        if (values.length > slots) {
            throw new IllegalArgumentException("values_size is too large");
        }

        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_modulus_size = coeff_modulus.length;
        int coeff_count = parms.polyModulusDegree();

        // Quick sanity check
        if (!Common.productFitsIn(false, coeff_modulus_size, coeff_count)) {
            throw new IllegalStateException("invalid parameters");
        }

        // Check that scale is positive and not too large
        if (scale <= 0 || ((int) (Math.log(scale) / Math.log(2)) + 1 >= context_data.totalCoeffModulusBitCount())) {
            throw new IllegalArgumentException("scale out of bounds");
        }

        NttTables[] ntt_tables = context_data.smallNttTables();

        // values_size is guaranteed to be no bigger than slots_
        int n = Common.mulSafe(slots, 2, false);

        double[][] conj_values = new double[n][2];
        for (int i = 0; i < values.length; i++) {
            // conj_values[matrix_reps_index_map_[i]] = values[i];
            Arithmetic.set(conj_values[matrix_reps_index_map[i]], values[i]);
            // TODO: if values are real, the following values should be set to zero, and multiply results by 2.
            // conj_values[matrix_reps_index_map_[i + slots_]] = std::conj(values[i]);
            Arithmetic.set(conj_values[matrix_reps_index_map[i + slots]], values[i]);
            Arithmetic.conji(conj_values[matrix_reps_index_map[i + slots]]);
        }
        double fix = scale / (double) (n);
        double[] fix_scalar = new double[]{fix, 0};
        DwtHandler.transform_from_rev(conj_values, UintCore.getPowerOfTwo(n), inv_root_powers, fix_scalar);

        double max_coeff = 0;
        for (int i = 0; i < n; i++) {
            max_coeff = Math.max(max_coeff, Math.abs(Arithmetic.real(conj_values[i])));
        }
        // Verify that the values are not too large to fit in coeff_modulus
        // Note that we have an extra + 1 for the sign bit
        // Don't compute logarithmis of numbers less than 1
        int max_coeff_bit_count = (int) (Math.ceil(Math.log(Math.max(max_coeff, 1.0)) / Math.log(2))) + 1;
        if (max_coeff_bit_count >= context_data.totalCoeffModulusBitCount()) {
            throw new IllegalArgumentException("encoded values are too large");
        }

        double two_pow_64 = Math.pow(2.0, 64);

        // Resize destination to appropriate size
        // Need to first set parms_id to zero, otherwise resize
        // will throw an exception.
        destination.setParmsId(ParmsId.parmsIdZero());
        destination.resize(Common.mulSafe(coeff_count, coeff_modulus_size, false));

        // Use faster decomposition methods when possible
        if (max_coeff_bit_count <= 64) {
            for (int i = 0; i < n; i++) {
                double coeffd = round(Arithmetic.real(conj_values[i]));
                // bool is_negative = std::signbit(coeffd);
                boolean is_negative = Math.signum(coeffd) < 0;

                long coeffu = (long) (Math.abs(coeffd));

                if (is_negative) {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)] = UintArithmeticSmallMod.negateUintMod(
                            UintArithmeticSmallMod.barrettReduce64(coeffu, coeff_modulus[j]), coeff_modulus[j]
                        );
                    }
                } else {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)]
                            = UintArithmeticSmallMod.barrettReduce64(coeffu, coeff_modulus[j]);
                    }
                }
            }
        } else if (max_coeff_bit_count <= 128) {
            for (int i = 0; i < n; i++) {
                double coeffd = round(Arithmetic.real(conj_values[i]));
                // bool is_negative = std::signbit(coeffd);
                boolean is_negative = Math.signum(coeffd) < 0;
                coeffd = Math.abs(coeffd);

                // std::uint64_t coeffu[2]{ static_cast<std::uint64_t>(std::fmod(coeffd, two_pow_64)),
                //                          static_cast<std::uint64_t>(coeffd / two_pow_64) };
                long[] coeffu = new long[]{(long) (fmod(coeffd, two_pow_64)), (long) (coeffd / two_pow_64)};

                if (is_negative) {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)] = UintArithmeticSmallMod.negateUintMod(
                            UintArithmeticSmallMod.barrettReduce128(coeffu, coeff_modulus[j]), coeff_modulus[j]
                        );
                    }
                } else {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)]
                            = UintArithmeticSmallMod.barrettReduce128(coeffu, coeff_modulus[j]);
                    }
                }
            }
        } else {
            // Slow case
            long[] coeffu = new long[coeff_modulus_size];
            for (int i = 0; i < n; i++) {
                double coeffd = round(Arithmetic.real(conj_values[i]));
                // bool is_negative = std::signbit (coeffd);
                boolean is_negative = Math.signum(coeffd) < 0;
                coeffd = Math.abs(coeffd);

                // We are at this point guaranteed to fit in the allocated space
                UintCore.setZeroUint(coeff_modulus_size, coeffu);
                // auto coeffu_ptr = coeffu.get();
                int coeffu_ptr = 0;
                while (coeffd >= 1) {
                    coeffu[coeffu_ptr] = (long) (fmod(coeffd, two_pow_64));
                    coeffu_ptr++;
                    coeffd /= two_pow_64;
                }

                // Next decompose this coefficient
                context_data.rnsTool().baseQ().decompose(coeffu);

                // Finally replace the sign if necessary
                if (is_negative) {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)]
                            = UintArithmeticSmallMod.negateUintMod(coeffu[j], coeff_modulus[j]);
                    }
                } else {
                    for (int j = 0; j < coeff_modulus_size; j++) {
                        destination.data()[i + (j * coeff_count)] = coeffu[j];
                    }
                }
            }
        }

        // Transform to NTT domain
        for (int i = 0; i < coeff_modulus_size; i++) {
            NttTool.nttNegacyclicHarvey(destination.data(), i * coeff_count, ntt_tables[i]);
        }

        destination.setParmsId(parms_id);
        destination.setScale(scale);
    }

    /**
     * Decodes a plaintext polynomial into double-precision floating-point
     * real or complex numbers. Dynamic memory allocations in the process are
     * allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param plain       The plaintext to decode.
     * @param destination The array to be overwritten with the values in the slots.
     */
    public void decode(final Plaintext plain, double[] destination) {
        double[][] complexDestination = new double[destination.length][2];
        decode_internal(plain, complexDestination);
        for (int i = 0; i < destination.length; i++) {
            destination[i] = Arithmetic.real(complexDestination[i]);
        }
    }

    /**
     * Decodes a plaintext polynomial into double-precision floating-point
     * real or complex numbers. Dynamic memory allocations in the process are
     * allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param plain       The plaintext to decode.
     * @param destination The array to be overwritten with the values in the slots.
     */
    public void decode(final Plaintext plain, double[][] destination) {
        decode_internal(plain, destination);
    }

    private void decode_internal(final Plaintext plain, double[][] destination) {
        // Verify parameters.
        if (!ValCheck.isValidFor(plain, context)) {
            throw new IllegalArgumentException("plain is not valid for encryption parameters");
        }
        if (!plain.isNttForm()) {
            throw new IllegalArgumentException("plain is not in NTT form");
        }
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }

        ContextData context_data = context.getContextData(plain.parmsId());
        EncryptionParameters parms = context_data.parms();
        int coeff_modulus_size = parms.coeffModulus().length;
        int coeff_count = parms.polyModulusDegree();
        int rns_poly_uint64_count = Common.mulSafe(coeff_count, coeff_modulus_size, true);

        NttTables[] ntt_tables = context_data.smallNttTables();

        // Check that scale is positive and not too large
        if (plain.scale() <= 0 || ((int) (Math.log(plain.scale()) / Math.log(2)) >= context_data.totalCoeffModulusBitCount())) {
            throw new IllegalArgumentException("scale out of bounds");
        }

        long[] decryption_modulus = context_data.totalCoeffModulus();
        long[] upper_half_threshold = context_data.upperHalfThreshold();
        int logn = UintCore.getPowerOfTwo(coeff_count);

        // Quick sanity check
        if ((logn < 0) ||
            (coeff_count < Constants.SEAL_POLY_MOD_DEGREE_MIN) || (coeff_count > Constants.SEAL_POLY_MOD_DEGREE_MAX)) {
            throw new IllegalStateException("invalid parameters");
        }

        double inv_scale = 1.0 / plain.scale();

        // Create mutable copy of input
        long[] plain_copy = new long[rns_poly_uint64_count];
        UintCore.setUint(plain.data(), rns_poly_uint64_count, plain_copy);

        // Transform each polynomial from NTT domain
        for (int i = 0; i < coeff_modulus_size; i++) {
            NttTool.inverseNttNegacyclicHarvey(plain_copy, i * coeff_count, ntt_tables[i]);
        }

        // CRT-compose the polynomial
        context_data.rnsTool().baseQ().composeArray(plain_copy, coeff_count);

        // Create floating-point representations of the multi-precision integer coefficients
        double two_pow_64 = Math.pow(2.0, 64);
        // auto res(util::allocate<std::complex<double>>(coeff_count, pool));
        double[][] res = new double[coeff_count][2];
        for (int i = 0; i < coeff_count; i++) {
            // res[i][0] = 0.0;
            if (UintCore.isGreaterThanOrEqualUint(
                plain_copy, i * coeff_modulus_size, upper_half_threshold, 0, coeff_modulus_size
            )) {
                double scaled_two_pow_64 = inv_scale;
                for (int j = 0; j < coeff_modulus_size; j++, scaled_two_pow_64 *= two_pow_64) {
                    if (Long.compareUnsigned(plain_copy[i * coeff_modulus_size + j], decryption_modulus[j]) > 0) {
                        long diff = plain_copy[i * coeff_modulus_size + j] - decryption_modulus[j];
                        Arithmetic.addi(res[i], (diff != 0 ? unsignedLongToDouble(diff) * scaled_two_pow_64 : 0.0));
                    } else {
                        long diff = decryption_modulus[j] - plain_copy[i * coeff_modulus_size + j];
                        Arithmetic.subi(res[i], (diff != 0 ? unsignedLongToDouble(diff) * scaled_two_pow_64 : 0.0));
                    }
                }
            } else {
                double scaled_two_pow_64 = inv_scale;
                for (int j = 0; j < coeff_modulus_size; j++, scaled_two_pow_64 *= two_pow_64) {
                    long curr_coeff = plain_copy[i * coeff_modulus_size + j];
                    Arithmetic.addi(res[i], (curr_coeff != 0 ? unsignedLongToDouble(curr_coeff) * scaled_two_pow_64 : 0.0));
                }
            }

            // Scaling instead incorporated above; this can help in cases
            // where otherwise pow(two_pow_64, j) would overflow due to very
            // large coeff_modulus_size and very large scale
            // res[i] = res_accum * inv_scale;
        }

        DwtHandler.transform_to_rev(res, logn, root_powers, null);

        // SEAL implementation uses vector while here we use double[][].
        // We need to provide error message when destination.length is less than slots.
        Preconditions.checkArgument(
            destination.length >= slots,
            "destination.length should not be less than " + slots + " (" + parms.polyModulusDegree() + " / 2)"
        );
        for (int i = 0; i < slots; i++) {
            // destination[i] = from_complex<T>(res[static_cast<std::size_t>(matrix_reps_index_map_[i])]);
            Arithmetic.set(destination[i], res[matrix_reps_index_map[i]]);
        }
    }

    /**
     * Encodes a double-precision floating-point real number into a plaintext
     * polynomial. The number repeats for N/2 times to fill all slots. The
     * encryption parameters used are the top level parameters for the given
     * context. Dynamic memory allocations in the process are allocated from
     * the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param value       The double-precision floating-point number to encode.
     * @param scale       Scaling parameter defining encoding precision.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(double value, double scale, Plaintext destination) {
        encode_internal(value, context.firstContextData().parmsId(), scale, destination);
    }

    /**
     * Encodes a double-precision complex number into a plaintext polynomial.
     * Append zeros to fill all slots. Dynamic memory allocations in the process
     * are allocated from the memory pool pointed to by the given MemoryPoolHandle.
     *
     * @param value       The double-precision complex number to encode.
     * @param parms_id    parms_id determining the encryption parameters to be used by the result plaintext.
     * @param scale       Scaling parameter defining encoding precision.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(double value, ParmsId parms_id, double scale, Plaintext destination) {
        encode_internal(value, parms_id, scale, destination);
    }

    private void encode_internal(double value, ParmsId parms_id, double scale, Plaintext destination) {
        // Verify parameters.
        ContextData context_data = context.getContextData(parms_id);
        if (context_data == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }

        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_modulus_size = coeff_modulus.length;
        int coeff_count = parms.polyModulusDegree();

        // Quick sanity check
        // if (!product_fits_in(coeff_modulus_size, coeff_count))
        if (!Common.productFitsIn(false, coeff_modulus_size, coeff_count)) {
            throw new IllegalStateException("invalid parameters");
        }

        // Check that scale is positive and not too large
        if (scale <= 0 || ((int) (Math.log(scale) / Math.log(2)) >= context_data.totalCoeffModulusBitCount())) {
            throw new IllegalArgumentException("scale out of bounds");
        }

        // Compute the scaled value
        value *= scale;

        int coeff_bit_count = (int) (Math.log((Math.abs(value))) / Math.log(2)) + 2;
        if (coeff_bit_count >= context_data.totalCoeffModulusBitCount()) {
            throw new IllegalArgumentException("encoded value is too large");
        }

        double two_pow_64 = Math.pow(2.0, 64);

        // Resize destination to appropriate size
        // Need to first set parms_id to zero, otherwise resize
        // will throw an exception.
        // destination.parms_id() = parms_id_zero;
        destination.setParmsId(ParmsId.parmsIdZero());
        destination.resize(coeff_count * coeff_modulus_size);

        double coeffd = round(value);
        // bool is_negative = signbit(coeffd);
        boolean is_negative = Math.signum(coeffd) < 0;
        coeffd = Math.abs(coeffd);

        // Use faster decomposition methods when possible
        if (coeff_bit_count <= 64) {
            long coeffu = (long) (Math.abs(coeffd));

            if (is_negative) {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    long coeffu_mod = UintArithmeticSmallMod.negateUintMod(
                        UintArithmeticSmallMod.barrettReduce64(coeffu, coeff_modulus[j]), coeff_modulus[j]
                    );
                    // fill_n(
                    //     destination.data() + (j * coeff_count), coeff_count,
                    //     negate_uint_mod(barrett_reduce_64(coeffu, coeff_modulus[j]), coeff_modulus[j])
                    // );
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu_mod);
                }
            } else {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    // fill_n(
                    //     destination.data() + (j * coeff_count), coeff_count,
                    //     barrett_reduce_64(coeffu, coeff_modulus[j])
                    // );
                    long coeffu_mod = UintArithmeticSmallMod.barrettReduce64(coeffu, coeff_modulus[j]);
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu_mod);
                }
            }
        } else if (coeff_bit_count <= 128) {
            // uint64_t coeffu[2]{ static_cast<uint64_t>(fmod(coeffd, two_pow_64)),
            //                     static_cast<uint64_t>(coeffd / two_pow_64) };
            long[] coeffu = new long[]{(long) (fmod(coeffd, two_pow_64)), (long) (coeffd / two_pow_64)};
            if (is_negative) {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    // fill_n(
                    //     destination.data() + (j * coeff_count), coeff_count,
                    //     negate_uint_mod(barrett_reduce_128(coeffu, coeff_modulus[j]), coeff_modulus[j])
                    // );
                    long coeffu_mod = UintArithmeticSmallMod.negateUintMod(
                        UintArithmeticSmallMod.barrettReduce128(coeffu, coeff_modulus[j]), coeff_modulus[j]
                    );
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu_mod);
                }
            } else {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    // fill_n(
                    //     destination.data() + (j * coeff_count), coeff_count,
                    //     barrett_reduce_128(coeffu, coeff_modulus[j])
                    // );
                    long coeffu_mod = UintArithmeticSmallMod.barrettReduce128(coeffu, coeff_modulus[j]);
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu_mod);
                }
            }
        } else {
            // Slow case
            // auto coeffu (allocate_uint(coeff_modulus_size, pool));
            long[] coeffu = new long[coeff_modulus_size];

            // We are at this point guaranteed to fit in the allocated space
            // set_zero_uint(coeff_modulus_size, coeffu.get());
            UintCore.setZeroUint(coeff_modulus_size, coeffu);
            // auto coeffu_ptr = coeffu.get();
            int coeffu_ptr = 0;
            while (coeffd >= 1) {
                // *coeffu_ptr++ = static_cast < uint64_t > (fmod(coeffd, two_pow_64));
                coeffu[coeffu_ptr] = (long) (fmod(coeffd, two_pow_64));
                coeffu_ptr++;
                coeffd /= two_pow_64;
            }

            // Next decompose this coefficient
            context_data.rnsTool().baseQ().decompose(coeffu);

            // Finally replace the sign if necessary
            if (is_negative) {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    // fill_n(
                    //     destination.data() + (j * coeff_count), coeff_count,
                    //     negate_uint_mod(coeffu[j], coeff_modulus[j])
                    // );
                    long coeffu_mod = UintArithmeticSmallMod.negateUintMod(coeffu[j], coeff_modulus[j]);
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu_mod);
                }
            } else {
                for (int j = 0; j < coeff_modulus_size; j++) {
                    // fill_n(destination.data() + (j * coeff_count), coeff_count, coeffu[j]);
                    Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, coeffu[j]);
                }
            }
        }

        destination.setParmsId(parms_id);
        destination.setScale(scale);
    }

    /**
     * Encodes an integer number into a plaintext polynomial without any scaling.
     * The number repeats for N/2 times to fill all slots. The encryption
     * parameters used are the top level parameters for the given context.
     *
     * @param value       The integer number to encode.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(long value, Plaintext destination) {
        encode_internal(value, context.firstParmsId(), destination);
    }

    /**
     * Encodes an integer number into a plaintext polynomial without any scaling.
     * The number repeats for N/2 times to fill all slots.
     *
     * @param value       The integer number to encode.
     * @param parms_id    parms_id determining the encryption parameters to be used by the result plaintext.
     * @param destination The plaintext polynomial to overwrite with the result.
     */
    public void encode(long value, ParmsId parms_id, Plaintext destination) {
        encode_internal(value, parms_id, destination);
    }

    private void encode_internal(long value, ParmsId parms_id, Plaintext destination) {
        // Verify parameters.
        ContextData context_data = context.getContextData(parms_id);
        if (context_data == null) {
            throw new IllegalArgumentException("parms_id is not valid for encryption parameters");
        }

        EncryptionParameters parms = context_data.parms();
        Modulus[] coeff_modulus = parms.coeffModulus();
        int coeff_modulus_size = coeff_modulus.length;
        int coeff_count = parms.polyModulusDegree();

        // Quick sanity check
        if (!Common.productFitsIn(false, coeff_modulus_size, coeff_count)) {
            throw new IllegalStateException("invalid parameters");
        }

        // int coeff_bit_count = get_significant_bit_count(static_cast<uint64_t>(llabs(value))) + 2;
        int coeff_bit_count = UintCore.getSignificantBitCount(Math.abs(value)) + 2;
        if (coeff_bit_count >= context_data.totalCoeffModulusBitCount()) {
            throw new IllegalArgumentException("encoded value is too large");
        }

        // Resize destination to appropriate size
        // Need to first set parms_id to zero, otherwise resize
        // will throw an exception.
        destination.setParmsId(ParmsId.parmsIdZero());
        destination.resize(coeff_count * coeff_modulus_size);

        if (value < 0) {
            for (int j = 0; j < coeff_modulus_size; j++) {
                long tmp = value;
                tmp += coeff_modulus[j].value();
                tmp = UintArithmeticSmallMod.barrettReduce64(tmp, coeff_modulus[j]);
                // fill_n(destination.data() + (j * coeff_count), coeff_count, tmp);
                Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, tmp);
            }
        } else {
            for (int j = 0; j < coeff_modulus_size; j++) {
                long tmp = value;
                tmp = UintArithmeticSmallMod.barrettReduce64(tmp, coeff_modulus[j]);
                // fill_n(destination.data() + (j * coeff_count), coeff_count, tmp);
                Arrays.fill(destination.data(), j * coeff_count, j * coeff_count + coeff_count, tmp);
            }
        }

        destination.setParmsId(parms_id);
        destination.setScale(1.0);
    }

    /**
     * Returns the number of complex numbers encoded.
     *
     * @return the number of complex numbers encoded.
     */
    public int slotCount() {
        return slots;
    }

    /**
     * Computes round. Given <code>d</code>, we cannot simply compute and return <code>Math.round(d)</code>, because
     * when <code>d > Long.MAX_VALUE </code> or <code>d < Long.MIN_VALUE</code>, <code>Math.round(d)</code> will return
     * a double that represents <code>0x7FFFFFFF_FFFFFFFFL</code>. We need to handle this special case.
     *
     * @param d the double to round.
     * @return the rounded double.
     */
    static double round(double d) {
        /*
         * The most correct way to do that is:
         *     BigDecimal bigDecimal = BigDecimal.valueOf(d);
         *     bigDecimal = bigDecimal.setScale(0, RoundingMode.HALF_UP);
         *     return bigDecimal.doubleValue();
         * Here we find a more efficient way to do that.
         */
        if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
            return d;
        } else {
            return Math.round(d);
        }
    }

    /**
     * Computes fmod. Given <code>d</code> and <code>m</code>, we cannot directly compute <code>r = d % m</code> and
     * return <code>(long) r</code>, because when <code>r > Long.MAX_VALUE </code> or <code>r < Long.MIN_VALUE</code>,
     * <code>(long) r</code> will return <code>0x7FFFFFFF_FFFFFFFFL</code>. We need to handle this special case.
     *
     * @param d the dividend.
     * @param m the divisor.
     * @return the result of <code>d % m</code>.
     */
    static double fmod(double d, double m) {
        double r = d % m;
        if (r > Long.MAX_VALUE || r < Long.MIN_VALUE) {
            // In this case, (long) r returns 0x7fffffffffffffffL.
            // We have tried Double.doubleToLongBits() and other methods, but none of them can correctly do that.
            return BigDecimal.valueOf(r).longValue();
        } else {
            // In this case, (long) r can correctly return result.
            return r;
        }
    }

    /**
     * Converts an unsigned long represented as <code>long</code> to a positive <code>double</code>. If we directly
     * converts <code>long</code> to a double, the sign bit will be interpreted as the sign bit of the double, thus the
     * result will be negative. This method can correctly convert the unsigned long to a double.
     * <p>
     * See <a href="https://stackoverflow.com/questions/24193788/convert-unsigned-64-bit-decimal-to-java-double">
     * Convert unsigned 64-bit decimal to Java double</a> for more details.
     *
     * @param longValue unsigned long represented as <code>long</code>.
     * @return the converted positive <code>double</code>.
     */
    static double unsignedLongToDouble(long longValue) {
        double doubleValue = (double) (longValue & 0x7fffffffffffffffL);
        if (longValue < 0) {
            doubleValue += 0x1.0p63;
        }
        return doubleValue;
    }
}
