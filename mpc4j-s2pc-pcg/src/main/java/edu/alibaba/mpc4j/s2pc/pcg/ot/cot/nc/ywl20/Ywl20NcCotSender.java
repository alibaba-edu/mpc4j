package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotSenderOutput;

/**
 * YWL20-NC-COT sender.
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
public class Ywl20NcCotSender extends AbstractNcCotSender {
    /**
     * MSP-COT config
     */
    private final MspCotConfig mspCotConfig;
    /**
     * MSP-COT sender
     */
    private final MspCotSender mspCotSender;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * iteration LPN parameter k
     */
    private int iterationK;
    /**
     * iteration LPN parameter n
     */
    private int iterationN;
    /**
     * iteration LPN parameter t
     */
    private int iterationT;
    /**
     * COT sender output used in iteration
     */
    private CotSenderOutput vCotSenderOutput;
    /**
     * COT num used in MSP-COT
     */
    private int sCotPreNum;
    /**
     * COT sender output used in MSP-COT
     */
    private CotSenderOutput sCotSenderOutput;
    /**
     * matrix A
     */
    private LocalLinearCoder matrixA;

    public Ywl20NcCotSender(Rpc senderRpc, Party receiverParty, Ywl20NcCotConfig config) {
        super(Ywl20NcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        mspCotConfig = config.getMspCotConfig();
        mspCotSender = MspCotFactory.createSender(senderRpc, receiverParty, config.getMspCotConfig());
        addSubPto(mspCotSender);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        LpnParams setupLpnParams = Ywl20NcCotPtoDesc.getSetupLpnParams(mspCotConfig, num);
        int initK = setupLpnParams.getK();
        int initN = setupLpnParams.getN();
        int initT = setupLpnParams.getT();
        LpnParams iterationLpnParams = Ywl20NcCotPtoDesc.getIterationLpnParams(mspCotConfig, num);
        iterationK = iterationLpnParams.getK();
        iterationN = iterationLpnParams.getN();
        iterationT = iterationLpnParams.getT();
        // init core COT and MSP-COT
        coreCotSender.init(delta);
        mspCotSender.init(delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 COT used in setup
        CotSenderOutput vInitCotSenderOutput = coreCotSender.send(initK);
        stopWatch.stop();
        long kInitCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, kInitCotTime);

        stopWatch.start();
        // get seed for matrix A
        DataPacketHeader matrixKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX_KEYS.ordinal(),
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixKeysPayload = rpc.receive(matrixKeysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(matrixKeysPayload.size() == 2);
        byte[] initKey = matrixKeysPayload.get(0);
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, initKey);
        matrixInitA.setParallel(parallel);
        byte[] matrixKey = matrixKeysPayload.get(1);
        matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKey);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, keyInitTime);

        stopWatch.start();
        // execute MSP-COT
        MspCotSenderOutput sInitMspCotSenderOutput = mspCotSender.send(initT, initN);
        stopWatch.stop();
        long sInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, sInitTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] initY = matrixInitA.encodeBlock(vInitCotSenderOutput.getR0Array());
        IntStream.range(0, initN).forEach(index ->
            BytesUtils.xori(initY[index], sInitMspCotSenderOutput.getR0(index))
        );
        sCotSenderOutput = CotSenderOutput.create(delta, initY);
        vCotSenderOutput = sCotSenderOutput.split(iterationK);
        sCotPreNum = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
        sCotSenderOutput.reduce(sCotPreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute MSP-COT
        MspCotSenderOutput sMspCotSenderOutput = mspCotSender.send(iterationT, iterationN, sCotSenderOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime, "Sender runs MSP-COT");

        stopWatch.start();
        // y = v * A + s
        byte[][] y = matrixA.encodeBlock(vCotSenderOutput.getR0Array());
        IntStream.range(0, iterationN).forEach(index ->
            BytesUtils.xori(y[index], sMspCotSenderOutput.getR0(index))
        );
        // split COT output into k0 + MSP-COT + output
        CotSenderOutput senderOutput = CotSenderOutput.create(delta, y);
        vCotSenderOutput = senderOutput.split(iterationK);
        sCotSenderOutput = senderOutput.split(sCotPreNum);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime, "Sender extends outputs");

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
