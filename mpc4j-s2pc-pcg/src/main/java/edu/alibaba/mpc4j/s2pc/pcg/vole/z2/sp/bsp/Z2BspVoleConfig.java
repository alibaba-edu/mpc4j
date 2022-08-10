package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * Z2-BSP-VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public interface Z2BspVoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2BspVoleFactory.Z2BspVoleType getPtoType();
}
