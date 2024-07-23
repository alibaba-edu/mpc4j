package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.AbstractZ2TripleGenParty;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * fake Z2 triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class FakeZ2TripleGenReceiver extends AbstractZ2TripleGenParty {
    /**
     * PRNG
     */
    private SecureRandom prng;
    /**
     * seed
     */
    private long seed;

    public FakeZ2TripleGenReceiver(Rpc receiverRpc, Party senderParty, FakeZ2TripleGenConfig config) {
        super(FakeZ2TripleGenPtoDesc.getInstance(), receiverRpc, senderParty, config);
        seed = 0L;
    }

    @Override
    public void init(int expectTotalNum) throws MpcAbortException {
        setInitInput(expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        resetSeed();
        Z2Triple senderTriple = Z2Triple.createRandom(num, prng);
        Z2Triple receiverTriple = Z2Triple.createRandom(senderTriple, prng);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return receiverTriple;
    }

    private void resetSeed() {
        try {
            // init prng and seed, we must setSeed(byte[]) instead of setSeed(long).
            prng = SecureRandom.getInstance("SHA1PRNG");
            prng.setSeed(LongUtils.longToByteArray(seed));
            seed++;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
