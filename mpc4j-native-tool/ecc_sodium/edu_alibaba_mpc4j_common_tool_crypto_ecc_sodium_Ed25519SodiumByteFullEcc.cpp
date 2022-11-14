/*
 * Created by Weiran Liu on 2022/9/6.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include <sodium.h>
#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc.h"

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc_nativeMul
        (JNIEnv *env, jobject context, jbyteArray jEcByteArray, jbyteArray jZnByteArray) {
    jbyte* ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[crypto_core_ed25519_BYTES];
    memcpy(p, ecBuffer, crypto_core_ed25519_BYTES);
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[crypto_core_ed25519_SCALARBYTES];
    memcpy(k, znBuffer, crypto_core_ed25519_SCALARBYTES);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);
    uint8_t r[crypto_core_ed25519_BYTES];
    crypto_scalarmult_ed25519_noclamp(r, k, p);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)crypto_core_ed25519_BYTES);
    (*env).SetByteArrayRegion(jMulByteArray, 0, crypto_core_ed25519_BYTES, (const jbyte*)r);

    return jMulByteArray;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc_nativeBaseMul
        (JNIEnv *env, jobject context, jbyteArray jZnByteArray) {
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[crypto_core_ed25519_SCALARBYTES];
    memcpy(k, znBuffer, crypto_core_ed25519_SCALARBYTES);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);
    uint8_t r[crypto_core_ed25519_BYTES];
    crypto_scalarmult_ed25519_base_noclamp(r, k);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)crypto_core_ed25519_BYTES);
    (*env).SetByteArrayRegion(jMulByteArray, 0, crypto_core_ed25519_BYTES, (const jbyte*)r);

    return jMulByteArray;
}