package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.Config;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 安全两方计算协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public interface MultiPartyPtoConfig extends Config {
    /**
     * 设置环境类型。
     *
     * @param envType 环境类型。
     */
    void setEnvType(EnvType envType);

    /**
     * 返回环境类型。
     *
     * @return 环境类型。
     */
    EnvType getEnvType();

    /**
     * 返回安全模型。
     *
     * @return 安全模型。
     */
    SecurityModel getSecurityModel();
}
