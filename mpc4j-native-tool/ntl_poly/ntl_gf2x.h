//
// Created by Weiran Liu on 2021/12/11.
//
#include <NTL/GF2E.h>
#include <NTL/GF2EX.h>
#include <NTL/GF2XFactoring.h>
#include <vector>

#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_GF2X_H
#define MPC4J_NATIVE_TOOL_NTL_GF2X_H

/**
  * 将byte[]类型数据转换为GF2E类型数据。
  *
  * @param element 转换结果。
  * @param field
  * @param data byte[]类型数据。
  * @param size byte[]类型数据的字节长度。
*/
void GF2EFromBytes(NTL::GF2E &element, uint8_t* data, uint64_t size);

/**
 * 将GF2E类型数据转换为byte[]类型数据。
 *
 * @param data 转换结果。
 * @param element GF2E类型数据。
 * @param size byte[]类型数据的字节长度。
 */
void BytesFromGF2E(uint8_t* data, NTL::GF2E &element, uint64_t size);

/**
  * 给定byte[]格式的{x_i, y_i}，得到插值多项式。
  *
  * @param num 插值数量。如果实际插值数量不够，则自动补充虚拟元素。
  * @param setX 集合{x_i}。
  * @param setY 集合{y_i}。
  * @param coeffs 插值多项式系数。
  */
void gf2x_interpolate(uint64_t lBytes, uint64_t num, std::vector<uint8_t*> &setX, std::vector<uint8_t*> &setY, std::vector<uint8_t*> &coeffs);

/**
  * 给定byte[]格式的{x_i}和byte[]格式的y，得到插值多项式。
  *
  * @param num 插值数量。如果实际插值数量不够，则自动补充虚拟元素。
  * @param setX 集合{x_i}。
  * @param y y的取值。
  * @param coeffs 插值多项式系数。
  */
void gf2x_root_interpolate(uint64_t lBytes, uint64_t num, std::vector<uint8_t*> &setX, uint8_t* y, std::vector<uint8_t*> &coeffs);

/**
  * 给定byte[]格式的多项式系数和byte[]格式的x，求y = f(x)。
  *
  * @param coeffs byte[]格式的多项式系数。
  * @param x 输入x。
  * @param y 输出y。
  */
void gf2x_evaluate(uint64_t lBytes, std::vector<uint8_t*> &coeffs, uint8_t* x, uint8_t* y);

#endif //MPC4J_NATIVE_TOOL_NTL_GF2X_H
