package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;

/**
 * 核zp64三元组生成协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public interface Zp64CoreMtgConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Zp64CoreMtgFactory.Zp64CoreMtgType getPtoType();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxAllowNum();

    /**
     * 返回模数。
     *
     * @return 模数。
     */
    Zp64 getZp64();
}
