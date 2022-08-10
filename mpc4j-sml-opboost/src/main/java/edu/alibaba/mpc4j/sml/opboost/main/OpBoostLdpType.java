package edu.alibaba.mpc4j.sml.opboost.main;

/**
 * OpBoost的LDP机制类型。
 *
 * @author Weiran Liu
 * @date 2022/7/10
 */
public enum OpBoostLdpType {
    /**
     * 明文
     */
    PLAIN,
    /**
     * 分段
     */
    PIECEWISE,
    /**
     * 全局映射
     */
    GLOBAL_MAP,
    /**
     * 全局指数映射
     */
    GLOBAL_EXP_MAP,
    /**
     * 本地映射
     */
    LOCAL_MAP,
    /**
     * 本地指数映射
     */
    LOCAL_EXP_MAP,
    /**
     * 调整映射
     */
    ADJ_MAP,
    /**
     * 调整指数映射
     */
    ADJ_EXP_MAP,
}
