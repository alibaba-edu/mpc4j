package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory.BaseOtType;

/**
 * 基础OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public interface BaseOtConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BaseOtType getPtoType();
}
