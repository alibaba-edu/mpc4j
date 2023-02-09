#include "index_pir.h"
#include "seal/seal.h"
#include "tfhe/params.h"
#include "tfhe/tfhe.h"

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

Ciphertext decomp_mul(vector<Ciphertext> ct_decomp, vector<uint64_t *> pt_decomp, const SEALContext& context) {
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto &coeff_modulus = parms.coeff_modulus();
    uint32_t poly_modulus_degree = parms.poly_modulus_degree();
    uint32_t coeff_modulus_size = coeff_modulus.size();
    Ciphertext dst, product;
    dst.resize(context, 2);
    product.resize(context, 2);
    auto ntt_tables = context.first_context_data()->small_ntt_tables();
    util::RNSIter res_iter0(dst.data(0), poly_modulus_degree);
    util::RNSIter res_iter1(dst.data(1), poly_modulus_degree);
    util::RNSIter prod_iter0(product.data(0), poly_modulus_degree);
    util::RNSIter prod_iter1(product.data(1), poly_modulus_degree);
    for (uint32_t i = 0; i < 2; i++) {
        util::RNSIter pt_iter(pt_decomp[i], poly_modulus_degree);
        util::RNSIter ct_iter0(ct_decomp[i].data(0), poly_modulus_degree);
        util::RNSIter ct_iter1(ct_decomp[i].data(1), poly_modulus_degree);
        util::ntt_negacyclic_harvey_lazy(pt_iter, coeff_modulus_size, ntt_tables);
        util::ntt_negacyclic_harvey_lazy(ct_iter0, coeff_modulus_size, ntt_tables);
        util::ntt_negacyclic_harvey_lazy(ct_iter1, coeff_modulus_size, ntt_tables);
        util::dyadic_product_coeffmod(pt_iter, ct_iter0, coeff_modulus_size, coeff_modulus, prod_iter0);
        util::dyadic_product_coeffmod(pt_iter, ct_iter1, coeff_modulus_size, coeff_modulus, prod_iter1);
        util::add_poly_coeffmod(res_iter0, prod_iter0, coeff_modulus_size, coeff_modulus, res_iter0);
        util::add_poly_coeffmod(res_iter1, prod_iter1, coeff_modulus_size, coeff_modulus, res_iter1);
    }
    dst.is_ntt_form() = true;
    return dst;
}

