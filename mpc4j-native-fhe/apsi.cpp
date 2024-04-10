#include "apsi.h"
#include <cstdint>
#include "polynomials.h"
#include "utils.h"

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                 vector<Plaintext> coeff_plaintexts, uint32_t ps_low_power, const RelinKeys& relin_keys,
                                 const PublicKey& public_key) {
    SEALContext context(parms);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    uint32_t ps_high_degree = ps_low_power + 1;
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, high_powers_parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    uint32_t ps_high_degree_powers = degree / ps_high_degree;
    Plaintext plain_zero(parms.poly_modulus_degree());
    plain_zero.set_zero();
    Ciphertext cipher_zero1, cipher_zero2;
    encryptor.encrypt(plain_zero, cipher_zero1);
    evaluator.mod_switch_to_inplace(cipher_zero1, high_powers_parms_id);
    evaluator.transform_to_ntt_inplace(cipher_zero1);
    encryptor.encrypt(plain_zero, cipher_zero2);
    evaluator.mod_switch_to_inplace(cipher_zero2, low_powers_parms_id);
    evaluator.transform_to_ntt_inplace(cipher_zero2);
    // Calculate polynomial for i = 1,...,ps_high_degree_powers-1
    for (uint32_t i = 1; i < ps_high_degree_powers; i++) {
        // Evaluate inner polynomial. The free term is left out and added later on.
        // The evaluation result is stored in temp_in.
        for (uint32_t j = 1; j < ps_high_degree; j++) {
            if (coeff_plaintexts[j + i * ps_high_degree].is_zero()) {
                cipher_temp = cipher_zero2;
            } else {
                evaluator.multiply_plain(encrypted_powers[j - 1], coeff_plaintexts[j + i * ps_high_degree],
                                         cipher_temp);
            }
            if (j == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[i - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Calculate polynomial for i = ps_high_degree_powers.
    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
    // Once again, the free term will only be added later on.
    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
        for (uint32_t i = 1; i <= degree % ps_high_degree; i++) {
            if (coeff_plaintexts[ps_high_degree * ps_high_degree_powers + i].is_zero()) {
                cipher_temp = cipher_zero2;
            } else {
                evaluator.multiply_plain(encrypted_powers[i - 1],
                                         coeff_plaintexts[ps_high_degree * ps_high_degree_powers + i],
                                         cipher_temp);
            }
            if (i == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, high_powers_parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[ps_high_degree_powers - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Relinearize sum of ciphertext-ciphertext products
    if (!f_evaluated.is_transparent()) {
        evaluator.relinearize_inplace(f_evaluated, relin_keys);
    }
    // Calculate inner polynomial for i=0.
    // Done separately since there is no multiplication with a power of high-degree
    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
    for (uint32_t j = 1; j <= length; j++) {
        if (coeff_plaintexts[j].is_zero()) {
            cipher_temp = cipher_zero2;
        } else {
            evaluator.multiply_plain(encrypted_powers[j - 1], coeff_plaintexts[j], cipher_temp);
        }
        evaluator.transform_from_ntt_inplace(cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
    for (uint32_t i = 1; i < ps_high_degree_powers + 1; i++) {
        if (coeff_plaintexts[ps_high_degree * i].is_zero()) {
            cipher_temp = cipher_zero2;
        } else {
            evaluator.multiply_plain(encrypted_powers[i - 1 + ps_low_power], coeff_plaintexts[ps_high_degree * i],
                                     cipher_temp);
        }
        evaluator.mod_switch_to_inplace(cipher_temp, high_powers_parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    } else {
        encryptor.encrypt(coeff_plaintexts[0], f_evaluated);
    }
    while (f_evaluated.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(f_evaluated);
    }
    return f_evaluated;
}

Ciphertext ucpsi_polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                       vector<Plaintext> coeff_plaintexts, uint32_t ps_low_power, const RelinKeys& relin_keys,
                                       const PublicKey& public_key) {
    SEALContext context(parms);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    uint32_t ps_high_degree = ps_low_power + 1;
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, 3);
    f_evaluated.is_ntt_form() = false;
    uint32_t ps_high_degree_powers = degree / ps_high_degree;
    // Calculate polynomial for i = 1,...,ps_high_degree_powers-1
    for (uint32_t i = 1; i < ps_high_degree_powers; i++) {
        // Evaluate inner polynomial. The free term is left out and added later on.
        // The evaluation result is stored in temp_in.
        for (uint32_t j = 1; j < ps_high_degree; j++) {
            evaluator.multiply_plain(encrypted_powers[j - 1], coeff_plaintexts[j + i * ps_high_degree], cipher_temp);
            if (j == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[i - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Calculate polynomial for i = ps_high_degree_powers.
    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
    // Once again, the free term will only be added later on.
    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
        for (uint32_t i = 1; i <= degree % ps_high_degree; i++) {
            evaluator.multiply_plain(encrypted_powers[i - 1],
                                     coeff_plaintexts[ps_high_degree * ps_high_degree_powers + i],
                                     cipher_temp);
            if (i == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[ps_high_degree_powers - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Relinearize sum of ciphertext-ciphertext products
    if (!f_evaluated.is_transparent()) {
        evaluator.relinearize_inplace(f_evaluated, relin_keys);
    }
    // Calculate inner polynomial for i=0.
    // Done separately since there is no multiplication with a power of high-degree
    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
    for (uint32_t j = 1; j <= length; j++) {
        evaluator.multiply_plain(encrypted_powers[j-1], coeff_plaintexts[j], cipher_temp);
        evaluator.transform_from_ntt_inplace(cipher_temp);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
    for (uint32_t i = 1; i < ps_high_degree_powers + 1; i++) {
        evaluator.multiply_plain(encrypted_powers[i - 1 + ps_low_power], coeff_plaintexts[ps_high_degree * i], cipher_temp);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    } else {
        encryptor.encrypt(coeff_plaintexts[0], f_evaluated);
    }
    return f_evaluated;
}

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers, vector<Plaintext> coeff_plaintexts,
                                 uint32_t ps_low_power, const RelinKeys& relin_keys) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    uint32_t ps_high_degree = ps_low_power + 1;
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    uint32_t ps_high_degree_powers = degree / ps_high_degree;
    // Calculate polynomial for i=1,...,ps_high_degree_powers-1
    for (uint32_t i = 1; i < ps_high_degree_powers; i++) {
        // Evaluate inner polynomial. The free term is left out and added later on.
        // The evaluation result is stored in temp_in.
        for (uint32_t j = 1; j < ps_high_degree; j++) {
            evaluator.multiply_plain(encrypted_powers[j - 1], coeff_plaintexts[j + i * ps_high_degree], cipher_temp);
            if (j == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[i - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Calculate polynomial for i=ps_high_degree_powers.
    // Done separately because here the degree of the inner poly is degree % ps_high_degree.
    // Once again, the free term will only be added later on.
    if (degree % ps_high_degree > 0 && ps_high_degree_powers > 0) {
        for (uint32_t i = 1; i <= degree % ps_high_degree; i++) {
            evaluator.multiply_plain(encrypted_powers[i - 1],
                                     coeff_plaintexts[ps_high_degree * ps_high_degree_powers + i],
                                     cipher_temp);
            if (i == 1) {
                temp_in = cipher_temp;
            } else {
                evaluator.add_inplace(temp_in, cipher_temp);
            }
        }
        // Transform inner polynomial to coefficient form
        evaluator.transform_from_ntt_inplace(temp_in);
        evaluator.mod_switch_to_inplace(temp_in, parms_id);
        // The high powers are already in coefficient form
        evaluator.multiply_inplace(temp_in, encrypted_powers[ps_high_degree_powers - 1 + ps_low_power]);
        evaluator.add_inplace(f_evaluated, temp_in);
    }
    // Relinearize sum of ciphertext-ciphertext products
    if (!f_evaluated.is_transparent()) {
        evaluator.relinearize_inplace(f_evaluated, relin_keys);
    }
    // Calculate inner polynomial for i=0.
    // Done separately since there is no multiplication with a power of high-degree
    uint32_t length = ps_high_degree_powers == 0 ? degree : ps_low_power;
    for (uint32_t j = 1; j <= length; j++) {
        evaluator.multiply_plain(encrypted_powers[j-1], coeff_plaintexts[j], cipher_temp);
        evaluator.transform_from_ntt_inplace(cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficients of the inner polynomials multiplied by the respective powers of high-degree
    for (uint32_t i = 1; i < ps_high_degree_powers + 1; i++) {
        evaluator.multiply_plain(encrypted_powers[i - 1 + ps_low_power], coeff_plaintexts[ps_high_degree * i], cipher_temp);
        evaluator.mod_switch_to_inplace(cipher_temp, parms_id);
        evaluator.add_inplace(f_evaluated, cipher_temp);
    }
    // Add the constant coefficient
    evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    return f_evaluated;
}

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                 vector<Plaintext> coeff_plaintexts, const PublicKey& public_key) {
    SEALContext context(parms);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    Plaintext plain_zero(parms.poly_modulus_degree());
    plain_zero.set_zero();
    Ciphertext cipher_zero;
    encryptor.encrypt(plain_zero, cipher_zero);
    evaluator.transform_to_ntt_inplace(cipher_zero);
    for (uint32_t i = 1; i <= degree; i++) {
        if (coeff_plaintexts[i].is_zero()) {
            cipher_temp = cipher_zero;
        } else {
            evaluator.multiply_plain(encrypted_powers[i - 1], coeff_plaintexts[i], cipher_temp);
        }
        if (i == 1) {
            temp_in = cipher_temp;
        } else {
            evaluator.add_inplace(temp_in, cipher_temp);
        }
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.transform_from_ntt(temp_in, f_evaluated);
        evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    } else {
        encryptor.encrypt(coeff_plaintexts[0], f_evaluated);
    }
    return f_evaluated;
}

Ciphertext ucpsi_polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                       vector<Plaintext> coeff_plaintexts, const PublicKey& public_key) {
    SEALContext context(parms);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, 3);
    f_evaluated.is_ntt_form() = false;
    for (uint32_t i = 1; i <= degree; i++) {
        evaluator.multiply_plain(encrypted_powers[i-1], coeff_plaintexts[i], cipher_temp);
        if (i == 1) {
            temp_in = cipher_temp;
        } else {
            evaluator.add_inplace(temp_in, cipher_temp);
        }
    }
    // Add the constant coefficient
    if (degree > 0) {
        evaluator.transform_from_ntt(temp_in, f_evaluated);
        evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    } else {
        encryptor.encrypt(coeff_plaintexts[0], f_evaluated);
    }
    return f_evaluated;
}

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                 vector<Plaintext> coeff_plaintexts) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    uint32_t degree = coeff_plaintexts.size() - 1;
    Ciphertext f_evaluated, cipher_temp, temp_in;
    f_evaluated.resize(context, parms_id, 3);
    f_evaluated.is_ntt_form() = false;
    for (uint32_t i = 1; i <= degree; i++) {
        evaluator.multiply_plain(encrypted_powers[i-1], coeff_plaintexts[i], cipher_temp);
        if (i == 1) {
            temp_in = cipher_temp;
        } else {
            evaluator.add_inplace(temp_in, cipher_temp);
        }
    }
    // Add the constant coefficient
    evaluator.transform_from_ntt(temp_in, f_evaluated);
    evaluator.add_plain_inplace(f_evaluated, coeff_plaintexts[0]);
    return f_evaluated;
}

vector<Ciphertext> compute_encrypted_powers(const EncryptionParameters& parms, vector<Ciphertext> query,
                                            vector<vector<uint32_t>> parent_powers, vector<uint32_t> source_power_index,
                                            uint32_t ps_low_power, const RelinKeys& relin_keys) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    uint32_t target_power_size = parent_powers.size();
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Ciphertext> encrypted_powers;
    encrypted_powers.resize(target_power_size);
    if (ps_low_power > 0) {
        // Paterson-Stockmeyer algorithm
        uint32_t ps_high_degree = ps_low_power + 1;
        for (uint32_t i = 0; i < query.size(); i++) {
            if (source_power_index[i] <= ps_low_power) {
                encrypted_powers[source_power_index[i] - 1] = query[i];
            } else {
                encrypted_powers[ps_low_power + (source_power_index[i] / ps_high_degree) - 1] = query[i];
            }
        }
        for (uint32_t i = 0; i < ps_low_power; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (uint32_t i = ps_low_power; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power],
                                       encrypted_powers[parent_powers[i][1] - 1 + ps_low_power], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (uint32_t i = 0; i < ps_low_power; i++) {
            // Low powers must be at a higher level than high powers
            evaluator.mod_switch_to_inplace(encrypted_powers[i], low_powers_parms_id);
            // Low powers must be in NTT form
            evaluator.transform_to_ntt_inplace(encrypted_powers[i]);
        }
        for (uint32_t i = ps_low_power; i < target_power_size; i++) {
            // High powers are only modulus switched
            evaluator.mod_switch_to_inplace(encrypted_powers[i], high_powers_parms_id);
        }
    } else {
        // naive algorithm
        for (uint32_t i = 0; i < query.size(); i++) {
            encrypted_powers[source_power_index[i] - 1] = query[i];
        }
        for (uint32_t i = 0; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (auto &encrypted_power: encrypted_powers) {
            // Only one ciphertext-plaintext multiplication is needed after this
            evaluator.mod_switch_to_inplace(encrypted_power, high_powers_parms_id);
            // All powers must be in NTT form
            evaluator.transform_to_ntt_inplace(encrypted_power);
        }
    }
    return encrypted_powers;
}

vector<Ciphertext> ucpsi_compute_encrypted_powers(const EncryptionParameters& parms, vector<Ciphertext> query,
                                                  vector<vector<uint32_t>> parent_powers, vector<uint32_t> source_power_index,
                                                  uint32_t ps_low_power, const RelinKeys& relin_keys) {
    SEALContext context(parms);
    Evaluator evaluator(context);
    uint32_t target_power_size = parent_powers.size();
    vector<Ciphertext> encrypted_powers;
    encrypted_powers.resize(target_power_size);
    if (ps_low_power > 0) {
        // Paterson-Stockmeyer algorithm
        uint32_t ps_high_degree = ps_low_power + 1;
        for (uint32_t i = 0; i < query.size(); i++) {
            if (source_power_index[i] <= ps_low_power) {
                encrypted_powers[source_power_index[i] - 1] = query[i];
            } else {
                encrypted_powers[ps_low_power + (source_power_index[i] / ps_high_degree) - 1] = query[i];
            }
        }
        for (uint32_t i = 0; i < ps_low_power; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (uint32_t i = ps_low_power; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1 + ps_low_power],
                                       encrypted_powers[parent_powers[i][1] - 1 + ps_low_power], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (uint32_t i = 0; i < ps_low_power; i++) {
            // Low powers must be in NTT form
            evaluator.transform_to_ntt_inplace(encrypted_powers[i]);
        }
    } else {
        // naive algorithm
        for (uint32_t i = 0; i < query.size(); i++) {
            encrypted_powers[source_power_index[i] - 1] = query[i];
        }
        for (uint32_t i = 0; i < target_power_size; i++) {
            if (parent_powers[i][1] != 0) {
                if (parent_powers[i][0] - 1 == parent_powers[i][1] - 1) {
                    evaluator.square(encrypted_powers[parent_powers[i][0] - 1], encrypted_powers[i]);
                } else {
                    evaluator.multiply(encrypted_powers[parent_powers[i][0] - 1],
                                       encrypted_powers[parent_powers[i][1] - 1], encrypted_powers[i]);
                }
                evaluator.relinearize_inplace(encrypted_powers[i], relin_keys);
            }
        }
        for (auto &encrypted_power: encrypted_powers) {
            // All powers must be in NTT form
            evaluator.transform_to_ntt_inplace(encrypted_power);
        }
    }
    return encrypted_powers;
}