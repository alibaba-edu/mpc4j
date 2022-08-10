package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * Z2-核VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public interface Z2CoreVoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2CoreVoleFactory.Z2CoreVoleType getPtoType();
}
