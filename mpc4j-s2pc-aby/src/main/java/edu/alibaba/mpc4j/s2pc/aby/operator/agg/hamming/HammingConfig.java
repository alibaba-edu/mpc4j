package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 汉明距离协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public interface HammingConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    HammingFactory.HammingType getPtoType();
}
