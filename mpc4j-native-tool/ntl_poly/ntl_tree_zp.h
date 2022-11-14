//
// Created by Weiran Liu on 2022/10/28.
//
#include <NTL/ZZ_p.h>
#include <NTL/ZZ_pX.h>
#include <NTL/ZZ.h>
#include <vector>

#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H
#define MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H

NTL::ZZ_pX* build_binary_tree(uint64_t primeByteLength, std::vector<uint8_t*> &setX);

void zp_free_binary_tree(NTL::ZZ_pX binary_tree[]);

NTL::ZZ_p* zp_derivative_inverses(NTL::ZZ_pX* binary_tree, long point_num);

void zp_free_derivative_inverse(NTL::ZZ_p derivative_inverses[]);

void zp_tree_evaluate(uint64_t primeByteLength, std::vector<uint8_t *> &coeffs, NTL::ZZ_pX *binary_tree, std::vector<uint8_t *> &setY);

void zp_tree_interpolate(uint64_t primeByteLength, NTL::ZZ_pX* binary_tree, NTL::ZZ_p* derivative_inverses,
                         std::vector<uint8_t*> &setY, std::vector<uint8_t*> &coeffs);

#endif //MPC4J_NATIVE_TOOL_NTL_TREE_ZP_H
