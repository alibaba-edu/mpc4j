package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 安全计算协议接口。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public interface SecurePto {
    /**
     * 设置是否并发计算。
     *
     * @param parallel 是否并发计算。
     */
    void setParallel(boolean parallel);

    /**
     * 返回是否并发计算。
     *
     * @return 是否并发计算。
     */
    boolean getParallel();

    /**
     * 返回环境类型。
     *
     * @return 环境类型。
     */
    EnvType getEnvType();

    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @SuppressWarnings("rawtypes")
    Enum getPtoType();
}
