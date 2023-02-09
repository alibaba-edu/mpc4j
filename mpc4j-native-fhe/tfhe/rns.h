#pragma once
#include "params.h"
#include "seal/seal.h"
#include "seal/util/common.h"
#include "seal/util/ntt.h"
#include "seal/util/polyarithsmallmod.h"
#include "seal/util/scalingvariant.h"
#include <bitset>
#include <cassert>
#include <cstddef>
#include <cstdint>
#include <vector>
using namespace std;
namespace seal
{

    class TFHERNS
    {
    public:
        explicit TFHERNS(SEALContext &context) : context_(context)
        {
            auto &context_data = *context_.first_context_data();
            auto &parms = context_data.parms();
            auto &coeff_modulus = parms.coeff_modulus();
            auto Mcnt = coeff_modulus.size();
            auto Qlen = targetP::digits;
            size_t Wlen = targetP::Bgbit;
            Qword = Qlen / Wlen + 1;
            coeff_modulus_size = Mcnt;
            poly_degree = parms.poly_modulus_degree();
            auto punctured_prod_array = context_data.rns_tool()->base_q()->punctured_prod_array();
            auto inv_punctured_prod = context_data.rns_tool()->base_q()->inv_punctured_prod_mod_base_array();
            auto base_prod = context_data.rns_tool()->base_q()->base_prod();
            size_t Qsize = context_data.rns_tool()->base_q()->size();
            Qrst.resize(Mcnt);
            Qinv.resize(Mcnt);
            Qtot.resize(Qword);
            basered(base_prod, Qtot, Qsize, 0);
            for (size_t i = 0; i < Mcnt; i++)
                Qinv[i] = inv_punctured_prod[i].operand;
            for (size_t i = 0; i < Mcnt; i++) {
                Qrst[i].resize(Qword);
                basered(punctured_prod_array + Qsize * i, Qrst[i], Qsize, 0);
            }
        }
        void CRTDecPoly(Ciphertext::ct_coeff_type *poly, vector<vector<uint64_t>> &crtdec)
        {
            size_t Wlen = targetP::Bgbit;
            uint64_t mask = targetP::Bg - 1;
            auto &coeff_modulus = context_.first_context_data()->parms().coeff_modulus();
            for (size_t i = 0; i < coeff_modulus_size; i++)
                for (size_t j = 0; j < poly_degree; j++)
                    poly[j + i * poly_degree] = util::multiply_uint_mod(poly[j + i * poly_degree], Qinv[i], coeff_modulus[i]);
            crtdec.resize(Qword);
            for (size_t k = 0; k < Qword; k++)
                crtdec[k].resize(poly_degree);
            for (size_t j = 0; j < poly_degree; j++)
                for (size_t i = 0; i < coeff_modulus_size; i++) {
                    __uint128_t prod = 0;
                    for (size_t k = 0; k < Qword; k++) {
                        uint64_t val = Qrst[i][Qword - 1 - k];
                        prod += static_cast<__uint128_t>(poly[j + i * poly_degree]) * val + crtdec[Qword - 1 - k][j];
                        crtdec[Qword - 1 - k][j] = prod & mask;
                        prod >>= Wlen;
                    }
                }
        }

        inline void basered(const uint64_t *vec_in, vector<uint64_t> &vec_out, size_t in_cnt, const size_t sft) const
        {
            std::bitset<targetP::digits + targetP::Bgbit + 4> buff(0);
            std::bitset<targetP::digits + targetP::Bgbit + 4> mask(targetP::Bg - 1);
            for (size_t i = 0; i < in_cnt; i++) {
                buff <<= 64;
                buff |= vec_in[in_cnt - 1 - i];
            }
            buff <<= sft;
            for (size_t i = 0; i < Qword; i++) {
                if ((i + 1) * targetP::Bgbit > targetP::digits)
                    vec_out[i] = static_cast<uint64_t>(((buff << ((i + 1) * targetP::Bgbit - targetP::digits)) & mask).to_ulong());
                else
                    vec_out[i] = static_cast<uint64_t>(((buff >> (targetP::digits - (i + 1) * targetP::Bgbit)) & mask).to_ulong());
            }
        }
        vector<vector<uint64_t>> Qrst;
        vector<uint64_t> Qtot;
        vector<uint64_t> Qinv;
        SEALContext context_;
        size_t coeff_modulus_size;
        size_t poly_degree;
        size_t Qword;
    };
} // namespace seal
