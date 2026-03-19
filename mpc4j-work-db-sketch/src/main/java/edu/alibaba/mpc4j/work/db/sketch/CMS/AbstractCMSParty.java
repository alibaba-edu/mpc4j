package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * abstract CMS computing party
 */
public abstract class AbstractCMSParty extends AbstractSketchPartyPto {
    /**
     * CMS table
     */
    protected AbstractCMSTable cmsTable;

    protected AbstractCMSParty(PtoDesc ptoDesc, Abb3Party abb3Party, CMSConfig config) {
        super(ptoDesc, abb3Party, config);
    }
}
