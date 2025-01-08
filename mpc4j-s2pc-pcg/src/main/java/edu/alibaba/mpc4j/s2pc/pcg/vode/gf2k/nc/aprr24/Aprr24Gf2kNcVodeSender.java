package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.Gf2kCoreVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.AbstractGf2kNcVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodePtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeSender;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeSenderOutput;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * APRR24 GF2K-NC-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public class Aprr24Gf2kNcVodeSender extends AbstractGf2kNcVodeSender {
    /**
     * GF2K-MSP-VODE config
     */
    private final Gf2kMspVodeConfig gf2kMspVodeConfig;
    /**
     * GF2K-MSP-VODE sender
     */
    private final Gf2kMspVodeSender gf2kMspVodeSender;
    /**
     * GF2K-core-VODE sender
     */
    private final Gf2kCoreVodeSender gf2kCoreVodeSender;
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
     * GF2K-VODE sender output used in iteration
     */
    private Gf2kVodeSenderOutput uwGf2kVodeSenderOutput;
    /**
     * GF2K-VODE num used in GF2K-MSP-VODE
     */
    private int mGf2kVodePreNum;
    /**
     * GF2K-VODE sender output used in GF2K-MSP-VODE
     */
    private Gf2kVodeSenderOutput mGf2kVodeSenderOutput;
    /**
     * matrix A
     */
    private LocalLinearCoder matrixA;

    public Aprr24Gf2kNcVodeSender(Rpc senderRpc, Party receiverParty, Aprr24Gf2kNcVodeConfig config) {
        super(Aprr24Gf2kNcVodePtoDesc.getInstance(), senderRpc, receiverParty, config);
        gf2kCoreVodeSender = Gf2kCoreVodeFactory.createSender(senderRpc, receiverParty, config.getCoreVodeConfig());
        addSubPto(gf2kCoreVodeSender);
        gf2kMspVodeConfig = config.getMspVodeConfig();
        gf2kMspVodeSender = Gf2kMspVodeFactory.createSender(senderRpc, receiverParty, config.getMspVodeConfig());
        addSubPto(gf2kMspVodeSender);
    }

    @Override
    public void init(int subfieldL, int num) throws MpcAbortException {
        setInitInput(subfieldL, num);
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
        gf2kCoreVodeSender.init(subfieldL);
        gf2kMspVodeSender.init(subfieldL);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 GF2K-VODE used in setup, randomly generate xs
        byte[][] xs = IntStream.range(0, initK)
            .mapToObj(index -> subfield.createRandom(secureRandom))
            .toArray(byte[][]::new);
        Gf2kVodeSenderOutput uwInitGf2kVodeSenderOutput = gf2kCoreVodeSender.send(xs);
        stopWatch.stop();
        long k0InitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, k0InitTime);

        stopWatch.start();
        // get seed for matrix A
        List<byte[]> matrixKeysPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_KEYS.ordinal());
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
        // execute GF2K-MSP-VODE
        Gf2kMspVodeSenderOutput ecInitGf2kMspVodeSenderOutput = gf2kMspVodeSender.send(initT, initN);
        stopWatch.stop();
        long ecInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, ecInitTime);

        stopWatch.start();
        // x = u * A + e
        byte[][] initX = matrixInitA.encode(uwInitGf2kVodeSenderOutput.getX());
        for (int eIndex : ecInitGf2kMspVodeSenderOutput.getAlphaArray()) {
            subfield.addi(initX[eIndex], ecInitGf2kMspVodeSenderOutput.getX(eIndex));
        }
        // z = w * A + r
        byte[][] initZ = matrixInitA.encodeBlock(uwInitGf2kVodeSenderOutput.getT());
        IntStream.range(0, initN).forEach(index ->
            field.addi(initZ[index], ecInitGf2kMspVodeSenderOutput.getT(index))
        );
        mGf2kVodeSenderOutput = Gf2kVodeSenderOutput.create(field, initX, initZ);
        uwGf2kVodeSenderOutput = mGf2kVodeSenderOutput.split(iterationK);
        mGf2kVodePreNum = Gf2kMspVodeFactory.getPrecomputeNum(gf2kMspVodeConfig, subfieldL, iterationT, iterationN);
        mGf2kVodeSenderOutput.reduce(mGf2kVodePreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        init(CommonConstants.BLOCK_BIT_LENGTH, num);
    }

    @Override
    public Gf2kVodeSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // execute GF2K-MSP-VODE
        Gf2kMspVodeSenderOutput ecGf2kMspVodeSenderOutput = gf2kMspVodeSender.send(
            iterationT, iterationN, mGf2kVodeSenderOutput
        );
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, sTime);

        stopWatch.start();
        // x = u * A + e
        byte[][] x = matrixA.encode(uwGf2kVodeSenderOutput.getX());
        for (int eIndex : ecGf2kMspVodeSenderOutput.getAlphaArray()) {
            subfield.addi(x[eIndex], ecGf2kMspVodeSenderOutput.getX(eIndex));
        }
        // z = w * A + r
        byte[][] z = matrixA.encodeBlock(uwGf2kVodeSenderOutput.getT());
        IntStream.range(0, iterationN).forEach(index ->
            field.addi(z[index], ecGf2kMspVodeSenderOutput.getT(index))
        );
        // split GF2K-VODE output into k0 + MSP-COT + output
        Gf2kVodeSenderOutput senderOutput = Gf2kVodeSenderOutput.create(field, x, z);
        uwGf2kVodeSenderOutput = senderOutput.split(iterationK);
        mGf2kVodeSenderOutput = senderOutput.split(mGf2kVodePreNum);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
