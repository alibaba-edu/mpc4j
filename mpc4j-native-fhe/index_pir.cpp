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
                ++pt_iter; // pt_iter 很快指向会出问题的
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
        Ciphertext left_sum = get_sum(query, evaluator, gal_keys, encoded_db, start, start + mid - 1);
        Ciphertext right_sum = get_sum(query, evaluator, gal_keys, encoded_db, start + mid, end);
        evaluator.rotate_rows_inplace(right_sum, -mid, gal_keys);
        evaluator.add_inplace(left_sum, right_sum);
        return left_sum;
    } else {
        Ciphertext column_sum;
        Ciphertext temp_ct;
        uint32_t query_size = query.size();
        evaluator.multiply_plain(query[0], encoded_db[query_size * start], column_sum);
        for (uint32_t j = 1; j < query_size; j++) {
            if (!encoded_db[query_size * start + j].is_zero()) {
                evaluator.multiply_plain(query[j], encoded_db[query_size * start + j], temp_ct);
                evaluator.add_inplace(column_sum, temp_ct);
            }
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

uint32_t get_next_power_of_two(uint32_t number) {
    if (!(number & (number - 1))) {
        return number;
    }
    uint32_t number_of_bits = get_number_of_bits(number);
    return (1 << number_of_bits);
}

vector<Ciphertext> new_expand_query(const EncryptionParameters& parms, const std::vector<Ciphertext>& cts, uint32_t total_items,
                                    const GaloisKeys& galois_keys) {
    uint32_t poly_modulus_degree = parms.poly_modulus_degree();
    uint32_t expect_cts_size = (total_items % parms.poly_modulus_degree() == 0)
            ? total_items / parms.poly_modulus_degree() : total_items / parms.poly_modulus_degree() + 1;
    if (cts.size() != expect_cts_size) {
        throw logic_error("Number of ciphertexts doesn't match number of items for oblivious expansion.");
    }
    // Consider a more specific example, indices[0] = 1, indices[1] = 0, the corresponding PT is: c_4x^4 + c_1x^1.
    // The expanded result is: [E(0) E(1) E(0) E(0) , E(1) E(0) E(0) E(0)]
    std::vector<Ciphertext> results;
    results.reserve(total_items);
    for (const auto& ct : cts) {
        vector<Ciphertext> temp = new_single_expand_query(parms, ct, std::min(poly_modulus_degree, total_items), galois_keys);
        results.insert(results.end(), std::make_move_iterator(temp.begin()),std::make_move_iterator(temp.end()));
        // Except for the last ciphertext, each previous ciphertext is expanded to a vector of length N
        total_items -= poly_modulus_degree;
    }
    return results;
}

vector<Ciphertext> new_single_expand_query(const EncryptionParameters& parms, const Ciphertext& ct, const uint32_t num_items,
                                           const GaloisKeys& galois_keys) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    const uint32_t poly_modulus_degree = parms.poly_modulus_degree();
    // single ct is expanded to a vector of length N at most
    if (num_items > poly_modulus_degree) {
        throw logic_error("Cannot expand more items from a CT than poly modulus degree.");
    }
    size_t logm = ceil(log2(num_items));
    // if num_items is just power of 2, return itself.
    std::vector<seal::Ciphertext> results(get_next_power_of_two(num_items));
    results[0] = ct;
    for (size_t j = 0; j < logm; ++j) {
        const size_t two_power_j = (1 << j);
        for (size_t k = 0; k < two_power_j; ++k) {
            auto c0 = results[k];
            evaluator.apply_galois_inplace(c0, (poly_modulus_degree >> j) + 1, galois_keys);
            uint32_t index1 =  ((poly_modulus_degree << 1) - two_power_j) % (poly_modulus_degree << 1);
            // This essentially produces what the paper calls c1
            multiply_power_of_X(results[k], results[k + two_power_j], index1, context);
            // Do the multiply by power of x after substitution operator to avoid
            // having to do the substitution operator a second time, since it's about
            // 20x slower. Except that now instead of multiplying by x^(-2^j) we have
            // to do the substitution first ourselves, producing
            // (x^(N/2^j + 1))^(-2^j) = 1/x^(2^j * (N/2^j + 1)) = 1/x^(N + 2^j)
            seal::Ciphertext c1;
            uint32_t index2 =  ((poly_modulus_degree << 1) - (poly_modulus_degree + two_power_j)) % (poly_modulus_degree << 1);
            multiply_power_of_X(c0, c1, index2, context);
            evaluator.add_inplace(results[k], c0);
            evaluator.add_inplace(results[k + two_power_j], c1);
        }
    }
    results.resize(num_items);
    return results;
}

vector<Ciphertext> multiply_mulpir(const EncryptionParameters& parms, const RelinKeys* const relin_keys, const vector<Plaintext>& database,
                                   uint32_t database_it, vector<Ciphertext>& selection_vector, uint32_t selection_vector_it,
                                   vector<int32_t>& dimensions, uint32_t depth) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    const size_t this_dimension = dimensions[0];
    vector<int32_t> remaining_dimensions = vector<int32_t>(dimensions.begin() + 1, dimensions.end());
    vector<Ciphertext> result;
    bool first_pass = true;
    for (size_t i = 0; i < this_dimension; ++i) {
        // make sure we don't go past end of DB
        if (database_it == database.size()) break;
        vector<Ciphertext> temp_ct;
        // When recursing to the last dimension, execute ct*pt, and then accumulate
        if (remaining_dimensions.empty()) {
            // base case: have to multiply against DB
            temp_ct.resize(1);
            evaluator.multiply_plain(selection_vector[selection_vector_it + i],database[database_it++], temp_ct[0]);
        } else {
            // enter recursion
            vector<Ciphertext> lower_result =
                multiply_mulpir(parms, relin_keys, database, database_it, selection_vector, selection_vector_it + this_dimension, remaining_dimensions, depth + 1);
            uint32_t ramain_dim_prod = std::accumulate(remaining_dimensions.begin(), remaining_dimensions.end(), 1, multiplies<uint32_t>());
            database_it += ramain_dim_prod;
            temp_ct.resize(1);
            // when ciphertext * ciphertext , can not be NTT form
            // lower_result[0] has been handled, here we handle the selection_vector
            if (selection_vector[selection_vector_it + i].is_ntt_form()) {
                evaluator.transform_from_ntt_inplace(selection_vector[selection_vector_it + i]);
            }
            evaluator.multiply(lower_result[0], selection_vector[selection_vector_it + i], temp_ct[0]);
            evaluator.relinearize_inplace(temp_ct[0], *relin_keys);
        }
        // this is the start point for ct + ct
        if (first_pass) {
            result = temp_ct;
            first_pass = false;
        } else {
            for (size_t j = 0; j < result.size(); ++j) {
                evaluator.add_inplace(result[j], temp_ct[j]);
            }
        } // next for loop
    }
    // ensure when ciphertext * ciphertext, the ct is not the NTT form
    for (auto& ct : result) {
        if (ct.is_ntt_form()) {
            evaluator.transform_from_ntt_inplace(ct);
        }
    }
    return result;                  
}

