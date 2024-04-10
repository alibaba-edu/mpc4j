package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator;

import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;

/**
 * abstract replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public abstract class AbstractRpZ2Mtg extends AbstractAbbThreePartyPto implements RpZ2Mtg{
    /**
     * the correlated randomness provider
     */
    protected final S3pcCrProvider crProvider;
    /**
     * the environment party which provide the required z2 calculation
     */
    protected final RpZ2EnvParty envParty;
    /**
     * the number of bits
     */
    protected long totalBitNum;

    protected AbstractRpZ2Mtg(PtoDesc ptoDesc, Rpc rpc, RpZ2MtgConfig config, RpZ2EnvParty rpZ2EnvParty) {
        super(ptoDesc, rpc, config);
        envParty = rpZ2EnvParty;
        crProvider = envParty.getCrProvider();
        addSubPto(envParty);
    }

    @Override
    public void init(long totalBit) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        totalBitNum = totalBit;
        if(totalBit > 0){
            initTripleParam(totalBit);
        }
        envParty.init();
        initState();
    }

    protected abstract void initTripleParam(long totalBit);
}
