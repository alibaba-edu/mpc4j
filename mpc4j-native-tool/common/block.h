//
// Created by Weiran Liu on 2022/1/15.
// 分组寄存器定义，参考代码：https://github.com/emp-toolkit/emp-tool/blob/master/emp-tool/utils/block.h
//

#ifndef MPC4J_NATIVE_TOOL_BLOCK_H
#define MPC4J_NATIVE_TOOL_BLOCK_H
#ifdef __x86_64__
#include <immintrin.h>
#elif __aarch64__
#include "sse2neon.h"
inline __m128i _mm_aesimc_si128(__m128i a) {
    return vreinterpretq_m128i_u8(vaesimcq_u8(vreinterpretq_u8_m128i(a)));
}
#endif

/**
 * 用于置换__m128i的位置，代码参考：
 * https://wunkolo.github.io/post/2020/11/gf2p8affineqb-bit-reversal/
 */
const __m128i reverse_byte = _mm_set_epi8(
    0, 1, 2, 3, 4, 5, 6, 7,
    8, 9, 10, 11, 12, 13, 14, 15);

/**
 * 将输入的字节数组转换为__m128i。
 *
 * @param data 字节数组。
 * @return 转换结果。
 */
inline __m128i make_block(const uint8_t *data) {
    return _mm_set_epi8(
            (signed char) data[0], (signed char) data[1], (signed char) data[2], (signed char) data[3],
            (signed char) data[4], (signed char) data[5], (signed char) data[6], (signed char) data[7],
            (signed char) data[8], (signed char) data[9], (signed char) data[10], (signed char) data[11],
            (signed char) data[12], (signed char) data[13], (signed char) data[14], (signed char) data[15]
    );
}

#endif //MPC4J_NATIVE_TOOL_BLOCK_H
