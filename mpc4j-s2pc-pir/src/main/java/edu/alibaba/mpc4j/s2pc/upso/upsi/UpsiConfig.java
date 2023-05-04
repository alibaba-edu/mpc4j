package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiFactory.UpsiType;

/**
 * 非平衡PSI协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    UpsiType getPtoType();
}
