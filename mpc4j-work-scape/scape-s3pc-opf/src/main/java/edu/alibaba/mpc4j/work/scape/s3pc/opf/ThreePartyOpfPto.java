package edu.alibaba.mpc4j.work.scape.s3pc.opf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;

/**
 * Interface for three-party oblivious function evaluation
 *
 * @author Feng Han
 * @date 2024/03/01
 */
public interface ThreePartyOpfPto extends ThreePartyPto {
    /**
     * initialize the party
     */
    void init() throws MpcAbortException;

    /**
     * get the abb3 party
     */
    Abb3Party getAbb3Party();
}
