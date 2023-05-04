package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;

/**
 * 布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public interface Z2MtgConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2MtgType getPtoType();
}
