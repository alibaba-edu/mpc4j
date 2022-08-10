/**
 * Modified from https://mischasan.wordpress.com/2011/10/03/the-full-sse2-bit-matrix-transpose-routine/
 * with inner most loops changed to _mm_set_epi8 and _mm_set_epi16
 */

#ifndef MPC4J_NATIVE_TOOL_BIT_MATRIX_TRANS_H
#define MPC4J_NATIVE_TOOL_BIT_MATRIX_TRANS_H

#ifdef __x86_64__
#include <immintrin.h>
#include <cstdint>
#include <cassert>
#elif __aarch64__
#include "sse2neon.h"
#include <cstdint>
#include <cassert>
#endif

#define INP(x, y) inp[(x)*ncols / 8 + (y) / 8]
#define OUT(x, y) out[(y)*nrows / 8 + (x) / 8]

#ifdef __x86_64__
__attribute__((target("sse2")))
#endif
inline void sse_trans(uint8_t *out, uint8_t const *inp, int64_t nrows, int64_t ncols) {
    int64_t rr, cc;
    int i, h;
    union {
        __m128i x;
        uint8_t b[16];
    } tmp;
    __m128i vec;
    assert(nrows % 8 == 0 && ncols % 8 == 0);
    // Do the main body in 16x8 blocks:
    for (rr = 0; rr <= nrows - 16; rr += 16) {
        for (cc = 0; cc < ncols; cc += 8) {
            vec = _mm_set_epi8(INP(rr + 8, cc),
                               INP(rr + 9, cc),
                               INP(rr + 10, cc),
                               INP(rr + 11, cc),
                               INP(rr + 12, cc),
                               INP(rr + 13, cc),
                               INP(rr + 14, cc),
                               INP(rr + 15, cc),
                               INP(rr + 0, cc),
                               INP(rr + 1, cc),
                               INP(rr + 2, cc),
                               INP(rr + 3, cc),
                               INP(rr + 4, cc),
                               INP(rr + 5, cc),
                               INP(rr + 6, cc),
                               INP(rr + 7, cc));
            for (i = 8; --i >= 0; vec = _mm_slli_epi64(vec, 1)) {
                *(uint16_t *) &OUT(rr, cc + 7 - i) = _mm_movemask_epi8(vec);
            }
        }
    }
    if (rr == nrows) {
        return;
    }
    // The remainder is a block of 8x(16n+8) bits (n may be 0).
    //  Do a PAIR of 8x8 blocks in each step:
    if ((ncols % 8 == 0 && ncols % 16 != 0) ||
        (nrows % 8 == 0 && nrows % 16 != 0)) {
        // The fancy optimizations in the else-branch don't work if the above if-condition
        // holds, so we use the simpler non-simd variant for that case.
        for (cc = 0; cc <= ncols - 16; cc += 16) {
            for (i = 0; i < 8; ++i) {
                tmp.b[i] = h = *(uint16_t const *) &INP(rr + 7 - i, cc);
                tmp.b[i + 8] = h >> 8;
            }
            for (i = 8; --i >= 0; tmp.x = _mm_slli_epi64(tmp.x, 1)) {
                OUT(rr, cc + 7 - i) = h = _mm_movemask_epi8(tmp.x);
                OUT(rr, cc + 15 - i) = h >> 8;
            }
        }
    } else {
        for (cc = 0; cc <= ncols - 16; cc += 16) {
            vec = _mm_set_epi16(*(uint16_t const *) &INP(rr + 7, cc),
                                *(uint16_t const *) &INP(rr + 6, cc),
                                *(uint16_t const *) &INP(rr + 5, cc),
                                *(uint16_t const *) &INP(rr + 4, cc),
                                *(uint16_t const *) &INP(rr + 3, cc),
                                *(uint16_t const *) &INP(rr + 2, cc),
                                *(uint16_t const *) &INP(rr + 1, cc),
                                *(uint16_t const *) &INP(rr + 0, cc));
            for (i = 8; --i >= 0; vec = _mm_slli_epi64(vec, 1)) {
                OUT(rr, cc + i) = h = _mm_movemask_epi8(vec);
                OUT(rr, cc + i + 8) = h >> 8;
            }
        }
    }
    if (cc == ncols) {
        return;
    }

    //  Do the remaining 8x8 block:
    for (i = 0; i < 8; ++i) {
        tmp.b[7 - i] = INP(rr + i, cc);
    }
    for (i = 8; --i >= 0; tmp.x = _mm_slli_epi64(tmp.x, 1)) {
        OUT(rr, cc + 7 - i) = _mm_movemask_epi8(tmp.x);
    }
}

#endif //MPC4J_NATIVE_TOOL_BIT_MATRIX_TRANS_H
