package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Permuted Matrix Private Equality Test config.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public interface PmPeqtConfig extends MultiPartyPtoConfig {
    /**
     * Gets type.
     *
     * @return type.
     */
    PmPeqtFactory.PmPeqtType getPtoType();
}
