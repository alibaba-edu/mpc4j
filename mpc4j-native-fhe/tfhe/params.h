#pragma once
#include <cmath>
#include <cstdint>
#include <vector>

struct targetP {
    static constexpr size_t l_ = 7; // lvl2 param
    static constexpr size_t digits = 124; // uint64_t
    static constexpr size_t Bgbit = 16;   // lvl2 param
    static constexpr uint64_t Bg = 1 << Bgbit;
};
