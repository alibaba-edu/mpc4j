package edu.alibaba.mpc4j.common.tool;

/**
 * 执行环境要求。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public enum EnvType {
    /**
     * JDK下国产密码学算法
     */
    INLAND_JDK,
    /**
     * JDK下标准密码学算法
     */
    STANDARD_JDK,
    /**
     * 国产密码学算法
     */
    INLAND,
    /**
     * 标准密码学算法
     */
    STANDARD,
}
