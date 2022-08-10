#include <cmath>
#include "benes_network.h"
#include "../common/stdc++.h"

using namespace std;

/**
 * [N] -> [T]的映射关系
 */
std::vector<int32_t> perm;
/**
 * [N] <- [T]的映射关系
 */
std::vector<int32_t> inv_perm;
/**
 * 交换门
 */
std::vector<std::vector<int8_t>> switched;
/**
 * 临时路径
 */
std::vector<int8_t> path;

/**
 * 以n比特为单位，对数字i右循环移位。
 * 例如：n = 8，      i = 00010011
 * 则有：rightCycleShift(i, n) = 10001001
 *
 * @param i 整数i。
 * @param n 单位长度。
 * @return 以n为单位长度，将i右循环移位。
 */
int32_t rightCycleShift(int32_t i, int32_t n) {
    return ((i & 1) << (n - 1)) | (i >> 1);
}

/**
 * 深度优先搜索。
 *
 * @param idx 交换网络索引值。
 * @param route 当前路径（1表示交换、0表示不交换、-1表示未定义）。
 */
void depthFirstSearch(int32_t idx, int8_t route) {
    stack<pair<int32_t, int8_t>> pathStack;
    pathStack.push({idx, route});
    pair<int32_t, int8_t> idxRoutePair;
    while (!pathStack.empty()) {
        idxRoutePair = pathStack.top();
        pathStack.pop();
        path[idxRoutePair.first] = idxRoutePair.second;
        // if the next item in the vertical array is unassigned
        if (path[idxRoutePair.first ^ 1] < 0) {
            // the next item is always assigned the opposite of this item,
            // unless it was part of path/cycle of previous node
            pathStack.push({idxRoutePair.first ^ 1, idxRoutePair.second ^ (int8_t) 1});
        }
        idx = perm[inv_perm[idxRoutePair.first] ^ 1];
        if (path[idx] < 0) {
            pathStack.push({idx, idxRoutePair.second ^ (int8_t) 1});
        }
    }
}

/**
 * 给定置换方法，构造贝奈斯网络（Benes Network）。
 *
 * @param subLogN 要生成的子网络层数。
 * @param lvl_p 当前处理的网络层数（用于迭代）。
 * @param perm_idx 当前处理的置换位置（用于迭代）。
 * @param src [N] 每个数字代表一个位置的编号。例如src[0] = 2，dest[1] = 2，意味着位置0要映射到位置1。
 * @param dest [T] 每个数字代表一个位置的编号。
 */
