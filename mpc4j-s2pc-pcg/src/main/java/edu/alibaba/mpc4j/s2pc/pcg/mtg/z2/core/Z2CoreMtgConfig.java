package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 核布尔三元组生成协议信息。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface Z2CoreMtgConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2CoreMtgFactory.Z2CoreMtgType getPtoType();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxNum();
}
