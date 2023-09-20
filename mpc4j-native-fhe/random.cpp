#include <cassert>

#include "random.h"

uint64_t random_bits(const shared_ptr<UniformRandomGenerator>& random, uint64_t bits) {
    assert((bits > 0) && (bits <= 64));
    uint64_t result;
    if (bits <= 32) {
        // generate 64 bits of randomness
        result = random->generate();
        // reduce that to k bits of randomness;
        result = (result >> (32 - bits));
    } else {
        // generate 64 bits of randomness
        result = random->generate() | ((uint64_t) random->generate() << 32);
        // reduce that to k bits of randomness;
        result = (result >> (64 - bits));
    }
    return result;
}

uint64_t random_integer(const shared_ptr<UniformRandomGenerator>& random, uint64_t limit) {
    /* here's the trick: suppose 2^k < modulus <= 2^{k+1}. then we draw a random
       number x between 0 and 2^{k+1}. if it's less than modulus, we return it,
       otherwise we draw again (so the probability of success is at least 1/2). */
    assert(limit > 0);
    if (limit == 1) {
        return 0;
    }

    uint64_t k = 0;
    while (limit > (1ULL << k)) {
        k++;
    }

    uint64_t result;
    do {
        result = random_bits(random, k);
    } while (result >= limit);

    return result;
}

uint64_t random_nonzero_integer(const shared_ptr<UniformRandomGenerator>& random, uint64_t limit) {
    assert (limit > 1);

    uint64_t result;
    do {
        result = random_integer(random, limit);
    } while (result == 0);

    return result;
}
