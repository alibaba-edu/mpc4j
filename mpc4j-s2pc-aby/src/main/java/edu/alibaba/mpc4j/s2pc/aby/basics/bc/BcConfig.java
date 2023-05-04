package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 布尔电路配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public interface BcConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BcFactory.BcType getPtoType();
}
