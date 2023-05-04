package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 核关联不经意传输（Core Correlated Oblivious Transfer，核COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public interface CoreCotConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    CoreCotFactory.CoreCotType getPtoType();
}
