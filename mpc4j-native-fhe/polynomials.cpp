#include "polynomials.h"

uint64_t mul_mod(uint64_t a, uint64_t b, uint64_t m, bool small_modulus) {
    if (small_modulus) {
        return ((a)*(b)) % (m);
    } else {
        return (((__uint128_t) (a)) * ((__uint128_t) (b))) % (m);
    }
}

uint64_t mod_exp(uint64_t base, uint64_t exponent, uint64_t modulus, bool small_modulus) {
    uint64_t result = 1;
    while (exponent > 0) {
        if (exponent & 1) {
            result = mul_mod(result, base, modulus, small_modulus);
        }
        base = mul_mod(base, base, modulus, small_modulus);
        exponent = (exponent >> 1);
    }
    return result;
}