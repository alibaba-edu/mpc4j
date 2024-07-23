package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.AbstractZlTripleGenParty;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * fake Zl triple generation sender.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public class FakeZlTripleGenSender extends AbstractZlTripleGenParty {
    /**
     * PRNG
     */
    private SecureRandom prng;
    /**
     * seed
     */
    private long seed;

    public FakeZlTripleGenSender(Rpc senderRpc, Party receiverParty, FakeZlTripleGenConfig config) {
        super(FakeZlTripleGenPtoDesc.getInstance(), senderRpc, receiverParty, config);
        seed = 0L;
    }

    @Override
    public void init(int maxL, int expectTotalNum) throws MpcAbortException {
        setInitInput(maxL, expectTotalNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ZlTriple generate(Zl zl, int num) {
        setPtoInput(zl, num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        resetSeed();
        ZlTriple senderTriple = ZlTriple.createRandom(zl, num, prng);
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
