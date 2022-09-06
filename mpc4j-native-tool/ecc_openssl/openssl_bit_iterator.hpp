/*
 * Created by Weiran Liu on 2022/8/22.
 * BitIterator implementation, modified from:
 * <p>
 * https://github.com/herumi/mcl/blob/master/include/mcl/util.hpp
 * </p>
 */
#include "openssl_ecc.h"

#ifndef MPC4J_NATIVE_TOOL_OPENSSL_BIT_ITERATOR_HPP
#define MPC4J_NATIVE_TOOL_OPENSSL_BIT_ITERATOR_HPP

template<class T>
class BitIterator {
    /**
     * 数据
     */
    const T *data_;
    /**
     * 当前位置，要使用有符号变量
     */
    int32_t bitPosition_;
    /**
     * 总比特长度
     */
    size_t bitSize_;
    /**
     * 填充长度，要使用有符号变量
     */
    int32_t PadLength_;
    static const size_t TbitSize = sizeof(T) * 8;
public:
    explicit BitIterator(const T *data = 0, size_t bit_len = 0) {
        data_ = data;
        bitSize_ = bit_len;
        int32_t totalSize = ((bit_len + TbitSize - 1) / TbitSize) * TbitSize;
        // the last bit position
        bitPosition_ = totalSize - 1;
        PadLength_ = totalSize - bitSize_;
    }

    [[nodiscard]] bool hasNext() const {
        return bitPosition_ >= PadLength_;
    }

    T getNext(size_t w) {
        assert(0 < w && w <= TbitSize);
        if (!hasNext()) {
            return 0;
        }
        const size_t q = bitPosition_ / TbitSize;
        const size_t r = bitPosition_ % TbitSize;
        const size_t remain = bitPosition_ - PadLength_ + 1;
        if (w > remain) w = remain;
        T v = data_[q] >> (TbitSize - 1 - r);
        if (r < (w - 1)) {
            v |= data_[q - 1] << (r + 1);
        }
        bitPosition_ -= w;
        return v & mask(w);
    }

    T mask(size_t w) const {
        assert(w <= TbitSize);
        return (w == TbitSize ? T(0) : (T(1) << w)) - 1;
    }
};

#endif //MPC4J_NATIVE_TOOL_OPENSSL_BIT_ITERATOR_HPP
