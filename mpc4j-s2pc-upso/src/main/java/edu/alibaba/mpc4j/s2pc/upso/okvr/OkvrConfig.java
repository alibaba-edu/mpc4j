package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory.OkvrType;

/**
 * OKVR config.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface OkvrConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    OkvrType getPtoType();
}
