//
// Created by Weiran Liu on 2024/3/20.
//

#include <cmath>
#include "../common/stdc++.h"
#include "benes_newtork.h"

/**
 * [N] -> [T]
 */
std::vector<int32_t> benes_perm;
/**
 * [N] <- [T]
 */
std::vector<int32_t> benes_inv_perm;
/**
 * benes_network
 */
std::vector<std::vector<int8_t>> benes_network;
/**
 * path
 */
std::vector<int8_t> benes_path;

int32_t benes_right_cycle_shift(int32_t num, int32_t logN) {
    return ((num & 1) << (logN - 1)) | (num >> 1);
}

/**
 * depth-first search.
 *
 * @param idx switching benes_network index.
 * @param route routh for the given index.
 */
void benes_depth_first_search(int32_t idx, int8_t route) {
    std::stack<std::pair<int32_t, int8_t>> pathStack;
    pathStack.push({idx, route});
    std::pair<int32_t, int8_t> idxRoutePair;
    while (!pathStack.empty()) {
        idxRoutePair = pathStack.top();
        pathStack.pop();
        benes_path[idxRoutePair.first] = idxRoutePair.second;
        // if the next item in the vertical array is unassigned
        if (benes_path[idxRoutePair.first ^ 1] < 0) {
            // the next item is always assigned the opposite of this item,
            // unless it was part of path/cycle of previous node
            pathStack.push({idxRoutePair.first ^ 1, idxRoutePair.second ^ (int8_t) 1});
        }
        idx = benes_perm[benes_inv_perm[idxRoutePair.first] ^ 1];
        if (benes_path[idx] < 0) {
            pathStack.push({idx, idxRoutePair.second ^ (int8_t) 1});
        }
    }
}

