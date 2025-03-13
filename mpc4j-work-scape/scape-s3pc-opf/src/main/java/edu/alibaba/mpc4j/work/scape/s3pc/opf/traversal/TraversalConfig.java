package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory.TraversalType;

/**
 * Interface for three-party traversal configure
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface TraversalConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    TraversalType getTraversalType();
}
