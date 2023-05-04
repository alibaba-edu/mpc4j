package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 基础n选1-OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public interface BaseNotConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BaseNotFactory.BaseNotType getPtoType();
}
