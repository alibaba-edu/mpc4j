//
// Created by Weiran Liu on 2024/3/22.
//

#include "waksman_network.h"
#include <cmath>
#include "../common/stdc++.h"

/**
 * [N] -> [T]
 */
std::vector<int32_t> waksman_perm;
/**
 * [N] <- [T]
 */
std::vector<int32_t> waksman_inv_perm;
/**
 * Waksman network
 */
std::vector<std::vector<int8_t>> waksman_network;
/**
 * path
 */
std::vector<int8_t> waksman_path;

int32_t waksman_right_cycle_shift(int32_t num, int32_t logN) {
    return ((num & 1) << (logN - 1)) | (num >> 1);
}

/**
 * depth-first search.
 *
 * @param idx switching benes_network index.
 * @param route routh for the given index.
 */
void waksman_depth_first_search(int32_t idx) {
    std::stack<std::pair<int32_t, int8_t>> stack;
    stack.push({idx, 0});
    std::pair<int32_t, int8_t> idxRoutePair;
    while (!stack.empty()) {
        idxRoutePair = stack.top();
        stack.pop();
        waksman_path[idxRoutePair.first] = idxRoutePair.second;
        // if the next item in the vertical array is unassigned
        if (waksman_path[idxRoutePair.first ^ 1] < 0) {
            // the next item is always assigned the opposite of this item,
            // unless it was part of path/cycle of previous node
            stack.push({idxRoutePair.first ^ 1, idxRoutePair.second ^ (int8_t) 1});
        }
        idx = waksman_perm[waksman_inv_perm[idxRoutePair.first] ^ 1];
        if (waksman_path[idx] < 0) {
            stack.push({idx, idxRoutePair.second ^ (int8_t) 1});
        }
    }
}

void waksman_even_depth_first_search() {
    assert (waksman_path.size() > 4 && waksman_path.size() % 2 == 0);
    // set the last path to be 0
    int32_t idx = waksman_perm[waksman_path.size() - 1];
    std::stack<std::pair<int32_t, int8_t>> stack;
    stack.push({idx, 1});
    std::pair<int32_t, int8_t> idxRoutePair;
    while (!stack.empty()) {
        idxRoutePair = stack.top();
        stack.pop();
        waksman_path[idxRoutePair.first] = idxRoutePair.second;
        // if the next item in the vertical array is unassigned
        if (waksman_path[idxRoutePair.first ^ 1] < 0) {
            // the next item is always assigned the opposite of this item,
            // unless it was part of path/cycle of previous node
            stack.push({idxRoutePair.first ^ 1, idxRoutePair.second ^ (int8_t) 1});
        }
        idx = waksman_perm[waksman_inv_perm[idxRoutePair.first] ^ 1];
        if (waksman_path[idx] < 0) {
            stack.push({idx, idxRoutePair.second ^ (int8_t) 1});
        }
    }
}

