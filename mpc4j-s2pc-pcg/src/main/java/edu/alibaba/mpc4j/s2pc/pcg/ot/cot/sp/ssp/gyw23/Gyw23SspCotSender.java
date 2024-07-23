package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

import java.util.concurrent.TimeUnit;

/**
 * GYW23-SSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23SspCotSender extends AbstractSspCotSender {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT
     */
    private final PreCotSender preCotSender;
    /**
     * SP-CDPPRF
     */
    private final SpCdpprfSender spCdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Gyw23SspCotSender(Rpc senderRpc, Party receiverParty, Gyw23SspCotConfig config) {
        super(Gyw23SspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
        spCdpprfSender = SpCdpprfFactory.createSender(senderRpc, receiverParty, config.getSpCdpprfConfig());
        addSubPto(spCdpprfSender);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        preCotSender.init();
        spCdpprfSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SspCotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        return send();
    }

    @Override
    public SspCotSenderOutput send(int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private SspCotSenderOutput send() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P_0 send (extend, h) to F_COT, which returns (K[r_1], ..., K[r_n]) ∈ {{0,1}^κ}^h to P_0
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
        } else {
            cotSenderOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        SspCotSenderOutput senderOutput;
        if (num == 1) {
            assert cotNum == 1;
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            senderOutput = SspCotSenderOutput.create(delta, cotSenderOutput.getR0Array());
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            int h = LongUtils.ceilLog2(num);
            SpCdpprfSenderOutput spCdpprfSenderOutput = spCdpprfSender.puncture(1 << h, cotSenderOutput);
            byte[][] r0Array = spCdpprfSenderOutput.getV0Array();
            if (num < (1 << h)) {
                byte[][] reduceR0Array = new byte[num][];
                System.arraycopy(r0Array, 0, reduceR0Array, 0, num);
                r0Array = reduceR0Array;
            }
            senderOutput = SspCotSenderOutput.create(delta, r0Array);
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        }

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
