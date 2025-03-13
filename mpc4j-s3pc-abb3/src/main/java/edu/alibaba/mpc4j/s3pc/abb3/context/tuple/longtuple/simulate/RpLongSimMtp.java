package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.simulate;

import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.AbstractRpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.simulate.RpLongSimMtpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer.RpZ2BufferMtpPtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * simulate long mt provider
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpLongSimMtp extends AbstractRpLongMtp implements RpLongMtp {
    /**
     * randomness generator for 3pc
     */
    private final S3pcCrProvider crProvider;
    /**
     * max buffer size
     */
    private final int maxBufferSize;
    /**
     * random generated triples
     */
    private TripletRpLongVector[] constantBuffer;

    public RpLongSimMtp(Rpc rpc, RpLongSimMtpConfig config, S3pcCrProvider crProvider) {
        super(RpZ2BufferMtpPtoDesc.getInstance(), rpc, config);
        this.crProvider = crProvider;
        this.maxBufferSize = config.getMaxBufferSize();
    }

    @Override
    public void init(long totalBit) {
        if (partyState.equals(PartyState.INITIALIZED)) {
            return;
        }
        crProvider.init();
        // generate zero shares and use them as the random generated mt.
        int[] sizes = new int[3];
        Arrays.fill(sizes, maxBufferSize);
        LongVector[] selfVec = crProvider.randZeroZl64Vector(sizes);
        sendLongVectors(PtoStep.SEND_ZERO_SHARE.ordinal(), leftParty(), selfVec);
        LongVector[] fromRight = receiveLongVectors(PtoStep.SEND_ZERO_SHARE.ordinal(), rightParty());
        constantBuffer = IntStream.range(0, 3)
            .mapToObj(i -> TripletRpLongVector.create(selfVec[i], fromRight[i]))
            .toArray(TripletRpLongVector[]::new);

        initState();
    }

    @Override
    protected void fillBuffer() {
        buffer = Arrays.stream(constantBuffer)
            .map(TripletRpLongVector::copy)
            .toArray(TripletRpLongVector[]::new);
        currentLongIndex = 0;
    }
}
