package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidFactory.PmidType;

/**
 * PMID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PmidType getPtoType();
}
