#pragma once
#include "seal/seal.h"
namespace seal
{
    namespace util
    {
        void dyadic_coeffmod(
                ConstCoeffIter operand1, CoeffIter operand2, size_t coeff_count, const Modulus &modulus,
                CoeffIter result);
        inline void dyadic_coeffmod(
                ConstRNSIter operand1, RNSIter operand2, std::size_t coeff_modulus_size, ConstModulusIter modulus,
                RNSIter result)
        {
#ifdef SEAL_DEBUG
            if (!operand1 && coeff_modulus_size > 0) {
        throw std::invalid_argument("operand1");
    }
    if (!operand2 && coeff_modulus_size > 0) {
        throw std::invalid_argument("operand2");
    }
    if (!result && coeff_modulus_size > 0) {
        throw std::invalid_argument("result");
    }
    if (!modulus && coeff_modulus_size > 0) {
        throw std::invalid_argument("modulus");
    }
    if (operand1.poly_modulus_degree() != result.poly_modulus_degree() ||
        operand2.poly_modulus_degree() != result.poly_modulus_degree()) {
        throw std::invalid_argument("incompatible iterators");
    }
#endif
            auto poly_modulus_degree = result.poly_modulus_degree();
            SEAL_ITERATE(iter(operand1, operand2, modulus, result), coeff_modulus_size, [&](auto I) {
                dyadic_coeffmod(get<0>(I), get<1>(I), poly_modulus_degree, get<2>(I), get<3>(I));
            });
        }
        inline void dyadic_coeffmod(
                ConstPolyIter operand1, PolyIter operand2, std::size_t size, ConstModulusIter modulus, PolyIter result)
        {
#ifdef SEAL_DEBUG
            if (!operand1 && size > 0) {
        throw std::invalid_argument("operand1");
    }
    if (!operand2 && size > 0) {
        throw std::invalid_argument("operand2");
    }
    if (!result && size > 0) {
        throw std::invalid_argument("result");
    }
    if (!modulus && size > 0) {
        throw std::invalid_argument("modulus");
    }
    if (operand1.coeff_modulus_size() != result.coeff_modulus_size() ||
        operand2.coeff_modulus_size() != result.coeff_modulus_size()) {
        throw std::invalid_argument("incompatible iterators");
    }
#endif
            auto coeff_modulus_size = result.coeff_modulus_size();
            SEAL_ITERATE(iter(operand1, operand2, result), size, [&](auto I) {
                dyadic_coeffmod(get<0>(I), get<1>(I), coeff_modulus_size, modulus, get<2>(I));
            });
        }
        void dyadic_product_accumulate(
                ConstCoeffIter operand1, ConstCoeffIter operand2, size_t coeff_count,
                CoeffIter result_low, CoeffIter result_high);
        inline void dyadic_product_accumulate(
                ConstRNSIter operand1, ConstRNSIter operand2, std::size_t coeff_modulus_size,
                RNSIter result_low, RNSIter result_high)
        {
#ifdef SEAL_DEBUG
            if (!operand1 && coeff_modulus_size > 0) {
        throw std::invalid_argument("operand1");
    }
    if (!operand2 && coeff_modulus_size > 0) {
        throw std::invalid_argument("operand2");
    }
    if (!result_low && coeff_modulus_size > 0) {
        throw std::invalid_argument("result");
    }
    if (coeff_modulus_size > 0) {
        throw std::invalid_argument("modulus");
    }
    if (operand1.poly_modulus_degree() != result_low.poly_modulus_degree() ||
        operand2.poly_modulus_degree() != result_low.poly_modulus_degree()) {
        throw std::invalid_argument("incompatible iterators");
    }
#endif
            auto poly_modulus_degree = result_low.poly_modulus_degree();
            SEAL_ITERATE(iter(operand1, operand2, result_low, result_high), coeff_modulus_size, [&](auto I) {
                dyadic_product_accumulate(get<0>(I), get<1>(I), poly_modulus_degree, get<2>(I), get<3>(I));
            });
        }
        inline void dyadic_product_accumulate(
                ConstPolyIter operand1, ConstPolyIter operand2, std::size_t size, PolyIter result_low, PolyIter result_high)
        {
#ifdef SEAL_DEBUG
            if (!operand1 && size > 0) {
        throw std::invalid_argument("operand1");
    }
    if (!operand2 && size > 0) {
        throw std::invalid_argument("operand2");
    }
    if (!result_low && size > 0) {
        throw std::invalid_argument("result");
    }
    if (operand1.coeff_modulus_size() != result_low.coeff_modulus_size() ||
        operand2.coeff_modulus_size() != result_low.coeff_modulus_size()) {
        throw std::invalid_argument("incompatible iterators");
    }
#endif
            auto coeff_modulus_size = result_low.coeff_modulus_size();
            SEAL_ITERATE(iter(operand1, operand2, result_low, result_high), size, [&](auto I) {
                dyadic_product_accumulate(get<0>(I), get<1>(I), coeff_modulus_size, get<2>(I), get<3>(I));
            });
        }
    } // namespace util
} // namespace seal