void gen_quadruple_switches(int8_t* switches, const std::vector<int32_t> &src, const std::vector<int32_t> &dest) {
    assert (src.size() == 4);
    assert (dest.size() == 4);
    if (dest[0] == src[0]) {
        // [0, 1, 2, 3] -> [0, ?, ?, ?]
        if (dest[1] == src[1]) {
            // [0, 1, 2, 3] -> [0, 1, ?, ?]
            if (dest[2] == src[2]) {
                /*
                 * [0, 1, 2, 3] -> [0, 1, 2, 3], █ █ █ = 0 0 0
                 *                               █ █ □   0 0
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 0, switches[3] = 0, switches[4] = 0;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [0, 1, 3, 2], █ █ █ = 0 0 0
                 *                               █ █ □   1 0
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 0, switches[3] = 0, switches[4] = 0;
            }
        } else if (dest[1] == src[2]) {
            // [0, 1, 2, 3] -> [0, 2, ?, ?]
            if (dest[2] == src[1]) {
                /*
                 * [0, 1, 2, 3] -> [0, 2, 1, 3], █ █ █ = 1 1 1
                 *                               █ █ □   0 0
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 1, switches[3] = 0, switches[4] = 1;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [0, 2, 3, 1], █ █ █ = 0 0 0
                 *                               █ █ □   1 1
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 0, switches[3] = 1, switches[4] = 0;
            }
        } else {
            assert (dest[1] == src[3]);
            // [0, 1, 2, 3] -> [0, 3, ?, ?]
            if (dest[2] == src[1]) {
                /*
                 * [0, 1, 2, 3] -> [0, 3, 1, 2], █ █ █ = 1 1 1
                 *                               █ █ □   1 0
                 */
                switches[0] = 1, switches[1] = 1, switches[2] = 1, switches[3] = 0, switches[4] = 1;
            } else {
                assert (dest[2] == src[2]);
                /*
                 * [0, 1, 2, 3] -> [0, 3, 2, 1], █ █ █ = 0 0 0
                 *                               █ █ □   0 1
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 0, switches[3] = 1, switches[4] = 0;
            }
        }
    } else if (dest[0] == src[1]) {
        // [0, 1, 2, 3] -> [1, ?, ?, ?]
        if (dest[1] == src[0]) {
            // [0, 1, 2, 3] -> [1, 0, ?, ?]
            if (dest[2] == src[2]) {
                /*
                 * [0, 1, 2, 3] -> [1, 0, 2, 3], █ █ █ = 0 0 1
                 *                               █ █ □   0 0
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 0, switches[3] = 0, switches[4] = 1;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [1, 0, 3, 2], █ █ █ = 0 0 1
                 *                               █ █ □   1 0
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 0, switches[3] = 0, switches[4] = 1;
            }
        } else if (dest[1] == src[2]) {
            // [0, 1, 2, 3] -> [1, 2, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [1, 2, 0, 3], █ █ █ = 0 1 1
                 *                               █ █ □   0 0
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 1, switches[3] = 0, switches[4] = 1;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [1, 2, 3, 0], █ █ █ = 1 0 0
                 *                               █ █ □   1 1
                 */
                switches[0] = 1, switches[1] = 1, switches[2] = 0, switches[3] = 1, switches[4] = 0;
            }
        } else {
            assert (dest[1] == src[3]);
            // [0, 1, 2, 3] -> [1, 3, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [1, 3, 0, 2], █ █ █ = 0 1 1
                 *                               █ █ □   1 0
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 1, switches[3] = 0, switches[4] = 1;
            } else {
                assert (dest[2] == src[2]);
                /*
                 * [0, 1, 2, 3] -> [1, 3, 2, 0], █ █ █ = 1 0 0
                 *                               █ █ □   0 1
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 0, switches[3] = 1, switches[4] = 0;
            }
        }
    } else if (dest[0] == src[2]) {
        // [0, 1, 2, 3] -> [2, ?, ?, ?]
        if (dest[1] == src[0]) {
            // [0, 1, 2, 3] -> [2, 0, ?, ?]
            if (dest[2] == src[1]) {
                /*
                 * [0, 1, 2, 3] -> [2, 0, 1, 3], █ █ █ = 1 1 0
                 *                               █ █ □   0 0
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 1, switches[3] = 0, switches[4] = 0;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [2, 0, 3, 1], █ █ █ = 0 0 1
                 *                               █ █ □   1 1
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 0, switches[3] = 1, switches[4] = 1;
            }
        } else if (dest[1] == src[1]) {
            // [0, 1, 2, 3] -> [2, 1, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [2, 1, 0, 3], █ █ █ = 0 1 0
                 *                               █ █ □   0 0
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 1, switches[3] = 0, switches[4] = 0;
            } else {
                assert (dest[2] == src[3]);
                /*
                 * [0, 1, 2, 3] -> [2, 1, 3, 0], █ █ █ = 1 0 1
                 *                               █ █ □   1 1
                 */
                switches[0] = 1, switches[1] = 1, switches[2] = 0, switches[3] = 1, switches[4] = 1;
            }
        } else {
            assert (dest[1] == src[3]);
            // [0, 1, 2, 3] -> [2, 3, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [2, 3, 0, 1], █ █ █ = 0 1 0
                 *                               █ █ □   0 1
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 1, switches[3] = 1, switches[4] = 0;
            } else {
                assert (dest[2] == src[1]);
                /*
                 * [0, 1, 2, 3] -> [2, 3, 1, 0], █ █ █ = 1 1 0
                 *                               █ █ □   0 1
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 1, switches[3] = 1, switches[4] = 0;
            }
        }
    } else {
        assert (dest[0] == src[3]);
        // [0, 1, 2, 3] -> [3, ?, ?, ?]
        if (dest[1] == src[0]) {
            // [0, 1, 2, 3] -> [3, 0, ?, ?]
            if (dest[2] == src[1]) {
                /*
                 * [0, 1, 2, 3] -> [3, 0, 1, 2], █ █ █ = 1 1 0
                 *                               █ █ □   1 0
                 */
                switches[0] = 1, switches[1] = 1, switches[2] = 1, switches[3] = 0, switches[4] = 0;
            } else {
                assert (dest[2] == src[2]);
                /*
                 * [0, 1, 2, 3] -> [3, 0, 2, 1], █ █ █ = 0 0 1
                 *                               █ █ □   0 1
                 */
                switches[0] = 0, switches[1] = 0, switches[2] = 0, switches[3] = 1, switches[4] = 1;
            }
        } else if (dest[1] == src[1]) {
            // [0, 1, 2, 3] -> [3, 1, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [3, 1, 0, 2], █ █ █ = 0 1 0
                 *                               █ █ □   1 0
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 1, switches[3] = 0, switches[4] = 0;
            } else {
                assert (dest[2] == src[2]);
                /*
                 * [0, 1, 2, 3] -> [3, 1, 2, 0], █ █ █ = 1 0 1
                 *                               █ █ □   0 1
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 0, switches[3] = 1, switches[4] = 1;
            }
        } else {
            assert (dest[1] == src[2]);
            // [0, 1, 2, 3] -> [3, 2, ?, ?]
            if (dest[2] == src[0]) {
                /*
                 * [0, 1, 2, 3] -> [3, 2, 0, 1], █ █ █ = 0 1 0
                 *                               █ █ □   1 1
                 */
                switches[0] = 0, switches[1] = 1, switches[2] = 1, switches[3] = 1, switches[4] = 0;
            } else {
                assert (dest[2] == src[1]);
                /*
                 * [0, 1, 2, 3] -> [3, 2, 1, 0], █ █ █ = 1 1 1
                 *                               █ █ □   0 1
                 */
                switches[0] = 1, switches[1] = 0, switches[2] = 1, switches[3] = 1, switches[4] = 1;
            }
        }
    }
}

