package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * GF2K-核VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Gf2kCoreVoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType();
}
