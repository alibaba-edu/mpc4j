package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 布尔电路配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public interface BcConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BcFactory.BcType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxBaseNum();
}
