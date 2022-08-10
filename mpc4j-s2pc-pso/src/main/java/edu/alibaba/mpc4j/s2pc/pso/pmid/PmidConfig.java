package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory.PmidType;

/**
 * PMID协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PmidType getPtoType();
}
