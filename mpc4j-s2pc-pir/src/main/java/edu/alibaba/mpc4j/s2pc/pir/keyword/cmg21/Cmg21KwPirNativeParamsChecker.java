package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

/**
 * CMG21关键词PIR协议本地参数检查器。
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
public class Cmg21KwPirNativeParamsChecker {

    private Cmg21KwPirNativeParamsChecker() {
        // empty
    }

    /**
     * 检查SEAL参数是否有效。
     *
     * @param modulusDegree    多项式阶。
     * @param plainModulus     明文模数。
     * @param coeffModulusBits 系数模数的比特值。
     * @param parentPowers     父幂次方。
     * @param sourcePowers     源幂次方。
     * @param psLowDegree      Paterson-Stockmeyer方法的低阶值。
     * @param maxBinSize       每个哈希桶内分块的最大元素个数。
     * @return SEAL参数是否有效。
     */
    static native boolean checkSealParams(int modulusDegree, long plainModulus, int[] coeffModulusBits,
                                          int[][] parentPowers, int[] sourcePowers, int psLowDegree, int maxBinSize);
}
