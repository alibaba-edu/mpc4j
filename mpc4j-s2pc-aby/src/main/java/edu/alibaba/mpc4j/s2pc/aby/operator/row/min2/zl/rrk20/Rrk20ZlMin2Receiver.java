package edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.min2.zl.AbstractZlMin2Party;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;

import java.util.concurrent.TimeUnit;

/**
 * RRK+20 Zl MIn2 Receiver.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMin2Receiver extends AbstractZlMin2Party {
    /**
     * zl circuit receiver.
     */
    private final ZlcParty zlcReceiver;
    /**
     * zl mux receiver.
     */
    private final ZlMuxParty zlMuxReceiver;
    /**
     * zl DReLU receiver.
     */
    private final ZlDreluParty zlDreluReceiver;
    /**
     * z2 circuit receiver.
     */
    private final Z2cParty z2cReceiver;

    public Rrk20ZlMin2Receiver(Z2cParty z2cReceiver, Party senderParty, Rrk20ZlMin2Config config) {
        super(Rrk20ZlMin2PtoDesc.getInstance(), z2cReceiver.getRpc(), senderParty, config);
        zlcReceiver = ZlcFactory.createReceiver(z2cReceiver.getRpc(), senderParty, config.getZlcConfig());
        addSubPto(zlcReceiver);
        zlMuxReceiver = ZlMuxFactory.createReceiver(z2cReceiver.getRpc(), senderParty, config.getZlMuxConfig());
        addSubPto(zlMuxReceiver);
        zlDreluReceiver = ZlDreluFactory.createReceiver(z2cReceiver, senderParty, config.getZlDreluConfig());
        addSubPto(zlDreluReceiver);
        this.z2cReceiver = z2cReceiver;
        addSubPto(z2cReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlcReceiver.init(maxL, 1);
        zlMuxReceiver.init(maxNum);
        zlDreluReceiver.init(maxL, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector min2(SquareZlVector xi, SquareZlVector yi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZlVector w = zlcReceiver.sub(xi, yi);
        SquareZ2Vector v = zlDreluReceiver.drelu(w);
        v = z2cReceiver.not(v);
        SquareZlVector t = zlMuxReceiver.mux(v, w);
        SquareZlVector z = zlcReceiver.add(yi, t);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return z;
    }
}
