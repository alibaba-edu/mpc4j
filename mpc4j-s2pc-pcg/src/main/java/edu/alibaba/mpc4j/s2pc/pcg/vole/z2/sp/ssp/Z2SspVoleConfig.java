package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * Z2-SSP-VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public interface Z2SspVoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2SspVoleFactory.Z2SspVoleType getPtoType();
}
