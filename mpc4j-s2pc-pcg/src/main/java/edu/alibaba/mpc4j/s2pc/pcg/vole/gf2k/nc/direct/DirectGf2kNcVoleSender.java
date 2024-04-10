package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.AbstractGf2kNcVoleSender;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * direct GF2K-NC-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class DirectGf2kNcVoleSender extends AbstractGf2kNcVoleSender {
    /**
     * core VOLE sender
     */
    private final Gf2kCoreVoleSender coreVoleSender;

    public DirectGf2kNcVoleSender(Rpc senderRpc, Party receiverParty, DirectGf2kNcVoleConfig config) {
        super(DirectGf2kNcVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getCoreVoleConfig());
        addSubPto(coreVoleSender);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreVoleSender.init(num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVoleSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // randomly generate xs
        byte[][] xs = IntStream.range(0, num)
            .mapToObj(index -> gf2k.createRandom(secureRandom))
            .toArray(byte[][]::new);
        Gf2kVoleSenderOutput senderOutput = coreVoleSender.send(xs);
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