vector<Ciphertext> mk22_expand_input_ciphers(const EncryptionParameters& parms, const GaloisKeys& galois_keys,
                                             vector<Ciphertext>& input_ciphers, uint64_t num_input_ciphers, uint64_t num_bits) {
    vector<Ciphertext> answer;
    vector<Ciphertext> temp_expanded;
    // m
    uint64_t remaining_bits = num_bits;
    // Corresponding paper Algorithm 5, line-3
    for (uint32_t i = 0; i < num_input_ciphers; i++){
        temp_expanded.clear();
        // A single ciphertext expands to 2^c ciphertexts
        temp_expanded = mk22_expand_procedure(parms, galois_keys, input_ciphers[i], min<uint64_t>(remaining_bits, parms.poly_modulus_degree()));
        for (const auto & j : temp_expanded)
            answer.push_back(j);
        remaining_bits -= temp_expanded.size();
    }
    return answer;
}

// convert single Ciphertext to 2^c Ciphertext 
vector<Ciphertext> mk22_expand_procedure(const EncryptionParameters& parms, const GaloisKeys& galois_keys, const Ciphertext &input_cipher, uint64_t used_slots) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    vector<Ciphertext> ciphers(used_slots);
    // Corresponding to the paper Algorithm 5, the part after line-4, not including for j \in [h]  
    uint64_t expansion_level = (int)ceil(log2(used_slots));
    ciphers[0] = input_cipher; // line-4
    for (uint64_t a = 0; a < expansion_level; a++) { // line-5
        for (uint64_t b = 0; b < (1<<a); b++) { // line-6
            auto temp_0=ciphers[b];
            auto temp_2=ciphers[b];
            evaluator.apply_galois_inplace(temp_0, (parms.poly_modulus_degree() >> a) + 1, galois_keys);
            evaluator.add_inplace(ciphers[b], temp_0);
            if (b + (1 << a) < used_slots) {
                Ciphertext temp_1;
                // multiply_power_of_X almost equals multiply_inverse_power_of_X in constant-weight PIR opensource
                uint32_t index = ((parms.poly_modulus_degree() << 1) - (1 << a)) % (parms.poly_modulus_degree() << 1);
                multiply_power_of_X(temp_2, ciphers[b + (1 << a) ], index, context);
                multiply_power_of_X(temp_0, temp_1, index, context);
                evaluator.sub_inplace(ciphers[b + (1<<a)], temp_1);
            }
        }
    }
    return ciphers;
}

