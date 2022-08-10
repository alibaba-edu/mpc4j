package edu.alibaba.mpc4j.s2pc.pso.oprp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpFactory.OprpType;

/**
 * OPRP协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface OprpConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    OprpType getPtoType();
}
