package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp.BpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.AbstractBspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GYW23-BSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23BspCotReceiver extends AbstractBspCotReceiver {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * pre-compute COT
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * BP-CDPPRF
     */
    private final BpCdpprfReceiver bpCdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Gyw23BspCotReceiver(Rpc receiverRpc, Party senderParty, Gyw23BspCotConfig config) {
        super(Gyw23BspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
        bpCdpprfReceiver = BpCdpprfFactory.createReceiver(receiverRpc, senderParty, config.getBpCdpprfConfig());
        addSubPto(bpCdpprfReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        preCotReceiver.init();
        bpCdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int eachNum) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum);
        return receive();
    }

    @Override
    public BspCotReceiverOutput receive(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(alphaArray, eachNum, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private BspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P_1 send (extend, 1) to F_COT, which returns (r_1, M[r_1] ∈ {0,1} × {0,1}^κ to P_1
        if (cotReceiverOutput == null) {
            boolean[] rs = new boolean[cotNum];
            IntStream.range(0, cotNum).forEach(index -> rs[index] = secureRandom.nextBoolean());
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        SspCotReceiverOutput[] receiverOutputs;
        if (eachNum == 1) {
            assert cotNum == batchNum;
            stopWatch.start();
            boolean[] choices = new boolean[batchNum];
            Arrays.fill(choices, true);
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, choices);
            receiverOutputs = IntStream.range(0, batchNum)
                .mapToObj(batchIndex -> {
                    assert alphaArray[batchIndex] == 0;
                    return SspCotReceiverOutput.create(
                        alphaArray[batchIndex], new byte[][]{cotReceiverOutput.getRb(batchIndex)}
                    );
                })
                .toArray(SspCotReceiverOutput[]::new);
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            stopWatch.start();
            int h = LongUtils.ceilLog2(eachNum, 1);
            BpCdpprfReceiverOutput bpCdpprfReceiverOutput = bpCdpprfReceiver.puncture(alphaArray, 1 << h);
            IntStream batchIndexIntStream = IntStream.range(0, batchNum);
            batchIndexIntStream = parallel ? batchIndexIntStream.parallel() : batchIndexIntStream;
            receiverOutputs = batchIndexIntStream
                .mapToObj(batchIndex -> {
                    int alpha = alphaArray[batchIndex];
                    // R sets w[i] = X_i^h for i ∈ [n] \ {α}
                    byte[][] rbArray = bpCdpprfReceiverOutput.get(batchIndex).getV1Array();
                    // computes w[α] = ⊕_{j ∈ [0, 2^n), j ≠ α} X_n^j d
                    rbArray[alpha] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                    // j ∈ [0, 2^n), j ≠ α
                    for (int j = 0; j < (1 << h); j++) {
                        if (j != alpha) {
                            BytesUtils.xori(rbArray[alpha], rbArray[j]);
                        }
                    }
                    // total number of elements is 2^h, reduce to num
                    if (eachNum < (1 << h)) {
                        byte[][] reduceRbArray = new byte[eachNum][];
                        System.arraycopy(rbArray, 0, reduceRbArray, 0, eachNum);
                        rbArray = reduceRbArray;
                    }
                    return SspCotReceiverOutput.create(alpha, rbArray);
                })
                .toArray(SspCotReceiverOutput[]::new);
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        }
        logPhaseInfo(PtoState.PTO_END);
        return new BspCotReceiverOutput(receiverOutputs);
    }
}
