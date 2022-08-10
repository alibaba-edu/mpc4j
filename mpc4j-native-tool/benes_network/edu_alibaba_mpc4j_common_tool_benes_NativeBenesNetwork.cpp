//
// Created by Weiran Liu on 2021/12/25.
//
#include "edu_alibaba_mpc4j_common_tool_benes_NativeBenesNetwork.h"
#include "benes_network.h"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_benes_NativeBenesNetwork_generateBenesNetwork
    (JNIEnv *env, jobject context, jintArray jpermutationMap) {
    // 获得数组的长度
    int length = (*env).GetArrayLength(jpermutationMap);
    std::vector<int32_t> dest(length);
    auto permPointer = (*env).GetIntArrayElements(jpermutationMap, JNI_FALSE);
    // 读取数据
    for (int index = 0; index < length; index++) {
        dest[index] = permPointer[index];
    }
    // 释放指针
    (*env).ReleaseIntArrayElements(jpermutationMap, permPointer, 0);
    // 构造Benes网络
    std::vector<std::vector<int8_t>> switched = generateBenesNetwork(dest);
    // 构造返回值
    auto switchedSize = (jsize)switched.size();
    jclass byteArrayType = (*env).FindClass("[B");
    jobjectArray jBenesNetwork = (*env).NewObjectArray(switchedSize, byteArrayType, nullptr);
    // 复制结果
    for (int index = 0; index < switchedSize; index++) {
        auto columnSize = (jsize)switched[index].size();
        jbyteArray jByteArray = (*env).NewByteArray(columnSize);
        (*env).SetByteArrayRegion(jByteArray, 0, columnSize, switched[index].data());
        (*env).SetObjectArrayElement(jBenesNetwork, index, jByteArray);
        (*env).DeleteLocalRef(jByteArray);
    }
    reset();
    return jBenesNetwork;
}