void mk22_generate_selection_vector(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                                    uint32_t hamming_weight, uint32_t eq_type, vector<Ciphertext>& expanded_query,
                                    vector<vector<uint32_t>>& pt_index_codewords, vector<Ciphertext>& selection_vector) {
    for (uint32_t ch = 0; ch < pt_index_codewords.size(); ch++) {
        selection_vector[ch] = mk22_generate_selection_bit(evaluator, relin_keys, codeword_bit_length, hamming_weight,
                                                           eq_type,expanded_query,pt_index_codewords[ch]);
    }
}

Ciphertext mk22_faster_inner_product(Evaluator& evaluator, vector<Ciphertext>& selection_vector, vector<Plaintext>& database){
    if(selection_vector.size() !=  database.size()) {
        throw logic_error("the size of selection vector should be equal the size of the database.");
    }
    for (auto & i : selection_vector){
        if(!i.is_ntt_form()) {
            evaluator.transform_to_ntt_inplace(i);
        }
    }
    vector<Ciphertext> sub_ciphers;
    Ciphertext operand;
    for (uint32_t ch = 0; ch < database.size(); ch++){
        evaluator.multiply_plain(selection_vector[ch], database[ch], operand);
        sub_ciphers.push_back(operand);
    }
    Ciphertext encrypted_answer;
    evaluator.add_many(sub_ciphers, encrypted_answer);
    return encrypted_answer;
}

Ciphertext mk22_generate_selection_bit(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                                       uint32_t hamming_weight, uint32_t eq_type, vector<Ciphertext>& encrypted_query,
                                       vector<uint32_t>& single_pt_index_codeword) {
    if (single_pt_index_codeword.size() != encrypted_query.size()) {
        throw logic_error(" codewords bit length should equal between plain codewords and cipher codewords vector");
    }
    Ciphertext temp_ciphertext;
    if (eq_type == 0) {
        // eq_type = 0 -->  folklore_eq
        temp_ciphertext = mk22_folklore_eq(evaluator,relin_keys, codeword_bit_length,  encrypted_query, single_pt_index_codeword);
    } else if (eq_type == 1) {
        // eq_type = 1 --> constant_weight_eq
        temp_ciphertext = mk22_constant_weight_eq(evaluator, relin_keys, codeword_bit_length, hamming_weight, encrypted_query, single_pt_index_codeword);
    }else {
        throw logic_error("eq_type must be 0 or 1.");
    }
    return temp_ciphertext;
}

Ciphertext mk22_folklore_eq(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                            vector<Ciphertext>& encrypted_query, vector<uint32_t>& single_pt_index_codeword) {
    Ciphertext temp_ciphertext;
    vector<Ciphertext> mult_operands;
    for (uint32_t i = 0; i < codeword_bit_length; i++){
        if (single_pt_index_codeword[i] == 1) {
            mult_operands.push_back(encrypted_query[i]);
        } else {
            Ciphertext operand;
            evaluator.sub_plain(encrypted_query[i], Plaintext("1"), operand);
            evaluator.negate_inplace(operand);
            mult_operands.push_back(operand);
        }
    }
    evaluator.multiply_many(mult_operands, *relin_keys, temp_ciphertext);
    return temp_ciphertext;
}

