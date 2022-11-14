//
// Created by Weiran Liu on 2022/10/28.
//

#include "ntl_tree_zp.h"
#include "ntl_zp_util.h"

#define LEFT(X) (2 * X + 1)
#define RIGHT(X) (2 * X + 2)

/**
 * 内部构建二叉树。
 *
 * @param prime Zp域。
 * @param binary_tree 二叉树存储地址。
 * @param points 插值点。
 * @param point_num 插值点数量。
 * @param leaf_node_num 叶子节点数量。
 * @param index 当前构造的二叉树节点索引值。
 */
void
inner_build_binary_tree(NTL::ZZ_pX *binary_tree, NTL::ZZ_p *points, long point_num, long leaf_node_num, long index) {
    NTL::ZZ_p negated;
    if (index >= leaf_node_num - 1) {
        // 如果为叶子节点，则在对应的位置上构造多项式
        if (index + 1 - leaf_node_num < point_num) {
            // 如果有点的位置，则构造x - x_i
            NTL::negate(negated, points[index + 1 - leaf_node_num]);
            SetCoeff(binary_tree[index], 0, negated);
            SetCoeff(binary_tree[index], 1, 1);
        } else {
            // 如果没有点的位置，此多项式设置为1
            SetCoeff(binary_tree[index], 0, 1);
        }
        return;
    }
    // 迭代构造左右孩子节点
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, LEFT(index));
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, RIGHT(index));
    binary_tree[index] = binary_tree[LEFT(index)] * binary_tree[RIGHT(index)];
}

long get_leaf_node_num(long point_num) {
    // 二叉树的叶子节点数量必须是2的阶，找到离point_num最近的n = 2^i
    return point_num == 0 ? 1 : 1 << ceilLog2(point_num);
}

long get_binary_tree_size(long leaf_node_num) {
    // 二叉树的节点数量 = 2 * leaf_node_num - 1
    return 2 * leaf_node_num - 1;
}

void zp_tree_main_inner_evaluate(NTL::ZZ_pX &polynomial_a, NTL::ZZ_pX *binary_tree, long binary_tree_size,
                                 long leaf_node_num, long index, NTL::ZZ_p *values, long point_num) {
    NTL::ZZ_pX polynomial_b = binary_tree[index];
    // 如果polynomialA的阶特别小，则继续循环，这是测试时发现的bug，有可能计算完商后就是非常小
    // 此外要注意，当插值多项式的y只有一个元素时，polynomialA的阶会一直特别小，陷入死循环。因此要验证2 * index + 2的长度
    if (NTL::deg(polynomial_b) > NTL::deg(polynomial_a) && RIGHT(index) <= binary_tree_size) {
        zp_tree_main_inner_evaluate(polynomial_a, binary_tree, binary_tree_size, leaf_node_num, LEFT(index), values, point_num);
        zp_tree_main_inner_evaluate(polynomial_a, binary_tree, binary_tree_size, leaf_node_num, RIGHT(index), values, point_num);
    } else {
        long n = NTL::deg(polynomial_a);
        long m = NTL::deg(polynomial_b);
        // 当A的阶是n，B的阶是m(m <= n)时，Q的阶是(n - m)，R的阶是(m - 1)，创建多项式Q，依次设置Q的每一个系数
        NTL::ZZ_pX polynomial_q(0);
        NTL::ZZ_pX polynomial_r(polynomial_a);
        for (long i = 0; i <= n - m; i++) {
            NTL::ZZ_pX polynomial_quotient(0);
            NTL::ZZ_p quotient = NTL::coeff(polynomial_r, n - i) / NTL::coeff(polynomial_b, m);
            SetCoeff(polynomial_quotient, n - m - i, quotient);
            SetCoeff(polynomial_q, n - m - i, quotient);
            polynomial_r = polynomial_r - (polynomial_b * polynomial_quotient);
        }
        if (index >= leaf_node_num - 1 && leaf_node_num <= 2 * leaf_node_num - 1) {
            // 如果为叶子节点，计算得到二叉树节点索引值所对应的插值点索引值
            long j = index + 1 - leaf_node_num;
            if (j < point_num) {
                // 如果j所对应的叶子节点没有插值点，则不用进行任何操作，否则j所对应的值为R，这里R应该是一个常数
                values[j] = NTL::coeff(polynomial_r, 0);
            }
            return;
        }
        // 分别计算左右孩子节点
        zp_tree_main_inner_evaluate(polynomial_r, binary_tree, binary_tree_size, leaf_node_num, LEFT(index), values,
                                    point_num);
        zp_tree_main_inner_evaluate(polynomial_r, binary_tree, binary_tree_size, leaf_node_num, RIGHT(index), values,
                                    point_num);
    }
}

