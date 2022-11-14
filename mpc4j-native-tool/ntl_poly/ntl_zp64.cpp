//
// Created by Weiran Liu on 2022/8/4.
//

#include "ntl_zp64.h"

void zp64_interpolate(uint64_t num, std::vector<long> &setX, std::vector<long> &setY, std::vector<long> &coeffs) {
    NTL::vec_ZZ_p x;
    NTL::vec_ZZ_p y;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;

    for (uint64_t i = 0; i < setX.size(); ++i) {
        // 转换x
        e_ZZ_p = NTL::to_ZZ_p(setX[i]);
        x.append(e_ZZ_p);
        // 转换y
        e_ZZ_p = NTL::to_ZZ_p(setY[i]);
        y.append(e_ZZ_p);
    }

    NTL::ZZ_pX polynomial = NTL::interpolate(x, y);

    /*
     * Indeed, we do not need to pad dummy items to max_bin_size.
     * We can compute a polynomial over real items that has dummy items.
     *
     * For example, there are 3 items in a bin (xi, yi) => interpolate poly p_1(x) of a degree 2.
     * 1. Generate a root poly pRoot(x) of degree 2 over (xi, 0).
     * 2. Generate a dummy poly dummy(x) of degree max_bin_size - degree of p_1(x).
     * 3. Send coefficients of the polynomial dummy(x) * pRoot(x) + p_1(x).
     *
     * If x^* = x_i => pRoot(xi) = 0 => get p_1(x^*).
     */

    NTL::ZZ_pX root_polynomial;
    NTL::BuildFromRoots(root_polynomial, x);

    NTL::ZZ_pX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(num - setX.size()));
    NTL::ZZ_pX d_polynomial;
    polynomial = polynomial + dummy_polynomial * root_polynomial;

    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e_ZZ = rep(NTL::coeff(polynomial, i));
        coeffs[i] = NTL::to_long(e_ZZ);
    }
}

void zp64_root_interpolate(uint64_t num, std::vector<long> &setX, long y, std::vector<long> &coeffs) {
    NTL::vec_ZZ_p x;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    // 将插值点导入到GF2E向量中
    for (long longX : setX) {
        e_ZZ_p = NTL::to_ZZ_p(longX);
        x.append(e_ZZ_p);
    }
    e_ZZ_p = NTL::to_ZZ_p(y);
    NTL::ZZ_pX root_polynomial;
    // 构建根多项式
    NTL::BuildFromRoots(root_polynomial, x);
    // 构建虚拟多项式
    NTL::ZZ_pX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(num - setX.size()));
    // 把虚拟多项式的最高阶设置为1
    NTL::ZZ_p one = NTL::to_ZZ_p(1L);
    NTL::SetCoeff(dummy_polynomial, static_cast<long>(num - setX.size()), one);
    NTL::ZZ_pX polynomial = e_ZZ_p + dummy_polynomial * root_polynomial;
    // 构建返回系数
    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e_ZZ = rep(NTL::coeff(polynomial, i));
        coeffs[i] = NTL::to_long(e_ZZ);
    }
}

long zp64_evaluate(std::vector<long> &coeffs, long x) {
    NTL::ZZ_pX res_polynomial;
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    for (uint64_t i = 0; i < coeffs.size(); ++i) {
        e_ZZ_p = NTL::to_ZZ_p(coeffs[i]);
        // build res_polynomial
        NTL::SetCoeff(res_polynomial, static_cast<long>(i), e_ZZ_p);
    }
    // get y = f(x) in ZZ_p
    e_ZZ_p = NTL::to_ZZ_p(x);
    e_ZZ_p = NTL::eval(res_polynomial, e_ZZ_p);
    // convert to byte[]
    e_ZZ = NTL::rep(e_ZZ_p);
    return NTL::to_long(e_ZZ);
}