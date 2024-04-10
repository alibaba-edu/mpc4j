package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.AbstractRpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2Mtg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;

/**
 * replicated 3p sharing z2 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpZ2BufferMtp extends AbstractRpZ2Mtp implements RpZ2Mtp {
    /**
     * replicated Z2 multiplication tuple generator
     */
    private final RpZ2Mtg rpZ2Mtg;

    public RpZ2BufferMtp(Rpc rpc, RpZ2BufferMtpConfig config, S3pcCrProvider crProvider) {
        super(RpZ2BufferMtpPtoDesc.getInstance(), rpc, config);
        RpZ2EnvParty envParty = new RpZ2EnvParty(rpc, config.getRpZ2EnvConfig(), crProvider);
        rpZ2Mtg = RpZ2MtgFactory.createParty(rpc, config.getRpZ2MtgConfig(), envParty);
        addSubPto(rpZ2Mtg);
    }

    @Override
    public void init(long totalBit) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        rpZ2Mtg.init(totalBit);
        initState();
    }

    @Override
    public RpZ2EnvParty getEnv() {
        return rpZ2Mtg.getEnv();
    }

    @Override
    protected void fillBuffer() throws MpcAbortException {
        buffer = Arrays.stream(rpZ2Mtg.genMtOnline()).map(TripletRpZ2Vector::mergeWithPadding).toArray(TripletRpZ2Vector[]::new);
        currentByteIndex = 0;
    }
}
