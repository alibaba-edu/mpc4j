package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public interface PsuConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PsuFactory.PsuType getPtoType();
}
