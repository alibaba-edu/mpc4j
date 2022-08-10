/*
 * Created by Weiran Liu on 2022/4/18.
 * ECB-AES实现，源代码参考：
 * <https://github.com/shadowsocks/crypto2/blob/master/src/blockcipher/aes/aarch64.rs>
 * <https://github.com/emp-toolkit/emp-tool/blob/master/emp-tool/utils/aes.h>
 */
/* crypto/aes/aes.h -*- mode:C; c-file-style: "eay" -*- */
/* ====================================================================
 * Copyright (c) 1998-2002 The OpenSSL Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software developed by the OpenSSL Project
 *    for use in the OpenSSL Toolkit. (http://www.openssl.org/)"
 *
 * 4. The names "OpenSSL Toolkit" and "OpenSSL Project" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please contact
 *    openssl-core@openssl.org.
 *
 * 5. Products derived from this software may not be called "OpenSSL"
 *    nor may "OpenSSL" appear in their names without prior written
 *    permission of the OpenSSL Project.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the OpenSSL Project
 *    for use in the OpenSSL Toolkit (http://www.openssl.org/)"
 *
 * THIS SOFTWARE IS PROVIDED BY THE OpenSSL PROJECT ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */

#ifndef MPC4J_NATIVE_TOOL_AES_H
#define MPC4J_NATIVE_TOOL_AES_H

#include "defines.h"
#include "block.h"

/**
 * AES key structure
 */
typedef struct {
    __m128i rd_key[20];
} AES_KEY;

#define EXPAND_ASSIST(v1, v2, v3, v4, shuff_const, aes_const)                    \
    v2 = _mm_aeskeygenassist_si128(v4,aes_const);                           \
    v3 = _mm_castps_si128(_mm_shuffle_ps(_mm_castsi128_ps(v3),              \
                                         _mm_castsi128_ps(v1), 16));        \
    v1 = _mm_xor_si128(v1,v3);                                              \
    v3 = _mm_castps_si128(_mm_shuffle_ps(_mm_castsi128_ps(v3),              \
                                         _mm_castsi128_ps(v1), 140));       \
    v1 = _mm_xor_si128(v1,v3);                                              \
    v2 = _mm_shuffle_epi32(v2,shuff_const);                                 \
    v1 = _mm_xor_si128(v1,v2)

inline void
#ifdef __x86_64__
__attribute__((target("aes,sse2")))
#endif
aes_set_key(const uint8_t *user_key, AES_KEY *key) {
    __m128i x0, x1, x2;
    __m128i *kp = key->rd_key;
    kp[0] = x0 = _mm_loadu_si128((const __m128i *) user_key);
    x2 = _mm_setzero_si128();
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 1);
    kp[1] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 2);
    kp[2] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 4);
    kp[3] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 8);
    kp[4] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 16);
    kp[5] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 32);
    kp[6] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 64);
    kp[7] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 128);
    kp[8] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 27);
    kp[9] = x0;
    EXPAND_ASSIST(x0, x1, x2, x0, 255, 54);
    kp[10] = x0;
    // decrypt key
    kp[11] = _mm_aesimc_si128(kp[9]);
    kp[12] = _mm_aesimc_si128(kp[8]);
    kp[13] = _mm_aesimc_si128(kp[7]);
    kp[14] = _mm_aesimc_si128(kp[6]);
    kp[15] = _mm_aesimc_si128(kp[5]);
    kp[16] = _mm_aesimc_si128(kp[4]);
    kp[17] = _mm_aesimc_si128(kp[3]);
    kp[18] = _mm_aesimc_si128(kp[2]);
    kp[19] = _mm_aesimc_si128(kp[1]);
}

#ifdef __x86_64__
__attribute__((target("aes,sse2")))
inline void aes_ecb_encrypt(uint8_t *plaintext, const AES_KEY *key) {
    __m128i m = _mm_loadu_si128((const __m128i*)plaintext);
    // 第1轮加密
    m = _mm_xor_si128(m, key->rd_key[0]);
    // 第2-10轮加密
    m = _mm_aesenc_si128(m, key->rd_key[1]);
    m = _mm_aesenc_si128(m, key->rd_key[2]);
    m = _mm_aesenc_si128(m, key->rd_key[3]);
    m = _mm_aesenc_si128(m, key->rd_key[4]);
    m = _mm_aesenc_si128(m, key->rd_key[5]);
    m = _mm_aesenc_si128(m, key->rd_key[6]);
    m = _mm_aesenc_si128(m, key->rd_key[7]);
    m = _mm_aesenc_si128(m, key->rd_key[8]);
    m = _mm_aesenc_si128(m, key->rd_key[9]);
    m = _mm_aesenclast_si128(m, key->rd_key[10]);
    _mm_storeu_si128((__m128i *)plaintext, m);
}
#elif __aarch64__

