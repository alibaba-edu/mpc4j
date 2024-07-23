package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.AbstractZl64TripleGenParty;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * fake Zl64 triple generation sender.
 *
 * @author Weiran Liu
 * @date 2024/6/30
 */
public class FakeZl64TripleGenSender extends AbstractZl64TripleGenParty {
    /**
     * PRNG
     */
    private SecureRandom prng;
    /**
     * seed
     */
    private long seed;

    public FakeZl64TripleGenSender(Rpc senderRpc, Party receiverParty, FakeZl64TripleGenConfig config) {
        super(FakeZl64TripleGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        seed = 0L;
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Zl64Triple generate(Zl64 zl64, int num) {
        setPtoInput(zl64, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        resetSeed();
        Zl64Triple senderTriple = Zl64Triple.createRandom(zl64, num, prng);
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, time);

        logPhaseInfo(PtoState.PTO_END);
        return senderTriple;
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
