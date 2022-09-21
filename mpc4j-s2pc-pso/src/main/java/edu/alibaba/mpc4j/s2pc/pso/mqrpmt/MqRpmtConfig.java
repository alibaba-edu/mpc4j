package edu.alibaba.mpc4j.s2pc.pso.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public interface MqRpmtConfig extends SecurePtoConfig {
    /**
     * 返回mqRPMT协议类型。
     *
     * @return mqRPMT协议类型。
     */
    MqRpmtFactory.MqRpmtType getPtoType();
}
