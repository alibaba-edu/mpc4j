//
// Created by Weiran Liu on 2022/7/7.
//
#include "mcl_ecc.h"

void ecFromString(const std::string &ecString, Ec &ecPoint) {
    cybozu::StringInputStream is(ecString);
    ecPoint.load(is, MCL_RADIX);
}

void znFromString(const std::string &znString, Zn &zn) {
    cybozu::StringInputStream is(znString);
    zn.load(is, MCL_RADIX);
}

void stringSetToZnSet(std::vector<std::string> &stringSet, std::vector<Zn> &znSet) {
    znSet.resize(stringSet.size());
    for (std::vector<std::string>::size_type index = 0; index < stringSet.size(); index++) {
        znFromString(stringSet[index], znSet[index]);
    }
}

void ecSetToStringSet(std::vector<Ec> &ecSet, std::vector<std::string> &stringSet) {
    stringSet.resize(ecSet.size());
    for (std::vector<std::string>::size_type index = 0; index < stringSet.size(); index++) {
        stringSet[index] = ecSet[index].getStr(MCL_RADIX);
    }
}

void mcl_init(int curveType) {
    mcl::initCurve<Ec, Zn>(curveType);
}

jobject mcl_precompute(JNIEnv *env, jstring jEcString) {
    // 读取椭圆曲线点
    const char *jEcStringHandler = (*env).GetStringUTFChars(jEcString, JNI_FALSE);
    const std::string ecString = std::string(jEcStringHandler);
    (*env).ReleaseStringUTFChars(jEcString, jEcStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 预计算
    auto *windowHandler = new mcl::fp::WindowMethod<Ec>();
    (*windowHandler).init(ec, Zn::getBitSize(), MCL_WIN_SIZE);

    return (*env).NewDirectByteBuffer(windowHandler, 0);
}

void mcl_destroy_precompute(JNIEnv *env, jobject jWindowHandler) {
    delete (mcl::fp::WindowMethod<Ec> *)(*env).GetDirectBufferAddress(jWindowHandler);
}

jstring mcl_single_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jstring jZnString) {
    // 读取幂指数
    const char* jZnStringHandler = (*env).GetStringUTFChars(jZnString, JNI_FALSE);
    std::string znString = std::string(jZnStringHandler);
    (*env).ReleaseStringUTFChars(jZnString, jZnStringHandler);
    Zn zn;
    znFromString(znString, zn);
    // 定点计算
    auto * windowHandler = (mcl::fp::WindowMethod<Ec> *)(*env).GetDirectBufferAddress(jWindowHandler);
    Ec ec;
    (*windowHandler).mul(ec, zn);
    ec.normalize();
    // 返回结果
    std::string ecString = ec.getStr(MCL_RADIX);
    return (*env).NewStringUTF(ecString.data());
}

jobjectArray mcl_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jobjectArray jZnStringArray) {
    // 读取幂指数集合
    std::vector<std::string> znStringSet;
    jStringArrayToSet(env, jZnStringArray, znStringSet);
    std::vector<Zn> znSet;
    stringSetToZnSet(znStringSet, znSet);
    znStringSet.clear();
    auto *windowHandler = (mcl::fp::WindowMethod<Ec> *) (*env).GetDirectBufferAddress(jWindowHandler);
    // 定点计算
    std::vector<Ec> ecSet(znSet.size());
    for (std::vector<Ec>::size_type index = 0; index < znSet.size(); index++) {
        (*windowHandler).mul(ecSet[index], znSet[index]);
        ecSet[index].normalize();
    }
    znSet.clear();
    // 返回结果
    std::vector<std::string> ecStringSet;
    ecSetToStringSet(ecSet, ecStringSet);
    ecSet.clear();
    jobjectArray jEcStringArray;
    setTojStringArray(env, ecStringSet, jEcStringArray);
    ecStringSet.clear();

    return jEcStringArray;
}

jstring mcl_single_multiply(JNIEnv *env, jstring jEcString, jstring jZnString) {
    // 读取幂指数
    const char* jZnStringHandler = (*env).GetStringUTFChars(jZnString, JNI_FALSE);
    std::string znString = std::string(jZnStringHandler);
    (*env).ReleaseStringUTFChars(jZnString, jZnStringHandler);
    Zn zn;
    znFromString(znString, zn);
    // 读取椭圆曲线点
    const char* jEcStringHandler = (*env).GetStringUTFChars(jEcString, JNI_FALSE);
    const std::string ecString = std::string(jEcStringHandler);
    (*env).ReleaseStringUTFChars(jEcString, jEcStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 计算乘法
    Ec mulEc;
    Ec::mul(mulEc, ec, zn);
    mulEc.normalize();
    // 返回结果
    std::string mulEcString = mulEc.getStr(MCL_RADIX);
    return (*env).NewStringUTF(mulEcString.data());
}

jobjectArray mcl_multiply(JNIEnv *env, jstring jEcString, jobjectArray jZnStringArray) {
    // 读取椭圆曲线点
    const char* jEcStringHandler = (*env).GetStringUTFChars(jEcString, JNI_FALSE);
    const std::string ecString = std::string(jEcStringHandler);
    (*env).ReleaseStringUTFChars(jEcString, jEcStringHandler);
    Ec ec;
    ecFromString(ecString, ec);
    // 读取幂指数集合
    std::vector<std::string> znStringSet;
    jStringArrayToSet(env, jZnStringArray, znStringSet);
    std::vector<Zn> znSet;
    stringSetToZnSet(znStringSet, znSet);
    znStringSet.clear();
    // 计算乘法
    std::vector<Ec> ecMulSet(znSet.size());
    for (std::vector<Ec>::size_type index = 0; index < znSet.size(); index++) {
        Ec::mul(ecMulSet[index], ec, znSet[index]);
        ecMulSet[index].normalize();
    }
    znSet.clear();
    // 返回结果
    std::vector<std::string> mulEcStringSet;
    ecSetToStringSet(ecMulSet, mulEcStringSet);
    ecMulSet.clear();
    jobjectArray jMulEcStringArray;
    setTojStringArray(env, mulEcStringSet, jMulEcStringArray);
    mulEcStringSet.clear();

    return jMulEcStringArray;
}