//
// Created by Weiran Liu on 2024/3/20.
//

#ifndef MPC4J_NATIVE_TOOL_BENES_NEWTORK_H
#define MPC4J_NATIVE_TOOL_BENES_NEWTORK_H

#include <cstdint>
#include <vector>

/**
 * Generates the Benes network.
 *
 * @param dest permutation map.
 * @return the Benes network.
 */
std::vector<std::vector<int8_t>> generate_benes_network(std::vector<int32_t> &dest);

/**
 * free variables used to generate the Benes network.
 */
void free_benes_network();

#endif //MPC4J_NATIVE_TOOL_BENES_NEWTORK_H
