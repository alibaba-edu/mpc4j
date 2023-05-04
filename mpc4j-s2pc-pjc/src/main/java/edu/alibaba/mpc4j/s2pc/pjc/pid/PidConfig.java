package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory.PidType;

/**
 * PID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public interface PidConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PidType getPtoType();
}
