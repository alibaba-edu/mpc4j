package edu.alibaba.mpc4j.s2pc.pso.pid;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory.PidType;

/**
 * PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public interface PidConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PidType getPtoType();
}
