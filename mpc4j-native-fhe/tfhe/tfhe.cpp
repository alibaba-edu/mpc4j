#include "tfhe.h"
#include "seal/util/iterator.h"
#include "seal/util/polyarithsmallmod.h"
#include "seal/util/scalingvariant.h"
#include "util.h"

using namespace std;
namespace seal
{

    void TFHEcipher::encrypt_zero(RLWECipher &cipher)
    {
        encryptor_.encrypt_zero(cipher);
        evaluator_.transform_to_ntt_inplace(cipher);
    }

    inline uint64_t GadgetRed(uint64_t plain, size_t idx, const seal::Modulus &modulus)
    {
        size_t Qlen = targetP::digits - (idx + 1) * targetP::Bgbit;
        size_t Wlen = 63;
        uint64_t res = util::barrett_reduce_64(plain << (Qlen % Wlen), modulus);
        for (size_t i = 0; i < Qlen / Wlen; i++) {
            res = util::multiply_uint_mod(res, (1ULL << Wlen), modulus);
        }
        return res;
    }

    void TFHEcipher::encrypt(Plaintext &plain, RGSWCipher &cipher)
    {
        size_t rlwe_count = 2 * targetP::l_;
        auto &modulus = parms.coeff_modulus();
        cipher.reserve(rlwe_count);
        for (size_t i = 0; i < rlwe_count; i++) {
            RLWECipher tmp(context_);
            encrypt_zero(tmp);
            cipher.emplace_back(move(tmp));
        }
        Plaintext scaled_plain(poly_modulus_degree * coeff_modulus_size);
        auto scaled_plain_iter = util::RNSIter(scaled_plain.data(), poly_modulus_degree);
        for (size_t k = 0; k < 2; k++) {
            for (size_t i = 0; i < targetP::l_; i++) {
                for (size_t m = 0; m < coeff_modulus_size; m++) {
                    uint64_t red = GadgetRed(1, i, modulus[m]);
                    util::multiply_poly_scalar_coeffmod(plain.data() + m * poly_modulus_degree, poly_modulus_degree, red, modulus[m], scaled_plain.data() + m * poly_modulus_degree);
                }
                auto cipher_iter = util::RNSIter(cipher[i + k * targetP::l_].data(k), poly_modulus_degree);
                util::add_poly_coeffmod(scaled_plain_iter, cipher_iter, coeff_modulus_size, modulus, cipher_iter);
            }
        }
    }
#ifdef SEAL_USE_INTEL_HEXL
    void TFHEcipher::ExternalProduct(RLWECipher &dst, RLWECipher &src, RGSWCipher operand)
{
    auto &coeff_modulus = parms.coeff_modulus();
    vector<vector<uint64_t>> crtdec[2];
    for (size_t i = 0; i < 2; i++) {
        rns_.CRTDecPoly(src.data(i), crtdec[i]);
    }
    RLWECipher decntt(context_), product(context_);
    decntt.resize(2);
    product.resize(2);
    util::RNSIter res_iter0(dst.data(0), poly_modulus_degree);
    util::RNSIter res_iter1(dst.data(1), poly_modulus_degree);
    util::RNSIter decntt_iter(decntt.data(1), poly_modulus_degree);
    util::RNSIter prod_iter0(product.data(0), poly_modulus_degree);
    util::RNSIter prod_iter1(product.data(1), poly_modulus_degree);
    for (size_t k = 0; k < 2; k++) {
        for (size_t i = 0; i < targetP::l_; i++) {
            for (size_t j = 0; j < poly_modulus_degree; j++) {
                for (size_t l = 0; l < coeff_modulus_size; l++) {
                    decntt.data(1)[j + l * poly_modulus_degree] = crtdec[k][i][j];
                }
            }
            util::RNSIter data_iter0(operand[i + k * targetP::l_].data(0), poly_modulus_degree);
            util::RNSIter data_iter1(operand[i + k * targetP::l_].data(1), poly_modulus_degree);
            util::ntt_negacyclic_harvey_lazy(decntt_iter, coeff_modulus_size,
                                             context_.first_context_data()->small_ntt_tables());
            util::dyadic_product_coeffmod(decntt_iter, data_iter0, coeff_modulus_size, coeff_modulus, prod_iter0);
            util::dyadic_product_coeffmod(decntt_iter, data_iter1, coeff_modulus_size, coeff_modulus, prod_iter1);
            util::add_poly_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
            util::add_poly_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);
        }
    }
}
#else

    [[maybe_unused]] void TFHEcipher::ExternalProduct_internal(util::RNSIter res_iter0, util::RNSIter res_iter1, util::RNSIter decntt_iter, util::RNSIter prod_iter0, util::RNSIter prod_iter1, RLWECipher &src, RGSWCipher operand)
    {
        RLWECipher decntt(context_), product(context_);
        decntt.resize(2);
        product.resize(2);
        auto &coeff_modulus = parms.coeff_modulus();
        vector<vector<uint64_t>> crtdec[2];
        for (size_t i = 0; i < 2; i++)
            rns_.CRTDecPoly(src.data(i), crtdec[i]);
        for (size_t k = 0; k < 2; k++)
            for (size_t i = 0; i < targetP::l_; i++) {
                for (size_t j = 0; j < poly_modulus_degree; j++)
                    for (size_t l = 0; l < coeff_modulus_size; l++)
                        decntt.data(1)[j + l * poly_modulus_degree] = crtdec[k][i][j];
                util::RNSIter data_iter0(operand[i + k * targetP::l_].data(0), poly_modulus_degree);
                util::RNSIter data_iter1(operand[i + k * targetP::l_].data(1), poly_modulus_degree);
                decntt.is_ntt_form() = false;
                product.is_ntt_form() = true;
                util::ntt_negacyclic_harvey_lazy(decntt_iter, coeff_modulus_size, context_.first_context_data()->small_ntt_tables());
                util::dyadic_product_accumulate(decntt_iter, data_iter0, coeff_modulus_size, res_iter0, prod_iter0);
                util::dyadic_product_accumulate(decntt_iter, data_iter1, coeff_modulus_size, res_iter1, prod_iter1);
            }
        util::dyadic_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
        util::dyadic_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);
    }

    void TFHEcipher::ExternalProduct(RLWECipher &dst, RLWECipher &src, RGSWCipher operand)
    {
        RLWECipher decntt(context_), product(context_);
        decntt.resize(2);
        product.resize(2);
        util::RNSIter res_iter0(dst.data(0), poly_modulus_degree);
        util::RNSIter res_iter1(dst.data(1), poly_modulus_degree);
        util::RNSIter decntt_iter(decntt.data(1), poly_modulus_degree);
        util::RNSIter prod_iter0(product.data(0), poly_modulus_degree);
        util::RNSIter prod_iter1(product.data(1), poly_modulus_degree);
        auto &coeff_modulus = parms.coeff_modulus();
        vector<vector<uint64_t>> crtdec[2];
        for (size_t i = 0; i < 2; i++)
            rns_.CRTDecPoly(src.data(i), crtdec[i]);
        for (size_t k = 0; k < 2; k++)
            for (size_t i = 0; i < targetP::l_; i++) {
                for (size_t j = 0; j < poly_modulus_degree; j++)
                    for (size_t l = 0; l < coeff_modulus_size; l++)
                        decntt.data(1)[j + l * poly_modulus_degree] = crtdec[k][i][j];
                util::RNSIter data_iter0(operand[i + k * targetP::l_].data(0), poly_modulus_degree);
                util::RNSIter data_iter1(operand[i + k * targetP::l_].data(1), poly_modulus_degree);
                decntt.is_ntt_form() = false;
                product.is_ntt_form() = true;
                util::ntt_negacyclic_harvey_lazy(decntt_iter, coeff_modulus_size, context_.first_context_data()->small_ntt_tables());
                util::dyadic_product_accumulate(decntt_iter, data_iter0, coeff_modulus_size, res_iter0, prod_iter0);
                util::dyadic_product_accumulate(decntt_iter, data_iter1, coeff_modulus_size, res_iter1, prod_iter1);
            }
        util::dyadic_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
        util::dyadic_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);
    }

#endif
} // namespace seal
