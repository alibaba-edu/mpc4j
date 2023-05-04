package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * 2^l选1-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/21
 */
public interface LcotConfig extends MultiPartyPtoConfig {
    /**
     * 返回2^l选1-COT协议类型。
     *
     * @return 2^l选1-COT协议类型。
     */
    LcotFactory.LcotType getPtoType();
}
