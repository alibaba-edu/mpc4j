package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 抗关联哈希函数工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public class CrhfFactory {
    /**
     * 私有构造函数
     */
    private CrhfFactory() {
        // empty
    }

    /**
     * 抗关联哈希函数类型
     */
    public enum CrhfType {
        /**
         * MMO(x)
         */
        MMO,
        /**
         * MMO_σ(x)
         */
        MMO_SIGMA,
    }

    /**
     * 根据类型，返回最适合的抗关联哈希函数。
     *
     * @param envType 环境类型。
     * @param type 抗关联哈希函数类型。
     * @return 抗关联哈希函数。
     */
    public static Crhf createInstance(EnvType envType, CrhfType type) {
        switch (type) {
            case MMO:
                return new MmoCrhf(envType);
            case MMO_SIGMA:
                return new MmoSigmaCrhf(envType);
            default:
                throw new IllegalArgumentException("Invalid CrhfType: " + type);
        }
    }
}
