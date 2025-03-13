package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

/**
 * the abstract traversal party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public abstract class AbstractTraversalParty extends AbstractThreePartyOpfPto implements TraversalParty {
    protected AbstractTraversalParty(PtoDesc ptoDesc, Abb3Party abb3Party, TraversalConfig config) {
        super(ptoDesc, abb3Party, config);
    }
}
