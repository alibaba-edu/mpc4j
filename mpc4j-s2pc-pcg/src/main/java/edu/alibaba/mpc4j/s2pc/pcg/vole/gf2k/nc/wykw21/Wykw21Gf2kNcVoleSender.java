package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.primal.LocalLinearCoder;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.AbstractGf2kNcVoleSender;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21.Wykw21Gf2kNcVolePtoDesc.PtoStep;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * WYKW21-GF2K-NC-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Wykw21Gf2kNcVoleSender extends AbstractGf2kNcVoleSender {
    /**
     * GF2K-MSP-VOLE config
     */
    private final Gf2kMspVoleConfig mspVoleConfig;
    /**
     * GF2K-MSP-VOLE sender
     */
    private final Gf2kMspVoleSender mspVoleSender;
    /**
     * core GF2K-VOLE sender
     */
    private final Gf2kCoreVoleSender coreVoleSender;
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
     * GF2K-VOLE sender output used in iteration
     */
    private Gf2kVoleSenderOutput uwVoleSenderOutput;
    /**
     * GF2K-VOLE num used in GF2K-MSP-VOLE
     */
    private int mVolePreNum;
    /**
     * GF2K-VOLE sender output used in GF2K-MSP-VOLE
     */
    private Gf2kVoleSenderOutput mVoleSenderOutput;

    public Wykw21Gf2kNcVoleSender(Rpc senderRpc, Party receiverParty, Wykw21Gf2kNcVoleConfig config) {
        super(Wykw21Gf2kNcVolePtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreVoleSender = Gf2kCoreVoleFactory.createSender(senderRpc, receiverParty, config.getCoreVoleConfig());
        addSubPto(coreVoleSender);
        mspVoleConfig = config.getMspVoleConfig();
        mspVoleSender = Gf2kMspVoleFactory.createSender(senderRpc, receiverParty, config.getMspVoleConfig());
        addSubPto(mspVoleSender);
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        LpnParams setupLpnParams = Wykw21Gf2kNcVolePtoDesc.getSetupLpnParams(mspVoleConfig, num);
        int initK = setupLpnParams.getK();
        int initN = setupLpnParams.getN();
        int initT = setupLpnParams.getT();
        LpnParams iterationLpnParams = Wykw21Gf2kNcVolePtoDesc.getIterationLpnParams(mspVoleConfig, num);
        iterationK = iterationLpnParams.getK();
        iterationN = iterationLpnParams.getN();
        iterationT = iterationLpnParams.getT();
        // init core GF2K-VOLE and GF2K-MSP-VOLE
        coreVoleSender.init(initK);
        mspVoleSender.init(iterationT, iterationN);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 5, initTime);

        stopWatch.start();
        // get k0 GF2K-VOLE used in setup, randomly generate xs
        byte[][] xs = IntStream.range(0, initK)
            .mapToObj(index -> gf2k.createRandom(secureRandom))
            .toArray(byte[][]::new);
        Gf2kVoleSenderOutput uwInitVoleSenderOutput = coreVoleSender.send(xs);
        stopWatch.stop();
        long k0InitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 5, k0InitTime);

        stopWatch.start();
        // get seed for matrix A used in setup
        DataPacketHeader matrixInitKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_SETUP_KEY.ordinal(),
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixInitKeyPayload = rpc.receive(matrixInitKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(matrixInitKeyPayload.size() == 1);
        byte[] initKey = matrixInitKeyPayload.get(0);
        LocalLinearCoder matrixInitA = new LocalLinearCoder(envType, initK, initN, initKey);
        matrixInitA.setParallel(parallel);
        stopWatch.stop();
        long keyInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 5, keyInitTime);

        stopWatch.start();
        // execute GF2K-MSP-VOLE
        Gf2kMspVoleSenderOutput ecInitMspVoleSenderOutput = mspVoleSender.send(initT, initN);
        stopWatch.stop();
        long ecInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 5, ecInitTime);

        stopWatch.start();
        // x = u * A + e
        byte[][] initX = matrixInitA.encode(uwInitVoleSenderOutput.getX());
        for (int eIndex : ecInitMspVoleSenderOutput.getAlphaArray()) {
            gf2k.addi(initX[eIndex], ecInitMspVoleSenderOutput.getX(eIndex));
        }
        // z = w * A + r
        byte[][] initZ = matrixInitA.encode(uwInitVoleSenderOutput.getT());
        IntStream.range(0, initN).forEach(index ->
            gf2k.addi(initZ[index], ecInitMspVoleSenderOutput.getT(index))
        );
        mVoleSenderOutput = Gf2kVoleSenderOutput.create(initX, initZ);
        uwVoleSenderOutput = mVoleSenderOutput.split(iterationK);
        mVolePreNum = Gf2kMspVoleFactory.getPrecomputeNum(mspVoleConfig, iterationT, iterationN);
        mVoleSenderOutput.reduce(mVolePreNum);
        stopWatch.stop();
        long extendInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 5, 5, extendInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Gf2kVoleSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // get seed for matrix A used in iteration
        DataPacketHeader matrixKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_ITERATION_LEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixKeyPayload = rpc.receive(matrixKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(matrixKeyPayload.size() == 1);
        byte[] matrixKey = matrixKeyPayload.get(0);
        LocalLinearCoder matrixA = new LocalLinearCoder(envType, iterationK, iterationN, matrixKey);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, keyTime);

        stopWatch.start();
        // execute GF2K-MSP-VOLE
        Gf2kMspVoleSenderOutput ecMspVoleSenderOutput = mspVoleSender.send(iterationT, iterationN, mVoleSenderOutput);
        stopWatch.stop();
        long sTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, sTime);

        stopWatch.start();
        // x = u * A + e
        byte[][] x = matrixA.encode(uwVoleSenderOutput.getX());
        for (int eIndex : ecMspVoleSenderOutput.getAlphaArray()) {
            gf2k.addi(x[eIndex], ecMspVoleSenderOutput.getX(eIndex));
        }
        // z = w * A + r
        byte[][] z = matrixA.encode(uwVoleSenderOutput.getT());
        IntStream.range(0, iterationN).forEach(index ->
            gf2k.addi(z[index], ecMspVoleSenderOutput.getT(index))
        );
        // split GF2K-VOLE output into k0 + MSP-COT + output
        Gf2kVoleSenderOutput senderOutput = Gf2kVoleSenderOutput.create(x, z);
        uwVoleSenderOutput = senderOutput.split(iterationK);
        mVoleSenderOutput = senderOutput.split(mVolePreNum);
        senderOutput.reduce(num);
        stopWatch.stop();
        long extendTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, extendTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
