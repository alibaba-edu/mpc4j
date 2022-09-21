package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * DPPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface DpprfConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    DpprfFactory.DpprfType getPtoType();
}
