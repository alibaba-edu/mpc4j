//
// Created by Weiran Liu on 2022/1/15.
// GF(2^128)本地运算，参考代码：https://github.com/emp-toolkit/emp-tool/blob/master/emp-tool/utils/f2k.h
// emp-tool中的运算参考：
// https://www.intel.com/content/dam/develop/external/us/en/documents/clmul-wp-rev-2-02-2014-04-20.pdf
//

#ifndef MPC4J_NATIVE_TOOL_GF2K_H
#define MPC4J_NATIVE_TOOL_GF2K_H
#include "../common/block.h"

#ifdef __x86_64__

__attribute__((target("sse2,pclmul")))
/**
 * multiplication in galois field without reduction.
 *
 * @param a input a.
 * @param b input b.
 * @param res1 result1.
 * @param res2 result2.
 */
inline void gf2k_multiply_128(__m128i a, __m128i b, __m128i *res1, __m128i *res2) {
    __m128i tmp3, tmp4, tmp5, tmp6;
    tmp3 = _mm_clmulepi64_si128(a, b, 0x00);
    tmp4 = _mm_clmulepi64_si128(a, b, 0x10);
    tmp5 = _mm_clmulepi64_si128(a, b, 0x01);
    tmp6 = _mm_clmulepi64_si128(a, b, 0x11);

    tmp4 = _mm_xor_si128(tmp4, tmp5);
    tmp5 = _mm_slli_si128(tmp4, 8);
    tmp4 = _mm_srli_si128(tmp4, 8);
    tmp3 = _mm_xor_si128(tmp3, tmp5);
    tmp6 = _mm_xor_si128(tmp6, tmp4);
    // initial mul now in tmp3, tmp6
    *res1 = tmp3;
    *res2 = tmp6;
}

#elif __aarch64__
inline void gf2k_multiply_128(__m128i a, __m128i b, __m128i *res1, __m128i *res2) {
        __m128i tmp3, tmp4, tmp5, tmp6;
        poly64_t a_lo = (poly64_t)vget_low_u64(vreinterpretq_u64_m128i(a));
        poly64_t a_hi = (poly64_t)vget_high_u64(vreinterpretq_u64_m128i(a));
        poly64_t b_lo = (poly64_t)vget_low_u64(vreinterpretq_u64_m128i(b));
        poly64_t b_hi = (poly64_t)vget_high_u64(vreinterpretq_u64_m128i(b));
        tmp3 = (__m128i)vmull_p64(a_lo, b_lo);
        tmp4 = (__m128i)vmull_p64(a_hi, b_lo);
        tmp5 = (__m128i)vmull_p64(a_lo, b_hi);
        tmp6 = (__m128i)vmull_p64(a_hi, b_hi);

        tmp4 = _mm_xor_si128(tmp4, tmp5);
        tmp5 = _mm_slli_si128(tmp4, 8);
        tmp4 = _mm_srli_si128(tmp4, 8);
        tmp3 = _mm_xor_si128(tmp3, tmp5);
        tmp6 = _mm_xor_si128(tmp6, tmp4);
        // initial mul now in tmp3, tmp6
        *res1 = tmp3;
        *res2 = tmp6;
    }
#endif

#ifdef __x86_64__
__attribute__((target("sse2,pclmul")))
#endif
/**
 * multiplication in galois field with reduction.
 *
 * @param a input a.
 * @param b input b.
 * @param res a * b.
 */
inline void gf2k_multiply(__m128i a, __m128i b, __m128i *res) {
    __m128i tmp3, tmp6, tmp7, tmp8, tmp9, tmp10, tmp11, tmp12, tmp13;
    __m128i XMMMASK = _mm_setr_epi32(0xFFFFFFFF, 0x0, 0x0, 0x0);
    gf2k_multiply_128(a, b, &tmp3, &tmp6);
    tmp7 = _mm_srli_epi32(tmp6, 31);
    tmp8 = _mm_srli_epi32(tmp6, 30);
    tmp9 = _mm_srli_epi32(tmp6, 25);
    tmp7 = _mm_xor_si128(tmp7, tmp8);
    tmp7 = _mm_xor_si128(tmp7, tmp9);
    tmp8 = _mm_shuffle_epi32(tmp7, 147);

    tmp7 = _mm_and_si128(XMMMASK, tmp8);
    tmp8 = _mm_andnot_si128(XMMMASK, tmp8);
    tmp3 = _mm_xor_si128(tmp3, tmp8);
    tmp6 = _mm_xor_si128(tmp6, tmp7);
    tmp10 = _mm_slli_epi32(tmp6, 1);
    tmp3 = _mm_xor_si128(tmp3, tmp10);
    tmp11 = _mm_slli_epi32(tmp6, 2);
    tmp3 = _mm_xor_si128(tmp3, tmp11);
    tmp12 = _mm_slli_epi32(tmp6, 7);
    tmp3 = _mm_xor_si128(tmp3, tmp12);
    // 经过测试，执行上述运算得到的结果，字节变为了大端表示，为了保持统一，再转换为小端表示
    tmp13 = _mm_xor_si128(tmp3, tmp6);
    *res = _mm_shuffle_epi8(tmp13, reverse_byte);
}

#endif //MPC4J_NATIVE_TOOL_GF2K_H
