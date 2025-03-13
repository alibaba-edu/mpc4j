package edu.alibaba.mpc4j.work.scape.s3pc.db;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;

/**
 * Abstract three-party secure database querying protocol.
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public abstract class AbstractThreePartyDbPto extends AbstractAbbThreePartyPto implements ThreePartyDbPto {
    /**
     * whether the sorting process is malicious secure
     */
    protected final boolean isMalicious;
    /**
     * initialize the party
     */
    protected final Abb3Party abb3Party;
    /**
     * z2c party
     */
    protected final TripletZ2cParty z2cParty;
    /**
     * zLong party
     */
    protected final TripletLongParty zl64cParty;

    protected AbstractThreePartyDbPto(PtoDesc ptoDesc, Abb3Party abb3Party, MultiPartyPtoConfig config) {
        super(ptoDesc, abb3Party.getRpc(), config);
        this.abb3Party = abb3Party;
        z2cParty = abb3Party.getZ2cParty();
        zl64cParty = abb3Party.getLongParty();
        isMalicious = !config.getSecurityModel().equals(SecurityModel.SEMI_HONEST);
    }

    @Override
    public boolean isMalicious(){
        return isMalicious;
    }

    @Override
    public Abb3Party getAbb3Party() {
        return abb3Party;
    }

    @Override
    public void setParallel(boolean parallel) {
        abb3Party.setParallel(parallel);
        super.setParallel(parallel);
    }

    @Override
    public void setTaskId(int taskId) {
        abb3Party.setTaskId(taskId);
        super.setTaskId(taskId);
    }
}
