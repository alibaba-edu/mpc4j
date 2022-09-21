package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 核2^l选1-OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/21
 */
public interface CoreLotConfig extends SecurePtoConfig {
    /**
     * 返回核2^l选1-OT协议类型。
     *
     * @return 核2^l选1-OT协议类型。
     */
    CoreLotFactory.CoreLotType getPtoType();
}
