package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiFactory.UpsiType;

/**
 * UPSI config interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiConfig extends MultiPartyPtoConfig {
    /**
     * return protocol type.
     *
     * @return protocol type.
     */
    UpsiType getPtoType();
}
