package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.AbstractBspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GYW23-BSP-COT sender.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23BspCotSender extends AbstractBspCotSender {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT
     */
    private final PreCotSender preCotSender;
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfSender bpCdpprfSender;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;

    public Gyw23BspCotSender(Rpc senderRpc, Party receiverParty, Gyw23BspCotConfig config) {
        super(Gyw23BspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
        bpCdpprfSender = BpCdpprfFactory.createSender(senderRpc, receiverParty, config.getBpCdpprfConfig());
        addSubPto(bpCdpprfSender);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        preCotSender.init();
        bpCdpprfSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum) throws MpcAbortException {
        setPtoInput(batchNum, eachNum);
        return send();
    }

    @Override
    public BspCotSenderOutput send(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(batchNum, eachNum, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private BspCotSenderOutput send() throws MpcAbortException {
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

        SspCotSenderOutput[] senderOutputs;
        if (eachNum == 1) {
            assert cotNum == batchNum;
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            senderOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex ->
                    SspCotSenderOutput.create(delta, new byte[][]{cotSenderOutput.getR0(batchIndex)}))
                .toArray(SspCotSenderOutput[]::new);
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            int h = LongUtils.ceilLog2(eachNum, 1);
            BpCdpprfSenderOutput bpCdpprfSenderOutput = bpCdpprfSender.puncture(batchNum, 1 << h);
            senderOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    byte[][] r0Array = bpCdpprfSenderOutput.get(batchIndex).getV0Array();
                    if (eachNum < (1 << h)) {
                        byte[][] reduceR0Array = BlockUtils.zeroBlocks(eachNum);
                        System.arraycopy(r0Array, 0, reduceR0Array, 0, eachNum);
                        r0Array = reduceR0Array;
                    }
                    return SspCotSenderOutput.create(delta, r0Array);
                })
                .toArray(SspCotSenderOutput[]::new);
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        }

        logPhaseInfo(PtoState.PTO_END);
        return new BspCotSenderOutput(senderOutputs);
    }
}
