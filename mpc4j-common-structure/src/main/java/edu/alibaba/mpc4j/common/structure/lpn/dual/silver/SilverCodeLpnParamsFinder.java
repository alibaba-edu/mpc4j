package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParamsChecker;
import edu.alibaba.mpc4j.common.structure.lpn.dual.silver.SilverCodeCreatorUtils.SilverCodeType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * LdpcLpnParamsFinder 类，找到Ldpc相匹配的Lpn参数，满足128-bit安全要求，能够支持LdpcEncoder输出足够数量的OT且开销最小
 *
 * @author Hanwen Feng
 * @date 2022.3.21
 */
class SilverCodeLpnParamsFinder {

    /**
     * Ldpc的参数gapValue
     * n = 2k - gapValue
     */
    final int gapValue;

    /**
     * 构造函数
     *
     * @param silverCodeType 指定的Ldpc 类型，决定gapValue
     */
    SilverCodeLpnParamsFinder(SilverCodeType silverCodeType) {
        gapValue = SilverCodeCreatorUtils.getGap(silverCodeType);
    }

    /**
     * 设置最大可接受的t值
     */
    static final int MAX_T = 8192;

    /**
     * 根据需要输出的OT数量，计算安全的Lpn参数
     *
     * @param ceilLogN 目标输出OT数量的对数
     * @return 找到的Lpn参数
     */
    LpnParams computeLpnParams(int ceilLogN) {
        // 计算需要输出的OT数量。
        int outputOtNumber = 1 << ceilLogN;
        // 考虑 t = 0 时， k 的大小。增加t 会使k增大。将这个值设为k的初始值。
        int initK = outputOtNumber + gapValue + LongUtils.ceilLog2(outputOtNumber);
        // 将 2*initK设置为最大的k。
        int maxK = 2 * initK;

        for (int k = initK; k < maxK; k++) {
            int t = computeBestT(k, outputOtNumber);
            if (t != -1) {
                int n = 2 * k - gapValue;
                return LpnParams.uncheckCreate(n, k, t);
            }
        }
        throw new IllegalArgumentException("ceilLogN :" + ceilLogN + "cannot be supported");
    }

    /**
     * 计算给定k和需要输出的OT数量，对应的最小t值
     *
     * @param k              k
     * @param outputOtNumber 需要输出的OT数量
     * @return 返回 t。t=-1 表示不存在符合要求的t值
     */
    int computeBestT(int k, int outputOtNumber) {
        int n = 2 * k - gapValue;
        if (!LpnParamsChecker.validLpnParams(n, k, MAX_T)) {
            return -1;
        }

        int lowerT = 0;
        int upperT = MAX_T;
        int currentT = (upperT - lowerT) / 2;

        while (true) {
            while (!LpnParamsChecker.validLpnParams(n, k, currentT)) {
                lowerT = currentT;
                currentT += (upperT - currentT) / 2;
            }

            int smallerT = currentT - 1;
            if (!LpnParamsChecker.validLpnParams(n, k, smallerT)) {
                return (k - gapValue - getPreCotSize(n, currentT) >= outputOtNumber) ? currentT : -1;
            }

            upperT = currentT;
            currentT = lowerT + (currentT - lowerT) / 2;
        }
    }

    /**
     * 计算YWL20_UNI Mspcot 恶意安全协议消耗的COT数量
     * 该MSPCOT所需COT最多。据此计算预留COT数量可以满足其他协议需求
     * 为避免循环依赖，将计算Mspcot消耗的代码复制到此处
     *
     * @param n 参数n
     * @param t 参数t
     * @return 消耗的COT数量
     */
    static int getPreCotSize(int n, int t) {
        int binNum = IntCuckooHashBinFactory.getBinNum(IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE, t);
        int keyNum = IntCuckooHashBinFactory.getHashNum(IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE);
        int maxBinSize = MaxBinSizeUtils.expectMaxBinSize(keyNum * n, binNum);
        return LongUtils.ceilLog2(maxBinSize) * (binNum + 1) + CommonConstants.BLOCK_BIT_LENGTH;
    }
}
