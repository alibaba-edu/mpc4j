package edu.alibaba.mpc4j.s3pc.abb3.context;

import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.msg.VerificationMsg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;

/**
 * provider for 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class TripletProvider extends AbstractAbbThreePartyPto {
    /**
     * correlated randomness generator for 3pc
     */
    private final S3pcCrProvider crProvider;
    /**
     * z2 mtg for 3pc
     */
    private final RpZ2Mtp z2MtProvider;
    /**
     * z2 mtg for 3pc
     */
    private final RpLongMtp zl64MtProvider;
    /**
     * message synchronize for verification
     */
    private final VerificationMsg verificationMsg;

    public TripletProvider(Rpc rpc, TripletProviderConfig config) {
        super(TripletProviderPtoDesc.getInstance(), rpc, config);
        crProvider = new S3pcCrProvider(rpc, config.getCrProviderConfig());
        verificationMsg = new VerificationMsg();
        addSubPto(crProvider);
        if(config.getSecurityModel().equals(SecurityModel.MALICIOUS)){
            z2MtProvider = RpMtProviderFactory.createRpZ2MtParty(rpc, config.getRpZ2MtpConfig(), crProvider);
            zl64MtProvider = RpMtProviderFactory.createRpZl64MtParty(rpc, config.getRpZl64MtpConfig(), crProvider);
            addMultiSubPto(z2MtProvider, zl64MtProvider);
        }else{
            z2MtProvider = null;
            zl64MtProvider = null;
        }
    }

    public void init(long totalBit, long totalLong){
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        crProvider.init();
        if(zl64MtProvider != null){
            z2MtProvider.init(totalBit);
            zl64MtProvider.init(totalLong);
        }
        initState();
    }

    public S3pcCrProvider getCrProvider() {
        return crProvider;
    }

    public VerificationMsg getVerificationMsg() {
        return verificationMsg;
    }

    public RpZ2Mtp getZ2MtProvider() {
        return z2MtProvider;
    }

    public RpLongMtp getZl64MtProvider() {
        return zl64MtProvider;
    }
}
