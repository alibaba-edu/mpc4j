package edu.alibaba.mpc4j.s2pc.pso.osn;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * OSN协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public interface OsnConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    OsnFactory.OsnType getPtoType();
}
