//
// Created by pengliqiang on 2022/8/9.
//

#include <cstdint>
#include <vector>

using namespace std;

// if p is at most 32 bits, arithmetic modulo p can be implemented by directly
// using 64-bit arithmetic and reducing mod p. but if it's bigger,
// multiplication will overflow, so we have to use a slower multiplication
// algorithm.
uint64_t mul_mod(uint64_t a, uint64_t b, uint64_t m, bool small_modulus);

/* mod_exp(a, b, m) computes a^b mod m in O(log b) time. */
uint64_t mod_exp(uint64_t base, uint64_t exponent, uint64_t modulus, bool small_modulus);