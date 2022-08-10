package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 关联不经意传输（Correlated Oblivious Transfer，COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    CotFactory.CotType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxBaseNum();
}
