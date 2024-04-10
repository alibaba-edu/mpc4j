package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;

/**
 * Abstract class for three-party z2c
 *
 * @author Feng Han
 * @date 2023/12/25
 */
public abstract class AbstractTripletZ2cParty extends AbstractAbbThreePartyPto implements TripletZ2cParty {
    /**
     * context party for 3pc
     */
    protected final TripletProvider tripletProvider;
    /**
     * correlated randomness generator for 3pc
     */
    protected S3pcCrProvider crProvider;
    /**
     * z2 mtg for 3pc
     */
    protected RpZ2Mtp z2MtProvider;
    /**
     * 0,1,2; indicating the index of the current party
     */
    protected final int selfId;
    /**
     * flag of opening process
     */
    protected boolean duringVerificationFlag;

    protected AbstractTripletZ2cParty(PtoDesc ptoDesc, Rpc rpc, TripletZ2cConfig config, TripletProvider tripletProvider) {
        super(ptoDesc, rpc,
            rpc.getParty((rpc.ownParty().getPartyId() + 2) % 3),
            rpc.getParty((rpc.ownParty().getPartyId() + 1) % 3), config);
        this.tripletProvider = tripletProvider;
        crProvider = tripletProvider.getCrProvider();
        z2MtProvider = tripletProvider.getZ2MtProvider();
        tripletProvider.getVerificationMsg().addParty(this);
        selfId = rpc.ownParty().getPartyId();
        duringVerificationFlag = false;
    }

    @Override
    public TripletProvider getTripletProvider(){
        return tripletProvider;
    }

    @Override
    public void init(long updateBitNum) {
        throw new IllegalArgumentException("should not invoke this function");
    }

    @Override
    public boolean getDuringVerificationFlag(){
        return duringVerificationFlag;
    }

    @Override
    public void setDuringVerificationFlag(boolean duringVerificationFlag){
        this.duringVerificationFlag = duringVerificationFlag;
    }

    @Override
    public void checkUnverified() throws MpcAbortException {
        if (!duringVerificationFlag) {
            tripletProvider.getVerificationMsg().checkUnverified();
        }
    }
}
