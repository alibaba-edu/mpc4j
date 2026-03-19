package edu.alibaba.mpc4j.work.db.sketch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;

/**
 * sketch party interface
 */
public interface SketchPartyPto extends ThreePartyPto{
    /**
     * initialize the party
     */
    void init() throws MpcAbortException;

    /**
     * get the abb3 party
     */
    Abb3Party getAbb3Party();
}
