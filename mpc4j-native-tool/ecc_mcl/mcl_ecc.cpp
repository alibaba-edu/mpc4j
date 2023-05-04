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

jstring mcl_precompute_multiply(JNIEnv *env, jobject jWindowHandler, jstring jZnString) {
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

jstring mcl_multiply(JNIEnv *env, jstring jEcString, jstring jZnString) {
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