#include "utils.h"
#include "seal/seal.h"
#include "seal/util/polyarithsmallmod.h"
#include "seal/util/scalingvariant.h"

using namespace std;
using namespace seal;

uint32_t compute_expansion_ratio(const EncryptionParameters& parms) {
    uint32_t expansion_ratio = 0;
    auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    for (const auto & i : parms.coeff_modulus()) {
        double coeff_bit_size = log2(i.value());
        expansion_ratio += ceil(coeff_bit_size / pt_bits_per_coeff);
    }
    return expansion_ratio;
}

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& parms, const Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    const auto coeff_count = parms.poly_modulus_degree();
    const auto coeff_mod_count = parms.coeff_modulus().size();
    const uint64_t pt_bitmask = (1 << pt_bits_per_coeff) - 1;
    vector<Plaintext> result(compute_expansion_ratio(parms) * ct.size());
    auto pt_iter = result.begin();
    for (uint32_t poly_index = 0; poly_index < ct.size(); ++poly_index) {
        for (uint32_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(parms.coeff_modulus()[coeff_mod_index].value());
            const uint32_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            uint32_t shift = 0;
            for (uint32_t i = 0; i < local_expansion_ratio; ++i) {
                pt_iter->resize(coeff_count);
                for (uint32_t c = 0; c < coeff_count; ++c) {
                    (*pt_iter)[c] = (ct.data(poly_index)[coeff_mod_index * coeff_count + c] >> shift) & pt_bitmask;
                }
                ++pt_iter;
                shift += pt_bits_per_coeff;
            }
        }
    }
    return result;
}

void compose_to_ciphertext(const EncryptionParameters& parms, const vector<Plaintext> &pts, Ciphertext &ct) {
    return compose_to_ciphertext(parms, pts.begin(), pts.size() / compute_expansion_ratio(parms), ct);
}

