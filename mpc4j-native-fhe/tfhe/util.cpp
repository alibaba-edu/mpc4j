#include "util.h"
namespace seal
{
    namespace util
    {
        void dyadic_coeffmod(
                ConstCoeffIter operand1, CoeffIter operand2, size_t coeff_count, const Modulus &modulus,
                CoeffIter result)
        {
#ifdef SEAL_DEBUG
            if (!operand1) {
        throw invalid_argument("operand1");
    }
    if (!operand2) {
        throw invalid_argument("operand2");
    }
    if (!result) {
        throw invalid_argument("result");
    }
    if (coeff_count == 0) {
        throw invalid_argument("coeff_count");
    }
    if (modulus.is_zero()) {
        throw invalid_argument("modulus");
    }
#endif

            const uint64_t modulus_value = modulus.value();
            const uint64_t const_ratio_0 = modulus.const_ratio()[0];
            const uint64_t const_ratio_1 = modulus.const_ratio()[1];

            SEAL_ITERATE(iter(operand1, operand2, result), coeff_count, [&](auto I) {
                // Reduces z using base 2^64 Barrett reduction
                unsigned long long z[2], tmp1, tmp2[2], tmp3, carry;
                // multiply_uint64(get<0>(I), get<1>(I), z);

                // Multiply input and const_ratio
                // Round 1
                multiply_uint64_hw64(get<0>(I), const_ratio_0, &carry);
                multiply_uint64(get<0>(I), const_ratio_1, tmp2);
                tmp3 = tmp2[1] + add_uint64(tmp2[0], carry, &tmp1);

                // Round 2
                multiply_uint64(get<1>(I), const_ratio_0, tmp2);
                carry = tmp2[1] + add_uint64(tmp1, tmp2[0], &tmp1);

                // This is all we care about
                tmp1 = get<1>(I) * const_ratio_1 + tmp3 + carry;

                // Barrett subtraction
                tmp3 = get<0>(I) - tmp1 * modulus_value;

                // Claim: One more subtraction is enough
                get<1>(I) = 0;
                get<2>(I) = SEAL_COND_SELECT(tmp3 >= modulus_value, tmp3 - modulus_value, tmp3);
            });
        }
        void dyadic_product_accumulate(
                ConstCoeffIter operand1, ConstCoeffIter operand2, size_t coeff_count,
                CoeffIter result_low, CoeffIter result_high)
        {
#ifdef SEAL_DEBUG
            if (!operand1) {
        throw invalid_argument("operand1");
    }
    if (!operand2) {
        throw invalid_argument("operand2");
    }
    if (!result) {
        throw invalid_argument("result");
    }
    if (coeff_count == 0) {
        throw invalid_argument("coeff_count");
    }
    if (modulus.is_zero()) {
        throw invalid_argument("modulus");
    }
#endif

            SEAL_ITERATE(iter(operand1, operand2, result_low, result_high), coeff_count, [&](auto I) {
                // Reduces z using base 2^64 Barrett reduction
                unsigned long long z[2];
                multiply_uint64(get<0>(I), get<1>(I), z);
                get<3>(I) += z[1] + add_uint64(z[0], get<2>(I), &get<2>(I));
            });
        }
    } // namespace util
} // namespace seal
