package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public interface BnotConfig extends SecurePtoConfig {
    /**
     *
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BnotFactory.BnotType getPtoType();
}
