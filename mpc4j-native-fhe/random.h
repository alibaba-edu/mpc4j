/* 利用SEAL库的随机数发生器，生成固定比特长度范围内的随机数
 * This is implemented by Aleksejs Popovs.
* The source coder is available at
* https://github.com/aleksejspopovs/6857-private-categorization/blob/master/src/random.h */

#pragma once
#include <cstdint>

#include "seal/seal.h"

using namespace seal;
using namespace std;

/* This helper function uses a UniformRandomGenerator (which outputs 32-bit
   values) to uniformly pick an n-bit value, where n can be up to 64. */
uint64_t random_bits(const shared_ptr<UniformRandomGenerator>& random, uint64_t bits);

/* This helper function uniformly picks an integer x with 0 <= x < limit. */
uint64_t random_integer(const shared_ptr<UniformRandomGenerator>& random, uint64_t limit);

/* This helper function uniformly picks an integer x with 0 < x < limit. */
uint64_t random_nonzero_integer(const shared_ptr<UniformRandomGenerator>& random, uint64_t limit);