void zp_tree_inner_interpolate(NTL::ZZ_pX *temp_polynomials, NTL::ZZ_pX *binary_tree, long binary_tree_size, long leaf_node_num,
                               NTL::ZZ_p *values, NTL::ZZ_p *derivativeInverses, long point_num, long i) {
    if (i >= leaf_node_num - 1) {
        // 如果为叶子节点，计算得到二叉树节点索引值所对应的插值点索引值
        long j = i + 1 - leaf_node_num;
        if (j >= point_num) {
            // 如果j所对应的叶子节点没有插值点，这意味着j所对应的叶子节点用来补足满二叉树，直接返回1即可
            temp_polynomials[i] = NTL::ZZ_pX(1);
        } else {
            // 否则，j所对应的叶子节点有插值点，返回y_j * a_j
            temp_polynomials[i] = NTL::ZZ_pX(0);
            SetCoeff(temp_polynomials[i], 0, values[j] * derivativeInverses[j]);
        }
    } else {
        zp_tree_inner_interpolate(temp_polynomials, binary_tree, binary_tree_size, leaf_node_num, values,
                                  derivativeInverses, point_num, LEFT(i));
        zp_tree_inner_interpolate(temp_polynomials, binary_tree, binary_tree_size, leaf_node_num, values,
                                  derivativeInverses, point_num, RIGHT(i));
        temp_polynomials[i] =
            temp_polynomials[LEFT(i)] * binary_tree[RIGHT(i)] + temp_polynomials[RIGHT(i)] * binary_tree[LEFT(i)];
    }
}

NTL::ZZ_pX* build_binary_tree(uint64_t primeByteLength, std::vector<uint8_t*> &setX) {
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;

    uint32_t point_num = setX.size();
    auto *points = new NTL::ZZ_p[point_num];
    // build x
    for (uint32_t i = 0; i < point_num; i++) {
        NTL::ZZFromBytes(e_ZZ, setX[i], static_cast<long>(primeByteLength));
        points[i] = NTL::to_ZZ_p(e_ZZ);
    }
    long leaf_node_num = get_leaf_node_num(point_num);
    long binary_tree_size = get_binary_tree_size(leaf_node_num);
    // 构造满二叉树
    auto *binary_tree = new NTL::ZZ_pX[binary_tree_size];
    for (long index = 0; index < binary_tree_size; index++) {
        binary_tree[index] = NTL::ZZ_pX::zero();
    }
    inner_build_binary_tree(binary_tree, points, point_num, leaf_node_num, 0);
    delete[] points;
    return binary_tree;
}

void zp_free_binary_tree(NTL::ZZ_pX binary_tree[]) {
    delete[] binary_tree;
}

NTL::ZZ_p* zp_derivative_inverses(NTL::ZZ_pX* binary_tree, long point_num) {
    long leaf_node_num = get_leaf_node_num(point_num);
    long binary_tree_size = get_binary_tree_size(leaf_node_num);
    // 构造导数多项式，注意导数多项式的阶等于points.length，而不是leafNodeNum，cc.rings有求导的快速实现算法
    NTL::ZZ_pX derivativePolynomial;
    diff(derivativePolynomial, binary_tree[0]);
    // 计算导数
    auto *derivative_inverses = new NTL::ZZ_p[point_num];
    zp_tree_main_inner_evaluate(derivativePolynomial, binary_tree, binary_tree_size, leaf_node_num, 0,
                                derivative_inverses, point_num);
    // 计算导数的逆
    for (uint32_t i = 0; i < point_num; i++) {
        inv(derivative_inverses[i], derivative_inverses[i]);
    }
    return derivative_inverses;
}

