//
// Created by Weiran Liu on 2024/3/22.
//

#include "edu_alibaba_mpc4j_common_tool_network_waksman_NativeWaksmanNetwork.h"
#include "waksman_network.hpp"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_network_waksman_NativeWaksmanNetwork_generateNetwork
    (JNIEnv *env, jobject context, jintArray j_permutation_map) {
    // read data
    int length = (*env).GetArrayLength(j_permutation_map);
    std::vector<int32_t> dest(length);
    auto permPointer = (*env).GetIntArrayElements(j_permutation_map, JNI_FALSE);
    for (int index = 0; index < length; index++) {
        dest[index] = permPointer[index];
    }
    // release j_permutation_map
    (*env).ReleaseIntArrayElements(j_permutation_map, permPointer, 0);
    // generate the Benes benes_network
    auto* waksmanNetwork = new WaksmanNetwork(dest);
    std::vector<std::vector<int8_t>> switched = waksmanNetwork->get_waksman_network();
    // create return values
    auto switchedSize = (jsize) switched.size();
    jclass byteArrayType = (*env).FindClass("[B");
    jobjectArray jNetwork = (*env).NewObjectArray(switchedSize, byteArrayType, nullptr);
    // copy
    for (int index = 0; index < switchedSize; index++) {
        auto columnSize = (jsize) switched[index].size();
        jbyteArray jByteArray = (*env).NewByteArray(columnSize);
        (*env).SetByteArrayRegion(jByteArray, 0, columnSize, switched[index].data());
        (*env).SetObjectArrayElement(jNetwork, index, jByteArray);
        (*env).DeleteLocalRef(jByteArray);
    }
    delete waksmanNetwork;
    (*env).DeleteLocalRef(byteArrayType);
    return jNetwork;
}