void gen_waksman_route(int32_t subLogN, int32_t lvl_p, int32_t perm_idx,
                     const std::vector<int32_t> &src, const std::vector<int32_t> &dest) {
    auto subN = (int32_t) src.size();
    if (subN == 2) {
        assert (subLogN == 1 || subLogN == 2);
        if (subLogN == 1) {
            // logN == 1, we have 2 * log(N) - 1 = 1 level (█)
            waksman_network[lvl_p][perm_idx] = (int8_t) (src[0] != dest[0]);
        } else {
            // logN == 2，we have 2 * logN - 1 = 3 levels (□ █ □).
            waksman_network[lvl_p][perm_idx] = 2;
            waksman_network[lvl_p + 1][perm_idx] = (int8_t) (src[0] != dest[0]);
            waksman_network[lvl_p + 2][perm_idx] = 2;
        }
    } else if (subN == 3) {
        assert (subLogN == 2);
        if (src[0] == dest[0]) {
            /*
             * 0 -> 0，1 -> 1，2 -> 2, the benes_network is:
             * █ □ █ = 0   0
             * □ █ □     0
             *
             * 0 -> 0，1 -> 2，2 -> 1, the benes_network is:
             * █ □ █ = 0   0
             * □ █ □     1
             */
            waksman_network[lvl_p][perm_idx] = (int8_t) 0;
            waksman_network[lvl_p + 2][perm_idx] = (int8_t) 0;
            if (src[1] == dest[1]) {
                waksman_network[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                waksman_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else if (src[0] == dest[1]) {
            /*
             * 0 -> 1，1 -> 0，2 -> 2, the benes_network is:
             * █ □ █ = 0   1
             * □ █ □     0
             *
             * 0 -> 1，1 -> 2，2 -> 0, the benes_network is:
             * █ □ █ = 0   1
             * □ █ □     1
             */
            waksman_network[lvl_p][perm_idx] = (int8_t) 0;
            waksman_network[lvl_p + 2][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                waksman_network[lvl_p + 1][perm_idx] = (int8_t) 0;
            } else {
                waksman_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            }
        } else {
            /*
             * 0 -> 2，1 -> 0，2 -> 1, the benes_network is:
             * █ □ █ = 1   0
             * □ █ □     1
             *
             * 0 -> 2，1 -> 1，2 -> 0, the benes_network is:
             * █ □ █ = 1   1
             * □ █ □     1
             */
            waksman_network[lvl_p][perm_idx] = (int8_t) 1;
            waksman_network[lvl_p + 1][perm_idx] = (int8_t) 1;
            if (src[1] == dest[0]) {
                waksman_network[lvl_p + 2][perm_idx] = (int8_t) 0;
            } else {
                waksman_network[lvl_p + 2][perm_idx] = (int8_t) 1;
            }
        }
        return;
    } else if (subN == 4) {
        assert (subLogN == 2 || subLogN == 3);
        auto* switches = new int8_t[5];
        gen_quadruple_switches(switches, src, dest);
        if (subLogN == 2) {
            waksman_network[lvl_p][perm_idx] = switches[0];
            waksman_network[lvl_p][perm_idx + 1] = switches[1];
            waksman_network[lvl_p + 1][perm_idx] = switches[2];
            waksman_network[lvl_p + 1][perm_idx + 1] = switches[3];
            waksman_network[lvl_p + 2][perm_idx] = switches[4];
            waksman_network[lvl_p + 2][perm_idx + 1] = 2;
        } else {
            waksman_network[lvl_p][perm_idx] = switches[0];
            waksman_network[lvl_p][perm_idx + 1] = switches[1];
            waksman_network[lvl_p + 1][perm_idx] = 2;
            waksman_network[lvl_p + 1][perm_idx + 1] = 2;
            waksman_network[lvl_p + 2][perm_idx] = switches[2];
            waksman_network[lvl_p + 2][perm_idx + 1] = switches[3];
            waksman_network[lvl_p + 3][perm_idx] = 2;
            waksman_network[lvl_p + 3][perm_idx + 1] = 2;
            waksman_network[lvl_p + 4][perm_idx] = switches[4];
            waksman_network[lvl_p + 4][perm_idx + 1] = 2;
        }
        delete[] switches;
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
            waksman_inv_perm[src[i]] = i;
        }
        for (i = 0; i < subN; ++i) {
            waksman_perm[i] = waksman_inv_perm[dest[i]];
        }
        for (i = 0; i < subN; ++i) {
            waksman_inv_perm[waksman_perm[i]] = i;
        }
        // shorten the array
        waksman_path.resize(subN);
        // path, initialized by -1, we use 2 for empty node
        std::fill(waksman_path.begin(), waksman_path.end(), (int8_t) -1);
        if (subN % 2 == 1) {
            // handling odd n, the last node directly links to the bottom subnetwork.
            waksman_path[subN - 1] = (int8_t) 1;
            waksman_path[waksman_perm[subN - 1]] = (int8_t) 1;
            // if values - 1 == benes_perm[values - 1], then the last one is also a direct link. Handle other cases.
            if (waksman_perm[subN - 1] != subN - 1) {
                int32_t idx = waksman_perm[waksman_inv_perm[subN - 1] ^ 1];
                waksman_depth_first_search(idx);
            }
        } else {
            // handling even n
            waksman_even_depth_first_search();
        }
        // set other switches
        for (i = 0; i < subN; ++i) {
            if (waksman_path[i] < 0) {
                waksman_depth_first_search(i);
            }
        }
        // create left part of the network.
        for (i = 0; i < subN - 1; i += 2) {
            waksman_network[lvl_p][perm_idx + i / 2] = waksman_path[i];
            for (j = 0; j < 2; ++j) {
                x = waksman_right_cycle_shift((i | j) ^ waksman_path[i], subLogN);
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
            s = waksman_network[lvl_p + subLevel - 1][perm_idx + i / 2] = waksman_path[waksman_perm[i]];
            for (j = 0; j < 2; ++j) {
                x = waksman_right_cycle_shift((i | j) ^ s, subLogN);
                if (x < subN / 2) {
                    topDest[i / 2] = src[waksman_perm[i | j]];
                } else {
                    bottomDest[i / 2] = src[waksman_perm[i | j]];
                }
            }
        }
        if (subN % 2 == 1) {
            // add one more switch for the odd case.
            bottomDest[subN / 2] = dest[subN - 1];
        } else {
            // remove one switch for the even case.
            waksman_network[lvl_p + subLevel - 1][perm_idx + subN / 2 - 1] = 2;
        }
        // create top subnetwork, with (log(N) - 1) levels
        gen_waksman_route(subLogN - 1, lvl_p + 1, perm_idx, topSrc, topDest);
        // create bottom subnetwork with (log(N) - 1) levels.
        gen_waksman_route(subLogN - 1, lvl_p + 1, perm_idx + subN / 4, bottomSrc, bottomDest);
    }
}

std::vector<std::vector<int8_t>> generate_waksman_network(std::vector<int32_t> &dest) {
    auto n = (int32_t) dest.size();
    auto logN = int32_t(ceil(log2(n)));
    int32_t levels = 2 * logN - 1;
    waksman_perm.resize(n);
    waksman_inv_perm.resize(n);
    waksman_network.resize(levels);
    for (int32_t i = 0; i < levels; ++i) {
        waksman_network[i].resize(n / 2);
        std::fill(waksman_network[i].begin(), waksman_network[i].end(), -1);
    }
    // the input is [0, 1, ..., n)
    std::vector<int32_t> src(n);
    for (int32_t i = 0; i < n; ++i) {
        src[i] = i;
    }
    gen_waksman_route(logN, 0, 0, src, dest);

    return waksman_network;
}

void free_waksman_network() {
    waksman_perm.clear();
    waksman_perm.shrink_to_fit();
    waksman_inv_perm.clear();
    waksman_perm.shrink_to_fit();
    waksman_network.clear();
    waksman_network.shrink_to_fit();
    waksman_path.clear();
    waksman_path.shrink_to_fit();
}