void zp_free_derivative_inverse(NTL::ZZ_p derivative_inverses[]) {
    delete[] derivative_inverses;
}

void zp_tree_evaluate(uint64_t primeByteLength, std::vector<uint8_t *> &coeffs, NTL::ZZ_pX *binary_tree, std::vector<uint8_t *> &setY) {
    // 临时变量
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;
    // build polynomial
    uint32_t coeff_num = coeffs.size();
    NTL::ZZ_pX polynomial;
    for (uint32_t i = 0; i < coeff_num; i++) {
        NTL::ZZFromBytes(e_ZZ, coeffs[i], static_cast<long>(primeByteLength));
        e_ZZ_p = NTL::to_ZZ_p(e_ZZ);
        NTL::SetCoeff(polynomial, static_cast<long>(i), e_ZZ_p);
    }
    uint32_t point_num = setY.size();
    long leaf_node_num = get_leaf_node_num(point_num);
    long binary_tree_size = get_binary_tree_size(leaf_node_num);
    auto *values = new NTL::ZZ_p[leaf_node_num];
    // init y
    for (uint32_t i = 0; i < point_num; i++) {
        values[i] = NTL::to_ZZ_p(0);
    }
    // 不足的补0
    for (uint32_t i = point_num; i < leaf_node_num; i++) {
        values[i] = NTL::to_ZZ_p(0);
    }
    zp_tree_main_inner_evaluate(polynomial, binary_tree, binary_tree_size, leaf_node_num, 0, values, point_num);
    // 返回结果
    setY.resize(point_num);
    for (uint32_t i = 0; i < point_num; i++) {
        e_ZZ = rep(values[i]);
        setY[i] = new uint8_t[primeByteLength];
        NTL::BytesFromZZ(setY[i], e_ZZ, (long) (primeByteLength));
    }
    // 清空内存
    delete[] values;
}

void zp_tree_interpolate(uint64_t primeByteLength, NTL::ZZ_pX* binary_tree, NTL::ZZ_p* derivative_inverses,
                         std::vector<uint8_t*> &setY, std::vector<uint8_t*> &coeffs) {
    NTL::ZZ e_ZZ;
    NTL::ZZ_p e_ZZ_p;

    uint32_t point_num = setY.size();
    auto *values = new NTL::ZZ_p[point_num];
    // init values
    for (uint32_t i = 0; i < point_num; i++) {
        NTL::ZZFromBytes(e_ZZ, setY[i], static_cast<long>(primeByteLength));
        values[i] = NTL::to_ZZ_p(e_ZZ);
    }
    NTL::ZZ_pX polynomial;
    long leaf_node_num = get_leaf_node_num(point_num);
    long binary_tree_size = get_binary_tree_size(leaf_node_num);
    // 创建临时变量
    auto *temp_polynomials = new NTL::ZZ_pX[binary_tree_size];
    zp_tree_inner_interpolate(temp_polynomials, binary_tree, binary_tree_size, leaf_node_num, values,
                              derivative_inverses, point_num, 0);
    // 取第0个多项式，就是取值结果
    polynomial = temp_polynomials[0];
    delete[] values;
    delete[] temp_polynomials;
    coeffs.resize(NTL::deg(polynomial) + 1);
    for (uint32_t i = 0; i < coeffs.size(); i++) {
        // get the coefficient polynomial
        e_ZZ = rep(NTL::coeff(polynomial, i));
        coeffs[i] = new uint8_t[primeByteLength];
        NTL::BytesFromZZ(coeffs[i], e_ZZ, (long) (primeByteLength));
    }
}