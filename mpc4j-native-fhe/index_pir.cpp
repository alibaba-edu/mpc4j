#include "index_pir.h"
#include "seal/seal.h"

using namespace std;
using namespace seal;

uint32_t compute_expansion_ratio(const EncryptionParameters& params) {
    uint32_t expansion_ratio = 0;
    auto pt_bits_per_coeff = (uint32_t) log2(params.plain_modulus().value());
    for (const auto & i : params.coeff_modulus()) {
        double coeff_bit_size = log2(i.value());
        expansion_ratio += ceil(coeff_bit_size / pt_bits_per_coeff);
    }
    return expansion_ratio;
}

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& params, const Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(params.plain_modulus().value());
    const auto coeff_count = params.poly_modulus_degree();
    const auto coeff_mod_count = params.coeff_modulus().size();
    const uint64_t pt_bitmask = (1 << pt_bits_per_coeff) - 1;
    vector<Plaintext> result(compute_expansion_ratio(params) * ct.size());
    auto pt_iter = result.begin();
    for (size_t poly_index = 0; poly_index < ct.size(); ++poly_index) {
        for (size_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(params.coeff_modulus()[coeff_mod_index].value());
            const size_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            size_t shift = 0;
            for (size_t i = 0; i < local_expansion_ratio; ++i) {
                pt_iter->resize(coeff_count);
                for (size_t c = 0; c < coeff_count; ++c) {
                    (*pt_iter)[c] = (ct.data(poly_index)[coeff_mod_index * coeff_count + c] >> shift) & pt_bitmask;
                }
                ++pt_iter;
                shift += pt_bits_per_coeff;
            }
        }
    }
    return result;
}

void compose_to_ciphertext(const EncryptionParameters& params, const vector<Plaintext> &pts, Ciphertext &ct) {
    return compose_to_ciphertext(params, pts.begin(), pts.size() / compute_expansion_ratio(params), ct);
}

void compose_to_ciphertext(const EncryptionParameters& params, vector<Plaintext>::const_iterator pt_iter,
                           const size_t ct_poly_count, Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(params.plain_modulus().value());
    const auto coeff_count = params.poly_modulus_degree();
    const auto coeff_mod_count = params.coeff_modulus().size();
    ct.resize(ct_poly_count);
    for (size_t poly_index = 0; poly_index < ct_poly_count; ++poly_index) {
        for (size_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(params.coeff_modulus()[coeff_mod_index].value());
            const size_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            size_t shift = 0;
            for (size_t i = 0; i < local_expansion_ratio; ++i) {
                for (size_t c = 0; c < pt_iter->coeff_count(); ++c) {
                    if (shift == 0) {
                        ct.data(poly_index)[coeff_mod_index * coeff_count + c] = (*pt_iter)[c];
                    } else {
                        ct.data(poly_index)[coeff_mod_index * coeff_count + c] += ((*pt_iter)[c] << shift);
                    }
                }
                ++pt_iter;
                shift += pt_bits_per_coeff;
            }
        }
    }
}
