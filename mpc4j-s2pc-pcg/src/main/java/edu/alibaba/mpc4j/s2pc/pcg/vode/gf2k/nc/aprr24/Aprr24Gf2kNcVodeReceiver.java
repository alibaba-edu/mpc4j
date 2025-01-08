package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.AbstractGf2kNcVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * APRR24 GF2K-NC-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public class Aprr24Gf2kNcVodeReceiver extends AbstractGf2kNcVodeReceiver {
    /**
     * GF2K-MSP-VODE config
     */
    private final Gf2kMspVodeConfig gf2kMspVodeConfig;
    /**
     * GF2K-MSP-VODE receiver
     */
    private final Gf2kMspVodeReceiver gf2kMspVodeReceiver;
    /**
     * GF2K-core-VODE receiver
     */
    private final Gf2kCoreVodeReceiver gf2kCoreVodeReceiver;
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
     * GF2K-VODE receiver output used in iteration
     */
    private Gf2kVodeReceiverOutput vGf2kVodeReceiverOutput;
    /**
     * GF2K-VODE num used in GF2K-MSP-VODE
     */
    private int mGf2kVodePreNum;
    /**
     * GF2K-VODE receiver output used in GF2K-MSP-VODE
     */
    private Gf2kVodeReceiverOutput mGf2kVodeReceiverOutput;
    /**
     * matrix A
     */
    private LocalLinearCoder matrixA;

    public Aprr24Gf2kNcVodeReceiver(Rpc receiverRpc, Party senderParty, Aprr24Gf2kNcVodeConfig config) {
        super(Aprr24Gf2kNcVodePtoDesc.getInstance(), receiverRpc, senderParty, config);
        gf2kCoreVodeReceiver = Gf2kCoreVodeFactory.createReceiver(receiverRpc, senderParty, config.getCoreVodeConfig());
        addSubPto(gf2kCoreVodeReceiver);
        gf2kMspVodeConfig = config.getMspVodeConfig();
        gf2kMspVodeReceiver = Gf2kMspVodeFactory.createReceiver(receiverRpc, senderParty, config.getMspVodeConfig());
        addSubPto(gf2kMspVodeReceiver);
    }

    @Override
    public void init(int subfieldL, byte[] delta, int num) throws MpcAbortException {
        setInitInput(subfieldL, delta, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        LpnParams setupLpnParams = Aprr24Gf2kNcVodePtoDesc.getSetupLpnParams(gf2kMspVodeConfig, num);
        int initK = setupLpnParams.getK();
        int initN = setupLpnParams.getN();
        int initT = setupLpnParams.getT();
        LpnParams iterationLpnParams = Aprr24Gf2kNcVodePtoDesc.getIterationLpnParams(gf2kMspVodeConfig, num);
        iterationK = iterationLpnParams.getK();
        iterationN = iterationLpnParams.getN();
        iterationT = iterationLpnParams.getT();
        // init GF2K-core-VODE and GF2K-MSP-VODE
        gf2kCoreVodeReceiver.init(subfieldL, delta);
        gf2kMspVodeReceiver.init(subfieldL, delta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 GF2K-VODE used in setup
        Gf2kVodeReceiverOutput vInitGf2kVodeReceiverOutput = gf2kCoreVodeReceiver.receive(initK);
        stopWatch.stop();
        long k0InitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, k0InitTime);

        stopWatch.start();
        // get seed for matrix A used in setup
        byte[][] matrixKeys = BytesUtils.randomByteArrayVector(2, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        List<byte[]> matrixKeysPayload = Arrays.stream(matrixKeys).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_KEYS.ordinal(), matrixKeysPayload);
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, matrixKeys[0]);
        matrixInitA.setParallel(parallel);
        matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKeys[1]);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, keyInitTime);

        stopWatch.start();
        // execute GF2K-MSP-VODE
        Gf2kMspVodeReceiverOutput bInitGf2kMspVodeReceiverOutput = gf2kMspVodeReceiver.receive(initT, initN);
        stopWatch.stop();
        long bInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, bInitTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] initY = matrixInitA.encodeBlock(vInitGf2kVodeReceiverOutput.getQ());
        IntStream.range(0, initN).forEach(index ->
            field.addi(initY[index], bInitGf2kMspVodeReceiverOutput.getQ(index))
        );
        mGf2kVodeReceiverOutput = Gf2kVodeReceiverOutput.create(field, delta, initY);
        vGf2kVodeReceiverOutput = mGf2kVodeReceiverOutput.split(iterationK);
        mGf2kVodePreNum = Gf2kMspVodeFactory.getPrecomputeNum(gf2kMspVodeConfig, subfieldL, iterationT, iterationN);
        mGf2kVodeReceiverOutput.reduce(mGf2kVodePreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(byte[] delta, int num) throws MpcAbortException {
        init(CommonConstants.BLOCK_BIT_LENGTH, delta, num);
    }

    @Override
    public Gf2kVodeReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute GF2K-MSP-VODE
        Gf2kMspVodeReceiverOutput bGf2kMspVodeReceiverOutput = gf2kMspVodeReceiver.receive(
            iterationT, iterationN, mGf2kVodeReceiverOutput
        );
        stopWatch.stop();
        long bTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, bTime);

        stopWatch.start();
        // y = v * A + s
        byte[][] y = matrixA.encodeBlock(vGf2kVodeReceiverOutput.getQ());
        IntStream.range(0, iterationN).forEach(index ->
            field.addi(y[index], bGf2kMspVodeReceiverOutput.getQ(index))
        );
        // split GF2K-VODE output into k0 + MSP-COT + output
        Gf2kVodeReceiverOutput receiverOutput = Gf2kVodeReceiverOutput.create(field, delta, y);
        vGf2kVodeReceiverOutput = receiverOutput.split(iterationK);
        mGf2kVodeReceiverOutput = receiverOutput.split(mGf2kVodePreNum);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
