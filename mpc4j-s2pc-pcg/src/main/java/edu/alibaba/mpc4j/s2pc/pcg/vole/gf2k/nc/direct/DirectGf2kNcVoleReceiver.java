package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.AbstractGf2kNcVoleReceiver;

import java.util.concurrent.TimeUnit;

/**
 * direct GF2K-NC-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class DirectGf2kNcVoleReceiver extends AbstractGf2kNcVoleReceiver {
    /**
     * core VOLE receiver
     */
    private final Gf2kCoreVoleReceiver coreVoleReceiver;

    public DirectGf2kNcVoleReceiver(Rpc receiverRpc, Party senderParty, DirectGf2kNcVoleConfig config) {
        super(DirectGf2kNcVolePtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreVoleReceiver = Gf2kCoreVoleFactory.createReceiver(receiverRpc, senderParty, config.getCoreVoleConfig());
        addSubPto(coreVoleReceiver);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreVoleReceiver.init(delta, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVoleReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Gf2kVoleReceiverOutput receiverOutput = coreVoleReceiver.receive(num);
        stopWatch.stop();
        long coreCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreCotTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