void gen_benes_route(int32_t subLogN, int32_t lvl_p, int32_t perm_idx, const vector<int32_t> &src, const vector<int32_t> &dest) {
    auto subN = (int32_t)src.size();
    // 当[N] = 2时，只有一层网络，但迭代过程汇总可能出现logN = 3但values = 2，此时把中间的作为单独的网络。
    if (subN == 2) {
        if (subLogN == 1) {
            // logN = 1，有2 * logN - 1 = 1层交换门(█)，是否交换取决于输入和输出的[2] -> [2]映射
            switched[lvl_p][perm_idx] = (int8_t) (src[0] != dest[0]);
        } else {
            // logN = 2，有2 * logN - 1 = 3层交换门(█ █ █），此时只设置中间的交换门，取决于输入和输出的[2] -> [2]映射
            switched[lvl_p + 1][perm_idx] = (int8_t) (src[0] != dest[0]);
        }
    } else if (subN == 3) {
        /*
         * N = 3时的网络结构为（█表示交换门、□表示直连门）：
         * █ □ █
         * □ █ □
         */
        if (src[0] == dest[0]) {
            /*
             * 0 -> 0，1 -> 1，2 -> 2，网络结构为：
             * █ □ █ = 0   0
             * □ █ □     0
             *
             * 0 -> 0，1 -> 2，2 -> 1，网络结构为：
             * █ □ █ = 0   0
             * □ █ □     1
             */
            switched[lvl_p][perm_idx] = (int8_t) 0;
            switched[lvl_p + 2][perm_idx] = (int8_t) 0;
            if (src[1] == dest[1]) {
                switched[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                switched[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else if (src[0] == dest[1]) {
            /*
             * 0 -> 1，1 -> 0，2 -> 2，网络结构为：
             * █ □ █ = 0   1
             * □ █ □     0
             *
             * 0 -> 1，1 -> 2，2 -> 0，网络结构为：
             * █ □ █ = 0   1
             * □ █ □     1
             */
            switched[lvl_p][perm_idx] = (int8_t) 0;
            switched[lvl_p + 2][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                switched[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                switched[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else {
            /*
             * 0 -> 2，1 -> 0，2 -> 1，网络结构为：
             * █ □ █ = 1   0
             * □ █ □     1
             *
             * 0 -> 2，1 -> 1，2 -> 0，网络结构为：
             * █ □ █ = 1   1
             * □ █ □     1
             */
            switched[lvl_p][perm_idx] = (int8_t) 1;
            switched[lvl_p + 1][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                switched[lvl_p + 2][perm_idx] = (int8_t) 0;
            } else {
                switched[lvl_p + 2][perm_idx] = (int8_t) 1;
            }
        }
        return;
    } else {
        int32_t i, j, x;
        uint8_t s;
        /*
         * 当输入值>=3时，先计算网络层数：subLevel = 2 * log(N) - 1
         */
        int32_t subLevel = 2 * subLogN - 1;
        // 上方子Benes Network的输入和输出映射表，大小为(n / 2)，注意topSrc不能初始化长度，否则添加的时候会继续往后添加
        vector<int32_t> topSrc(0);
        vector<int32_t> topDest(subN / 2);
        // 下方子Benes Network的输入和输出映射表，大小为ceil(n / 2)，注意bottomSrc不能初始化长度，否则添加的时候会继续往后添加
        vector<int32_t> bottomSrc(0);
        vector<int32_t> bottomDest(int(ceil(subN * 0.5)));
        // 举例：如果src = [2, 1, 0]，dest = [1, 2, 0]，意味着2所在位置0的元素要换到位置2上。
        // 这个函数没有要求src一定是按照顺序排列的，因此通过下述三个过程将排列整理为perm = [1, 0, 2]，inv_perm = [1, 0, 2]
        for (i = 0; i < subN; ++i) {
            inv_perm[src[i]] = i;
        }
        for (i = 0; i < subN; ++i) {
            perm[i] = inv_perm[dest[i]];
        }
        for (i = 0; i < subN; ++i) {
            inv_perm[perm[i]] = i;
        }
        /*
         * 把存储路径的缓存区取值均设置为-1，但path只需要values长
         */
        path.resize(subN);
        std::fill(path.begin(), path.end(), (int8_t) -1);
        // 只要最开始的[N]为奇数，则所有lower sub-network都会是奇数。如果是奇数，最后一个点需要特殊处理。
        if (subN % 2 == 1) {
            // 最后一个点和lower sub-network是直连，所以path[subN - 1] = 1，且perm的最后一个也为1
            path[subN - 1] = (int8_t) 1;
            path[perm[subN - 1]] = (int8_t) 1;
            // 如果values - 1 = perm[subN - 1]，意味着最后一个映射是直连的，不涉及到其他点
            if (perm[subN - 1] != subN - 1) {
                // 如果不相等，看输出最后一个值影响的是哪一个输出值
                int32_t idx = perm[inv_perm[subN - 1] ^ 1];
                // 6和0的深度搜索
                depthFirstSearch(idx, (int8_t) 0);
            }
        }
        for (i = 0; i < subN; ++i) {
            if (path[i] < 0) {
                depthFirstSearch(i, (int8_t) 0);
            }
        }
        // 生成子网络的输入
        for (i = 0; i < subN - 1; i += 2) {
            switched[lvl_p][perm_idx + i / 2] = path[i];
            for (j = 0; j < 2; ++j) {
                x = rightCycleShift((i | j) ^ path[i], subLogN);
                if (x < subN / 2) {
                    topSrc.push_back(src[i | j]);
                } else {
                    bottomSrc.push_back(src[i | j]);
                }
            }
        }
        // 如果是奇数，需要在下方增加一个直连节点
        if (subN % 2 == 1) {
            bottomSrc.push_back(src[subN - 1]);
        }
        // 生成子网络的输出
        for (i = 0; i < subN - 1; i += 2) {
            s = switched[lvl_p + subLevel - 1][perm_idx + i / 2] = path[perm[i]];
            for (j = 0; j < 2; ++j) {
                x = rightCycleShift((i | j) ^ s, subLogN);
                if (x < subN / 2) {
                    topDest[x] = src[perm[i | j]];
                } else {
                    bottomDest[i / 2] = src[perm[i | j]];
                }
            }
        }
        auto idx = int32_t(ceil(subN * 0.5));
        // 如果是奇数，要在下方增加一个直连节点
        if (subN % 2 == 1) {
            bottomDest[idx - 1] = dest[subN - 1];
        }
        // 构建上方的子Benes Network，包含logN - 1层，这logN - 1层位于上一层加1的位置。
        gen_benes_route(subLogN - 1, lvl_p + 1, perm_idx, topSrc, topDest);
        // 构建下方的子Benes Network，包含logN - 1层，这logN - 1层位于上一层加1的位置。
        gen_benes_route(subLogN - 1, lvl_p + 1, perm_idx + subN / 4, bottomSrc, bottomDest);
    }
}

std::vector<std::vector<int8_t>> generateBenesNetwork(std::vector<int32_t> &dest) {
    // 计算常量
    auto n = (int32_t)dest.size();
    auto logN = int32_t(ceil(log2(n)));
    int32_t levels = 2 * logN - 1;
    // 设置全局变量
    perm.resize(n);
    inv_perm.resize(n);
    switched.resize(levels);
    for (int32_t i = 0; i < levels; ++i) {
        switched[i].resize(n / 2);
    }
    // 输入固定为[0, 1, ..., n)
    std::vector<int32_t> src(n);
    for (int32_t i = 0; i < n; ++i) {
        src[i] = i;
    }
    gen_benes_route(logN, 0, 0, src, dest);

    return switched;
}

void reset() {
    perm.clear();
    perm.shrink_to_fit();
    inv_perm.clear();
    perm.shrink_to_fit();
    switched.clear();
    switched.shrink_to_fit();
    path.clear();
    path.shrink_to_fit();
}