void poc_expand_flat(vector<vector<Ciphertext>>::iterator &result, vector<Ciphertext> &packed_swap_bits,
                     const SEALContext& context, uint32_t size, seal::GaloisKeys &galois_keys) {
    auto &parms = context.first_context_data()->parms();
    uint32_t coeff_count = parms.poly_modulus_degree();
    vector<Ciphertext> expanded_ciphers(coeff_count);
    for (uint32_t i = 0; i < packed_swap_bits.size(); i++) {
        expanded_ciphers = poc_rlwe_expand(packed_swap_bits[i], context, galois_keys, size);
        for (uint32_t j = 0; j < size; j++) {
            // put jth expanded ct in ith idx slot of jt gsw_ct
            result[j][i] = expanded_ciphers[j];
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

vector<Ciphertext> poc_rlwe_expand(const Ciphertext& packed_query, const SEALContext& context, const seal::GaloisKeys& galois_keys, uint32_t size) {
    // this function return size vector of RLWE ciphertexts it takes a single RLWE packed ciphertext
    Evaluator evaluator(context);
    const auto &context_data = context.first_context_data();
    auto &parms = context_data->parms();
    auto &coeff_modulus = parms.coeff_modulus();
    uint32_t N2 = parms.poly_modulus_degree();
    Ciphertext tempctxt_rotated;
    Ciphertext tempctxt_shifted;
    vector<Ciphertext> temp;
    Ciphertext tmp;
    temp.push_back(packed_query);
    uint32_t numIters = ceil(log2(size));
    if (numIters > ceil(log2(N2))) {
        throw logic_error("m > coeff_count is not allowed.");
    }
    uint32_t startIndex = static_cast<int>(log2(N2) - numIters);
    for (uint32_t i = 0; i < numIters; i++) {
        vector<Ciphertext> newtemp(temp.size() << 1);
        uint32_t index = startIndex + i;
        uint32_t power = (N2 >> index) + 1;
        uint32_t ai = (1 << index);
        for (uint32_t j = 0; j < (1 << i); j++) {
            // temp_ctxt_rotated = subs(result[j])
            evaluator.apply_galois(temp[j], power, galois_keys, tempctxt_rotated);
            // result[j+ 2**i] = result[j] - temp_ctxt_rotated;
            evaluator.sub(temp[j], tempctxt_rotated, newtemp[j + (1 << i)]);
            // divide by x^ai = multiply by x^(2N - ai).
            multiply_power_of_X(newtemp[j + (1 << i)], tempctxt_shifted, (N2 << 1) - ai, context);
            newtemp[j + (1 << i)] = tempctxt_shifted;
            evaluator.add(tempctxt_rotated, temp[j], newtemp[j]);
        }
        temp = newtemp;
    }
    return temp;
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

void plain_decomposition(Plaintext &pt, const SEALContext &context, uint32_t decomp_size, uint32_t base_bit,
                         vector<uint64_t *> &plain_decomp) {
    auto context_data = context.first_context_data();
    auto parms = context_data->parms();
    const auto& coeff_modulus = parms.coeff_modulus();
    uint32_t coeff_modulus_size = coeff_modulus.size();
    uint32_t coeff_count = parms.poly_modulus_degree();
    auto plain_modulus = parms.plain_modulus();
    const uint64_t base = UINT64_C(1) << base_bit;
    const uint64_t mask = base - 1;
    uint32_t r_l = decomp_size;
    std::uint64_t *res;
    uint32_t total_bits = plain_modulus.bit_count();
    uint64_t *raw_ptr = pt.data();
    for (uint32_t p = 0; p < r_l; p++) {
        vector<uint64_t *> results;
        res = (std::uint64_t *) calloc((coeff_count * coeff_modulus_size), sizeof(uint64_t));
        uint32_t shift_amount = (total_bits) - ((p + 1) * base_bit);
        for (uint32_t k = 0; k < coeff_count; k++) {
            auto ptr(seal::util::allocate_uint(2, MemoryManager::GetPool()));
            auto ptr1(seal::util::allocate_uint(2, MemoryManager::GetPool()));
            ptr[0] = 0;
            ptr[1] = 0;
            ptr1[0] = raw_ptr[k];
            ptr1[1] = 0;
            seal::util::right_shift_uint128(ptr1.get(), (int) shift_amount, ptr.get());
            uint64_t temp1 = ptr[0] & mask;
            res[k * coeff_modulus_size] = temp1;
        }
        plain_decomp.push_back(res);
    }
    for (auto & i : plain_decomp) {
        poc_decompose_array(i, coeff_count, coeff_modulus, coeff_modulus_size);
    }
}

void poc_decompose_array(uint64_t *value, uint32_t count, std::vector<Modulus> coeff_modulus, uint32_t coeff_mod_count) {
    if (!value) {
        throw invalid_argument("value cannot be null");
    }
    if (coeff_mod_count > 1) {
        if (!seal::util::product_fits_in(count, coeff_mod_count)) {
            throw logic_error("invalid parameters");
        }
        // Decompose an array of multi-precision integers into an array of arrays, one per each base element
        auto temp_array(seal::util::allocate_uint(count * coeff_mod_count, MemoryManager::GetPool()));
        // Merge the coefficients first
        for (uint32_t i = 0; i < count; i++) {
            for (uint32_t j = 0; j < coeff_mod_count; j++) {
                temp_array[j + (i * coeff_mod_count)] = value[j + (i * coeff_mod_count)];
            }
        }
        seal::util::set_zero_uint(count * coeff_mod_count, value);
        for (uint32_t i = 0; i < count; i++) {
            // Temporary space for 128-bit reductions
            for (uint32_t j = 0; j < coeff_mod_count; j++) {
                // Reduce in blocks
                uint64_t temp[2]{0, temp_array[(i * coeff_mod_count) + coeff_mod_count - 1]};
                for (uint32_t k = coeff_mod_count - 1; k--;) {
                    temp[0] = temp_array[(i * coeff_mod_count) + k];
                    temp[1] = seal::util::barrett_reduce_128(temp, coeff_modulus[j]);
                }
                // Save the result modulo i-th base element
                value[(j * count) + i] = temp[1];
            }
        }
    }
}

Ciphertext get_sum(vector<Ciphertext> &query, Evaluator& evaluator, GaloisKeys &gal_keys, vector<Plaintext> &encoded_db,
                   uint32_t start, uint32_t end) {
    Ciphertext result;
    if (start != end) {
        uint32_t count = (end - start) + 1;
        uint32_t next_power_of_two = get_next_power_of_two(count);
        int32_t mid = (int32_t) next_power_of_two / 2;
        seal::Ciphertext left_sum = get_sum(query, evaluator, gal_keys, encoded_db, start, start + mid - 1);
        seal::Ciphertext right_sum = get_sum(query, evaluator, gal_keys, encoded_db, start + mid, end);
        evaluator.rotate_rows_inplace(right_sum, -mid, gal_keys);
        evaluator.add_inplace(left_sum, right_sum);
        return left_sum;
    } else {
        Ciphertext column_sum;
        Ciphertext temp_ct;
        uint32_t query_size = query.size();
        evaluator.multiply_plain(query[0], encoded_db[query_size * start], column_sum);
        for (uint32_t j = 1; j < query_size; j++) {
            evaluator.multiply_plain(query[j], encoded_db[query_size * start + j], temp_ct);
            evaluator.add_inplace(column_sum, temp_ct);
        }
        evaluator.transform_from_ntt_inplace(column_sum);
        return column_sum;
    }
}

uint32_t get_number_of_bits(uint64_t number) {
    uint32_t count = 0;
    while (number) {
        count++;
        number /= 2;
    }
    return count;
}

uint32_t get_next_power_of_two(uint32_t number)
{
    if (!(number & (number - 1))) {
        return number;
    }
    uint32_t number_of_bits = get_number_of_bits(number);
    return (1 << number_of_bits);
}