inline void aes_ecb_encrypt(uint8_t *plaintext, const AES_KEY *key) {
    auto m = vld1q_u8(plaintext);
    auto *keys = (uint8x16_t *) (key->rd_key);
    // Round 1: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[0]);
    m = vaesmcq_u8(m);
    // Round 2: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[1]);
    m = vaesmcq_u8(m);
    // Round 3: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[2]);
    m = vaesmcq_u8(m);
    // Round 4: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[3]);
    m = vaesmcq_u8(m);
    // Round 5: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[4]);
    m = vaesmcq_u8(m);
    // Round 6: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[5]);
    m = vaesmcq_u8(m);
    // Round 7: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[6]);
    m = vaesmcq_u8(m);
    // Round 8: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[7]);
    m = vaesmcq_u8(m);
    // Round 9: single round encryption + mix columns
    m = vaeseq_u8(m, (uint8x16_t) keys[8]);
    m = vaesmcq_u8(m);
    // Round 10: single round encryption + bitwise XOR
    m = vaeseq_u8(m, (uint8x16_t) keys[9]);
    m = veorq_u8(m, (uint8x16_t) keys[10]);
    vst1q_u8(plaintext, m);
}

#endif

#ifdef __x86_64__
__attribute__((target("aes,sse2")))
inline void aes_ecb_decrypt(uint8_t *plaintext, const AES_KEY *key) {
    __m128i m = _mm_loadu_si128((const __m128i*)plaintext);
    // 第1轮解密
    m = _mm_xor_si128(m, key->rd_key[10]);
    // 第2-10轮加密
    m = _mm_aesdec_si128(m, key->rd_key[11]);
    m = _mm_aesdec_si128(m, key->rd_key[12]);
    m = _mm_aesdec_si128(m, key->rd_key[13]);
    m = _mm_aesdec_si128(m, key->rd_key[14]);
    m = _mm_aesdec_si128(m, key->rd_key[15]);
    m = _mm_aesdec_si128(m, key->rd_key[16]);
    m = _mm_aesdec_si128(m, key->rd_key[17]);
    m = _mm_aesdec_si128(m, key->rd_key[18]);
    m = _mm_aesdec_si128(m, key->rd_key[19]);
    m = _mm_aesdeclast_si128(m, key->rd_key[0]);
    _mm_storeu_si128((__m128i *)plaintext, m);
}
#elif __aarch64__

inline void aes_ecb_decrypt(uint8_t *ciphertext, const AES_KEY *key) {
    auto m = vld1q_u8(ciphertext);
    auto *keys = (uint8x16_t *) (key->rd_key);
    // Round 1: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[10]);
    m = vaesimcq_u8(m);
    // Round 2: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[11]);
    m = vaesimcq_u8(m);
    // Round 3: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[12]);
    m = vaesimcq_u8(m);
    // Round 4: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[13]);
    m = vaesimcq_u8(m);
    // Round 5: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[14]);
    m = vaesimcq_u8(m);
    // Round 6: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[15]);
    m = vaesimcq_u8(m);
    // Round 7: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[16]);
    m = vaesimcq_u8(m);
    // Round 8: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[17]);
    m = vaesimcq_u8(m);
    // Round 9: single round encryption + mix columns
    m = vaesdq_u8(m, (uint8x16_t) keys[18]);
    m = vaesimcq_u8(m);
    // Round 10: single round encryption + bitwise XOR
    m = vaesdq_u8(m, (uint8x16_t) keys[19]);
    m = veorq_u8(m, (uint8x16_t) keys[0]);
    vst1q_u8(ciphertext, m);
}

#endif

#endif //MPC4J_NATIVE_TOOL_AES_H