Ciphertext mk22_constant_weight_eq(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                                   uint32_t hamming_weight, vector<Ciphertext>& encrypted_query,
                                   vector<uint32_t>& single_pt_index_codeword){
    Ciphertext temp_ciphertext;
    if (hamming_weight > 1) { // k > 1
        vector<Ciphertext> mult_operands;
        // m-bit
        for (uint32_t i = 0; i < codeword_bit_length; i++){
            if (single_pt_index_codeword[i] == 1) {
                mult_operands.push_back(encrypted_query[i]);
            }
        }
        evaluator.multiply_many(mult_operands, *relin_keys, temp_ciphertext);
    } else {
        for (uint32_t i = 0; i < codeword_bit_length; i++){
            if (single_pt_index_codeword[i] == 1){
                temp_ciphertext = encrypted_query[i];
            }
        }
    }
    return temp_ciphertext;
}

vector<Ciphertext> merge_response(const SEALContext& context, const GaloisKeys& galois_keys, vector<Ciphertext> response,
                                  int32_t num_slots_per_entry, uint32_t first_two_dimension_size) {
    Evaluator evaluator(context);
    BatchEncoder batch_encoder(context);
    uint32_t row_size = context.first_context_data()->parms().poly_modulus_degree() / 2;
    auto g = (int32_t) (row_size / first_two_dimension_size);
    uint32_t size = response.size() / num_slots_per_entry;
    vector<vector<Ciphertext>> responses;
    responses.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        vector<Ciphertext> temp;
        temp.reserve(num_slots_per_entry);
        for (uint32_t j = 0; j < num_slots_per_entry; j++) {
            temp.push_back(response[j * size + i]);
        }
        responses.push_back(temp);
    }
    uint32_t num_slots_per_entry_rounded = get_next_power_of_two(num_slots_per_entry);
    uint32_t max_empty_slots = first_two_dimension_size;
    auto num_chunk_ctx = ceil(num_slots_per_entry * 1.0 / max_empty_slots);
    vector<Ciphertext> chunk_response;
    for (uint32_t i = 0; i < size; i++) {
        auto remaining_slots_entry = num_slots_per_entry;
        for (uint32_t j = 0; j < num_chunk_ctx; j++) {
            auto chunk_idx = j * max_empty_slots;
            int32_t loop = std::min((int32_t) max_empty_slots, remaining_slots_entry);
            Ciphertext chunk_ct_acc = responses[i][chunk_idx];
            for (int32_t k = 1; k < loop; k++) {
                evaluator.rotate_rows_inplace(responses[i][chunk_idx + k], -k * g, galois_keys);
                evaluator.add_inplace(chunk_ct_acc, responses[i][chunk_idx + k]);
            }
            remaining_slots_entry -= loop;
            chunk_response.push_back(chunk_ct_acc);
        }
    }
    auto current_fill = g * num_slots_per_entry;
    size_t num_buckets_merged = row_size / current_fill;
    if (ceil(num_slots_per_entry * 1.0 / max_empty_slots) > 1 || num_buckets_merged <= 1 || chunk_response.size() == 1) {
        return chunk_response;
    }
    current_fill = g * (int32_t) num_slots_per_entry_rounded;
    auto merged_ctx_needed = ceil(((double) chunk_response.size() * current_fill * 1.0) / row_size);
    vector<Ciphertext> chunk_bucket_responses;
    for (int32_t i = 0; i < merged_ctx_needed; i++) {
        Ciphertext ct_acc;
        for (int32_t j = 0; j < num_buckets_merged; j++) {
            if (i * num_buckets_merged + j < chunk_response.size()) {
                Ciphertext copy_ct_acc = chunk_response[i * num_buckets_merged + j];
                Ciphertext tmp_ct = copy_ct_acc;
                for (int32_t k = 1; k < row_size / current_fill; k *= 2) {
                    evaluator.rotate_rows_inplace(tmp_ct, -k * current_fill, galois_keys);
                    evaluator.add_inplace(copy_ct_acc, tmp_ct);
                    tmp_ct = copy_ct_acc;
                }
                std::vector<uint64_t> selection_vector(context.first_context_data()->parms().poly_modulus_degree(), 0ULL);
                std::fill_n(selection_vector.begin() + (j * current_fill), current_fill, 1ULL);
                std::fill_n(selection_vector.begin() + row_size + (j * current_fill), current_fill, 1ULL);
                Plaintext selection_pt;
                batch_encoder.encode(selection_vector, selection_pt);
                evaluator.multiply_plain_inplace(copy_ct_acc, selection_pt);
                if (j == 0) {
                    ct_acc = copy_ct_acc;
                } else {
                    evaluator.add_inplace(ct_acc, copy_ct_acc);
                }
            }
        }
        chunk_bucket_responses.push_back(ct_acc);
    }
    return chunk_bucket_responses;
}