void gen_benes_route(int32_t subLogN, int32_t lvl_p, int32_t perm_idx,
                     const std::vector<int32_t> &src, const std::vector<int32_t> &dest) {
    auto subN = (int32_t) src.size();
    if (subN == 2) {
        if (subLogN == 1) {
            // logN == 1, we have 2 * log(N) - 1 = 1 level (█)
            benes_network[lvl_p][perm_idx] = (int8_t) (src[0] != dest[0]);
        } else {
            // logN == 2，we have 2 * logN - 1 = 3 levels (□ █ □).
            benes_network[lvl_p][perm_idx] = 2;
            benes_network[lvl_p + 1][perm_idx] = (int8_t) (src[0] != dest[0]);
            benes_network[lvl_p + 2][perm_idx] = 2;
        }
    } else if (subN == 3) {
        if (src[0] == dest[0]) {
            /*
             * 0 -> 0，1 -> 1，2 -> 2, the network is:
             * █ □ █ = 0   0
             * □ █ □     0
             *
             * 0 -> 0，1 -> 2，2 -> 1, the network is:
             * █ □ █ = 0   0
             * □ █ □     1
             */
            benes_network[lvl_p][perm_idx] = (int8_t) 0;
            benes_network[lvl_p + 2][perm_idx] = (int8_t) 0;
            if (src[1] == dest[1]) {
                benes_network[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                benes_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else if (src[0] == dest[1]) {
            /*
             * 0 -> 1，1 -> 0，2 -> 2, the network is:
             * █ □ █ = 0   1
             * □ █ □     0
             *
             * 0 -> 1，1 -> 2，2 -> 0, the network is:
             * █ □ █ = 0   1
             * □ █ □     1
             */
            benes_network[lvl_p][perm_idx] = (int8_t) 0;
            benes_network[lvl_p + 2][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                benes_network[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                benes_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else {
            /*
             * 0 -> 2，1 -> 0，2 -> 1, the network is:
             * █ □ █ = 1   0
             * □ █ □     1
             *
             * 0 -> 2，1 -> 1，2 -> 0, the network is:
             * █ □ █ = 1   1
             * □ █ □     1
             */
            benes_network[lvl_p][perm_idx] = (int8_t) 1;
            benes_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                benes_network[lvl_p + 2][perm_idx] = (int8_t) 0;
            } else {
                benes_network[lvl_p + 2][perm_idx] = (int8_t) 1;
            }
        }
        return;
    } else {
        int32_t i, j, x;
        uint8_t s;
        int32_t subLevel = 2 * subLogN - 1;
        // top subnetwork map, with size Math.floor(n / 2)
        std::vector<int32_t> topSrc(0);
        std::vector<int32_t> topDest(subN / 2);
        // bottom subnetwork map, with size Math.ceil(n / 2)
        std::vector<int32_t> bottomSrc(0);
        std::vector<int32_t> bottomDest(int(ceil(subN * 0.5)));
        // create forward/backward lookup tables
        // subSrcList stores the position map. For example, src = [2, 4, 6], dest = [6, 4, 2].
        // We re-organize the map to the form [0, subN - 1) -> [0, subN - 1)
        for (i = 0; i < subN; ++i) {
            benes_inv_perm[src[i]] = i;
        }
        for (i = 0; i < subN; ++i) {
            benes_perm[i] = benes_inv_perm[dest[i]];
        }
        for (i = 0; i < subN; ++i) {
            benes_inv_perm[benes_perm[i]] = i;
        }
        // shorten the array
        benes_path.resize(subN);
        // path, initialized by -1, we use 2 for empty node
        std::fill(benes_path.begin(), benes_path.end(), (int8_t) -1);
        // handling odd n
        if (subN % 2 == 1) {
            // the last node directly links to the bottom subnetwork.
            benes_path[subN - 1] = (int8_t) 1;
            benes_path[benes_perm[subN - 1]] = (int8_t) 1;
            // if values - 1 == benes_perm[values - 1], then the last one is also a direct link. Handle other cases.
            if (benes_perm[subN - 1] != subN - 1) {
                int32_t idx = benes_perm[benes_inv_perm[subN - 1] ^ 1];
                benes_depth_first_search(idx, (int8_t) 0);
            }
        }
        // set other switches
        for (i = 0; i < subN; ++i) {
            if (benes_path[i] < 0) {
                benes_depth_first_search(i, (int8_t) 0);
            }
        }
        // create left part of the network.
        for (i = 0; i < subN - 1; i += 2) {
            benes_network[lvl_p][perm_idx + i / 2] = benes_path[i];
            for (j = 0; j < 2; ++j) {
                x = benes_right_cycle_shift((i | j) ^ benes_path[i], subLogN);
                if (x < subN / 2) {
                    topSrc.push_back(src[i | j]);
                } else {
                    bottomSrc.push_back(src[i | j]);
                }
            }
        }
        if (subN % 2 == 1) {
            // add one more switch for the odd case.
            bottomSrc.push_back(src[subN - 1]);
        }
        // create right part of the subnetwork.
        for (i = 0; i < subN - 1; i += 2) {
            s = benes_network[lvl_p + subLevel - 1][perm_idx + i / 2] = benes_path[benes_perm[i]];
            for (j = 0; j < 2; ++j) {
                x = benes_right_cycle_shift((i | j) ^ s, subLogN);
                if (x < subN / 2) {
                    topDest[i / 2] = src[benes_perm[i | j]];
                } else {
                    bottomDest[i / 2] = src[benes_perm[i | j]];
                }
            }
        }
        if (subN % 2 == 1) {
            // add one more switch for the odd case.
            bottomDest[subN / 2] = dest[subN - 1];
        }
        // create top subnetwork, with (log(N) - 1) levels
        gen_benes_route(subLogN - 1, lvl_p + 1, perm_idx, topSrc, topDest);
        // create bottom subnetwork with (log(N) - 1) levels.
        gen_benes_route(subLogN - 1, lvl_p + 1, perm_idx + subN / 4, bottomSrc, bottomDest);
    }
}

std::vector<std::vector<int8_t>> generate_benes_network(std::vector<int32_t> &dest) {
    auto n = (int32_t) dest.size();
    auto logN = int32_t(ceil(log2(n)));
    int32_t levels = 2 * logN - 1;
    benes_perm.resize(n);
    benes_inv_perm.resize(n);
    benes_network.resize(levels);
    for (int32_t i = 0; i < levels; ++i) {
        benes_network[i].resize(n / 2);
        std::fill(benes_network[i].begin(), benes_network[i].end(), -1);
    }
    // the input is [0, 1, ..., n)
    std::vector<int32_t> src(n);
    for (int32_t i = 0; i < n; ++i) {
        src[i] = i;
    }
    gen_benes_route(logN, 0, 0, src, dest);

    return benes_network;
}

void free_benes_network() {
    benes_perm.clear();
    benes_perm.shrink_to_fit();
    benes_inv_perm.clear();
    benes_perm.shrink_to_fit();
    benes_network.clear();
    benes_network.shrink_to_fit();
    benes_path.clear();
    benes_path.shrink_to_fit();
}