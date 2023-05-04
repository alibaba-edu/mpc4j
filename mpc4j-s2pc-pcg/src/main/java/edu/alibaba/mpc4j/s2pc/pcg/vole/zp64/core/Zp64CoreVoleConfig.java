package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * ZP64-核VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public interface Zp64CoreVoleConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Zp64CoreVoleFactory.Zp64CoreVoleType getPtoType();
}
