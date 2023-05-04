package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * 核l比特三元组生成协议信息。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    ZlCoreMtgFactory.ZlCoreMtgType getPtoType();

    /**
     * Gets the Zl instance.
     *
     * @return the Zl instance.
     */
    Zl getZl();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxAllowNum();
}
