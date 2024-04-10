package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer.RpZ2BufferMtpPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.AbstractRpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;

/**
 * replicated 3p sharing zl64 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongBufferMtp extends AbstractRpLongMtp implements RpLongMtp {
    /**
     * replicated Zl64 multiplication tuple generator
     */
    private final RpLongMtg rpLongMtg;

    public RpLongBufferMtp(Rpc rpc, RpLongBufferMtpConfig config, S3pcCrProvider crProvider) {
        super(RpZ2BufferMtpPtoDesc.getInstance(), rpc, config);
        RpLongEnvParty envParty = new RpLongEnvParty(rpc, config.getRpZl64EnvConfig(), crProvider);
        rpLongMtg = RpLongMtgFactory.createParty(rpc, config.getRpZl64MtgConfig(), envParty);
        addSubPto(rpLongMtg);
    }

    @Override
    public void init(long totalBit) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        rpLongMtg.init(totalBit);
        initState();
    }

    @Override
    public RpLongEnvParty getEnv() {
        return rpLongMtg.getEnv();
    }

    @Override
    protected void fillBuffer() throws MpcAbortException {
        buffer = Arrays.stream(rpLongMtg.genMtOnline()).map(TripletRpLongVector::mergeWithPadding).toArray(TripletRpLongVector[]::new);
        currentLongIndex = 0;
    }
}
