package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * YWL20-NC-COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/02
 */
public class Ywl20NcCotReceiver extends AbstractNcCotReceiver {
    /**
     * MSP-COT config
     */
    private final MspCotConfig mspCotConfig;
    /**
     * MSP-COT receiver
     */
    private final MspCotReceiver mspCotReceiver;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
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
     * COT receiver output used in iteration
     */
    private CotReceiverOutput wCotReceiverOutput;
    /**
     * COT num used in MSP-COT
     */
    private int rCotPreNum;
    /**
     * COT receiver output used in MSP-COT
     */
    private CotReceiverOutput rCotReceiverOutput;

    public Ywl20NcCotReceiver(Rpc receiverRpc, Party senderParty, Ywl20NcCotConfig config) {
        super(Ywl20NcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        mspCotConfig = config.getMspCotConfig();
        mspCotReceiver = MspCotFactory.createReceiver(receiverRpc, senderParty, config.getMspCotConfig());
        addSubPto(mspCotReceiver);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
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
        coreCotReceiver.init(initK);
        mspCotReceiver.init(iterationT, iterationN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 COT used in setup
        boolean[] choices = new boolean[initK];
        IntStream.range(0, initK).forEach(index -> choices[index] = secureRandom.nextBoolean());
        CotReceiverOutput wInitCotReceiverOutput = coreCotReceiver.receive(choices);
        stopWatch.stop();
        long kInitCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, kInitCotTime);

        stopWatch.start();
        // get seed for matrix A used in setup
        byte[] matrixInitKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(matrixInitKey);
        List<byte[]> matrixInitKeyPayload = Collections.singletonList(matrixInitKey);
        DataPacketHeader matrixInitKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SETUP_KEY.ordinal(),
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixInitKeyHeader, matrixInitKeyPayload));
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, matrixInitKey);
        matrixInitA.setParallel(parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, keyInitTime);

        stopWatch.start();
        // execute MSP-COT
        MspCotReceiverOutput rInitMspCotReceiverOutput = mspCotReceiver.receive(initT, initN);
        stopWatch.stop();
        long rInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, rInitTime);

        stopWatch.start();
        // x = u * A + e
        boolean[] initX = matrixInitA.encode(wInitCotReceiverOutput.getChoices());
        for (int eIndex : rInitMspCotReceiverOutput.getAlphaArray()) {
            initX[eIndex] = !initX[eIndex];
        }
        // z = w * A + r
        byte[][] initZ = matrixInitA.encode(wInitCotReceiverOutput.getRbArray());
        IntStream.range(0, initN).forEach(index ->
            BytesUtils.xori(initZ[index], rInitMspCotReceiverOutput.getRb(index))
        );
        rCotReceiverOutput = CotReceiverOutput.create(initX, initZ);
        wCotReceiverOutput = rCotReceiverOutput.split(iterationK);
        rCotPreNum = MspCotFactory.getPrecomputeNum(mspCotConfig, iterationT, iterationN);
        rCotReceiverOutput.reduce(rCotPreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // get seed for matrix A used in iteration
        byte[] matrixKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(matrixKey);
        List<byte[]> matrixKeyPayload = Collections.singletonList(matrixKey);
        DataPacketHeader matrixKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_ITERATION_LEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(matrixKeyHeader, matrixKeyPayload));
        LocalLinearCoder matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKey);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, keyTime);

        stopWatch.start();
        // execute MSP-COT
        MspCotReceiverOutput rMspCotReceiverOutput = mspCotReceiver.receive(iterationT, iterationN, rCotReceiverOutput);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, rTime);

        stopWatch.start();
        // x = u * A + e
        boolean[] x = matrixA.encode(wCotReceiverOutput.getChoices());
        for (int eIndex : rMspCotReceiverOutput.getAlphaArray()) {
            x[eIndex] = !x[eIndex];
        }
        // z = w * A + r
        byte[][] z = matrixA.encode(wCotReceiverOutput.getRbArray());
        IntStream.range(0, iterationN).forEach(index ->
            BytesUtils.xori(z[index], rMspCotReceiverOutput.getRb(index))
        );
        // split COT output into k0 + MSP-COT + output
        CotReceiverOutput receiverOutput = CotReceiverOutput.create(x, z);
        wCotReceiverOutput = receiverOutput.split(iterationK);
        rCotReceiverOutput = receiverOutput.split(rCotPreNum);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
