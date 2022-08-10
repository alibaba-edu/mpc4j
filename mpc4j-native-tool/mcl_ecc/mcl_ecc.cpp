//
// Created by Weiran Liu on 2022/7/7.
//
#include "mcl_ecc.h"

void ecFromString(const std::string &ecString, Ec &ecPoint) {
    cybozu::StringInputStream is(ecString);
    ecPoint.load(is, RADIX);
}

void znFromString(const std::string &znString, Zn &zn) {
    cybozu::StringInputStream is(znString);
    zn.load(is, RADIX);
}

void jStringArrayToSet(JNIEnv *env, jobjectArray jStringArray, std::vector<std::string> &set) {
    // 获得数组的长度
    uint64_t length = (*env).GetArrayLength(jStringArray);
    set.resize(static_cast<unsigned long>(length));
    for (uint64_t index = 0; index < length; index++) {
        auto jString = (jstring) (*env).GetObjectArrayElement(jStringArray, static_cast<jsize>(index));
        const char *jStringHandler = (*env).GetStringUTFChars(jString, JNI_FALSE);
        set[index] = std::string(jStringHandler);
        (*env).ReleaseStringUTFChars(jString, jStringHandler);
        (*env).DeleteLocalRef(jString);
    }
}

void setTojStringArray(JNIEnv *env, std::vector<std::string> &set, jobjectArray &jStringArray) {
    jclass jStringClass = (*env).FindClass("java/lang/String");
    jStringArray = (*env).NewObjectArray(static_cast<jsize>(set.size()), jStringClass, nullptr);
    // 复制结果
    for (uint64_t index = 0; index < set.size(); index++) {
        jstring jString = (*env).NewStringUTF(set[index].data());
        (*env).SetObjectArrayElement(jStringArray, static_cast<jsize>(index), jString);
        (*env).DeleteLocalRef(jString);
    }
}

void stringSetToZnSet(std::vector<std::string> &stringSet, std::vector<Zn> &znSet) {
    znSet.resize(stringSet.size());
    for (uint64_t index = 0; index < stringSet.size(); index++) {
        znFromString(stringSet[index], znSet[index]);
    }
}

void ecSetToStringSet(std::vector<Ec> &ecSet, std::vector<std::string> &stringSet) {
    stringSet.resize(ecSet.size());
    for (uint64_t index = 0; index < stringSet.size(); index++) {
        stringSet[index] = ecSet[index].getStr(RADIX);
    }
}

jobject precompute(JNIEnv *env, jstring jecString) {
    // 读取椭圆曲线点
    const char *jecStringHandler = (*env).GetStringUTFChars(jecString, JNI_FALSE);
    const std::string ecString = std::string(jecStringHandler);
    (*env).ReleaseStringUTFChars(jecString, jecStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 预计算
    auto *windowHandler = new mcl::fp::WindowMethod<Ec>();
    (*windowHandler).init(ec, Zn::getBitSize(), WIN_SIZE);

    return (*env).NewDirectByteBuffer(windowHandler, 0);
}

void destroyPrecompute(JNIEnv *env, jobject windowHandler) {
    delete (mcl::fp::WindowMethod<Ec> *)(*env).GetDirectBufferAddress(windowHandler);
}

jstring singleFixedPointMultiply(JNIEnv *env, jobject jwindowHandler, jstring jznString) {
    // 读取幂指数
    const char* jznStringHandler = (*env).GetStringUTFChars(jznString, JNI_FALSE);
    std::string znString = std::string(jznStringHandler);
    (*env).ReleaseStringUTFChars(jznString, jznStringHandler);
    Zn zn;
    znFromString(znString, zn);
    // 定点计算
    auto * windowHandler = (mcl::fp::WindowMethod<Ec> *)(*env).GetDirectBufferAddress(jwindowHandler);
    Ec ec;
    (*windowHandler).mul(ec, zn);
    ec.normalize();
    // 返回结果
    std::string ecString = ec.getStr(RADIX);
    return (*env).NewStringUTF(ecString.data());
}

jobjectArray fixedPointMultiply(JNIEnv *env, jobject jwindowHandler, jobjectArray jznStringArray) {
    // 读取幂指数集合
    std::vector<std::string> znStringSet;
    jStringArrayToSet(env, jznStringArray, znStringSet);
    std::vector<Zn> znSet;
    stringSetToZnSet(znStringSet, znSet);
    znStringSet.clear();
    auto *windowHandler = (mcl::fp::WindowMethod<Ec> *) (*env).GetDirectBufferAddress(jwindowHandler);
    // 定点计算
    std::vector<Ec> ecSet(znSet.size());
    for (uint64_t index = 0; index < znSet.size(); index++) {
        (*windowHandler).mul(ecSet[index], znSet[index]);
        ecSet[index].normalize();
    }
    znSet.clear();
    // 返回结果
    std::vector<std::string> ecStringSet;
    ecSetToStringSet(ecSet, ecStringSet);
    ecSet.clear();
    jobjectArray jecStringArray;
    setTojStringArray(env, ecStringSet, jecStringArray);
    ecStringSet.clear();

    return jecStringArray;
}

jstring singleMultiply(JNIEnv *env, jstring jecString, jstring jznString) {
    // 读取幂指数
    const char* jznStringHandler = (*env).GetStringUTFChars(jznString, JNI_FALSE);
    std::string znString = std::string(jznStringHandler);
    (*env).ReleaseStringUTFChars(jznString, jznStringHandler);
    Zn zn;
    znFromString(znString, zn);
    // 读取椭圆曲线点
    const char* jecStringHandler = (*env).GetStringUTFChars(jecString, JNI_FALSE);
    const std::string ecString = std::string(jecStringHandler);
    (*env).ReleaseStringUTFChars(jecString, jecStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 计算乘法
    Ec mulEc;
    Ec::mul(mulEc, ec, zn);
    mulEc.normalize();
    // 返回结果
    std::string mulEcString = mulEc.getStr(RADIX);
    return (*env).NewStringUTF(mulEcString.data());
}

jobjectArray multiply(JNIEnv *env, jstring jecString, jobjectArray jznStringArray) {
    // 读取椭圆曲线点
    const char* jecStringHandler = (*env).GetStringUTFChars(jecString, JNI_FALSE);
    const std::string ecString = std::string(jecStringHandler);
    (*env).ReleaseStringUTFChars(jecString, jecStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 读取幂指数集合
    std::vector<std::string> znStringSet;
    jStringArrayToSet(env, jznStringArray, znStringSet);
    std::vector<Zn> znSet;
    stringSetToZnSet(znStringSet, znSet);
    znStringSet.clear();
    // 计算乘法
    std::vector<Ec> ecMulSet(znSet.size());
    for (uint64_t index = 0; index < znSet.size(); index++) {
        Ec::mul(ecMulSet[index], ec, znSet[index]);
        ecMulSet[index].normalize();
    }
    znSet.clear();
    // 返回结果
    std::vector<std::string> mulEcStringSet;
    ecSetToStringSet(ecMulSet, mulEcStringSet);
    ecMulSet.clear();
    jobjectArray jmulEcStringArray;
    setTojStringArray(env, mulEcStringSet, jmulEcStringArray);
    mulEcStringSet.clear();

    return jmulEcStringArray;
}