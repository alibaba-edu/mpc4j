package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;

/**
 * abb3 Party, replicated secret sharing
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public class Abb3RpParty extends AbstractAbbThreePartyPto implements Abb3Party{
    private final TripletProvider tripletProvider;
    private final TripletZ2cParty z2cParty;
    private final TripletLongParty longParty;
    private final Cgh18RpLongParty macParty;
    private final Aby3ConvParty convParty;
    private final Aby3ShuffleParty shuffleParty;
    /**
     * estimated number of and gate
     */
    protected long estimateBitTupleNum;
    /**
     * estimated number of mul gate
     */
    protected long estimateLongTupleNum;

    public Abb3RpParty(Rpc rpc, Abb3RpConfig config, TripletProvider tripletProvider) {
        super(Abb3RpPtoDesc.getInstance(), rpc, config);
        this.tripletProvider = tripletProvider;
        z2cParty = Aby3Z2cFactory.createParty(rpc, config.getZ2cConfig(), tripletProvider);
        longParty = TripletRpLongCpFactory.createParty(rpc, config.getZl64cConfig(), tripletProvider);
        if (config.getSecurityModel().equals(SecurityModel.MALICIOUS)){
            if(config.getMacConfig().equals(config.getZl64cConfig())){
                macParty = (Cgh18RpLongParty) longParty;
            }else{
                macParty = (Cgh18RpLongParty) TripletRpLongCpFactory.createParty(rpc, config.getMacConfig(), tripletProvider);
            }
        }else{
            macParty = null;
        }
        convParty = Aby3ConvFactory.createParty(config.getConvConfig(), z2cParty, longParty);
        shuffleParty = Aby3ShuffleFactory.createParty(config.getShuffleConfig(), z2cParty, longParty, macParty);
        addMultiSubPto(tripletProvider, z2cParty, longParty, convParty, shuffleParty);

        estimateBitTupleNum = 0;
        estimateLongTupleNum = 0;
    }

    public Abb3RpParty(Rpc rpc, Abb3RpConfig config) {
        super(Abb3RpPtoDesc.getInstance(), rpc, config);
        this.tripletProvider = new TripletProvider(rpc, config.getTripletProviderConfig());
        z2cParty = Aby3Z2cFactory.createParty(rpc, config.getZ2cConfig(), tripletProvider);
        longParty = TripletRpLongCpFactory.createParty(rpc, config.getZl64cConfig(), tripletProvider);
        if (config.getSecurityModel().equals(SecurityModel.MALICIOUS)){
            if(config.getMacConfig().equals(config.getZl64cConfig())){
                macParty = (Cgh18RpLongParty) longParty;
            }else{
                macParty = (Cgh18RpLongParty) TripletRpLongCpFactory.createParty(rpc, config.getMacConfig(), tripletProvider);
            }
        }else{
            macParty = null;
        }
        convParty = Aby3ConvFactory.createParty(config.getConvConfig(), z2cParty, longParty);
        shuffleParty = Aby3ShuffleFactory.createParty(config.getShuffleConfig(), z2cParty, longParty, macParty);
        addMultiSubPto(tripletProvider, z2cParty, longParty, convParty, shuffleParty);

        estimateBitTupleNum = 0;
        estimateLongTupleNum = 0;
    }

    @Override
    public void updateNum(long bitTupleNum, long longTupleNum){
        estimateBitTupleNum += bitTupleNum;
        estimateLongTupleNum += longTupleNum;
    }

    @Override
    public void init() {
        if(partyState.equals(PartyState.INITIALIZED)){
            return;
        }
        tripletProvider.init(estimateBitTupleNum, estimateLongTupleNum);
        z2cParty.init();
        longParty.init();
        convParty.init();
        shuffleParty.init();
        initState();
    }

    @Override
    public void checkUnverified() throws MpcAbortException {
        z2cParty.checkUnverified();
    }

    @Override
    public TripletZ2cParty getZ2cParty() {
        return z2cParty;
    }

    @Override
    public TripletLongParty getLongParty() {
        return longParty;
    }

    @Override
    public Aby3ConvParty getConvParty() {
        return convParty;
    }

    @Override
    public Aby3ShuffleParty getShuffleParty() {
        return shuffleParty;
    }

    @Override
    public TripletProvider getTripletProvider(){
        return tripletProvider;
    }

    public Cgh18RpLongParty getMacParty() {
        return macParty;
    }
}
