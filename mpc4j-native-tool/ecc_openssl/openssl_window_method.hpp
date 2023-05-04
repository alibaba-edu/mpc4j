/*
 * Created by Weiran Liu on 2022/8/21.
 * 应用于OpenSSL的WindowMethod方法，部分代码参考：
 * <p>
 * https://github.com/herumi/mcl/blob/master/include/mcl/window_method.hpp
 * </p>
 */


#ifndef MPC4J_NATIVE_TOOL_OPENSSL_WINDOW_METHOD_HPP
#define MPC4J_NATIVE_TOOL_OPENSSL_WINDOW_METHOD_HPP

#include "openssl_ecc.h"
#include "openssl_bit_iterator.hpp"
#include <vector>


class WindowMethod {
public:
    /**
     * curve index
     */
    int curveIndex_;
    /**
     * 总比特大小
     */
    size_t bitSize_;
    /**
     * 窗口大小
     */
    size_t winSize_;
    /**
     * 查找表
     */
    std::vector<EC_POINT *> lookupTable_;

    WindowMethod(int curveIndex, const EC_POINT *x, size_t bitSize, size_t winSize) {
        curveIndex_ = curveIndex;
        bitSize_ = bitSize;
        winSize_ = winSize;
        const size_t tableNum = (bitSize + winSize - 1) / winSize;
        const size_t r = size_t(1) << winSize;
        lookupTable_.resize(tableNum * r);
        EC_POINT *t = EC_POINT_new(openssl_ec_group[curveIndex_]);
        EC_POINT_copy(t, x);
        for (size_t i = 0; i < tableNum; i++) {
            size_t start_index = i * r;
            lookupTable_[start_index] = EC_POINT_new(openssl_ec_group[curveIndex_]);
            EC_POINT_set_to_infinity(openssl_ec_group[curveIndex_], lookupTable_[start_index]);
            for (size_t d = 1; d < r; d *= 2) {
                for (size_t j = 0; j < d; j++) {
                    lookupTable_[start_index + j + d] = EC_POINT_new(openssl_ec_group[curveIndex_]);
                    CRYPTO_CHECK(EC_POINT_add(openssl_ec_group[curveIndex_], lookupTable_[start_index + j + d],
                                              lookupTable_[start_index + j], t, nullptr));
                }
                CRYPTO_CHECK(EC_POINT_dbl(openssl_ec_group[curveIndex_], t, t, nullptr));
            }
        }
    }

    ~WindowMethod() {
        // 清理查找表
        for (auto & index : lookupTable_) {
            EC_POINT_free(index);
        }
        lookupTable_.clear();
    }

    void multiply(EC_POINT *z, const BIGNUM *y) const {
        EC_POINT_set_to_infinity(openssl_ec_group[curveIndex_], z);
        int bitLength_ = BN_num_bits(y);
        int roundLongByteLength = ((bitLength_ + (1 << 6) - 1) >> 6) << 3;
        unsigned char x[roundLongByteLength];
        BN_bn2binpad(y, x, roundLongByteLength);
        CRYPTO_CHECK(powArray(z, (uint64_t *) x, bitLength_));
    }

    bool powArray(EC_POINT *z, uint64_t *y, size_t bitLength) const {
        for (size_t i = 0; i < (bitLength + (1 << 6) - 1) >> 6; i++) {
            // Little-endian to Big-endian
            y[i] = __builtin_bswap64(y[i]);
        }
        if (bitLength == 0) {
            CRYPTO_CHECK(EC_POINT_set_to_infinity(openssl_ec_group[curveIndex_], z));
            return true;
        }
        assert(bitLength <= bitSize_);
        if (bitLength > bitSize_) {
            return false;
        }
        size_t i = 0;
        BitIterator<uint64_t> ai(y, bitLength);
        do {
            uint64_t v = ai.getNext(winSize_);
            if (v) {
                CRYPTO_CHECK(EC_POINT_add(openssl_ec_group[curveIndex_], z, z, lookupTable_[(i << winSize_) + v], nullptr));
            }
            i++;
        } while (ai.hasNext());
        return true;
    }
};

#endif //MPC4J_NATIVE_TOOL_OPENSSL_WINDOW_METHOD_HPP
