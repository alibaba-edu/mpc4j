//
// Created by Weiran Liu on 2022/1/5.
//

#include "ntl_zp.h"
#include "ntl_zp_util.h"

void zp_interpolate(uint64_t primeByteLength, uint64_t expect_num, std::vector<uint8_t *> &setX, std::vector<uint8_t *> &setY, std::vector<uint8_t *> &coeffs) {
    NTL::vec_ZZ_p x;
    NTL::vec_ZZ_p y;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;

    for (uint64_t i = 0; i < setX.size(); ++i) {
        // 转换x
        NTL::ZZFromBytes(e_ZZ, setX[i], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        x.append(e_ZZ_p);
        // 转换y
        NTL::ZZFromBytes(e_ZZ, setY[i], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        y.append(e_ZZ_p);
    }

    NTL::ZZ_pX polynomial = NTL::interpolate(x, y);
    zp_polynomial_pad_dummy_item(polynomial, x, expect_num);
    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e_ZZ = rep(NTL::coeff(polynomial, i));
        coeffs[i] = new uint8_t[primeByteLength];
        NTL::BytesFromZZ(coeffs[i], e_ZZ, (long) (primeByteLength));
    }
}

void zp_root_interpolate(uint64_t primeByteLength, uint64_t expect_num, std::vector<uint8_t*> &setX, uint8_t* y, std::vector<uint8_t*> &coeffs) {
    NTL::vec_ZZ_p x;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    // 将插值点导入到GF2E向量中
    for (auto & xBytes : setX) {
        NTL::ZZFromBytes(e_ZZ, xBytes, static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        x.append(e_ZZ_p);
    }
    NTL::ZZFromBytes(e_ZZ, y, static_cast<long>(primeByteLength));
    e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
    NTL::ZZ_pX root_polynomial;
    // 构建根多项式
    NTL::BuildFromRoots(root_polynomial, x);
    // 构建虚拟多项式
    NTL::ZZ_pX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(expect_num - setX.size()));
    // 把虚拟多项式的最高阶设置为1
    NTL::ZZ_p one = NTL::to_ZZ_p(1L);
    NTL::SetCoeff(dummy_polynomial, static_cast<long>(expect_num - setX.size()), one);
    NTL::ZZ_pX polynomial = e_ZZ_p + dummy_polynomial * root_polynomial;
    // 构建返回系数
    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e_ZZ = rep(NTL::coeff(polynomial, i));
        coeffs[i] = new uint8_t [primeByteLength];
        NTL::BytesFromZZ(coeffs[i], e_ZZ, (long) (primeByteLength));
    }
}

void zp_evaluate(uint64_t primeByteLength, std::vector<uint8_t *> &coeffs, uint8_t *x, uint8_t *y) {
    NTL::ZZ_pX res_polynomial;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    for (uint64_t i = 0; i < coeffs.size(); ++i) {
        NTL::ZZFromBytes(e_ZZ, coeffs[i], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        // build res_polynomial
        NTL::SetCoeff(res_polynomial, static_cast<long>(i), e_ZZ_p);
    }
    // get y = f(x) in ZZ_p
    NTL::ZZFromBytes(e_ZZ, x, static_cast<long>(primeByteLength));
    e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
    e_ZZ_p = NTL::eval(res_polynomial, e_ZZ_p);
    // convert to byte[]
    e_ZZ = NTL::rep(e_ZZ_p);
    NTL::BytesFromZZ(y, e_ZZ, static_cast<long>(primeByteLength));
}