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
 * RRK+20 Zl Min2 Sender.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlMin2Sender extends AbstractZlMin2Party {
    /**
     * zl circuit sender.
     */
    private final ZlcParty zlcSender;
    /**
     * zl mux sender.
     */
    private final ZlMuxParty zlMuxSender;
    /**
     * zl DReLU sender.
     */
    private final ZlDreluParty zlDreluSender;
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;

    public Rrk20ZlMin2Sender(Z2cParty z2cSender, Party receiverParty, Rrk20ZlMin2Config config) {
        super(Rrk20ZlMin2PtoDesc.getInstance(), z2cSender.getRpc(), receiverParty, config);
        zlcSender = ZlcFactory.createSender(z2cSender.getRpc(), receiverParty, config.getZlcConfig());
        addSubPto(zlcSender);
        zlMuxSender = ZlMuxFactory.createSender(z2cSender.getRpc(), receiverParty, config.getZlMuxConfig());
        addSubPto(zlMuxSender);
        zlDreluSender = ZlDreluFactory.createSender(z2cSender, receiverParty, config.getZlDreluConfig());
        addSubPto(zlDreluSender);
        this.z2cSender = z2cSender;
        addSubPto(z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        zlcSender.init(maxL, 1);
        zlMuxSender.init(maxNum);
        zlDreluSender.init(maxL, maxNum);
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
        SquareZlVector w = zlcSender.sub(xi, yi);
        SquareZ2Vector v = zlDreluSender.drelu(w);
        v = z2cSender.not(v);
        SquareZlVector t = zlMuxSender.mux(v, w);
        SquareZlVector z = zlcSender.add(yi, t);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, ptoTime);

        logPhaseInfo(PtoState.PTO_END);

        return z;
    }
}