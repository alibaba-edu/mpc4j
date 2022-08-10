//
// Created by Weiran Liu on 2021/9/25.
//

#ifndef NATIVE_COMMON_TOOLS_BENES_H
#define NATIVE_COMMON_TOOLS_BENES_H


#include <cstdint>
#include <vector>

/**
 * 生成贝奈斯网络。
 *
 * @param dest 置换表。
 * @return 贝奈斯网络网络。
 */
std::vector<std::vector<int8_t>> generateBenesNetwork(std::vector<int32_t> &dest);

/**
 * 重置临时变量。
 */
void reset();


#endif //NATIVE_COMMON_TOOLS_BENES_H
