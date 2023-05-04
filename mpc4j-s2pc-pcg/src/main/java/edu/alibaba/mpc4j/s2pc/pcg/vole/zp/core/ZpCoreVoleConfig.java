package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * ZP-核VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public interface ZpCoreVoleConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    ZpCoreVoleFactory.ZpCoreVoleType getPtoType();
}
