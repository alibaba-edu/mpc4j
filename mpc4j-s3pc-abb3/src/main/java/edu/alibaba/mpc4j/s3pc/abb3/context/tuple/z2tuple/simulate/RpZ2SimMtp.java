package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.simulate;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.AbstractRpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.simulate.RpZ2SimMtpPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * simulate z2 mt provider
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpZ2SimMtp extends AbstractRpZ2Mtp implements RpZ2Mtp {
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
    private TripletRpZ2Vector[] constantBuffer;

    public RpZ2SimMtp(Rpc rpc, RpZ2SimMtpConfig config, S3pcCrProvider crProvider) {
        super(RpZ2SimMtpPtoDesc.getInstance(), rpc, config);
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
        BitVector[] selfVec = crProvider.randZeroBitVector(sizes);
        sendBitVectors(PtoStep.SEND_ZERO_SHARE.ordinal(), leftParty(), selfVec);
        BitVector[] fromRight = receiveBitVectors(PtoStep.SEND_ZERO_SHARE.ordinal(), rightParty(), sizes);
        constantBuffer = IntStream.range(0, 3)
            .mapToObj(i -> TripletRpZ2Vector.create(selfVec[i], fromRight[i]))
           .toArray(TripletRpZ2Vector[]::new);

        initState();
    }

    @Override
    protected void fillBuffer() throws MpcAbortException {
        buffer = Arrays.stream(constantBuffer)
            .map(TripletRpZ2Vector::copy)
            .toArray(TripletRpZ2Vector[]::new);
        currentByteIndex = 0;
    }
}
