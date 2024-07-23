package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.gyw23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.AbstractSspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotReceiverOutput;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * GYW23-SSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/11
 */
public class Gyw23SspCotReceiver extends AbstractSspCotReceiver {
    /**
     * core COT
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * pre-compute COT
     */
    private final PreCotReceiver preCotReceiver;
    /**
     * SP-CDPPRF
     */
    private final SpCdpprfReceiver spCdpprfReceiver;
    /**
     * COT receiver output
     */
    private CotReceiverOutput cotReceiverOutput;

    public Gyw23SspCotReceiver(Rpc receiverRpc, Party senderParty, Gyw23SspCotConfig config) {
        super(Gyw23SspCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        preCotReceiver = PreCotFactory.createReceiver(receiverRpc, senderParty, config.getPreCotConfig());
        addSubPto(preCotReceiver);
        spCdpprfReceiver = SpCdpprfFactory.createReceiver(receiverRpc, senderParty, config.getSpCdpprfConfig());
        addSubPto(spCdpprfReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init();
        preCotReceiver.init();
        spCdpprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num) throws MpcAbortException {
        setPtoInput(alpha, num);
        return receive();
    }

    @Override
    public SspCotReceiverOutput receive(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(alpha, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private SspCotReceiverOutput receive() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // P_1 send (extend, 1) to F_COT, which returns (r_1, M[r_1] ∈ {0,1} × {0,1}^κ to P_1
        if (cotReceiverOutput == null) {
            boolean[] rs = BinaryUtils.randomBinary(cotNum, secureRandom);
            cotReceiverOutput = coreCotReceiver.receive(rs);
        } else {
            cotReceiverOutput.reduce(cotNum);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        SspCotReceiverOutput receiverOutput;
        if (num == 1) {
            assert alpha == 0 && cotNum == 1;
            stopWatch.start();
            boolean[] choices = new boolean[1];
            Arrays.fill(choices, true);
            cotReceiverOutput = preCotReceiver.receive(cotReceiverOutput, choices);
            receiverOutput = SspCotReceiverOutput.create(alpha, cotReceiverOutput.getRbArray());
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN);

            stopWatch.start();
            int h = LongUtils.ceilLog2(num);
            SpCdpprfReceiverOutput spCdpprfReceiverOutput = spCdpprfReceiver.puncture(alpha, 1 << h, cotReceiverOutput);
            // R sets w[i] = X_i^h for i ∈ [n] \ {α}
            byte[][] rbArray = spCdpprfReceiverOutput.getV1Array();
            // computes w[α] = ⊕_{j ∈ [0, 2^n), j ≠ α} X_n^j d
            rbArray[alpha] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            // j ∈ [0, 2^n), j ≠ α
            for (int j = 0; j < (1 << h); j++) {
                if (j != alpha) {
                    BytesUtils.xori(rbArray[alpha], rbArray[j]);
                }
            }
            // total number of elements is 2^h, reduce to num
            if (num < (1 << h)) {
                byte[][] reduceRbArray = new byte[num][];
                System.arraycopy(rbArray, 0, reduceRbArray, 0, num);
                rbArray = reduceRbArray;
            }
            receiverOutput = SspCotReceiverOutput.create(alpha, rbArray);
            cotReceiverOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);
        }

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
