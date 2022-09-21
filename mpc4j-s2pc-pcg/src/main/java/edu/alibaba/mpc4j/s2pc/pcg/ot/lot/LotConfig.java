package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 2^l选1-OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public interface LotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    LotFactory.LotType getPtoType();
}
