package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * abstract MG party
 */
public abstract class AbstractSSParty extends AbstractSketchPartyPto {
    /**
     * CMS table
     */
    protected AbstractSSTable ssTable;

    protected AbstractSSParty(PtoDesc ptoDesc, Abb3Party abb3Party, SSConfig config) {
        super(ptoDesc, abb3Party, config);
    }

    public void setssTable(AbstractSSTable cmsTable) {
        this.ssTable = cmsTable;
    }

    public AbstractSSTable getSSTable() {
        return ssTable;
    }
}
