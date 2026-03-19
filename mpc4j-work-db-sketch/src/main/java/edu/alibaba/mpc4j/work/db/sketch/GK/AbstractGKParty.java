package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * abstract GK party
 */
public abstract class AbstractGKParty extends AbstractSketchPartyPto {
    /**
     * GK table
     */
    protected GKTable gkTable;

    protected AbstractGKParty(PtoDesc ptoDesc, Abb3Party abb3Party, GKConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    public void setGkTable(GKTable gkTable) {
        this.gkTable = gkTable;
    }

    public GKTable getGkTable() {
        return gkTable;
    }


}
