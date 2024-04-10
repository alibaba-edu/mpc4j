//
// Created by Weiran Liu on 2024/3/22.
//

#ifndef MPC4J_NATIVE_TOOL_WAKSMAN_NETWORK_H
#define MPC4J_NATIVE_TOOL_WAKSMAN_NETWORK_H

#include <cstdint>
#include <vector>

/**
 * Generates the Waksman network.
 *
 * @param dest permutation map.
 * @return the Waksman network.
 */
std::vector<std::vector<int8_t>> generate_waksman_network(std::vector<int32_t> &dest);

/**
 * free variables used to generate the Waksman network.
 */
void free_waksman_network();


#endif //MPC4J_NATIVE_TOOL_WAKSMAN_NETWORK_H