void compose_to_ciphertext(const EncryptionParameters& parms, vector<Plaintext>::const_iterator pt_iter,
                           const uint32_t ct_poly_count, Ciphertext &ct) {
    const auto pt_bits_per_coeff = (uint32_t) log2(parms.plain_modulus().value());
    const auto coeff_count = parms.poly_modulus_degree();
    const auto coeff_mod_count = parms.coeff_modulus().size();
    ct.resize(ct_poly_count);
    for (uint32_t poly_index = 0; poly_index < ct_poly_count; ++poly_index) {
        for (uint32_t coeff_mod_index = 0; coeff_mod_index < coeff_mod_count; ++coeff_mod_index) {
            const double coeff_bit_size = log2(parms.coeff_modulus()[coeff_mod_index].value());
            const uint32_t local_expansion_ratio = ceil(coeff_bit_size / pt_bits_per_coeff);
            uint32_t shift = 0;
            for (uint32_t i = 0; i < local_expansion_ratio; ++i) {
                for (uint32_t c = 0; c < pt_iter->coeff_count(); ++c) {
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

vector<Ciphertext> expand_query(const EncryptionParameters& parms, const Ciphertext &encrypted,
                                const GaloisKeys& galois_keys, uint32_t m) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    // Assume that m is a power of 2. If not, round it to the next power of 2.
    uint32_t logm = ceil(log2(m));
    Plaintext two("2");
    vector<uint32_t> galois_elts;
    auto n = parms.poly_modulus_degree();
    if (logm > ceil(log2(n))) {
        throw logic_error("m > n is not allowed.");
    }
    for (int i = 0; i < ceil(log2(n)); i++) {
        galois_elts.push_back((n + seal::util::exponentiate_uint(2, i)) /seal::util::exponentiate_uint(2, i));
    }
    vector<Ciphertext> temp;
    temp.push_back(encrypted);
    Ciphertext tempctxt;
    Ciphertext tempctxt_rotated;
    Ciphertext tempctxt_shifted;
    Ciphertext tempctxt_rotatedshifted;
    for (uint32_t i = 0; i < logm - 1; i++) {
        vector<Ciphertext> newtemp(temp.size() << 1);
        // temp[a] = (j0 = a (mod 2**i) ? ) : Enc(x^{j0 - a}) else Enc(0).
        uint32_t index_raw = (n << 1) - (1 << i);
        uint32_t index = (index_raw * galois_elts[i]) % (n << 1);

        for (uint32_t a = 0; a < temp.size(); a++) {
            evaluator.apply_galois(temp[a], galois_elts[i], galois_keys,tempctxt_rotated);
            evaluator.add(temp[a], tempctxt_rotated, newtemp[a]);
            multiply_power_of_X(temp[a], tempctxt_shifted, index_raw, context);
            // cout << "mul by x^pow: " <<
            multiply_power_of_X(tempctxt_rotated, tempctxt_rotatedshifted, index, context);
            // cout << "mul by x^pow: " <<
            // Enc(2^i x^j) if j = 0 (mod 2**i).
            evaluator.add(tempctxt_shifted, tempctxt_rotatedshifted,newtemp[a + temp.size()]);
        }
        temp = newtemp;
    }
    // Last step of the loop
    vector<Ciphertext> newtemp(temp.size() << 1);
    uint32_t index_raw = (n << 1) - (1 << (logm - 1));
    uint32_t index = (index_raw * galois_elts[logm - 1]) % (n << 1);
    for (uint32_t a = 0; a < temp.size(); a++) {
        if (a >= (m - (1 << (logm - 1)))) { // corner case.
            evaluator.multiply_plain(temp[a], two,
                                       newtemp[a]); // plain multiplication by 2.
            // cout << client.decryptor_->invariant_noise_budget(newtemp[a]) << ", ";
        } else {
            evaluator.apply_galois(temp[a], galois_elts[logm - 1], galois_keys,
                                     tempctxt_rotated);
            evaluator.add(temp[a], tempctxt_rotated, newtemp[a]);
            multiply_power_of_X(temp[a], tempctxt_shifted, index_raw, context);
            multiply_power_of_X(tempctxt_rotated, tempctxt_rotatedshifted, index, context);
            evaluator.add(tempctxt_shifted, tempctxt_rotatedshifted,
                            newtemp[a + temp.size()]);
        }
    }
    auto first = newtemp.begin();
    auto last = newtemp.begin() + m;
    vector<Ciphertext> newVec(first, last);
    return newVec;
}

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination, uint32_t index, const SEALContext& context) {
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto coeff_mod_count = parms.coeff_modulus().size();
    auto coeff_count = parms.poly_modulus_degree();
    auto encrypted_count = encrypted.size();
    destination = encrypted;
    for (uint32_t i = 0; i < encrypted_count; i++) {
        for (uint32_t j = 0; j < coeff_mod_count; j++) {
            seal::util::negacyclic_shift_poly_coeffmod(encrypted.data(i) + (j * coeff_count),
                                                       coeff_count, index,
                                                       parms.coeff_modulus()[j],
                                                       destination.data(i) + (j * coeff_count));
        }
    }

}

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod) {
    uint64_t inverse = 0;
    seal::util::try_invert_uint_mod(m, mod.value(), inverse);
    return inverse;
}

Serializable<GaloisKeys> generate_galois_keys(const SEALContext& context, KeyGenerator &keygen) {
    std::vector<uint32_t> galois_elts;
    auto &parms = context.first_context_data()->parms();
    uint32_t degree = parms.poly_modulus_degree();
    uint32_t logN = util::get_power_of_two(degree);
    for (uint32_t i = 0; i < logN; i++) {
        galois_elts.push_back((degree + util::exponentiate_uint(2, i)) / util::exponentiate_uint(2, i));
    }
    Serializable<GaloisKeys> galois_keys = keygen.create_galois_keys(galois_elts);
    return galois_keys;
}