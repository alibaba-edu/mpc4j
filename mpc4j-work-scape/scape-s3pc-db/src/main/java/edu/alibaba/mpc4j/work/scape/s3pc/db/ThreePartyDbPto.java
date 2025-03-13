package edu.alibaba.mpc4j.work.scape.s3pc.db;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;

/**
 * Interface for three-party secure database querying
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public interface ThreePartyDbPto extends ThreePartyPto {

    /**
     * initialize the party
     */
    void init() throws MpcAbortException;

    /**
     * whether the current instance is malicious
     */
    boolean isMalicious();

    /**
     * get the abb3 party
     */
    Abb3Party getAbb3Party();
}
