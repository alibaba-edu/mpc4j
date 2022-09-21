//
// Created by Weiran Liu on 2022/9/6.
//

#include <sodium.h>
#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc.h"

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc_nativeMul
        (JNIEnv *env, jobject context, jbyteArray jEcByteArray, jbyteArray jZnByteArray) {
    jbyte* ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    auto* p = new unsigned char[crypto_core_ed25519_BYTES];
    memcpy(p, ecBuffer, crypto_core_ed25519_BYTES);
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    auto* k = new unsigned char[crypto_core_ed25519_SCALARBYTES];
    memcpy(k, znBuffer, crypto_core_ed25519_SCALARBYTES);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);
    auto* r = new unsigned char[crypto_core_ed25519_BYTES];
    crypto_scalarmult_ed25519_noclamp(r, k, p);
    delete[] p;
    delete[] k;
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)crypto_core_ed25519_BYTES);
    (*env).SetByteArrayRegion(jMulByteArray, 0, crypto_core_ed25519_BYTES, (const jbyte*)r);
    delete[] r;
    return jMulByteArray;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc_nativeBaseMul
        (JNIEnv *env, jobject context, jbyteArray jZnByteArray) {
    jbyte* znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    auto* k = new unsigned char[crypto_core_ed25519_SCALARBYTES];
    memcpy(k, znBuffer, crypto_core_ed25519_SCALARBYTES);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);
    auto* r = new unsigned char[crypto_core_ed25519_BYTES];
    crypto_scalarmult_ed25519_base_noclamp(r, k);
    delete[] k;
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize)crypto_core_ed25519_BYTES);
    (*env).SetByteArrayRegion(jMulByteArray, 0, crypto_core_ed25519_BYTES, (const jbyte*)r);
    delete[] r;
    return jMulByteArray;
}