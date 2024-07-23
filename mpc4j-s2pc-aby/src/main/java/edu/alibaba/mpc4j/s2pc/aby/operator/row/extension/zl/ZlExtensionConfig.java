package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Zl Value Extension Config.
 *
 * @author Liqiang Peng
 * @date 2024/5/29
 */
public interface ZlExtensionConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ZlExtensionFactory.ZlExtensionType getPtoType();

    /**
     * Whether the protocol is signed extension.
     *
     * @return whether the protocol is signed extension.
     */
    boolean isSigned();
}
