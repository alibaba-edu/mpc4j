//
// Created by Weiran Liu on 2022/8/4.
//
#include <NTL/ZZ_p.h>
#include <NTL/ZZ_pX.h>
#include <NTL/ZZ.h>
#include <vector>

#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_ZP64_H
#define MPC4J_NATIVE_TOOL_NTL_ZP64_H

/**
  * 给定long格式的{x_i, y_i}，得到插值多项式。
  *
  * @param num 插值数量。如果实际插值数量不够，则自动补充虚拟元素。
  * @param xArray 集合{x_i}。
  * @param yArray 集合{y_i}。
  * @param coeffs 插值多项式系数。
  */
void zp64_interpolate(uint64_t num, std::vector<long> &setX, std::vector<long> &setY, std::vector<long> &coeffs);

/**
  * 给定long格式的{x_i}和long格式的y，得到插值多项式。
  *
  * @param num 插值数量。如果实际插值数量不够，则自动补充虚拟元素。
  * @param setX 集合{x_i}。
  * @param y y的取值。
  * @param coeffs 插值多项式系数。
  */
void zp64_root_interpolate(uint64_t num, std::vector<long> &setX, long y, std::vector<long> &coeffs);

/**
  * 给定long格式的多项式系数和long格式的x，求y = f(x)。
  *
  * @param coeffs byte[]格式的多项式系数。
  * @param x 输入x。
  * @return 输出y。
  */
long zp64_evaluate(std::vector<long> &coeffs, long x);

#endif //MPC4J_NATIVE_TOOL_NTL_ZP64_H
