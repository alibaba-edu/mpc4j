package edu.alibaba.mpc4j.common.tool.crypto.tcrhf;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 可调抗关联哈希函数工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public class TcrhfFactory {
    /**
     * 私有构造函数
     */
    private TcrhfFactory() {
        // empty
    }

    /**
     * 可调抗关联哈希函数类型
     */
    public enum TcrhfType {
        /**
         * TMMO(x)
         */
        TMMO,
    }

    /**
     * 根据类型，返回最适合的抗关联哈希函数。
     *
     * @param envType 环境类型。
     * @param type 抗关联哈希函数类型。
     * @return 抗关联哈希函数。
     */
    public static Tcrhf createInstance(EnvType envType, TcrhfType type) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case TMMO:
                return new TmmoTcrhf(envType);
            default:
                throw new IllegalArgumentException("Invalid TcrhfType: " + type);
        }
    }
}
