//
// Created by Weiran Liu on 2021/12/11.
//
#include "ntl_gf2x.h"

void GF2EFromBytes(NTL::GF2E &element, uint8_t* data, uint64_t size) {
    const NTL::GF2X fromBytes = NTL::GF2XFromBytes(data, (long)size);
    element = to_GF2E(fromBytes);
}

void BytesFromGF2E(uint8_t *data, NTL::GF2E &element, uint64_t size) {
    // The function rep returns the representation of GF2E as the related GF2X. It returns as read only.
    const NTL::GF2X fromElement = NTL::rep(element);
    BytesFromGF2X(data, fromElement, (long)size);
}

void gf2x_interpolate(uint64_t lBytes, uint64_t num, std::vector<uint8_t*> &setX, std::vector<uint8_t*> &setY, std::vector<uint8_t*> &coeffs) {
    NTL::vec_GF2E x;
    NTL::vec_GF2E y;
    NTL::GF2E temp;
    // 将插值点导入到GF2E向量中
    for (uint64_t i = 0; i < setX.size(); ++i) {
        GF2EFromBytes(temp, setX[i], lBytes);
        x.append(temp);
        GF2EFromBytes(temp, setY[i], lBytes);
        y.append(temp);
    }
    // 简单插值
    NTL::GF2EX polynomial = NTL::interpolate(x, y);
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
    NTL::GF2EX root_polynomial;
    NTL::BuildFromRoots(root_polynomial, x);
    // 构建虚拟多项式
    NTL::GF2EX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(num - setX.size()));
    NTL::GF2EX d_polynomial;
    polynomial = polynomial + dummy_polynomial * root_polynomial;
    // 构建返回系数
    coeffs.resize(static_cast<unsigned long>(NTL::deg(polynomial) + 1));
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        temp = NTL::coeff(polynomial, i);
        coeffs[i] = new uint8_t [lBytes];
        BytesFromGF2E(coeffs[i], temp, lBytes);
    }
}

void gf2x_root_interpolate(uint64_t lBytes, uint64_t num, std::vector<uint8_t*> &setX, uint8_t* y, std::vector<uint8_t*> &coeffs) {
    NTL::vec_GF2E x;
    NTL::GF2E e;
    // 将插值点导入到GF2E向量中
    for (auto & xBytes : setX) {
        GF2EFromBytes(e, xBytes, lBytes);
        x.append(e);
    }
    GF2EFromBytes(e, y, lBytes);
    NTL::GF2EX root_polynomial;
    // 构建根多项式
    NTL::BuildFromRoots(root_polynomial, x);
    // 构建虚拟多项式
    NTL::GF2EX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(num - setX.size()));
    // 把虚拟多项式的最高阶设置为1
    NTL::GF2E one = NTL::to_GF2E(1L);
    NTL::SetCoeff(dummy_polynomial, static_cast<long>(num - setX.size()), one);
    NTL::GF2EX polynomial = e + dummy_polynomial * root_polynomial;
    // 构建返回系数
    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e = NTL::coeff(polynomial, i);
        coeffs[i] = new uint8_t [lBytes];
        BytesFromGF2E(coeffs[i], e, lBytes);
    }
}

void gf2x_evaluate(uint64_t lBytes, std::vector<uint8_t*> &coeffs, uint8_t* x, uint8_t* y) {
    NTL::GF2EX res_polynomial;
    NTL::GF2E temp;
    // parse f(x)
    for (uint64_t i = 0; i < coeffs.size(); ++i) {
        GF2EFromBytes(temp, coeffs[i], lBytes);
        // build res_polynomial
        NTL::SetCoeff(res_polynomial, (long)i, temp);
    }
    GF2EFromBytes(temp, x, lBytes);
    // get y = f(x) in GF2E
    temp = NTL::eval(res_polynomial, temp);
    // convert to byte[]
    BytesFromGF2E(y, temp, lBytes);
}