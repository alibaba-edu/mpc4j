package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotFactory.NotType;

/**
 * NOT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public interface NotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    NotType getPtoType();
}
