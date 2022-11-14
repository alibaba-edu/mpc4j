/*
 * Created by pengliqiang on 2022/7/14.
 * This implementation is modified from
 * <p>
 * https://github.com/aleksejspopovs/6857-private-categorization/blob/master/src/polynomials.cpp
 * </p>
 */

#include <cstdint>
#include <vector>

using namespace std;

uint64_t mul_mod(uint64_t a, uint64_t b, uint64_t m, bool small_modulus);

uint64_t mod_exp(uint64_t base, uint64_t exponent, uint64_t modulus, bool small_modulus);