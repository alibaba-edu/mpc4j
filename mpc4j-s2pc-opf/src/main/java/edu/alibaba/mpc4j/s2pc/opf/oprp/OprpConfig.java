package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * OPRP协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface OprpConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    OprpFactory.OprpType getPtoType();
}
