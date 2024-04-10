package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator;

import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;

/**
 * abstract replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public abstract class AbstractRpLongMtg extends AbstractAbbThreePartyPto implements RpLongMtg {
    /**
     * the correlated randomness provider
     */
    protected final S3pcCrProvider crProvider;
    /**
     * the environment party which provide the required zl64 calculation
     */
    protected final RpLongEnvParty envParty;
    /**
     * the number of tuples
     */
    protected long totalData;

    protected AbstractRpLongMtg(PtoDesc ptoDesc, Rpc rpc, RpLongMtgConfig config, RpLongEnvParty rpLongEnvParty) {
        super(ptoDesc, rpc, config);
        envParty = rpLongEnvParty;
        crProvider = envParty.getCrProvider();
        addSubPto(envParty);
    }

    @Override
    public void init(long totalData) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        this.totalData = totalData;
        if (totalData > 0) {
            initTripleParam(totalData);
        }
        envParty.init();
        initState();
    }

    protected abstract void initTripleParam(long totalData);
}
